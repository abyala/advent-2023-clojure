# Day 22: Sand Slabs

* [Problem statement](https://adventofcode.com/2023/day/22)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day22.clj)

## Intro

This was a good puzzle, although I had to make a small refactoring to my part 2 solution when I realized that the
Clojure "everything is just data" mantra fell apart in the presence of a small amount of "object" identity. My solution
isn't screaming fast, taking about 30 seconds for part 2, but I'm fine with it.

## Part One

We are given a list of strings that represent bricks, each with several blocks in a straight line, and we need to play
a little bit of Tetris to see how they fall. It turns out the simplest way I found to think about bricks was to
immediately transform each of them into the set of cubes that compose them. So let's parse the input into a vector
of cube sets.

```clojure
(defn parse-brick-points [line]
  (let [[x0 y0 z0 x1 y1 z1] (split-longs line)]
    (set (for [x (range x0 (inc x1))
               y (range y0 (inc y1))
               z (range z0 (inc z1))]
           [x y z]))))

(defn parse-input [input] (mapv parse-brick-points (str/split-lines input)))
```

`parse-input` just splits the input string by line, mapping each line to `parse-brick-points`. Note that we use `mapv`
instead of `map` since we'll want to remember which brick is which, and we'll do that by always keeping them in a
vector. `parse-brick-points` ignores the commas and tilde and instead just calls `split-longs` to get back the 6 long
values that represent the starting and ending cube dimensions of the brick. Then we use some list comprehension to
derive all cubes in the brick for all possible `x`, `y`, and `z` values, realizing that 2 out of 3 dimensions will only
have a single value. Finally, the cubes that compose a brick exist within a `set` for easier comparisons later.

A large portion of this logic comes down to dropping bricks as far down to the bottom of the provided space as
possible, so the `drop-bricks-to-ground` and `drop-brick` function come into play here. I spent a lot of time trying
to optimize this, but every so-called improvement didn't make much of a difference, so I'm keeping my original
implementation.

```clojure
(defn grounded? [brick] (some #(= (% 2) 1) brick))
(defn drop-brick [brick] (set (map #(update % 2 dec) brick)))
(defn drop-bricks-to-ground [bricks]
  (loop [state bricks]
    (let [state' (first (reduce (fn [[acc acc-union] brick-id]
                                  (let [brick (acc brick-id)
                                        brick' (drop-brick brick)
                                        other-bricks (set/difference acc-union brick)]
                                    (if (or (grounded? brick) (some other-bricks brick'))
                                      [acc acc-union]
                                      [(assoc acc brick-id brick') (set/union brick' other-bricks)])))
                                [state (apply set/union state)]
                                (range (count bricks))))]
      (if (= state state') state (recur state')))))
```

First we introduce two helper functions. `grounded?` returns `true` if the provided brick is on the lowest allowed
level, meaning where any `z` ordinate is equal to 1. It looks funny, but `(some #(= (% 2) 1) brick)` is equivalent to
`(some (fn [[x y z]] (= z 1)) brick)`. Recall that a Clojure vector is a map of the index to the value at that index.
So if the lambda function receives a single argument (`%`) that can be used as a map, then the map at position 2
(0-indexed) would be the `z` ordinate. The second helper function is `drop-brick`, which lowers the `z` ordinate for
all points within the brick. Again, `(map #(update % 2 dec) brick)` says to take the third value in each vector and
decrement it.

Now `drop-bricks-to-ground` takes the vector of bricks and tries to drop them, which it will do as long as at least
one brick drops. Since we don't know which bricks should drop before the others, we'll try to drop each of them once,
and if any of them did drop successfully, we'll try them all again. The function does a `loop-recur`, binding `state`
to the initial `bricks`, and then running a `reduce` over it. (I see now that one of my so-called optimizations
survived the last refactorings, but I don't feel like ripping it out to see if the performance changes.) The `reduce`
looks at each brick ID  in the vector, and runs a reduction over a tuple of the state (vector of bricks) and a set of
all points connected to any brick. For each `brick-id`, calculate `brick'` for its position if it were to be dropped,
and `other-bricks` to remove the current brick's cubes from the `acc-union` set of all bricks, to identify where every
_other_ brick is. Then check to see if either the brick was already on the ground or if any other brick occupies the
space where the dropped brick wants to go. If so, don't block the brick, and return the same accumulator as was passed
in. If neither of those conditions hold, the brick successfully drops one level, so associate in the new location of
the brick into `acc`, and `union` the new brick's cubes into `other-bricks`. One the `reduce` is complete and `first`
pulls away just the new `bricks'` vector and abandons the set, we check to see if the old `state` is the same as the
new `state'`, meaning that nothing dropped. If so, then we're done and return `state`. If not, `recur` and try again.

Now there are multiple of ways to get from here to the end of part 1, but given some knowledge about how part 2 will
go, here's a reasonable algorithm to find the number of bricks that could be disintegrated. First, create a function
`supporting-brick-ids` which takes in a `brick-id` and returns the IDs of the bricks keeping it from falling. Then,
implement `critical-brick-ids` to provide the IDs of all bricks what are singularly holding up another brick. Finally,
return the number of bricks that are not "critical."

```clojure
(defn supporting-brick-ids [bricks dead-brick-id]
  (->> (drop-brick (bricks dead-brick-id))
       (mapcat (fn [cube] (keep-indexed (fn [brick-id brick] (when (and (not= brick-id dead-brick-id) (brick cube))
                                                               brick-id))
                                        bricks)))
       set))

(defn critical-brick-ids [bricks]
  (set (keep (fn [brick-id] (let [supporters (supporting-brick-ids bricks brick-id)]
                              (when (= (count supporters) 1)
                                (first supporters))))
             (range (count bricks)))))

(defn part1 [input]
  (let [bricks (drop-bricks-to-ground (parse-input input))
        critical (critical-brick-ids bricks)]
    (- (count bricks) (count critical))))
```

`supporting-brick-ids` takes in the `bricks` and the `dead-brick-id`, and looks to see which bricks are below it. The 
easiest way to do that is to attempt to drop the brick and see with which bricks it collides. So first, we call
`drop-brick` and then `mapcat` each of its cubes on a function that returns the `brick-id` of any other brick that
contains the cube. Finally, `set` returns the distinct set of such colliding bricks.

Then `critical-brick-ids` looks over the range of `brick-ids` and for each one it finds the `supporters` from
`supporting-brick-ids`. If there's only one supporter for a brick, that means the one below is "critical," so it
returns that one and only value from the `supporters` set. Once again, the function returns a `set` in case a critical
brick is holding up multiple bricks.

Finally, `part1` parses the input and drops the bricks down, finds the critical blocks, and subtracts them from the
total count of `bricks` to get our answer.

## Part Two

There's not much to do here considering how we solved part 1. We need to find the number of bricks that would drop if
we were to disintegrate each other block. For this, we'll implement `num-cascading-bricks`, which takes in all the
`bricks` and the `dead-brick-id` for calculation.

```clojure
(defn num-cascading-bricks [bricks dead-brick-id]
  (let [start-state (apply conj (subvec bricks 0 dead-brick-id) (subvec bricks (inc dead-brick-id)))
        end-state (drop-bricks-to-ground start-state)]
    (count-when false? (map = start-state end-state))))
```

The function first records the state of the `bricks` without the disintegrated one, but combining the two subvectors
before and after the index of the one being removed. Then it calls `drop-bricks-to-ground` on that state. Finally, it
counts the number of indexes in the before and after vectors to see which ones are different. Note that it is not
sufficiently simply maintain a set of bricks to compare before and after, because one brick may fall exactly into the
position where the previous one was, and that would skew the numbers. Speaking from experience...

It's time to finish it.

```clojure
(defn part2 [input]
  (let [bricks (drop-bricks-to-ground (parse-input input))]
    (transduce (map #(num-cascading-bricks bricks %)) + (critical-brick-ids bricks))))
```

No surprises here - parse and drop the bricks, and transduce over the critical brick IDs. For each one, map it to the
number of cascading bricks, and sum together the results.

Ok, ok, we'll make a unified `solve` function. After parsing the bricks and the critical brick IDs, the `solve`
function invokes the passed-in function to get to the answer.

```clojure
(defn solve [f input]
  (let [bricks (drop-bricks-to-ground (parse-input input))
        critical (critical-brick-ids bricks)]
    (f bricks critical)))

(def part1 (partial solve (fn [bricks critical-ids] (- (count bricks) (count critical-ids)))))
(def part2 (partial solve (fn [bricks critical-ids]
                            (transduce (map #(num-cascading-bricks bricks %)) + critical-ids))))
```