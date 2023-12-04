# Day 04: Scratchcards

* [Problem statement](https://adventofcode.com/2023/day/4)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day04.clj)

## Intro

This has got to be one of the most straightforward puzzles I've seen in a while. I didn't see the need to do any sort
of cleanup or migrate things to be more "Clojurey" this time around after initially solving it. Maybe I'll think of
some clever things to do with over the next day.

## Part One

The input is a bunch of scratchcards, where each line has a card number (we can ignore), a list of winning numbers, and
a list of numbers on our card. We never actually care about the individual numbers, as in both parts 1 and 2 we only
need to know the number of our numbers that are winners. In part 1, each row gets awarded double the number of previous
points, starting with 1 for 1 matching number.

To start off, let's implement `line-wins`, which takes in a raw line of text and returns the number of wins. Again,
this was such a simple puzzle that I didn't see a reason to parse the data into some map of `{:winners #{}, :mine #{}}`,
so we'll parse and calculate at once.

```clojure
(defn line-wins [line]
  (letfn [(parse-numbers [s] (set (map parse-long (re-seq #"\d+" s))))]
    (let [[_ winners mine] (re-seq #"[^:\|]+" line)]
      (count (set/intersection (parse-numbers winners) (parse-numbers mine))))))
```

First, we'll make an inner function called `parse-numbers`, which takes in a string and returns a set of each number
that was extracted. It's quite simple - `(re-seq #"\d+" s)` is logic we've used in the past to return a sequence of
numeric strings. We map each one using `parse-long` and then convert that sequence into a set. Yes, I could have
represented that as a transducer, but even I have limits.

Then we'll use `re-seq` to grab the three substrings, separated by a colon and a pipe - the game number (ignore), the
winners, and my numbers. There's plenty of whitespace that `parse-numbers` can deal with without trimming. So the
`line-wins` function just creates the two sets of numbers, calculates the intersection, and counts them up.

Next, we'll create `line-points` to convert the number of wins within a line to its point value. This forces zero wins
to result in zero points, but any other win number will be `2^(n-1)` points.

```clojure
(defn line-points [line]
  (let [wins (line-wins line)]
    (if (zero? wins) 0 (long (Math/pow 2 (dec wins))))))
```

Finally, it's time to make our simple `part1` function, this time with our transducer.

```clojure
(defn part1 [input]
  (transduce (map line-points) + (str/split-lines input)))
```

The input collection is the list of split lines, each of which gets transformed by mapping them to `line-points`, which
get collected together using `+`.

## Part Two

For part 2, each scratchcard win earns a bonus instance of an upcoming card, and we need to count the total number of
scratchcards we had, including the original 1 per line. We can accomplish this with a single `reduce` function call.

```clojure
(defn part2 [input]
  (last (reduce (fn [[cards row acc] line]
                  (let [wins (line-wins line)
                        num-cards-here (inc (get cards row 0))
                        future-cards (reduce #(assoc %1 (+ %2 row) num-cards-here) {} (range 1 (inc wins)))]
                    [(merge-with + cards future-cards) (inc row) (+ acc num-cards-here)]))
                [{} 1 0]
                (str/split-lines input))))
```

The input to the `reduce` is the sequence of input lines. The accumulator is a 3-element vector, representing a map of
the future earned cards, the current row number, and the total number of cards seen. Then the reducing function does
a few simple calculations. First, it determines the number of wins for the current line, and the number of cards for the
current line, which is one more than any number of bonus cards awarded. `future-cards` is a map of each bonus
scratchcard earned to the number of instances of cards earned. The card number is `(range 1 (inc wins))` ahead of the
current `row`, and the number of bonus instances is `num-cards-here`. Then the reducer returns an updated accumulator,
where the existing bonus cards are incremented by the new bonus cards using `merge-with`, the row number increments,
and the total number of cards seen increases by `num-cards-here`.

When the `reduce` function is done and the 3-element accumulator is returned, the `part2` function calls `last` to
return just the total number of cards seen. And we're done!