# Day 16: The Floor Will Be Lava

* [Problem statement](https://adventofcode.com/2023/day/16)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day16.clj)

## Intro

This was another straightforward puzzle today. I've waffled several times between parsing the cave as a full data
structure or keeping it as a simple map, and in the end I decided to stick with my original plan and just use a map
of points.

## Part One

The input we are given is a cave (grid) with empty spaces (dots), mirrors (slash and backslash), and splitters (pipe 
and dash). We need to determine how beams of light will move through the cave.

Before parsing, let's think about how to model our data. Traditionally I would make keywords for `:up`, `:down`, 
`:left`, and `:right`, mapping them to their `[dx dy]` coordinates when it's time to move, but this time I'm just going
to have simple vars that point to those coordinates. So let's get some simple definitions out of the way.

```clojure
(def up [0 -1])
(def down [0 1])
(def right [1 0])
(def left [-1 0])
(defn move [p dir] [(mapv + p dir) dir])
```

Since we parse from the top down, `up` has a negative `y` value, while `down` has a positive one. Calling `move` on
a point `p` facing direction `dir` involves just adding the two values together with `(mapv + p dir)`. We need to
retain the direction of travel, so `move` returns a vector of the new point and the original direction `[p' dir]`.

Now there's no need to parse the cave, since `p/parse-to-char-coords-map` will return a map of `{[x y] c}`, which is
all we need, so instead we'll focus on moving a point through the cave. For this, we'll implement the `next-steps`
function, which takes in the cave, a point, and its direction, and returns a +sequence+ of the next points-direction
combos where the beam would next go.

```clojure
(defn next-steps [cave p dir]
  (let [target (cave p)]
    (filter #(cave (first %)) (case target
                                \. [(move p dir)]
                                \/ [(move p ({up right, down left, left down, right up} dir))]
                                \\ [(move p ({up left, down right, left up, right down} dir))]
                                \| (if (#{up down} dir) [(move p dir)]
                                                        [(move p up) (move p down)])
                                \- (if (#{left right} dir) [(move p dir)]
                                                           [(move p left) (move p right)])))))
```

First, we call `(cave p)` to figure out what's exists in the cave at point `p`. The function just returns
`(filter #(cave (first %)) ...)`, so ensure that it only sends out steps that reside within the cave. Then to know
which next step(s) the beam can take, we use the `case` on the `target`. Empty spaces do not impact the beam, so just
call `move` through it. Mirrors (`/` and `\`) change the direction of the beam based on the incoming direction, so
move `p` in a new direction (moving up and hitting `/` points the beam to the right, and so on). Splitters either act
as empty spaces when the beam moves in its direction (moving up into a pipe `|`), or otherwise creates two 
perpendicular beams (moving up and hitting a dash `-`).

Now we can implement the real work of the puzzle, which is `energized-tiles` - a function that takes in the cave and
returns the number of energized tiles after the beams have run their course. We'll find out in part 2 that this
function also needs to take in the starting point and direction, so we'll just implement that now.

```clojure
(defn energized-tiles [cave starting-point starting-dir]
  (loop [beams (list [starting-point starting-dir]), seen #{}]
    (if-some [[p dir :as beam] (first beams)]
      (if (seen beam)
        (recur (rest beams) seen)
        (recur (apply conj (rest beams) (next-steps cave p dir)) (conj seen beam)))
      (-> (map first seen) set count))))
```

This function does a `loop-recur` on the beam-directions needing to be seen, and those which have already been seen.
While there are still beams to inspect, the loop checks if it's already been seen. If so, skip it and loop again. If
not, record this beam-direction as being seen, and add to the to-inspect list the output of calling `next-steps`.
When we've evaluated every beam-direction, take the `seen` set, extract out the first value from each (since `seen`
holds `[point dir]` and we only care about the points now), and count the number of unique values.

Finally, we implement `part1`.

```clojure
(defn part1 [input] (energized-tiles (p/parse-to-char-coords-map input) [0 0] right))
```

Hey, it's not a transducer for once! Look at us, branching out! We call `parse-to-char-coords-map` on the input, and
computer the number of energized tiles starting from the origin and heading right.

## Part Two

We now need to maximize the number of energized tiles by picking the optimal starting position and direction from all
points on the perimeter of the cave. Luckily, there's very little work to be done, as we just need to call
`energized-tiles` with all possible starting options and pick the best one.

```clojure
(defn starting-options [cave]
  (let [[_ [max-x max-y]] (p/bounding-box (map first cave))
        point-dir-range (fn [[range-x range-y dir]] (map vector (map vector range-x range-y) (repeat dir)))]
    (apply concat (map point-dir-range [[(repeat 0) (range 0 (inc max-y)) right]
                                        [(repeat max-x) (range 0 (inc max-y)) left]
                                        [(range 0 (inc max-x)) (repeat 0) down]
                                        [(range 0 (inc max-x)) (repeat max-y) up]]))))
```

To start, we'll call our good friend `p/bounding-box` to find the min and max coordinates in the grid; we know that the
min is always `[0 0]` so we'll ignore that. We'll also use an inner function `point-dir-range`, which takes in a
range of `x` and `y` values and the `dir` to use, and returns a vector of zipping together `x` and `y` values together
with the `dir`. It just keeps the rest of the code smaller. Then the function returns the concatenation of calling
`point-dir-range` for the left side pointing right (x=0, y is a range), right side pointing left (x=max-x, y is still
a range), etc.

Armed with that, we can implement part 2.

```clojure
(defn part2 [input]
  (let [cave (p/parse-to-char-coords-map input)]
    (transduce (map (fn [[p dir]] (energized-tiles cave p dir))) max 0 (starting-options cave))))
```

Silly me - of course there would be a transducer before long! After parsing the input, we transduce across all starting
options, calling `energized-tiles` for each and finding the max value.

And... yeah, let's combine parts 1 and 2 into common code. The `solve` function expects a function `f` that maps the
parsed `cave` to a list of starting options.

```clojure
(defn solve [f input]
  (let [cave (p/parse-to-char-coords-map input)]
    (transduce (map (fn [[p dir]] (energized-tiles cave p dir))) max 0 (f cave))))

(defn part1 [input] (solve (fn [_] [[[0 0] right]]) input))
(defn part2 [input] (solve starting-options input))
```

Part 1 always returns the single-length vector `[[[0 0] right]]`, while part 2 passes in the `starting-options`
function.