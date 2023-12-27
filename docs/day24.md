# Day 24: Never Tell Me The Odds

* [Problem statement](https://adventofcode.com/2023/day/24)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day24.clj)

## Intro

And we're back to absolute garbage puzzles again.
[Looking on Reddit](https://www.reddit.com/r/adventofcode/comments/18pnycy/2023_day_24_solutions/), it appears that
everyone doesn't remember the nuances of solving multiple simultaneous equations, perhaps with some background in
linear algebra, had to use some online equation solver. I've no interest in spending more time trying to derive this
algebra for a coding competition, so I found some code for part 2 and I just used it.

Part 1 was fun, but it's been completely overshadowed by the frustration of part 2.

## Part One

Without having taking math classes in about 25 years, I winged my solution to this puzzle, remembering only that the
equation for a straight line is `y=mx+b`, where `m` is the slope and `b` is the `y-intercept`. Everything else I
derived by hand.

Let's start with parsing the input. For each line we'll return a map with keys `:px`, `:py`, and `:pz` for the
hailstones' positions, `:dx`, `:dy`, and `:dz` for their velocities (I know the example uses `vx`, `vy`, and `vz`, but
I found using `d` for `delta` to be more intuitive), and `:slope` and `:y-intercept` for those values.

```clojure
(defn parse-line [line]
  (let [[px py pz dx dy dz] (map parse-long (re-seq #"-?\d+" line))
        slope (/ dy dx)]
    {:px    px, :py py, :pz pz, :dx dx, :dy dy, :dz dz,
     :slope slope :y-intercept (- py (* px slope))}))

(defn parse-input [input]
  (map parse-line (str/split-lines input)))
```

Since the slope is the so-called "rise over run," that's just the change in `y` over the change in `x`, or
`(/ dy dx)`. And knowing that `y=mx+b`, that means `b=y-mx`, so `:y-intercept` is `(- py (* px slope))`.

With that ready, we're going to need a few helper functions. The goal is that we'll take in pairs of parsed lines and
determine if and where they meet. If they do intersect, check that the intersection is within the permitted boundaries
and occurs in the future before considering it a good collision.

```clojure
(defn line-intersections [{m0 :slope, b0 :y-intercept} {m1 :slope, b1 :y-intercept}]
  (when (not= m0 m1)
    (let [x (/ (- b1 b0) (- m0 m1))
          y (+ (* m0 x) b0)]
      [x y])))

(defn cross-in-boundary? [line1 line2 low high]
  (when-let [[x y] (line-intersections line1 line2)]
    (and (<= low x high)
         (<= low y high)
         (= (signum (:dx line1)) (signum (- x (:px line1))))
         (= (signum (:dx line2)) (signum (- x (:px line2)))))))
```

Ok, how did we get to the definition of `line-intersections`? With a small amnount of math. Knowing that the two lines
are hitting the same point, and `y1=m1*x1+b1` and `y2=m2*x2+b2`, we know that `y1=y2` at the intersection, so the other
two sides must be equivalent. Then knowing that `x1=x2` for the same reason, we can solve the right sides of both
equations for `x`.

```
m1*x + b1 = m2*x + b2
x*(m1 - m2) = b2 - b1
x = (b2 - b1) / (m1 - m2)
```

The function knows the y-intercepts (`b`) and slopes (`m`) values for both functions, so we plug them in and then we
could have used either function to plug in `m`, `x`, and `b` to get to `y`.

Now for `cross-in-boundary?`, we first get the `[x y]` coordinates of the intersection using `line-intersections`, and
we verify that both fall within the range of the `low` and `high` arguments. I'm sure there was a more clever way to
determine whether the intersection is in the future rather than the past by solving for some time index `t`, but I
decided to check the sign of each line's `dx` to how its value changed from `px` to the intersecting `x`. An increasing
`dx` value would increase its value to get from `px` to `x` in the future, and so on.

Before getting back to the main part of the puzzle, I did create one more function in the 
`abyala.advent-utils-clojure.core` namespace, called `drop-until`. Similar to my `take-until` function, this one drops
all values in the incoming collection that fail the predicate, as well as the first one that passes it, returning all
values after that point. So the core language function `(drop-while odd? [1 3 5 6 7])` returns `[6 7]`, but my
`(drop-until even? [1 3 5 6 7])` function returns `[7]`.

```clojure
; abyala.advent-utils-clojure.core namespace
(defn drop-until [pred coll]
  (cond
    (empty? coll) ()
    (pred (first coll)) (rest coll)
    :else (recur pred (rest coll))))
```

So now we're ready to do the last part of `part1` - we pair together every unique combination of hailstones and count
how many combinations cross.

```clojure
(defn part1 [low high input]
  (count-when (fn [[line1 line2]] (cross-in-boundary? line1 line2 low high))
              (unique-combinations (parse-input input))))
```

Originally, part1 used a function called `all-line-pairs` to pair together the elements in a collection, but then I
implemented `unique-combinations` in the `abyala.advent-utils-clojure.core` namespace, so that's what `part1` ends up
using instead.

Then `part1` looks over all line pairs of the parsed input, and checks if `cross-in-boundary?` passes for the two
lines within their given `low` and `high` values, as specified by the test case.

## Part Two

Knock yourself out if you want to work on the math yourself. A lot of people used the `z3` library, but I didn't want
to install anything and the online service didn't seem to return in a reasonable amount of time. Instead, I found
someone who had used [Sagemath](https://sagecell.sagemath.org/) and showed how to solve simultaneous equations, so I at
least did the metaprogram which generates the output string to send to Sagemath.

The output declares 9 values to be derived, being the triordinates of the starting position and velocity, as well as
the time indices when the rock would hit the first three hailstones. Then, taking line0 and its `x` value as an
example, it passes the equation `x0 + dx0*t0 == xr + dxr*t0`, where `xr` is the x-position of the rock and `dxr` is
the velocity of the rock over x. Then the rest just says to solve the 3 equations (and 9 assertions) and return the
sum of `x`, `y`, and `z`.

If you also don't want to work through this insanity yourself, just plug your data values into this block of text,
paste it into Sagemath, and be done with it. From your puzzle data, paste your values in for the nine `px` values as 
the first clause, and the `dx` values as the second.

```
var('t0', 't1', 't2', 'x', 'y', 'z', 'vx', 'vy', 'vz')
a = solve([ px0 + dx0*t0 == x + vx*t0,
            px1 + dx1*t1 == x + vx*t1,
            px2 + dx2*t2 == x + vx*t2,
            py0 + dy0*t0 == y + vy*t0,
            py1 + dy1*t1 == y + vy*t1,
            py2 + dy2*t2 == y + vy*t2,
            pz0 + dz0*t0 == z + vz*t0,
            pz1 + dz1*t1 == z + vz*t1,
            pz2 + dz2*t2 == z + vz*t2,
], x, vx, y, vy, z, vz, t0, t1, t2,
solution_dict=True)
print(a[0][x] + a[0][y] + a[0][z])
```
