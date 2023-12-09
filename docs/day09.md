# Day 09: Mirage Maintenance

* [Problem statement](https://adventofcode.com/2023/day/9)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day09.clj)

## Intro

I was wondering when working on this puzzle if we were going to have to do something related to
[Pascal's triangle](https://en.wikipedia.org/wiki/Pascal%27s_triangle) in the second part, but it didn't happen. This
was just a nice, straightforward puzzle.

## Part One

We are given an input of lines of numbers, each one being a "history," and we need to compute the pairwise differences
for each value until they are all zero. Then we need to calculate the next (last) value of each line of differences
until we get the next (last) value of the history itself.

Let's parse. This one was simple enough to do, although I split it into two lines for clarity.

```clojure
(defn parse-line [line] (map parse-long (str/split line #" ")))
(defn parse-input [input] (map parse-line (str/split-lines input)))
```

`parse-line` splits apart a line of text by spaces, calling `parse-long` on each value. We could have also done this
with a regular expression, doing `(map parse-long (re-seq #"-?\d+" line))` if we wanted to, but because the values can
be negative, we'd need the more complex regex instead of the usual `#"\d+"` regex. Splitting by spaces was simpler.
Then `parse-input` just calls `parse-line` for each line of the input.

Now the strategy will be to look at each history, break it into a sequence of "differences" between all pairs, and then
reversing the order and extrapolating the last value. First, let's make `diff-seq` to map a history line into its
sequence of differences.

```clojure
(defn diff-seq [coll]
  (when (not-every? zero? coll)
    (cons coll (lazy-seq (diff-seq (map (fn [[a b]] (- b a))
                                        (partition 2 1 coll)))))))
```

This function uses lazy sequences and recursive calls again. First, `(not-every? zero? coll)` checks if the collection
is all zeros. We _could_ return it as the last value in the collection, but since we don't actually need it, we'll
use `when` and have it just return `nil` if we've reached the line of zeros. If we don't have all zeros, then
`(cons coll (lazy-seq ...))` will return a sequence of the collection itself added to the front of the lazy calculation
of the next value. That lazy collection calls `diff-seq` with the differences. So to calculate the differences, we'll
do `(partition 2 1 coll)` to pair up every two values in the collection, and binding the difference between them.
Thus `(diff-seq '(1 3 5))` returns `((1 3 5) (2 2))])`.

Next, we'll create `extrapolate` to determine the next value of the history, given the sequence of differences.

```clojure
(defn extrapolate [differences]
    (reduce (fn [below left] (+ below left))
            0
            (map last (reverse differences))))
```

Surprise, it's a reduction! Because we want to look from the last sequence up to the top, and we only care about the
final value in each sequence, we'll feed `(map last (reverse differences))` as the input collection, initialized with
`0` to represent the line of zeros. Then for the reducing function, since `right-left=below`), and we want the next 
value to the right, `right=left+below`. Then we're ready for the inevitable `transduce` function for `part1`.

```clojure
(defn part1 [input]
  (transduce (map (comp extrapolate diff-seq)) + (parse-input input)))
```

This should look like so many other `part1` functions - we call `parse-input` to get the sequence of histories, then
for each one we map it to `diff-seq` and then that sequence to `extrapolate` to get our sequence of next values.
Finally, we reduce those values using `+` to get to our answer.

## Part Two

This part looks exactly the same as part 1, except that we need to extrapolate our way to the _previous_ value in each
history, as opposed to its _next_ value. At this point, I would usually write the `part2` logic in isolation and then
show how to merge our functions together, but it's so intuitive that I'm going to skip a step. We're going to refactor
`extrapolate` to take an argument `next-value?` to suggest if we want the first or last value of the history. Here
we go.

```clojure
(defn extrapolate [next-value? differences]
  (let [[op dir] (if next-value? [+ last] [- first])]
    (reduce (fn [child sibling] (op sibling child))
            0
            (map dir (reverse differences)))))

(defn solve [next-value? input]
  (transduce (map (comp (partial extrapolate next-value?) diff-seq)) + (parse-input input)))

(defn part1 [input] (solve true input))
(defn part2 [input] (solve false input))
```

For `extrapolate` to work for the next or previous value, we need to know two things: which value in each sequence
of differences do we need (last for the next value, first for previous value), and which operation to use to calculate
that value (`(+ sibling child)` for the next value, `(- sibling child)` for the previous value). Then `solve`  passes
that into the `extrapolate` function of its transformation mapping. Finally, `part1` calls `(solve true input)` because
it wants the next value, and `part2` calls `(solve false input)` because it wants the last value.

Nice and easy puzzle. Happy Saturday!
