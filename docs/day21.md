# Day 21: Step Counter

* [Problem statement](https://adventofcode.com/2023/day/21)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day21.clj)

## Intro

I can admit when I'm wrong, and yesterday I was when I thought we had reached the low point of an otherwise excellent
year of Advent puzzles. Today's part 1 had a good start, but then I lost interest with part 2, which you'll see in my
solution to the puzzle.

## Part One

In part 1, we are given a grid that represents a garden, with a starting position `S`, garden plots `.`, and rocks `#`.
Our goal is to identify the number of distinct garden plots we could be standing on after taking 64 steps. Seems
reasonable, so let's parse our data, returning a garden of format `{:occupied [], :garden-plots #{}, :size n}`.

```clojure
(defn parse-garden [input]
  (let [all-points (p/parse-to-char-coords-map input)
        starting-point (first (first-when #(= \S (second %)) all-points))]
    {:occupied [starting-point]
     :garden-plots (set (keep #(when (= \. (second %)) (first %))
                              (assoc all-points starting-point \.)))
     :size         (inc (apply max (map ffirst all-points)))}))
```

First, we use `parse-to-char-coords-map` to build a map of `{[x y] c}`, and then we search for the coordinates of the
point containing the value `S` as the starting point. `:occupied` is just a vector of that value, and `:garden-plots`
finds all coordinates when the value is `.`, after we convert the `starting-point` into a `.` too. `:size` is the 
length of the garden, since we take as assumption that it is a square; this is necessary for part 2.

Next we want to take in a `garden` and return its state after taking each possible step from the currently occupied
positions.

```clojure
(defn take-step [garden]
  (let [{:keys [occupied garden-plots]} garden]
    (assoc garden :occupied (filter garden-plots (set (mapcat p/neighbors occupied))))))
```

`take-step` destructures the components of the `garden` argument, and calls `(set (mapcat p/neighbors occupied))` to
get a set of all possible points adjacent to the currently-occupied coordinates. Then `(filter garden-plots points)`
removes all points that are rocks, which then `(assoc garden :occupied points)` places into the new state of the
garden being returned.

And now we can build `num-occupied-at-step` to determine the number of occupied spaces after taking `n` steps.

```clojure
(defn num-occupied-at-step [garden n]
  (->> (iterate take-step garden) (drop n) first :occupied count))

(defn part1 [input] (num-occupied-at-step (parse-garden input) 64))
```

We call `(iterate take-step garden)` to build an infinite sequence of garden states, dropping the first `n` values;
since `iterate` returns the input value first, being the state before any steps, to get to the 64th step we have to
drop 64 states, not 63. Then we simply call `first` to get the state we want, pull out the `:occupied` collection, and
`count` the results.  That means that `part1` just parses the data and calls `num-occupied-at-step` with `n` equal to
64.

## Part Two

If you want to understand why this solution works, feel free to read through the
[Reddit thread](https://www.reddit.com/r/adventofcode/comments/18nevo3/2023_day_21_solutions/) for an explanation of
why the specific format of the data and some math works the way it does. I'm just going to provide the helper functions
and give the step-by-step instructions on what I did to get my star, since I honestly don't care about this puzzle.

First, refactor `take-step` and `num-occupied-at-step` to accept a function to use when determining whether a
neighboring coordinate is a garden plot.

```clojure
(defn take-step [f garden]
    (let [{:keys [occupied garden-plots size]} garden]
      (assoc garden :occupied (filter (comp garden-plots #(f size %)) (set (mapcat p/neighbors occupied))))))

(defn num-occupied-at-step [f garden n]
  (->> (iterate (partial take-step f) garden) (drop n) first :occupied count))
```

Now before filtering on whether a coordinate pair is within the set of `garden-plots`, we first call `(f size %)` on
the point, passing in the size of the garden. We'll see why when we compare how to resolve this for parts 1 and 2.

```clojure
(defn part1-pred [_ p] p)
(defn part2-pred [size p] (mapv #(mod % size) p))

(defn part1 [input] (num-occupied-at-step part1-pred (parse-garden input) 64))
```

In part 1, we don't need any transformation, so `part1-pred` takes in the two arguments and just returns the point `p`.
For part 2, since we'll have an infinite-sized garden, we'll call `(mod % size)` on both ordinates of the point to
transpose the point back onto the original carden and its `garden-plots`.

Now we build 

```clojure
(defn wolfram-alpha-string [input]
  (let [garden (parse-garden input)]
    (str "{{0, " (num-occupied-at-step garden 65)
         "}, {1, " (num-occupied-at-step garden 196)
         "}, {2, " (num-occupied-at-step garden 327) "}}")))
```

We need to call `num-occupied-at-step` on three values - 65 (the distance from the middle of the graph to the end),
196 (65 + 131 where 131 is the width of everyone's graph), and 327 (65 + (2 * 131)). This function does a little
string manipulation we'll need to get our answer, so for my input string, this function returns
`{{0, 3802}, {1, 33732}, {2, 93480}}"`.

Then, follow these steps:
1. Go to [WolframAlpha's Quadratic Fit](https://www.wolframalpha.com/input?i=quadratic+fit) page, and in the input box
labeled "data set of y values," paste the output string from `wolfram-alpha-string` without the double quotes. Click
the "Compute" button.
2. The output will provide an equation under "Least-squares best fit." Mine was `14909 x^2 + 15021 x + 3802`.
3. Plug the puzzle input value of `26501365` in to that equation to get your answer.

Enjoy whatever the hell that was. Fingers crossed that this season will redeem itself in the last 3.5 days!
