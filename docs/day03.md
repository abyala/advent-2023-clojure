# Day 03: Gear Ratios

* [Problem statement](https://adventofcode.com/2023/day/3)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day03.clj)

## Intro

For some reason, I struggled to find a pretty data structure that I liked for this puzzle, but I think this is
reasonably easy to read and the performance is good, so I'm going with it.

Also, this is the first year I've created a separate Advent Of Code utilities repo in GitHub, so in theory I can stop
copy-pasting namespaces from year to year. So I copied my `point` namespace over as-is, and made one other tiny
addition to the new `core` convenience namespace, which I'll show below. But at least this year I learned how to use
the `deps.edn` build file to import one Clojure project into another!

## Part One

The input is a map of multi-digit numbers, empty spaces (represented as periods), and other symbols. Our task is to
identify the numbers, keeping only the ones that have at least one symbol surrounding it (cardinally or diagonally),
and add those numbers together.

I looked at multiple ways to represent the data, and one option I considered but didn't implement was treating each
number as a box of height 1. I think that might result in a cleaner solution, but I'd probably need to make a new
namespace `box` that leverages `point`, and I just didn't feel like it. So instead, I chose to represent each number
as a map of `{:value v :points #{p1 p2 p3}}`, where the `value` is the actual numerical value, and `points` is the set
of all `[x y]` points covered by that number. To do this, I implemented the `parse-numbers` function.

Oh, I also decided to parse the input twice, once for numbers and once for symbols. Deal with it!

```clojure
(defn parse-numbers
  ([input] (->> (str/split-lines input)
                (map-indexed #(parse-numbers %2 %1))
                (apply concat)))
  ([line y] (let [m (re-matcher #"\d+" line)]
              (loop [acc ()]
                (if (.find m)
                  (recur (conj acc {:value  (parse-long (.group m))
                                    :points (set (map #(vector % y) (range (.start m) (.end m))))}))
                  acc)))))
```

Since I knew I would be parsing the input twice, I made `parse-numbers` multi-arity. The 1-argument instance takes in
the entire input data, and the 2-argument instance parses a single line. In the 1-argument function, we split the input
by line, call the 2-argument instance with the line string and the line number, and then concatenate the results so we
have a single sequence of numbers. To parse each line of text into a collection of numbers, I used regular expressions
again, this time leveraging Java's **stateful** `Matcher` class. Mutable state in Clojure is ugly and this code really
showcases why we don't like mutable state. Anyway, we create the matcher `m` and the run a `loop` to accumulate the
results as we mutate it. Whenever `Matcher.find()` returns `true`, we can call `.group` to get its value, `.start` to
get the starting index in the string, and `.end` to get the exclusive ending index. From this, we can form the `:value`
of the number by parsing the `.group` value, and we can make the `:points` set by creating vectors from each `x` value
in the start-to-end range, plus the `y` value passed in as a function argument. When we loop and `Matcher.find()`
returns false, we return the accumulator. There's a good chance I'm going to make my own lazy Clojure wrapper in a few
minutes since I don't like this Java-ish code.

Now that that's done, we can parse the symbols. The target is another collection of maps of `{:value \* :point [x y]}`.
This one is easier to handle.

```clojure
(defn space? [c] (= c \.))
(defn engine-symbol? [c] (not (or (digit? c) (space? c))))

(defn parse-symbols [input]
  (keep (fn [[p v]] (when (engine-symbol? v) {:value v :point p}))
        (p/parse-to-char-coords-map input)))
```

First I created two convenience functions for readability. `space?` checks of a character is a period, meaning we can
ignore it. And `engine-symbol?` returns whether or not the character can be considered a symbol; I didn't call this
function `symbol?` because that's part of the Clojure core namespace. And since I mentioned it above, there's actually
a third convenience function of `digit?`, which wraps Java's `Character/isDigit` function so it's easier to use. Anyway,
I use my `point` namespace's convenient `parse-to-char-coords-map` function that I've used in past years, which parses
a grid and returns a map of each `[x y]` coordinate to its character value. Then the `keep` function checks if the
value within the map is an engine symbol, and if so, returns our `{:value :point}` map. Very simple.

The last major function is `symbol-adjacencies`, which is intended to associate an additional `:adjacent-numbers` key
into the engine symbols to represent every number (the whole thing, not just the numeric value) that's adjacent to each
symbol.

```clojure
(defn symbol-adjacencies [symbols numbers]
  (map (fn [{:keys [point] :as m}]
         (let [surr (p/surrounding point)]
           (assoc m :adjacent-numbers (filter #(some (:points %) surr) numbers))))
       symbols))
```

This function just calls `map` on each element of the `symbols` collection, calling `assoc` to create the new key
binding. For the value of this `:adjacent-numbers` collection, we'll call `filter` on the parsed numbers. Here we use
one of my favorite Clojure tricks, where a set can act as a function - `(some (:points %) surr)` says that if there is
some value in the points surrounding the symbol is contained within the set of points within the number, then return a
truthy value. We don't have to use a `contains` or `in` function or anything like that.

Fun fact - when I first wrote this function, I called `(p/surrounding point)` within my `filter` function, and that
made the entire algorithm take 10x as long. Hooray for timely optimizations!

Ok, it's time to solve the puzzle.

```clojure
(defn part1 [input]
  (->> (symbol-adjacencies (parse-symbols input) (parse-numbers input))
       (mapcat :adjacent-numbers)
       (set)
       (map :value)
       (apply +)))
```

We'll call `symbol-adjacencies` on the raw symbols and numbers, and then `mapcat` (flat map) all `:adjacent-numbers`
to see which numbers are connected to any symbol. Since a number might touch two symbols, we convert them into a `set`
of numbers, and then extract their values and add them together. Not bad.

## Part Two

Now we need to look only at the symbols with an asterisk and which touch exactly two numbers, multiply the numbers
together, and add them up. We've got everything we need already, so we can jump straight to the `part2` function.

```clojure
(defn part2 [input]
  (->> (symbol-adjacencies (parse-symbols input) (parse-numbers input))
       (keep (fn [{:keys [value adjacent-numbers]}] (when (and (gear-symbol value)
                                                               (= (count adjacent-numbers) 2))
                                                      (transduce (map :value) * adjacent-numbers))))
       (apply +)))
```

First we again have to create our symbol adjacencies; originally, I filtered the symbols to only look at gear symbols'
adjacencies, but the performance didn't matter, so I went with the simpler view. Once we have the adjacencies, we call
`keep` to restrict the symbols that are both gears and have 2 adjacent numbers, and then we transduce their so-called
"gear ratios" by mapping each adjacent number to its `:value` and multiplying them together. Finally, we add the
results.

Note: I did create a unified `solve` function for both parts 1 and 2, but it was hideous so I trashed it.