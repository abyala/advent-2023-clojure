# Day 18: Lavaduct Lagoon

* [Problem statement](https://adventofcode.com/2023/day/18)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day18.clj)

## Intro

This is the second puzzle this year that I hated because it does not seem to be solvable without knowing the magical
mathematical formula to use. Thank you to the other programmers who went on Reddit and gave me the secret I needed, so
I could actually solve the puzzle. In theory, I could refactor Day 10's puzzle to use this too, but I probably won't.
Then again, given the past few days of puzzles, I have a feeling we're going to be building out a massive grid library
during the last week of Advent, so maybe I will.

The trick is to know about two formulas -- the [Shoelace Formula](https://en.wikipedia.org/wiki/Shoelace_formula),
which defines the total area of a polygon given a list of points vertices that define its perimeter; and
[Pick's Theorem](https://en.wikipedia.org/wiki/Pick's_theorem), which defines the relationship between a polygon's
area, number of interior points, and number of boundary/perimeter points.

It no longer makes sense to describe how I approached the problem since it didn't work without the math functions, so
I guess I'll just describe the algorithm as though I had figured it out? Sure, let's do that.

## Part One

We're given a few lines of text that represent the movements our "digger" makes as they define the perimeter. (Since
I'm bitter about the puzzle, I'll take a moment here to applaud Eric for using the genderless pronoun "they" to
describe a digger without a description.)  Anyway, each line has a direction, a number of steps, and a "color" we don't
seem to use in part 1. We'll be doing another parse method in part 2, so let's leave the door open now for that other
parsing logic.

```clojure
(defn parse-line [line]
  (let [[_ dir n _] (re-matches #"(\w{1}) (\d+) .*" line)]
    [({"U" [0 -1], "D" [0 1], "L" [-1 0], "R" [1 0]} dir) (parse-long n)]))

(defn parse-instructions [line-parser input]
  (map line-parser (str/split-lines input)))
```

`parse-line` grabs the one-character string for the direction (`U`, `D`, `L`, or `R`) and the digits that compose the
distance. It returns a vector of `[[dx dy] dist]` where `[dx dy]` is the same pair of ordinates we've used to represent
moving up, down, left, or right.  Then `parse-instructions`, expecting to have different parsing instructions later,
simply maps the `line-parser` function to each line of the input.

Let's keep doing the logic before turning to the math. We'll define the function `move-seq` to return an infinite
sequence of points moving in a direction from a starting point, which we'll in turn use in `all-turns` to return each
vertex the digger creates.

```clojure
(defn move-seq [p dir]
  (let [p' (mapv + p dir)]
    (cons p' (lazy-seq (move-seq p' dir)))))

(defn all-turns [instructions]
  (reduce (fn [acc [dir dist]] (conj acc (mapv + (last acc)
                                                 (mapv * dir (repeat dist)))))
          [p/origin]
          instructions))
```

`move-seq` calls `(mapv + p dir)` to take the point `p` (a vector) and the direction `dir` (another vector), and add
each pairwise value together, returning a third vector. Then it returns the new point `p'` attached to a lazy sequence
of the remaining values from recursively calling itself. Then `all-turns` calls `reduce` on each of the parsed
instructions, and adds the result of moving `dist` steps in the direction `dir`, and adding it to the last step seen.
As a very important note - we start at the `origin` and return every line segment the digger creates, which means we
end at the `origin` again. We'll use this assumption to make the next function a tiny bit simpler.

Ok, math time. First we'll implement `shoelace-area` to return the total area of the polygon identified by the
`all-turns` function. Wikipedia says that we'll get _twice_ the area by taking each pair of vertices in order, and
running `(- (* x1 y2) (* x2 y1))`, and adding them together. It's matrix math, which I haven't done in about 25 years, 
but I also verified this with some trivial shapes and it checks out.

```clojure
(defn shoelace-area
  ([points] (/ (transduce (map (partial apply shoelace-area)) + (partition 2 1 points)) 2))
  ([[x1 y1] [x2 y2]] (- (* x1 y2) (* x2 y1))))
```

When called in its 2-arity form, it takes two points and calculates the difference between the product of opposing `x`
and `y` ordinates, as described above. When called in its 1-arity form, it takes in the sequence of vertices along the
perimeter in path order. The function calls `(partition 2 1 points)` to zip together each adjacent pair of points;
because we know the first and last are the same, we don't need to do anything to make the last unique point connect
back to the first. Then we transduce these pairs by calling the 2-arity `shoelace-area`, adding the values together.
And then, every importantly, we divide the value by 2 since the Shoelace Formula returns twice the area.

Ok, now let's do what we really came here to do, which is to calculate the total number of points enclosed within the
perimeter path. Pick's Theorem says `A = i + b/2 - 1`, where `A` is the area, `i` is the number of interior points, and
`b` is the number of boundary/perimeter points. Once we solve all of its variables, the `total-points-within-path` will
add the number of points along the boundary/perimeter to the number of points on the interior.

```clojure
(defn total-points-within-path [path]
  (let [area (shoelace-area path)
        perimeter (transduce (map (partial apply p/manhattan-distance)) + (partition 2 1 path))
        interior (- (inc area) (/ perimeter 2))]
    (+ perimeter interior)))
```

The area `A` we already know how to get using `shoelace-area`. For the perimeter, we already have the vertices of the
perimeter, so if we again pairwise partition them, we can calculate the Manhattan Distance between each pair of points,
and add them together to form the perimeter. Then, since we know both `A` and `b`, and we need to solve for `i`,
`i = A + 1 - b/2`, or `(- (inc area) (/ perimeter 2))`.

Time to get to the end. I already spoiled the fun that the only difference between part 1 and part 2 is the line
parser, so let's immediately create the `solve` function.

```clojure
(defn solve [parse-fn input]
  (->> (parse-instructions parse-fn input) all-turns total-points-within-path))

(defn part1 [input] (solve parse-line input))
```

So we parse the input given the `parse-fn` (which is `parse-line` right now), calculate the turns of the perimeter,
and return the total number of points within the path.

## Part Two

Oh look, we have a new parser. Now we _only_ look at the so-called "color" of each input line, pulling apart the 
direction and distance. The distance is expressed as a 5-digit hex string, and the direction is the 6th character of
the hex string.

```clojure
(defn parse-line2 [line]
  (let [[_ dist-hex dir-hex] (re-matches #"\w \d+ \(\#(\w{5})(\w)\)" line)]
    [([[1 0] [0 1] [-1 0] [0 -1]] (parse-long dir-hex)) (Integer/parseInt dist-hex 16)]))
```

Once again this is easy with a regex. We skip the first two sections of the input string, and then extract out the
first 5 and last 1 character of the hex string. The encoded value of the direction has values 0 through 3, so we'll
use that value as the index of our lookup vector, and then use `(Integer/parseInt 3 16)` to read the distance by
parsing it as a number in base 16.

All that's left is our `part2` function and we're done.

```clojure
(defn part2 [input] (solve parse-line2 input))
```

So that's it. In my mind, it's another bad puzzle. Better luck tomorrow.

## Refactoring

Small update - I moved the `shoelace-area` and `total-points-within-path` functions to the
`abyala.advent-utils-clojure.point` namespace so I could refactor the day 10 solution too. I also renamed
`shoelace-area` to `polygon-area` because it abstracts the algorithm from the intent of the function. Also, I renamed
`total-points-within-path` to `polygon-total-point-count` so it would match the naming convention of
`polygon-area` and day 10's `polygon-interior-point-count` functions.