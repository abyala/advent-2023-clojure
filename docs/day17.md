# Day 17: Clumsy Crucible

* [Problem statement](https://adventofcode.com/2023/day/17)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day17.clj)

## Intro

I enjoyed this puzzle, even though my solution isn't especially fast (part 2 with the full data set takes about 20
seconds to execute). I suspect I'll read some other solutions and do a rewrite/refactor to match these faster
approaches, but I'm still happy that I came up with a solution on my own and even got to use one of my favorite
algorithms in the process!

### A* Algorithm

The [A* search algorithm](https://en.wikipedia.org/wiki/A*_search_algorithm) is a method of walking through a search
graph in priority order. The foundation is that each option to investigate next has a total estimate, defined as the
sum of the cost to reach that point plus an estimate to get from that point to the end. The big rule is that the
remaining estimate must never be less than the actual remaining cost, so the estimate must either be exactly correct
(not really an estimate) or be somewhat larger than the actual solution. With that estimate in place, by always
choosing the next value with the lowest estimate, the search should complete faster than with a simple breadth-first
or depth-first search, depending on the problem domain.

## Part One

We are given a grid of numbers, which represent the "heat loss" it takes to move into a space on a factory floor. Going
from the top-left to the bottom right corner, we need to find the shortest path. However, our method of transportation
limits requires taking between 1 and 3 steps in a direction before being forced to rotate 90 degrees.

Let's start with some simple utility declarations, some of which looking familiar from the `day16` puzzle. I'm reusing
the `up`, `down`, `left`, and `right` values from the `abyala.advent-utils-clojure.point` namespace, even though it's
always a little unclear if `up` should be `[0 1]` or `[0 -1]`. I also reuse the `move` function from that namespace,
even though I only added it after solving day 18.

```clojure
(defn turn90 [dir] (if (#{up down} dir) [left right] [up down]))
```

`turn90` defines how we can turn when we're done taking our 1-3 steps, and again it's nice and Clojurey - if the
direction `dir` is in the set of `#{up down}`, then the options are `[left right]`, or else it's `[up down]`. I always
enjoy using sets and maps as functions for `if` and `when` checks. And then `move` just adds together two numeric
vectors, ostensibly the first being the starting position and the second being the `[dx dy]` amount to move.

Now let's parse the input, and again like in day 16, we'll make a map of `{:points (), :target}` where `:target` is
derived from the bounding box of the data set; the bounding box is `[p0 p1]` for the lowest and highest coordinates,
and we'll want `second` to get the highest.

```clojure
(defn parse-input [input]
  (let [points (p/parse-to-char-coords-map (comp parse-long str) input)]
    {:points points, :target (-> points keys p/bounding-box second)}))
```

The only thing to note is that my `parse-to-char-coords-map` function has two arities - a 1-arg arity with just the
input string, and a 2-arg arity with a function to apply to each value before inserting it into the map. We use
`(p/parse-to-char-coords-map (comp parse-long str) input)` to convert each value from a character to a String, and to
then call `parse-long` on it, such that the output `:points` map is of form `[[x y] n]`.

Now it's time to create our A* estimate function, `estimate-step.`

```clojure
(defn estimate-step [island p cost-so-far]
  (+ cost-so-far (p/manhattan-distance p (:target island))))
```

Well this isn't surprising - the estimate for any point on the island (the grid) is the `cost-so-far` to get there,
plus the Manhattan Distance to the end. We can only move up, down, left, or right, and each movement costs has a move
value of at least 1, so the minimum cost would be a 1 in every step going directly to the end. Note that I did try to
be fancy and "punish" moving up or left, since moving up necessarily means at least 2 more steps to move to the side
and back down, or moving left means at least 2 more steps to move down and back to the right. But this did not impact
the overall performance, so I dropped it.

```clojure
(defn estimate-step [island p cost-so-far]
  (+ cost-so-far (p/manhattan-distance p (:target island))))
```

There's nothing to say about that function, so we'll move forward. As an usual step for me, I created a factory method 
for making a search `option`, since it means the rest of the code isn't peppered with the creation of little maps with
the same keys. We define an option as `{:p, :dir, :cost, :estimate}` where `:p` is the point from which we are moving,
`:dir` is the direction, `:cost` is the cost it took to get to that point, and `:estimate` is the total estimate for
that option, including both its cost-to-date and the estimate for going forward.

```clojure
(defn option-of [island p dir cost] 
  {:p p, :dir dir, :cost cost, :estimate (estimate-step island p cost)})
```

Ok, we're making good progress. Let's create the function `move-step-range`, which takes in the `island` and an
`option` being considered, and returns all possible options to take after that point. We'll also take in the
`min-steps` and `max-steps` values of 1 and 3, since that defines how far we can/must move before we have to rotate.
We'll need this to be flexible in part 2.

```clojure
(defn walk-from-option [island option]
  (let [{:keys [p dir cost]} option]
    (letfn [(next-step [from-p from-c] (let [p' (p/move from-p dir)
                                             cost' (get-in island [:points p'])]
                                         (when cost'
                                           (cons (option-of island p' dir (+ from-c cost'))
                                                 (lazy-seq (next-step p' (+ from-c cost')))))))]
      (next-step p cost))))

(defn move-step-range [min-steps max-steps island option]
  (let [turns (turn90 (:dir option))]
    (->> (walk-from-option island option)
         (take max-steps)
         (drop (dec min-steps))
         (mapcat (fn [opt] (map #(assoc opt :dir %) turns))))))
```

I originally wrote this as one complicated function, but then decided to break it into two smaller ones. First,
`walk-from-option` creates a theoretically-infinite list of options we can take from walking in the direction from a
given `option`. It essentially calls an internal function `next-step` given the previous step's position and cost, 
calculates what would be the next position and cost (if the next position is still on the island), and generates a
lazy sequence from that next point through the next ones. Then `move-step-range` calls `walk-from-option`, taking
`max-steps` for the longest number of steps we're allowed to move, and then `(drop (dec min-steps))` to make sure we
move at least the minimum number of steps. Finally, since each `option` returned from `walk-from-option` remained in
the same direction as the original `option`, we must set each option to one of the two directions that `turn90`
returns, and `mapcat` them to return all possible next options to consider.

With two more helper functions, we're ready to implement `part1`.

```clojure
(defn initial-options [island]
  (let [c (fn [opt1 opt2] (compare ((juxt :estimate :cost :p :dir) opt1)
                                   ((juxt :estimate :cost :p :dir) opt2)))]
    (into (sorted-set-by c) (map #(option-of island origin % 0) [right down]))))

(defn part1 [input]
  (let [island (parse-input input)
        cache-key (fn [{:keys [p dir]}] [p dir])]
    (loop [options (initial-options island), seen #{}]
      (when-let [option (first options)]
        (cond
          (= (:p option) (:target island)) (:cost option)
          (seen (cache-key option)) (recur (disj options option), seen)
          :else (recur (disj (apply conj options (move-step-range 1 3 island option)) option)
                       (conj seen (cache-key option))))))))
```

First, `initial-options` returns the starting options for moving either to the right or down from the origin point.
Now we know we're using an A* algorithm, so the proper response for this function is a sorted set of the two initial
options. To make an effective comparator for the sorted set, we'll compare all four fields of an option, in priority
order - the estimate, cost, point, and direction. All that really matters is the estimate, but we can't only compare
by the estimate or else the set would discard all but one path that led to the same estimate.

Then `part1` includes our search function. It parses the input and makes an internal function `cache-key` that will be
used for remembering which combination of points and directions have already been reviewed. Then we start looping over
the options, again in priority order of the sorted set we got from `initial-options`. If we ever reach the target, we
took the shortest path there, since every remaining option must take at least as long as this one (since the estimate
is never smaller than the cost and we chose the smallest estimate). If we've already seen this point and direction,
we skip it for the same reason - any other path we took to get to this point was by definition cheaper than this one.
Finally, if we haven't reached the destination, we add the new options from `move-step-range` to the known options,
and then remove the one we just took, caching it.

That's it!

## Part Two

Because of the way we implemented part 1, part 2 is a simple matter of changing the values for `min-steps` and
`max-steps`. We'll extract this logic out of `part1` into a common `solve` function and call it a win.

```clojure
(defn solve [min-steps max-steps input]
  (let [island (parse-input input)
        cache-key (fn [{:keys [p dir]}] [p dir])]
    (loop [options (initial-options island), seen #{}]
      (when-let [option (first options)]
        (cond
          (= (:p option) (:target island)) (:cost option)
          (seen (cache-key option)) (recur (disj options option), seen)
          :else (recur (disj (apply conj options (move-step-range min-steps max-steps island option)) option)
                       (conj seen (cache-key option))))))))

(defn part1 [input] (solve 1 3 input))
(defn part2 [input] (solve 4 10 input))
```

So yeah, it's not screaming fast, but I'm good with it.