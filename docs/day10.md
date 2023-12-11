# Day 10: Pipe Maze

* [Problem statement](https://adventofcode.com/2023/day/10)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day10.clj)

## Intro

Part 2 of this puzzle was rough, and to be honest I had to go online and read some spoilers to get a solution. I tried
3 or 4 approaches that just were simply incorrect, but once I learned the trick, the code wasn't bad.

## Part One

We are a map of a maze of pipes with a starting position labeled `S`, knowing that it is part of a closed loop of
pipes. Our goal is to find the furthest number of steps from that starting point to another point in the loop. To do
this, we will walk through the loop, count the number of steps, and take half of that value as the distance furthest
away from the start.

To start, I will reuse my `abyala.advent-utils-clojure.point` namespace, and then create a bunch of convenience data
and functions in the `day10` namespace:

```clojure
(def north [0 -1])
(def south [0 1])
(def east [1 0])
(def west [-1 0])
(def connecting-dirs {\| #{north south}, \- #{east west}, \L #{north east},
                      \J #{north west}, \7 #{south west}, \F #{south east}
                      \S #{north south east west}})
(defn reverse-dir [dir] ({north south, south north, east west, west east} dir))
```

I originally tried using the keywords `:north`, `:south`, `:east`, and `:west`, but I found them to be inconvenient,
so instead I created simple constant values for the `[x y]` differences that, when added to a point, goes in that
direction. Then `connecting-dirs` is a map of each character read from the input to the set of directions the loop can
take from that point. When we start at position `S`, we don't know in which two directions it will connect, so we let
it connect to all four. Finally, `reverse-dir` flips `north` and `south` with each other, and `east` and `west` with
each other. We'll use that shortly.

Now we parse the input into a map of each coordinate to its character in the map, removing all spaces (represented as
a period).

```clojure
(defn parse-maze [input]
  (into {} (remove #(= \. (second %)) (p/parse-to-char-coords input))))
```

It's nice to have a simple parse function for once! Using `p/parse-to-char-coords`, we aleady get back a sequence of
`[coords c]` tuples. We call `remove` on all tuples where the character `c` is a space, and then pull the remaining
values into a map.

While we're at it, we'll also implement `maze-start`, which searches the maze for the coordinates of the starting
point. We'll use `first-when` to find the tuple where the character is `S`, and then call `first` to pull out the
coordinates.

```clojure
(defn maze-start [maze] (first (first-when #(= \S (second %)) maze)))
```

Now we need to construct the path of the loop, meaning the list of all points, starting with the starting point, where
compose the loop. For this, we'll use two functions - `connected-steps` will identify the two adjacent points from a
location based on the type of pipe, and `loop-path` will construct the full sequence.

```clojure
(defn connected-steps [maze p]
  (->> (maze p)
       connecting-dirs
       (keep (fn [dir-taken] (let [p' (mapv + p dir-taken)
                                   c' (maze p')]
                               (when (and c' ((connecting-dirs c') (reverse-dir dir-taken)))
                                 p'))))))

(defn loop-path [maze]
  (let [start (maze-start maze)]
    (letfn [(next-step [p previous]
              (when (not (and previous (= p start)))
                (cons p (lazy-seq (next-step (first-when #(not= % previous)
                                                         (connected-steps maze p))
                                             p)))))]
      (next-step start nil))))
```

`connected-steps` takes in the parsed `maze` and the point `p` whose adjacent coordinates we wish to receive. First,
we call `(maze p)` to see what type of pipe is at that position, and `connecting-dirs` to see to which directions it
connects. Then we'll call `keep` on those directions, using a function creates the target point `p'`, looks up its
value `c'`, and checks whether the target's connecting directions matches the opposite of the direction taken to get
there. Why go through all this trouble, when going north from a valid pipe should connect to a location that by
definition can connect back south? Because we don't know which directions the starting point `S` connects to, so only
two of the adjacent points will be compatible with the reversed direction it took to get there.

Then `loop-path` creates a nested function `next-step` to enable the construction of a lazy sequence of points. This
function gets initialized wiht the starting location of the maze and `nil` to represent the last point inspected, since
obviously there won't be any to start. When it sees either the first point (`previous` is `nil`) or any other point
that isn't the loop back to the start, it outputs the point `p` and lazily calls `next-step` with the connected step
that doesn't match the previous step (to prevent going backwards).

Now that we have the complete path of the loop, `part1` is easy to solve. All we'll do is parse the input, generate the
loop path, count the number of points, and divide it in half.

```clojure
(defn part1 [input] (-> (parse-maze input) loop-path count (quot 2)))
```

Fun times! Part 2 will leverage much of this code, but we won't be refactoring `part1` into code that's common to
`part2`, so it's here to stay.

## Part Two

For part 2, we need to identify the number of points that are enclosed within two pipes of the loop. Again, all of my
strategies to solve this failed, so I read online that the trick is to count the number of pipes to the _left_ of each
position where the pipe points north, meaning pipes with value `L`, `J`, or `|`. If the number of vertical lines to the
left is odd, then the target point is enclosed. If it's even, then it's not enclosed.

To start off, we'll need to be able to substitute the correct pipe value for `S` into the maze after we've computed
the loop path. While actually none of my test or puzzle inputs strictly needed this, the solution would not have been
universally correct without it, so let's do it.

```clojure
(defn rebind-maze-start [maze]
  (let [start (maze-start maze)]
    (assoc maze start (->> [north south east west]
                           (filter #(maze (map + start %)))
                           set
                           ((set/map-invert connecting-dirs))))))
```

`rebind-maze-start` takes in the parsed `maze` and calls `maze-start` to identify the coordinates to replace. Then it
undoes some of the logic in `connected-steps` - starting with the four cardinal directions, it filters out the ones
where the points in that direction from the start exist in the maze; remember that we filtered out all the blank
spaces. Then turning it into a set, it looks up the values in `(set/map-invert connecting-dirs)`; `map-invert` flips
a map around, binding values to keys, such that the set of directions maps to the character as it should appear in the
maze. Knowing what that value is, we call `(assoc maze start c)` to return a uniform map.

The `num-enclosed-by-line` function is the work horse for this solution, as it returns the number of enclosed points
given the algorithm described above.

```clojure
(defn num-enclosed-by-line [maze points min-x max-x y]
  (let [flip (fn [v] (if (= v :outside) :inside :outside))]
    (first (reduce (fn [[n loc :as acc] p]
                     (let [c (maze p)]
                       (cond (or (nil? c) (not (points p))) [(if (= loc :inside) (inc n) n) loc]
                             (#{\L \J \|} c) [n (flip loc)]
                             :else acc)))
                   [0 :outside]
                   (map vector (range min-x max-x) (repeat y))))))
```

First, we implement a little `flip` function to turn `:inside` into `:outside` and back again. Then we `reduce` over
all possible `[x y]` points in the maze for the statically defined value of `y` that gets passed in. Our accumulator
is of form `[n loc]` where `n` is the number of accumulated enclosed values so far in the line, and `loc` is whether
the current coordinate resides inside or outside the loop, where inside means it's enclosed. Then we look up the value
of the map at that position and do a conditional check. If the value doesn't appear in the map `(nil? c)` or it's not
one of the points of the loop `(not (points p))`, then it's a potentially enclosed value; increment `n` if `loc` is
equal to `:inside`, otherwise leave `n` unchanged. If the value points north (one of the values `L`, `J`, or `|`), then
keep `n` constant but flip the value of `loc` since the parity of the number of north-facing path values has changed.
And if neither of those have happened, then we have an inert pipe, like maybe a `-` or a `7`, so just return the
accumulator completely unchanged. After the reduce is done, call `first` to return `n` from the accumulator.

Finally, we're ready to put it all together.

```clojure
(defn part2 [input]
  (let [maze (parse-maze input)
        points (set (loop-path maze))
        [[x0 y0] [x1 y1]] (p/bounding-box points)
        maze' (rebind-maze-start maze)]
    (transduce (map #(num-enclosed-by-line maze' points x0 x1 %)) + (range y0 y1))))
```

We'll parse the maze and create the `loop-path`, immediately converting it into a set for convenience later. We'll also
call `p/bounding-box`, which returns the min and max `x` and `y` values that inclusively enclose all values within the
path. Finally, we'll rebind the starting position of `maze` and bind that to `maze'`. Then once all of that is done,
we'll transduce over every row with `(range y0 y1)`, transform each row by calling `num-enclosed-by-line`, and add
together the resulting counts. Note that the bounding box is inclusive but the second argument of `range` is exclusive;
this doesn't matter since no value on the right side of the bounding box could be enclosed by the path, since the path
couldn't close it further to the right.

So, yeah, there you have it. I'm sure someone else can explain why this works, but it does, so we're going with it!
