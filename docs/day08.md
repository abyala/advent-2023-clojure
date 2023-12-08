# Day 08: Haunted Wasteland

* [Problem statement](https://adventofcode.com/2023/day/8)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day08.clj)

## Intro

Standard rant - I hate the puzzles that you can only solve by knowing how to apply math skills some of us haven't used
in decades. To solve part 2, I went on Reddit to learn what the trick was, and then I coded it myself. I just find it
hard to care at all about these puzzles.

Rant over. Let's code!

## Part One

Our input is a list of left-and-right instructions, and a list of turns we can take from one node in a graph to another
by turning either left or right. Our goal is to find out how many turns it takes to get from the start node `AAA` to
the end node `ZZZ`.

Let's start by parsing the input. Our goal is to convert the string into a map of `{:instructions s, :paths {}}` where
`:instructions` is just the single string from the first line, and `:paths` is a map of the node and direction (like
`["AAA" \L]`) to the target node.

```clojure
(defn parse-input [input]
  (let [[instructions _ & mappings] (str/split-lines input)]
    {:instructions instructions
     :paths        (reduce (fn [acc line]
                             (let [[src dest1 dest2] (re-seq #"\w{3}" line)]
                               (assoc acc [src \L] dest1 [src \R] dest2)))
                           {}
                           mappings)}))
```

We'll begin by splitting the input by lines, binding the first one as `instructions`, discarding the blank line, and
binding the rest of them using `& mappings` to a sequence called `mappings`. To convert `mappings` into the target
map, we'll use a `reduce` function. For each mapping, split the string into the three 3-character strings that
correspond to the source and left-and-right destinations. Then we'll just associate them into a resulting map. I did
also implement this as `(into {} (mapcat ... mappings))` but thought this looks cleaner.

Next, let's create an infinite sequence of rooms we'll travel through, so we can deal with sequences instead of loops.

```clojure
(defn all-steps [instructions paths start-loc]
  (letfn [(next-step [loc turns]
            (let [[turn & turns'] turns
                  loc' (paths [loc turn])]
              (cons loc (lazy-seq (next-step loc' turns')))))]
    (next-step start-loc (cycle instructions))))
```

`all-steps` takes in the parsed `instructions` and `paths`, plus the location from which we want to start, and returns
a sequence of node names. It uses a nested function called `next-step`, which takes in the current location `loc` and
an infinite sequence of turns to take, and it returns a lazy sequence of the current location and a recursive call to
the next locations, starting from whatever `(paths [loc turn])` says is the next node. To initialize the sequence, the
string `instructions` is fed into `(cycle instructions)` to generate an infinite looping sequence of every character
in the string.

Given that sequence, let's make a function to count how many steps it takes to get to the end node.

```clojure
(defn steps-to-end [instructions paths]
  (index-of-first #(= % "ZZZ")
                  (all-steps instructions paths "AAA")))
```

`index-of-first` comes from my `abyala.advent-utils-clojure.core` namespace, and it does what you'd think - returns
the index of the first value in the collection that satisfies the predicate. The function calls `all-steps` and looks
for the first index where the node is `"ZZZ"`. With that, it's time to solve the puzzle.

```clojure
(defn part1 [input]
  (let [{:keys [instructions paths]} (parse-input input)]
    (steps-to-end instructions paths)))
```

Just parse the input and call `steps-to-end`. Seems easy enough.  On to part 2.

## Part Two

This puzzle complicates the situation by having us start from any node that ends with an `\A`, running each of these
paths simultaneously, and only stopping when all of them hit a node that ends with `\Z` at the same time. The problem
states that this "is going to take significantly more steps," which means you have to find a secret solution instead of
just solving the puzzle as stated. As I read on Reddit, it turns out we'll need to find when each path hits an ending 
node, and then compute the Least Common Multiple (LCM) of those path lengths to get to the answer. So we're not that
far off, especially in previous years I've leveraged the `clojure.math.numeric-tower` package to get an implementation
of LCM, and I'm happy to do so again.

First, we'll implement `multi-start`, which takes in the `paths` and looks for all source locations that end with an
`\A`. Note that we'll have two of each, since there will be path mappings for turning both left and right, so we'll
throw the results into a set.

```clojure
(defn multi-start [paths] (->> (map ffirst paths)
                               (filter #(str/ends-with? % "A"))
                               set))
```

If a path is of the form `[[source turn] dest]`, then `ffirst` will grab the `source` out of the nested vectors.
We use `(filter #(str/ends-with? % "A"))` to find the appropriate names, and `set` to finish it out.

And now we can implement `part2` given a small change to `steps-to-end`:

```clojure
(defn steps-to-end [end-loc? instructions paths start-loc]
  (index-of-first #(end-loc? %)
                  (all-steps instructions paths start-loc)))

(defn part1 [input]
  (let [{:keys [instructions paths]} (parse-input input)]
    (steps-to-end #(= % "ZZZ") instructions paths "AAA")))

(defn part2 [input]
  (let [{:keys [instructions paths]} (parse-input input)]
    (reduce math/lcm (map (fn [loc] (steps-to-end #(str/ends-with? % "Z") instructions paths loc))
                          (multi-start paths)))))
```

`steps-to-end` now takes in a predicate function argument `end-loc?`, which is expected to check whether a node is at
the end. The `part1` now passes in the function `#(= % "ZZZ")` instead of it already being inside `steps-to-end`. And
then `part2` should look very similar to `part1`. After parsing the input, we'll `map` each starting location obtained
from `multi-start` into the `steps-to-end`, using `#(str/ends-with? % "Z")` as the `end-loc?` function. Finally, when
the `map` returns a sequence of step counts, we `reduce` over them using `math/lcm` to get our answer.

One more thing - it's time to refactor out a common `solve` function.

```clojure
(defn solve [start-locs-fn end-loc? input]
  (let [{:keys [instructions paths]} (parse-input input)]
    (reduce math/lcm (map #(steps-to-end end-loc? instructions paths %)
                          (start-locs-fn paths)))))

(defn single-start [_] ["AAA"])
(defn multi-start [paths] (->> (map ffirst paths)
                               (filter #(str/ends-with? % "A"))
                               set))
(defn part1 [input]
  (solve single-start #(= % "ZZZ") input))

(defn part2 [input]
  (solve multi-start #(str/ends-with? % "Z") input))
```

The `solve` function takes in a function to derive the starting locations from the paths, the `end-loc?` function, and
the input. It always calls `(reduce math/lcm)` at the end, since the `lcm` of a single value is itself. Then `part1`
calls `solve` with a function that returns the single value `["AAA"]` and the `"ZZZ"` check, while `part2` calls it
with `multi-start` and the `(str/ends-with? % "Z")` check. Done!
