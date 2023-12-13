# Day 13: Point of Incidence

* [Problem statement](https://adventofcode.com/2023/day/13)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day13.clj)

## Intro

I liked today's puzzle because I thought it was going to be very grid-focused, and it turned out not to be. Plus even
though I'm quite convinced there are ways to simplify my implementation, doing things with a little brute force seems
to be just fine for the data set.

## Part One

We are given a list of "patterns," being grids with ash and rocks (periods and hash signs). Each pattern has either a
vertical mirror (lines to the left of it are mirror images to the ones on the right) or a horizontal mirror (lines
above it are mirror images to the ones below). The mirror always exists _between_ our rows and columns, never on them,
so we don't have to worry about those off-by-one errors. Our task is to do a little math on the numbers of rows to the
left or above the mirrors.

Other than calling `split-blank-line-groups` to break the input into groups of lines that are separated by a blank
line, there's no parsing to do. So let's get right to the basis of everything else - the `mirror?` function.

```clojure
(defn mirror? [line idx]
  (let [[left right] (split-at idx line)]
    (every? identity (map = (reverse left) right))))
```

This function takes a line (initially a string but later a vector of characters) and an index. It then breaks the line
into two segments at the given index, reverses the first (left), and checks that it's equal to the right. We reverse
the left because the two segments aren't identical, but rather are mirror images. So if we compare the first character
of the right segment against the _last_ character of the first, they might be mirrors. The other interesting thing to
see is that `(map = list1 list2)` does a lot of work. First, it executes the `=` mapping function against each zipped
up element of the two lists without making us create a merged tuple ourselves. And second, it only maps when there are
elements from both lists to apply; if we ran `(map + [1 2 3] [10 11 12 13 14])`, it would only combine `[1 2 3]` and
`[10 11 12]` giving us the output `(11 13 15)`.

Now let's implement `vertical-mirror-index`, which takes in pattern and returns all indexes of that pattern such that
all rows can mirror along the index. We only expect up to one index will be a vertical mirror, but I have a hunch that
allowing for more than one will benefit us soon. Foreshadowing!

```clojure
(defn vertical-mirror-index [pattern]
  (let [first-line (first pattern)
        possible-indexes (filter #(mirror? first-line %) (range 1 (count first-line)))]
    (filter (fn [idx] (every? #(mirror? % idx) pattern)) possible-indexes)))
```

This function first checks the first line to see which indexes there are mirrors; a mirror only exists if all rows in
the pattern mirrors on that index, which means that must apply to at least row 1. We use `(range 1 (count first-line))`
because the mirror can't be at index 0, as there are no characters to the left of it. We `filter` all of those indexes
where `mirror?` is true for the first line. Then from those possible indexes, we filter them such that every row in
the pattern is also a mirror, using `(every? #(mirror? % idx) pattern)`.

The `horizontal-mirror-indexes` is almost the same, except we need to do a little work to read columns instead of rows.

```clojure
(defn horizontal-mirror-indexes [pattern]
  (let [pattern-column (fn [idx] (map #(get % idx) pattern))
        first-column (pattern-column 0)
        possible-indexes (filter (partial mirror? first-column) (range 1 (count first-column)))]
    (filter (fn [idx] (every? #(mirror? % idx)
                              (map #(pattern-column %) (range (count (first pattern)))))) possible-indexes)))
```

First, we make an internal function `pattern-column`, which returns the column of characters corresponding to the
`nth` index in each row of the pattern. Once we have that, it's pretty much a copy-paste job from
`vertical-mirror-indexes`.

Did someone say copy-paste? That's completely unacceptable. Let's bring those two functions together using a common
`mirror-indexes` function, recognizing that it's actually identical to `vertical-mirror-indexes`. I could instead just have
`horizontal-mirror-indexes` call `vertical-mirror-indexes`, or call `mirror-indexes` directly instead of 
`horizontal-mirror-indexes`, but I think both aren't as readable as having one extra, somewhat silly function.

```clojure
(defn mirror-indexes [lines]
  (let [first-line (first lines)
        possible-indexes (filter #(mirror? first-line %) (range 1 (count first-line)))]
    (filter (fn [idx] (every? #(mirror? % idx) lines)) possible-indexes)))

(defn vertical-mirror-indexes [pattern]
  (mirror-indexes pattern))

(defn horizontal-mirror-indexes [pattern]
  (mirror-indexes (map (fn [idx] (map #(get % idx) pattern)) (range (count (first pattern))))))
```

Finally, we need to implement `pattern-points`. Now since both `vertical-mirror-indexes` and `horizontal-mirror-indexes`
return collections of indexes (again, we expect at most 1 value), we need to make this a tiny bit more complex than if
the values were scalars.

```clojure
(defn points-for [v-indexes h-indexes]
  (apply + (concat v-indexes (map #(* 100 %) h-indexes))))

(defn pattern-points [pattern]
  (points-for (vertical-mirror-indexes pattern) (horizontal-mirror-indexes pattern)))
```

We actually make two small functions, because in part 2 we'll reuse one of them. `points-for` takes in the sequence
of vertical and horizontal indexes. After multiplying each horizontal index by 100, it concatenates them with the
vertical indexes, and adds them together. Then `pattern-points` just calls `points-for` with the return values from
`vertical-mirror-indexes` and `horizontal-mirror-indexes`.

Now we can knock out `part1` with our friendly neighborhood `transduce` function.

```clojure
(defn part1 [input]
  (transduce (map pattern-points) + (split-blank-line-groups input)))
```

There's nothing fancy here - input the collection of patterns, then map each to its points, and add the points
together.

## Part Two

In part 2, we need to identify for each pattern which point should switch from either a period to a hash or vice versa,
find the new mirror (ignoring the old one if it still applies), and compute the new pattern points. Now I'm sure there
is a super clever way to do this, but the dataset is reasonable, so I went with the simpler, perhaps dumber solution.
To that end, let's see the `all-smudges` function.

```clojure
(defn all-smudges [pattern]
  (for [x (range (count pattern))
        y (range (count (first pattern)))]
    (update-in pattern [x y] {\. \#, \# \.})))
```

Using list comprehension again, we pair every line number (`x`) with its character number (`y`), and then update that
one value in the pattern, flipping values between periods and hash signs. Now how does `update-in` work in this
context? Well there are two ways it _won't_ work. First, if the pattern is a sequence of lines, it won't do anything
because vectors are indexed but sequences aren't. Second, even if the pattern were a vector, we can's `update` or
`assoc` a String (even though I think it would be a fine language extension), so we need to instead deal with more
indexable vectors. We'll see in a moment that `all-smudges` will be called with a vector of character vectors, instead
of a sequence of strings. Because Clojure is awesome with its collection functions, everything else already written
will work just fine.

Armed with `all-smudges`, let's implement `smudgy-pattern-points`.

```clojure
(defn smudgy-pattern-points [pattern]
  (let [h (set (horizontal-mirror-indexes pattern))
        v (set (vertical-mirror-indexes pattern))]
    (->> (all-smudges (mapv vec pattern))
         (keep (fn [smudge] (let [h' (-> (horizontal-mirror-indexes smudge) set (set/difference h))
                                  v' (-> (vertical-mirror-indexes smudge) set (set/difference v))]
                              (when (or (seq h') (seq v'))
                                (points-for v' h')))))
         first)))
```

First, we record the horizontal and vertical mirror indexes from the pattern as we read it; these are the values that
should _not_ be correct after we update the correct character. Then we call `(all-smudges (mapv vec pattern))` to get
our list of all possible permutations of the pattern; `vec` turns each line of the `pattern` into a character vector,
and `mapv` maps `vec` onto each line, creating a vector instead of a sequence. Then we call the `keep` function on each
smudged pattern. We calculate its horizontal and vertical mirror images, but then remove all values from the original
mirror indexes to determine what's new in this smudge. If we still have any new mirror indexes, then we calculate the
point values using the silly `points-for` function we created earlier. And once we've found the first smudge that
generates a point total, we can return it.

And we can implement `part2` now. Though I could combine this with `part1` because only the transformation function
changes from `pattern-points` to `smudgy-pattern-points`, I'm fine with it as it is.

```clojure
(defn part2 [input]
  (transduce (map smudgy-pattern-points) + (split-blank-line-groups input)))
```