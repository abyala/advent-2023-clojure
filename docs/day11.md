# Day 11: Cosmic Expansion

* [Problem statement](https://adventofcode.com/2023/day/11)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day11.clj)

## Intro

Today's puzzle was much easier than yesterday's, working with some basic grids. The wording for part 2 is, in my
opinion, incredibly misleading. I solved the puzzle without understanding why I had to create an off-by-one error, and
only while doing this write-up did I figure out why this ended up being correct. Very poor wording in the instructions.

## Part One

We are given a simple grid with galaxies marked as hash signs. We need to expand all rows or columns without galaxies
by one extra space, and then calculate the sum of shortest paths between the resulting galaxy locations. Usually I
create a parse function here, but I found I didn't want one. Instead, I'll operate entirely on sequences of `[x y]`
coordinates.

The bulk of this solution depends on the `expand-universe` function, which in turn depends on `expand-row` and
`expand-column`. Let's start with those helper functions.

```clojure
(defn expand-row [galaxies row]
  (map (fn [[x y :as coords]] (if (> y row) [x (inc y)] coords)) galaxies))

(defn expand-column [galaxies column]
  (map (fn [[x y :as coords]] (if (> x column) [(inc x) y] coords)) galaxies))
```

The two functions do the same things in different dimensions, so let's just look at `expand-row`. This function takes
in a sequence of galaxies, and the row number to expand; a row corresponds to a coordinate's `y` ordinate. So we'll
`map` each set of coordinates within the input `galaxies`, checking to see if that coordinate's `y` value is creater
than the row being expanded. If so, increment its value; if not, return the input coordinates unchanged.

```clojure
(defn expand-universe [galaxies bounds]
  (let [[[x0 y0] [x1 y1]] bounds
        expansion-rows (remove (set (map second galaxies)) (range y1 (dec y0) -1))
        expansion-columns (remove (set (map first galaxies)) (range x1 (dec x0) -1))]
    (as-> galaxies x
          (reduce #(expand-row %1 %2) x expansion-rows)
          (reduce #(expand-column %1 %2) x expansion-columns))))
```

`expand-universe` takes in the sequence of `galaxies`, as well as the bounding box `bounds`, which we'll immediately
deconstruct into `[[x0 y0] [x1 y1]]`; note that we could do this in the function arguments, so that line would look
like `(defn expand-universe [galaxies [[x0 y0] [x1 y1]]] ...)`, but I think that's uglier than an internal `let`
binding. Then we need to figure out which rows and columns need to be expanded, and again we'll just look at the rows.
To start, we'll create a range of all possible `y` values, starting from the largest value of `y1` and decrementing
down to `(dec y0)`; doing this will return a sequence of rows from largest to smallest; we could calculate how many
total rows each point needs to move down, and perhaps that would be more efficient, but starting from largest to 
smallest means we can do each expansion safely without having a smaller row number impacting which galaxies get 
expanded later. Once we have these two list of decreasing row and column numbers, we use the `as->` macro, which allows
us to pipe the output of each function call into the next one in any position. We'll call `reduce` on `expand-row` and
`expand-column` for each row and column, passing the collection of `galaxies` neatly from one `reduce` to the next.

Finally, we can implement `part1`.

```clojure
(defn part1 [input]
  (let [points (p/parse-to-char-coords input)
        bounds (p/bounding-box (map first points))
        galaxies (keep (fn [[p c]] (when (= c \#) p)) points)
        expanded-galaxies (expand-universe galaxies bounds)]
    (apply + (for [g1 expanded-galaxies
                   g2 expanded-galaxies
                   :when (= 1 (compare g1 g2))]
               (p/manhattan-distance g1 g2)))))
```

We'll parse the entire input grid into the sequence `points` so we can find the proper bounding box as `bounds`. Then
we can strip out the galaxy coordinates by calling `keep` on the `points`, searching for ones where the value in the
grid is the character `#`, and when so pulling out just the coordinates. We then bind `expanded-galaxies` to the
output of `expand-universe`, thus giving us the fully expanded galaxies. Now we need to create every unique pair of
galaxies, and I thought this would be a good time to use list comprehension with the `for` macro. For each galaxy
`g1` in `expanded-galaxies`, we look at each value `g2` in `expanded-galaxies`, stopping the search in `g2` once 
`(= g1 g2)`. We could also only look at a given pair of `g1` and `g2` by comparing the two and keeping only ones where
`g1` compares less than `g2`, but using this `:when` instead of a `:while` is a simpler way to only look at half of the
data. Come to think of it, we could have used all values and cut the results in half, but that's stilly. Anyway, for
each pair, the shortest distance on a 2-dimensional grid is the `manhattan-distance`, which we'll reuse from previous
years' solutions; this is in the `abyala.advent-utils-clojure.point` namespace. Finally, we add together these
distances to solve the puzzle.

## Day Two

Part 2 isn't difficult to do, other than dealing with some confusing instructions. The problem says "Now, instead of
"the expansion you did before, make each empty row or column one million times larger." I read that multiple times as
meaning that we add 1,000,000 rows or columns for each expansion row or column, but that's not exactly what it says.
To make a row 1,000,000 times larger, we need to add 999,999 rows, making the _total_ row one million lines tall; we
are not making the _increased row size_ one million times larger, or even one million times the previous size. So
when the instructions mention making the expansion 10 times larger, we need to add 9 rows/columns. Yikes.

Anyway, after figuring out the wording, this puzzle requires refactoring all the existing functions to take in a
function argument `expand-by`, such that `part1` passes in a value of `1` and `part2` passes in `999999`.

```clojure
(defn expand-row [expand-by galaxies row]
  (map (fn [[x y :as coords]] (if (> y row) [x (+ y expand-by)] coords)) galaxies))

(defn expand-column [expand-by galaxies column]
  (map (fn [[x y :as coords]] (if (> x column) [(+ x expand-by) y] coords)) galaxies))

(defn expand-universe [expand-by galaxies [[x0 y0] [x1 y1]]]
  (let [expansion-rows (remove (set (map second galaxies)) (range y1 (dec y0) -1))
        expansion-columns (remove (set (map first galaxies)) (range x1 (dec x0) -1))]
    (as-> galaxies x
          (reduce #(expand-row expand-by %1 %2) x expansion-rows)
          (reduce #(expand-column expand-by %1 %2) x expansion-columns))))

(defn solve [expand-by input]
  (let [points (p/parse-to-char-coords input)
        bounds (p/bounding-box (map first points))
        galaxies (keep (fn [[p c]] (when (= c \#) p)) points)
        expanded-galaxies (expand-universe expand-by galaxies bounds)]
    (apply + (for [g1 expanded-galaxies
                   g2 expanded-galaxies
                   :while (not= g1 g2)]
               (p/manhattan-distance g1 g2)))))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 999999 input))                   ; Not 1,000,000
```

The only functions that don't just pass `expand-by` around are `expand-row` and `expand-column`. Instead of their
invoking `(inc y)`, then now call `(+ y expand-by)`, and the equivalent calls for `x`.

So yeah, super easy code, but damn confusing instructions.
