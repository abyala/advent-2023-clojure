# Day 01: Trebuchet?!

* [Problem statement](https://adventofcode.com/2023/day/1)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day01.clj)
* [Alternate solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day01_indexes.clj)

## Intro

It's day 1 of Advent Of Code! And... part 2 took me a full hour to complete because I had a silly bug. Yep, a full
hour. We're in for an adventure this year for sure!

## Part One

We're given lines of text, each of which containing a "calibration value," being the concatenation of the first and
last numeric digit within the text. Our job is to add together all of these calibration values. The bulk of the work,
therefore, is coming up with a `calibration-value` function for each line of text.

Pretending I don't know what's coming up in part two, this isn't too tough.

```clojure
(defn first-and-last [vals]
  [(first vals) (last vals)])

(defn calibration-value [s]
  (parse-long (apply str (first-and-last (re-seq #"\d" s)))))
```

We make a helper function `first-and-last` that returns a collection of the first and last elements within an input
collection. Then `calibration-value` makes a sequence of numeric strings from the input `s` using a regular expression,
grabs the first and last values, concatenates them into a string, and parses it into a number.

It took me a moment to remember that Clojure has the lovely `juxt` function, which merges one or more functions into a
new one that applies each function to its input. Using that, we can inline `first-and-last` and give this a good second
pass.

```clojure
; Removed the first-and-last function
(defn calibration-value [s]
  (->> (re-seq #"\d" s)
       ((juxt first last))
       (apply str)
       parse-long))
```

In this version, we again make a sequence of numeric strings using `re-seq`, use `(juxt first last)` to return a
vector of those values, concatenate them, and parse them into a number. It's more lines of code with whitespace, but
easier to understand. With that out of the way, the `part1` function is easy:

```clojure
(defn part1 [input]
  (transduce (map calibration-value) + (str/split-lines input)))
```

We're transducing on day 1! Starting with each line of the input, we map it with the `calibration-value` function, and
then add the results together. Off we go to part 2.

## Part Two

In this part, we have to be able to parse both numeric and alphabetic characters for each digit, so both `1` and `one`
map to the value 1. Regular expressions are no longer the way to go. I did end up making a messy solution that used
`index-of` and `last-index-of` with some collection sorting, but I didn't like it. (Note: my 1-hour bug was forgetting to
use `last-index-of` instead of `index-of` when looking for the last digit in the string. This only showed up in my
puzzle text; the sample data didn't reveal this issue.)

Then I rewrote the solution to use string replacements, but there's a catch they showcase in the example. In the sample
string `eightwothree`, if the code replaces the string `two` with `2`, then we're left with `eigh2three`, and the
substitution of `eight` for `8` will fail because the `t` was removed as it was shared with `two`. We can accommodate
for this by replacing `two` with `two2two`. That preserves all the letters on both sides of the number, in case any
other number needs it. Let's write a function that does that.

```clojure
(defn replace-numeric-strings [s]
  (reduce-kv (fn [acc idx name] (str/replace acc name (str name (inc idx) name)))
             s
             ["one" "two" "three" "four" "five" "six" "seven" "eight" "nine"]))
```

This function uses `reduce-kv`, which is a special form of `reduce` that expects the reduction collection to be some
sort of key-value pair. In this case, I am passing in a vector of the numerical strings from one to nine, which can be
interpretted as a map from the index (0-8) to the value within the vector (one to nine). Then for each key-value pair
being reduced, we use the `str/replace` function, which calls Java's `String.replace` method under the hood. We want
to look for the numeric string `name`, and replace each instance with the concatenation of that name to its incremented
index in the vector (vectors are 0-indexed but we started with the string one), and then to the name again.

Finally, we put together the `part1` and `part2` functions, and anyone who's read my solutions before knows I like to
make a common `solve` function because I'm trying to be clever.

```clojure
(defn solve [f input]
  (transduce (map (comp calibration-value f)) + (str/split-lines input)))

(def part1 (partial solve identity))
(def part2 (partial solve replace-numeric-strings))
```

The solution is still a transducer which applies the `calibration-value` function onto each line of text and then adds
them together, but now `solve` takes in an extra function to be run before `calibration-value`. For part 1, we don't
want to do anything else, so we pass in `identity`. For part 2, we want to first call `replace-numeric-strings`.
And thus we have a clean solution for day 1!

## Alternate solution with index searches

My original solution for part 2, which is a bit longer than the one I kept, involved playing with String indexes
instead of modifying each line of text. Let's see how that worked.

Knowing that the difference between parts 1 and 2 is whether we look for just numeric strings or also numeric names,
let's start with two constants that represent sequences of tuples - the string to search for and the number it
represents.

```clojure
(def numbers (map vector (map str (range 10)) (range 10)))
(def numbers-and-names (into numbers (map vector ["one" "two" "three" "four" "five" "six" "seven" "eight" "nine"]
                                          (range 1 10))))
```

`numbers` makes its sequence of vectors using the range of values from 0 to 9. The first element is `(map str n)` to
stringify the number, and the second element is just the number. `numbers-and-names` adds additional values to the
`numbers` sequence, this time mapping each name to its numeric value from 1 to 9.

Now we need to find the numeric value of the first number within each line of text, whether that is the 10 values in
`numbers` or the 19 in `numbers-and-names`. We'll use `index-of`, which uses Java's `String.index-of` method, on each
value within the passed-in `search` function argument, but we'll need to record both the index and the value of the
number. Then we'll find the lowest index and use its value.

```clojure
(defn find-first [s search]
  (->> search
       (keep (fn [[n v]] (when-let [idx (str/index-of s n)] [idx v])))
       sort
       first
       second))
```

This function uses the `keep` function (equivalent of `Kotlin's mapNotNull` function) on each search vector. If it
finds an index using `str/index-of`, it emits a new vector of `[idx v]` to show the first index that contained the
numeric value. Then we sort the vectors, which sorts vectors in order of their elements, take the first one for the
lowest index, and return the value in the second position.

When that's done, we'll also need `find-last`, which is almost identical.

```clojure
(defn find-last [s search]
  (->> search
       (keep (fn [[n v]] (when-let [idx (str/last-index-of s n)] [idx v])))
       sort
       last
       second))
```

Naturally, I don't like this duplication, so I refactored them to use a common `find-digit` function that expects the
`str-fn` to use (`index-of` or `last-index-of`) and the `seq-fn` (`first` or `last`) within the sequence. `find-first`
and `find-last` then become partial functions over `find-digit`.

```clojure
(defn find-digit [str-fn seq-fn s search]
  (->> search
       (keep (fn [[n v]] (when-let [idx (str-fn s n)] [idx v])))
       sort
       seq-fn
       second))

(def find-first (partial find-digit str/index-of first))
(def find-last (partial find-digit str/last-index-of last))
```

Now we can build our `calibration-value` function, which takes in the `search` space and the string to examine. Since
`find-first` and `find-last` return numbers, rather than stringifying them again, I just multiply the first digit by
10 before adding it to the last digit.

```clojure
(defn calibration-value [search s]
  (+ (* 10 (find-first s search)) (find-last s search)))
```

Finally, we can solve parts 1 and 2 together using the same `transduce` solution shown in part 1 above. Only now, 
instead of calling using the transformation function `(map calibration-value)` we'll need to pass that function the
search space with `(map #(calibration-value search %))`. Then `part1` uses `numbers`, and `part2` uses
`numbers-and-names`.

```clojure
(defn solve [search input]
  (transduce (map #(calibration-value search %)) + (str/split-lines input)))
(def part1 (partial solve numbers))
(def part2 (partial solve numbers-and-names))
```