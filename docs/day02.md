# Day 02: Cube Conundrum

* [Problem statement](https://adventofcode.com/2023/day/2)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day02.clj)

## Intro

Today's puzzle turned out to be much easier than I originally hoped, and frankly more closely resembled a day 1 puzzle
to me! After cleaning it up, I was able to drop its size in half, much of that being parsing. So what I'm including
below is the cleaned up, simplified version.

## Part One

The input today is a list of strings representing games of pulling colored cubes out of a bag and then putting them
back in for the next pull. These elves sure do know how to have a good time! Our task is to sum up the game numbers for
all games that could be played with 12 red, 13 green, and 14 blue cubes. The way to simplify this (and which holds up
in part 2), is to realize our parsed data doesn't need to remember every pull, but rather just the most of each cube
type per game, which represents the minimum number of cubes of that color in the bag.

So let's parse. Usually I do this with a few smaller functions, but I thought this one was easy enough as it was.
We'll use a trivial regular expression to handle most everything. The goal is to return a map of the game number, and
a `:cubes` sub-map of each color to its minimum number of cubes.

```clojure
(defn parse-game [line]
  (let [[_ game-num & boxes] (re-seq #"\w+" line)]
    {:game-num (parse-long game-num)
     :cubes    (reduce (fn [acc [n cube]] (update acc (keyword cube) max (parse-long n)))
                       {:red 0, :green 0, :blue 0}
                       (partition 2 boxes))}))
```

The `re-seq` function returns a sequence of every match group within the string using the pattern, in this case the
"word characters," so numbers and letters. The first two groups are  always the word `"Game"` and the game number,
so we discard the first and bind the second to `game-num`. We want to keep the rest as a sequence so `& boxes`
accomplishes this, sort of like a varargs. Then after parsing the game number into a long, it's time to prepare the
cubes map.

The cubes map will be a single reduction, initialized with a map of zero red, green, and blue cubes. For the input
sequence, we window the `boxes` using `(partition 2 boxes)`, so `("6" "green" "2 "blue" "1" "red" "5" "blue")` becomes
`(["6" "green"] ["2 "blue"] ["1" "red"] ["5" "blue"])`. Then the reducing function updates the accumulated map at the
cube color with the max of its current cube count and the latest draw. `(update acc (keyword cube) max (parse-long n))`
does all the work, noting that we use each cube's keyword instead of string representation because we're good
Clojurians.

Next, we write a predicate function to see if this game is possible with the 12 red, 13 green, and 14 blue cubes.

```clojure
(defn playable? [{:keys [cubes]}]
  (= cubes (merge-with min cubes {:red 12, :green 13, :blue 14})))
```

Originally I did this with an `or` statement and a bunch of `<=` comparisons, but I played around and found this cute
alternative. Given the game's cube map, we call `(merge-with min cubes {:red 12, :green 13, :blue 14})`, thus merging
the two maps together. For each key (cube color), the merged map uses the minimum value. If the game is playable, then
all of its cube counts will be <= the values the elf has offered. In that case, the merged map will be equal to the
original map, hence `(= cubes merged-map)`.

Finally, we complete the problem with the `part1` function, naturally using a transducer.

```clojure
(defn part1 [input]
  (transduce (comp (map parse-game) (filter playable?) (map :game-num)) + (str/split-lines input)))
```

The collection of values, as we saw on Day 1 and which I'm sure we'll continue to see often, is
`(str/split-lines input)`. The transformation function for each line of text does three things - parse the game, filter
to see if it's playable, and then mapped to its game number. I won't go into the whole explanation of why this is, but
when a transducer's transformation function composes multiple functions using `comp`, the functions you want to apply
must appear in left-to-right order, whereas "normal" `comp` functions read right-to-left. Finally, the reducing function
is simply the `+` function.

## Part Two

With how small part 1 was, part 2 is even smaller. The puzzle asks us to multiply the minimum number of cubes by color
within each game, and then add up those so-called "powers." Since we already parsed each game with its minimum number
of cubes, this is trivial.

```clojure
(defn cube-power [game]
  (->> game :cubes vals (apply *)))

(defn part2 [input]
  (transduce (map (comp cube-power parse-game)) + (str/split-lines input)))
```

`cube-power` takes a game, grabs the `:cubes` map, and keeps just the values using `vals` since we don't care which
cube color has which quantity. Then with those counts, we multiply them together using `(apply *)`. Solving part 2
involves another transducer, this time mapping the `parse-game` and `cube-power` functions in the transformation
function. Easy.

But hold on! A second ago I said that if you use `comp` in a transformation function, it reads left-to-right, but in
`part2` above, it read right-to-left. It wasn't a lie, I swear! The issue is that in part 1, the transformation
function itself was a composition, whereas here it was a map of a composed function. An alternate implementation below
showcases what it would look like if the function were a composition of two maps, instead of a map of two composed
functions. You'll see in this case, they now go left-to-right. Personally, I like the solution I put above more than
this one, but they are equivalent.

```clojure
; Composing two map functions isn't as pretty as mapping a composition of two functions.
; I like the above solution much more than this, but note this reads L-R while the above reads R-L.
(defn part2 [input]
  (transduce (comp (map parse-game) (map cube-power)) + (str/split-lines input)))
```

This wasn't a bad puzzle at all, and I'm happy with how clean the solution was. I'm ready for day 3!
