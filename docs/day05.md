# Day 05: If You Give A Seed A Fertilizer

* [Problem statement](https://adventofcode.com/2023/day/5)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day05.clj)

## Intro

I enjoyed today's puzzle, because it seemed very "Adventy," especially part 2. All in all, this was challenging but not
insurmountable, which is the lovely sweet spot.

However, I did an almost complete rewrite of this puzzle once I implemented part 2. So what I'll describe below is my
solution to part 1 in isolation, and then almost none of the code will appear in the part 2 solution, that being the
one I commit to GitHub.

## Part One (initial implementation)

__Reminder:__ This code will be replaced entirely in the part 2 section. Feel free to skip ahead if you only want to
read the code and solution I accepted.

This puzzle involves getting an input set of numeric seed IDs, and a series of transformations that result in a final
"location" ID. Our goal is to find the smallest final location ID.

First let's parse. We'll reuse the `split-blank-line-groups` I've used in previous years, and which is now part of the
`abyala.advent-utils-clojure.core` namespace. I'll include its content here only for this puzzle.

```clojure
; abyala.advent-utils-clojure.core namespace

(defn split-blank-line-groups
  "Given an input string that represents multiple lines that get grouped together with blank line separators,
  returns a sequence of the line groups. Each line within a line group can optionally have a transformation
  function applied to it before being returned."
  ([input] (split-blank-line-groups identity input))
  ([xf input] (->> (str/split-lines input)
                   (partition-by str/blank?)
                   (take-nth 2)
                   (map (partial map xf)))))
```

That function will take in the multi-line input, and return a sequence of collections of lines. Those collections are
separated by a blank line, which does not itself get returned. Thus, the first line group returned will be a sequence
with a single string in it, being the line of seeds. Speaking of which, let's parse that.

```clojure
(defn parse-seeds [line]
  (map parse-long (re-seq #"\d+" (first line))))
```

This is quite simple, and uses a pattern we've seen many times - making a regex of `#"\d+"` to grab all numeric digits,
and then parsing each using `(map parse-long)`. Note that the input to the regex is `(first line)` since the input is
a collection of one string.

Next, we'll parse the rulesets, which is a collection of single-line rules. For my initial part 1 implementation, I
defined a rule as a map of `{:low x, :high y, :dest d}` where `:low` is the lowest source value to map, `:high` is the
largest source value to map (inclusive), and `:dest` is the mapped target value for the `:low` value.

```clojure
(defn parse-rule [line]
  (let [[dest src len] (map parse-long (re-seq #"\d+" line))]
    {:low src, :high (+ src len -1) :dest dest}))

(defn parse-ruleset [lines]
  (map parse-rule (rest lines)))

```

First, `parse-rule` takes in a single line for a conversion. It parses out the three numbers that compose a rule, and
returns the mapped representation. Note that for this instance of `:high` the value is `(+ src len -1)` since I chose
for `:high` to be inclusive.  Then `parse-ruleset` simply takes in the sequence of lines that compose a full ruleset,
skips the first line using `rest` because that's the title line, and maps each remaining line using `parse-rule`.

Note that I made the assumption from reading the input that the rules can all be executed in order. So thankfully that
meant I didn't have to parse the names of each ruleset to connect one conversion to another.

Then we'll wrap both functions in `parse-input`, which takes in the `input` string and returns a simple map of
`{:seeds (), :rulesets ()}`.

```clojure
(defn parse-input [input]
  (let [[seeds-str & ruleset-str-seq] (split-blank-line-groups input)]
    {:seeds (parse-seeds seeds-str)
     :rulesets (map parse-ruleset ruleset-str-seq)}))
```

Now that parsing is done, we can go about converting a source value into its target value. We'll use two functions -
`convert-source` which does a single mapping of a value, and `convert-all` which runs all of the conversions from an
initial seed value into its final location.

```clojure
(defn convert-source [ruleset src]
  (or (first (keep (fn [{:keys [low high dest]}] (when (<= low src high)
                                                   (+ dest (- src low))))
                   ruleset))
      src))

(defn convert-all [rulesets src]
  (reduce #(convert-source %2 %1) src rulesets))
```

`convert-source` starts off with an `or` statement - either we use the value converted from the ruleset or, if no rule
applied, we return `src` as the default rule. Then we use `keep` on the `ruleset` to test each rule in order. A rule
applies if `(<= low src high)` (which is why `high` is inclusive), and if `src` does fall within that range, its
converted value is the `dest` value plus its offset from `low`. The `convert-all` function just calls `reduce` to
invoke `convert-source` on each ruleset, with an initial value of the `src`.

Finally, we can implement the `part1` function. This just parses the input and calls `transduce` over each of the seeds.
It maps each seed using `(convert-all rulesets)` and finds the smallest one using `min`.

```clojure
(defn part1 [input]
  (let [{:keys [:seeds :rulesets]} (parse-input input)]
    (transduce (map #(convert-all rulesets %)) min Long/MAX_VALUE seeds)))
```

Ok, that was fun.  Let's delete most of that code.

## Part Two (and rewrite of part one)

Now we understand that the first line of seeds wasn't a list of seeds, but a list of seed ranges, where the numbers
alternate between the start of the seed range and the length of the range, similar to how parameters 2 and 3 of each
rule work. Our goal is still to find the smallest target location across all seeds within the seed ranges. A quick bit
of math shows we have about 1.9 billion seeds, so mapping each seed range to its possible seeds and doing the full
conversion is unreasonable. Instead, we'll have to work with seed ranges.

To start, let's parse the data. I'll use the same versions of `parse-seeds`, `parse-ruleset` and `parse-input` from
the original part 1 solution above, but I want a different and simpler implementation of `parse-rule`. This time,
instead of using `{:low :high :dest}`, I'll use the same `{:low :dest :len}` values that appear in the input text.
This results in a very simple function.

```clojure
(defn parse-rule [line]
  (let [[dest src len] (map parse-long (re-seq #"\d+" line))]
    {:low src, :dest dest, :len len}))
```

Next, instead of implementing `convert-source`, we'll build up to `convert-range`, since each seed range should map to
one or more seed ranges. To do that, we'll create `target-range`, which takes in a single rule and a seed range, and
checks to see whether it can convert the _lowest_ seed in the range. If so, it returns the seed range that it converted.
Note that if it could convert seeds later in the range, it'll ignore them for now. We could make a micro-optimization of
sorting the rules within a ruleset, but the performance is so good that it's unnecessary.

```clojure
(defn target-range [rule seed-range]
  (let [{:keys [low dest len]} rule
        [seed-low seed-len] seed-range]
    (when (<= low seed-low (+ low len -1))
      [(+ dest (- seed-low low))
       (min seed-len (+ low len (- seed-low)))])))
```

First, `target-range` destructures both the `rule` and the `seed-range` into their component parts for easier
manipulation. Then it checks to see if `seed-low` fits within the rule's range with
`(<= low seed-low (+ low len -1))`, where the high value again is inclusive. If the `seed-low` is in that range, then
the function returns a new seed range vector of type `[:low :length]`. As with the original implementation of
`convert-source`, `:low` is defined as `(+ dest (- seed-low low))`. The length of the target seed range is as much of
the seed range that doesn't go beyond the end of the rule range.

Now it's time to build `convert-range`, which takes in a ruleset and a _collection_ of seed ranges to run through the
ruleset. It will return a lazy sequence of target seed ranges, since a single seed range may be mapped by multiple
rules.

```clojure
(defn convert-range [ruleset seed-ranges]
  (when-some [[seed-low seed-len :as seed-range] (first seed-ranges)]
    (if-some [[_ target-len :as target] (first (keep #(target-range % seed-range) ruleset))]
      (let [next-seed-ranges (if (= target-len seed-len) (rest seed-ranges)
                                                         (cons [(+ seed-low target-len) (- seed-len target-len)]
                                                               (rest seed-ranges)))]
        (cons target (lazy-seq (convert-range ruleset next-seed-ranges))))
      (cons seed-range (lazy-seq (convert-range ruleset (rest seed-ranges)))))))
```

Because this generates its lazy sequence recursively, we first need to make sure that `seed-ranges` isn't empty yet,
meaning there are still seed ranges to evaluate. If so, we bind the first one as `seed-range` and destructure its
components into `seed-low` and `len`. Note the consiceness of `[[seed-low len :as seed-range] (first seed-ranges)]`
here, where it defines both the two destructured bindings _and_ the name of the complete binding using the keyword
`:as`. Nice job, Clojure. Then if we find a seed range, we'll call `target-range` for all rules within the ruleset
against the seed range, keeping the first (and only) non-null mapping. If we don't find one, the `else` clause will
use the current seed range as the target seed range, returning a lazy sequence by calling `convert-range` again with
`(rest seed-ranges)`. But if we find a rule and generate a new `target` seed range, then the function will emit it.
The recursive call back depends on whether or not the rule converted the entire seed range. If its length equals the
length of the original seed range, it was all converted, so the lazy recursive call just passes in `(rest seed-ranges)`.
If the rule only converted some of the seed range, then it will recursively call with the remainder of the source seed
range added to the remaining seed ranges. The `seed-low` of the new seed range is `(+ seed-low target-len)` or the next
value after the end of the new target seed range, and the `seed-len` is the target length subtracted from the original
seed length.

With that out of the way, we can convert a seed range through a single ruleset and get back a sequence of target
seed ranges, so it's time to solve part 2.  We'll then go back and solve part 1.

```clojure
(defn part2 [input]
  (let [{:keys [:seeds :rulesets]} (parse-input input)]
    (->> (partition 2 seeds)
         (mapcat (fn [seed-ranges] (reduce #(convert-range %2 %1) (list seed-ranges) rulesets)))
         (map first)
         (apply min))))
```

After parsing the input and extracting the initial seeds, we'll create seed ranges by using `(partition 2 seeds)`,
turning those values into tuples of `seed-low` and `seed-len`. Then those seed ranges will be fed into a `reduce`
function that calls `convert-range` with each of the rulesets. However, since `convert-range` expects to receive a
sequence of seed ranges, we initialize `reduce` with `(list seed-ranges)` to make a single-element sequence. Once
each of these seed ranges gets fully converted, we use `mapcat` to flat map them into a single list of target seed
ranges. Then we simply map each target seed range to its first value (`seed-low`) and find the smallest value.

Now part 1 is largely the same thing, but each seed range is of length 1. So we'll just call
`(map #(vector % 1) seeds)` to turn the list of seeds into the list of 1-length seed ranges. The rest of the function
is the same as part 2.

```clojure
(defn part1 [input]
    (let [{:keys [:seeds :rulesets]} (parse-input input)]
      (->> (map #(vector % 1) seeds)
           (mapcat (fn [seed-ranges] (reduce #(convert-range %2 %1) (list seed-ranges) rulesets)))
           (map first)
           (apply min))))
```

I may have only one arrow in the quiver, but it flies straight. Let's make a single `solve` function that takes in the
function to apply to the original list of seeds, and then have `part1` and `part2` call it:

```clojure
(defn solve [seed-fn input]
  (let [{:keys [:seeds :rulesets]} (parse-input input)]
    (->> (seed-fn seeds)
         (mapcat (fn [seed-ranges] (reduce #(convert-range %2 %1) (list seed-ranges) rulesets)))
         (map first)
         (apply min))))

(defn part1 [input]
  (solve (partial map #(vector % 1)) input))

(defn part2 [input]
  (solve #(partition 2 %) input))
```

And that's it! The rewrite was an adventure, but in the end, the solution looks clean to me, and it's incredibly fast,
so I'll call it a win.

## Minor refactoring

I decided that since I keep seeing the pattern for parsing a list of numeric strings into longs, I would create a new
`split-longs` function and put it into my `abyala.advent-utils-clojure.core` repo, for all to enjoy.

```clojure
; abyala.advent-utils-clojure.core namespace
(defn split-longs
  "Given an input string, returns a sequence of all numbers extracted, coerced into longs. Any delimiter is acceptable,
  including whitespace, symbols, or any non-numeric character."
  [input]
  (map parse-long (re-seq #"\d+" input)))
```

With this in place, we can slightly simplify our two main parse functions. I think this is expressive and easy enough
to read, and eliminates two regular expressions from the code. Plus this goes nicely with the existing
`split-blank-line-groups input` function.

```clojure
(defn parse-seeds [line] (split-longs (first line)))

(defn parse-rule [line]
  (let [[dest src len] (split-longs line)]
    {:low src, :dest dest, :len len}))
```
