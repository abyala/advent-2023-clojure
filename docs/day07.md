# Day 07: Camel Cards

* [Problem statement](https://adventofcode.com/2023/day/7)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day07.clj)

## Intro

Today's puzzle was cute. We got to play a few games of Camel Cards (aka Elven Poker) with varying rules. There was
nothing too inherently complicated, and I rather like the solution I have, even if I made a few extra smaller constructs
for clarity. 

## Part One

We start off with an input file of multiple lines of players, with the first word being their hand and the second their
bid. I want to parse each line into a map of `{:hand, :bid}`, where `:hand` is the 5-character string and `:bid` is the
numeric value of the player's bid. Originally I identified the hand type up-front, but I later decided to defer that
calculation.

The actual parsing, then, is rather straightforward.

```clojure
(defn parse-line [line]
  (let [[hand bid] (str/split line #" ")] {:hand hand, :bid (parse-long bid)}))

(defn parse-input [input]
  (map parse-line (str/split-lines input)))
```

`parse-line` destructures the two words on a line, split by the space, and forms the map of `{:hand :bid}`. And then
`parse-input` maps each line of the input file by using `parse-line`. We've seen this pattern many times already.

Then our goal will be to sort each hand from weakest to strongest, which means we'll need to want to provide comparable
values for the hand type and each card in order. Let's start from making the hand type sortable. The strategy will be 
to convert each hand into a vector of the frequency of each card type first; both five aces and five queens would
become `[5]` since the only card type exists 5 times, while two pairs (and one leftover card) would be represented as
`[2 2 1]`. Then we'll give each hand frequency group a numerical ranking, where "high card" (`[1 1 1 1 1]`) would be
ranked as 0, while "five of a kind" (`[5]`) would be ranked as 6.

```clojure
(defn- map-by-index [coll] (into {} (map-indexed #(vector %2 %1) coll)))
(def hand-freq-points (map-by-index [[1 1 1 1 1], [2 1 1 1], [2 2 1], [3 1 1], [3 2], [4 1], [5]]))

(defn hand-points [hand]
  (->> hand frequencies vals (sort-by -) hand-freq-points))
```

First off, we create a helper function `map-by-index`, which takes in a collection and returns a map of each collection
value to its position within the collection, so `["Advent" "Of" "Code"]` will become `{"Advent" 0, "Of" 1, "Code" 2}`.
This function maps each element to a vector of `[value index]`, and then feeds that resulting sequence of vectors into
the `(into {})` function to make the map. With that in hand, `hand-freq-points` calls `map-by-index` with all 7 of the
hand types, as represented by their sorted frequencies.

And how do we use those sorted frequencies? With `hand-points` of course! This function takes in a hand and calls
`frequencies`, which will create a map of each character to the number of times it appears within the hand. We don't
care about the characters themselves, so `vals` gets us a collection of the frequencies themselves, which we then sort
in reversed order using `(sort-by -)`. Finally, that collection of sorted frequencies gets called into the just-created
map `hand-freq-points` to get the relative point value of the hand type.

For two hands with the same hand type, we need to break the tie by calculating the relative point values of the cards
themselves, so let's build the `card-points` function.

```clojure
(def scoring-map (map-by-index "23456789TJQKA"))

(defn card-points [hand] (mapv scoring-map hand))
```

Just as we did before, we'll use `map-by-index` to map each card type to its relative value, from `2` to `A`. Then
`card-points` maps each card to its point value, returning a vector of the five card point values.

With that done, we can finally create our `sort-hands` function.

```clojure
(defn sort-hands [hands]
  (sort-by (fn [{hand :hand}] [(hand-points hand) (card-points hand)])
           hands))
```

This function has a few interesting syntactic elements to it. First, `sort-by` takes in a `key-fn`, which must return
a comparable to use against the other values in the collection. The easiest way to compare multiple properties in order
it to create a `key-fn` that returns a vector of numbers, as Clojure will compare each element in order. So we want to
return a vector of the hand points and the card points. To destructure the `:hand` out of the parsed hand, we have
two options. Usually, I would type `(fn [{:keys [hand]}] ...)` since that works with multiple bindings, but to show the
other approach, I used `(fn [{hand :hand}] ...)` instead. I don't actually like this more, but thought I'd use it on
principle.

Note that we could also use `juxt` to implement this same function. It looks great and is arguably easier to read
than the spelled-out function above, but the juxt version get uglier once we refactor it for part 2.

```clojure
; Alternate implementation of sort-hands
(defn sort-hands [hands]
  (sort-by (comp (juxt hand-points card-points) :hand) hands))
```

We're almost done with part 1. We've parsed the inputs and sorted them, so now it's time to calculate each (ordered)
hand's winnings, and then add them up. I'll make a simple `winnings` function, and then use it in `part1`.

```clojure
(defn winnings [rank hand] (* (inc rank) (:bid hand)))

(defn part1 [input]
  (transduce (map-indexed winnings) + (sort-hands (parse-input input))))
```

`winnings` takes the 0-indexed rank and the hand, and multiplies the 1-indexed rank by the bid for that hand. And then
`part1` parses and sorts the hands, and transduces each of them using `winnings`, adding the results together.

## Part Two

Part 2 requires making two changes to our algorithm. First, we need to let jokers be used to make the hand as strong as
possible. Second, we need to make the joker cards themselves the weakest in the deck.

To handle the use of jokers, we'll use the `apply-jokers` function, which takes in a hand and returns a new hand where
the jokers are replaced by the best cards they can be.

```clojure
(def joker \J)

(defn apply-jokers [hand]
  (if-some [most-common-card (as-> (frequencies hand) x
                                   (dissoc x joker)
                                   (sort-by (comp - second) x)
                                   (ffirst x))]
    (str/replace hand joker most-common-card)
    hand))
```

First, we'll make a constant `joker` so we don't have to keep using its character defintion of `\J`. That's just for
aesthetics, and because parsing should only be done once anyway. Then we'll look for the most common non-joker card in
hand, because the best hand is always the one with the most of a single card; without straights or flushes, the hand
priorities are much simpler. We'll look at the frequencies of card within the hand, removing all jokers from
consideration, leaving us with a map of `{:card n}`. We can sort these map entries by their frequencies in descending
order by calling `(sort-by (comp - second) x)`, which will extract the second value of each entry (its frequency) and
then return its negative value for reduced sorting. This will make the first element in the returning sequence be the
character and frequency of the most common card. `(ffirst x)` is the same as `(first (first x))`, so we'll grab the
first tuple and then the first element (the card type) from that sequence. Once we know what each joker will become,
we just do a string replace.

Note that there is one edge case of the hand `JJJJJ`, since there is no most common non-joker element. Thus, if the
`most-common-card` is `nil`, just return the original `hand`.

The second change we need for part 2 is to devalue the joker relative to the other cards. To do this, we'll replace
`scoring-map` with `scoring-map-1` and `scoring-map-2`.

```clojure
(def scoring-map-1 (map-by-index "23456789TJQKA"))
(def scoring-map-2 (map-by-index "J23456789TQKA"))
```

Now all that's left is to make update `card-points` take in the `scoring-map` it's supposed to use, and for 
`sort-hands` to take in both the `scoring-map` (to pass through) and the `joker-fn`.

```clojure
(defn card-points [scoring-map hand]
  (mapv scoring-map hand))

(defn sort-hands [joker-fn scoring-map hands]
  (sort-by (fn [{hand :hand}] [(-> hand joker-fn hand-points)
                               (card-points scoring-map hand)])
           hands))

; The juxt version; I definitely think this is less clean.
(defn sort-hands [joker-fn scoring-map hands]
  (sort-by (comp (juxt (comp hand-points joker-fn) (partial card-points scoring-map)) :hand) hands))
```

When calculating the hand points, we'll want to first call the `joker-fn`. How will this all come together? By making
a new `solve` function that both `part1` and `part2` will call.

```clojure
(defn solve [joker-fn scoring-map input]
  (transduce (map-indexed winnings) + (sort-hands joker-fn scoring-map (parse-input input))))

(defn part1 [input] (solve identity scoring-map-1 input))
(defn part2 [input] (solve apply-jokers scoring-map-2 input))
```

`solve` looks very similar to the original `part1` function, transducing over the sorted input and transforming each
line with the `winnings` function. But `solve` takes in those same two functions, `joker-fn` and `scoring-map`, to
send to the `sort-hands` function.

Part 1 doesn't need to modify the cards to account for jokers, so it'll use `identity` as the `joker-fn`, and it uses
the original `scoring-map-1` scoring map. Part 2 uses `apply-jokers` as its `joker-fn`, and of course it uses
`scoring-map-2` to score cards.

So yeah, I could have saved several lines by inlining several components, like each of the scoring maps or the `joker`
definition or even the `winnings` function, but I think each function now really does exactly one thing, which is kind
of the goal!
