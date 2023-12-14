# Day 14: Parabolic Reflector Dish

* [Problem statement](https://adventofcode.com/2023/day/14)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day14.clj)

## Intro

Hey, I figured out a "here's an enormous data set you can't fully compute through brute force" in my first go at it!
2023 really has been a pretty terrific year!

## Part One

We are given a grid for a platform that contains movable rounded rocks (represented by `O`) and immovable
cube-shaped rocks (represented by `#`). We need to tilt the platform north such that all rounded rocks roll up until
they hit another rock, and then we compute the sum of all rounded rocks' y-axes. An interesting note here is that in
the puzzle's description, the y-axis goes up, with the bottom level of the grid having a y-axis value of 1. Usually
I parse from the top-left, meaning that the bottom-left corner is `[0 -y]` whereas now it needs to be a proper `[0 1]`.
Why is the left-most x-value a 0 instead of a 1? Because I'm used to zero-indexing things and I don't want to go back
and change everything. Maybe if I edit this tomorrow I'll swap it around.

In today's puzzle, a lot of the complexity shows up in the `parse-input` function for once. The goal is to read the
input string and create a map of `{:rounded #{[x y]}, :cube #{[x y]}, :max-x m, :max-y n}`. The `:rounded` and `:cube`
keywords point to sets of `[x y]` coordinates where those rocks reside, while `max-x` and `max-y` represent the
largest values of `x` and `y` ordinates for all rocks, remembering again the conversion for all `y` values.

```clojure
(defn parse-input [input]
  (let [flipped (->> input str/split-lines reverse (str/join "\n"))
        all-points (update-keys (p/parse-to-char-coords-map flipped) #(update % 1 inc))
        rounded-rocks (set (keep (fn [[p c]] (when (= c \O) p)) all-points))
        cube-shaped-rocks (set (keep (fn [[p c]] (when (= c \#) p)) all-points))
        max-x (transduce (map first) max 0 (keys all-points))
        max-y (transduce (map second) max 0 (keys all-points))]
    {:rounded rounded-rocks, :cube cube-shaped-rocks, :max-x max-x, :max-y max-y}))
```

Let's take this nice and slowly. First, let's map every character in the input string to its "correct" `[x y]`
coordinates. To read from the bottom-up, `flipped` splits the string by line, reverses them, and puts them back into
a single string again. Why do all that? Because `p/parse-to-char-coords-map` already expects a single string and for a
trivial inefficiency, I avoided writing a lot of code. Now `parse-to-char-coords-map` returns a map of `{[x y] c}`
using the corrected, upside-down `y` values we want, but they're still 0-indexed. So to fix that, we call
`(update-keys m #(update % 1 inc))`; the keys of the map are of the format `[x y]`, so `(update % 1 inc)` calls the
`inc` function on the second value (index=1) of each key. Pretty snazzy. We'll keep that binding of `all-points`
because we'll need it several timews.

Now let's prepare each value in our map. `rounded-rocks` and `cube-shaped-rocks` both sift through `all-points`, 
checking when the character value `c` is either `\O` or `\#`, keeping just the point `p` in each case. Both values 
become sets for easier retrieval later. Then both `max-x` and `max-y` call `transduce` on every set of coordinates,
extracting out either the `x` or `y` values (`first` and `second` respectively), and calling `max` on the results.
Finally, the function just constructs the actual map.

This function was much more confusing before, running as one big honking thread-first macro, but I'm deleting that
monstrosity without leaving any evidence of it other than this brief eulogy.

The strategy for sliding all rocks north is to sort the rocks by those that are the furthest north so they can slide
first, and then slide the rest after them. I had a pretty strong suspicion that we'd need to be able to support other
slide directions later... and maaaaaybe we will. But let's pretend we don't know that very obvious future will soon be
upon us.

```clojure
(defn northmost-rounded [platform]
  (sort-by (comp - second) (:rounded platform)))

(defn occupied? [platform p]
  ((some-fn (:cube platform) (:rounded platform)) p))

(defn slide-north [platform]
  (let [max-y (:max-y platform)
        slide-rock (fn [acc [x y :as p]] (if-some [p' (->> (map vector (repeat x) (range (inc y) (inc max-y)))
                                                           (take-while #(not (occupied? acc %)))
                                                           last)]
                                           (-> acc (update :rounded disj p) (update :rounded conj p'))
                                           acc))]
    (reduce (partial slide-rock) platform (northmost-rounded platform))))
```

First we have two helper functions. `northmost-rounded` takes in the platform and sorts the rounded rocks in decreasing
order of their `y` values. We don't necessarily care about sorting by the `x` value too, so `(sort-by (comp - second))`
is fine. Then `occupied?` returns whether some rock, whether rounded or cube-shaped, exists in a point in the platform.

Then `slide-north` looks like its' doing a lot, but it's not bad. After pulling out the `max-y` value we'll need as
rocks slide "up" the graph, we make a hidden function called `slide-rock`. We can see after that definition that
`slide-north` reduces over the rounded rocks in `northmost-rounded` order, calling `slide-rock` on the accumulated
`platform` and each rock. As for `slide-rock`, it looks at all points to which the target rock could possibly roll,
using `(map vector (repeat x) (range (inc y) (inc max-y)))` - the `x` ordinate of each point doesn't change, but the `y`
ordinate starts from one above the current location, all the way to one above the max value (since `max-y` is
inclusive). Then `take-while` keeps all coordinates that aren't currently occupied, and `last` finds the northernmost
coordinate where it will rest. If we find a new home for the rock, then the accumulated platform removes the current
location of `p` using `disj`, and then replaces it with the new location of `p'` using `conj`. If the rock did not
slide at all, just return the unchanged platform state.

We're just about done with part 1!

```clojure
(defn total-load [platform]
  (transduce (map second) + (:rounded platform)))

(defn part1 [input]
  (->> input parse-input slide-north total-load))
```

`total-load` takes in a platform and returns the sum of all `y` values of the rounded rocks. Yep, it's a `transduce`
function; I don't think there's much to say. And then `part1` takes the input, parses it into a platform, slides it
north, and returns the total load.

## Part Two

Woah, wait a second - we need to slide the platform in all four directions. Who saw that coming! I think we can
handle this. Sliding in each of the four directions should all work the same way as `slide-north`, except in two ways:
we need to sort the rocks differently (for instance, `slide-north` sorted in decreasing `y` values, while `slide-west`
should sort in increasing `x` values), and we need to create the sequence of available coordinates differently
(`slide-north` locks `x` while `y` ranges from one above `y` to `max-y`, while `slide-west` locks `y` and `x` ranges
from one below `x` to 0).

We'll use a common `slide` function that takes in these two worker functions, and then define `slide-north`,
`slide-west`, `slide-south`, and `slide-east` to call `slide`. I considered making a Clojure protocol for the four 
slide functions, and indeed I might still do that later. But for now, these are pretty easy to construct.

```clojure
(defn- slide [rock-sorter range-creator platform]
  (let [slide-rock (fn [acc p] (let [acc' (update acc :rounded disj p)]
                                 (if-some [p' (->> (range-creator acc p)
                                                   (take-while #(not (occupied? acc' %)))
                                                   last)]
                                   (update acc' :rounded conj p')
                                   acc)))]
    (reduce (partial slide-rock) platform (sort-by rock-sorter (:rounded platform)))))

(defn slide-north [platform]
  (slide (comp - second)
         (fn [{:keys [max-y]} [x y]] (map vector (repeat x) (range (inc y) (inc max-y)))) platform))

(defn slide-west [platform]
  (slide first (fn [_ [x y]] (map vector (range (dec x) -1 -1) (repeat y))) platform))

(defn slide-south [platform]
  (slide second (fn [_ [x y]] (map vector (repeat x) (range (dec y) 0 -1))) platform))

(defn slide-east [platform]
  (slide (comp - first)
         (fn [{:keys [max-x]} [x y]] (map vector (range (inc x) (inc max-x)) (repeat y))) platform))
```

I'm actually not going to describe the above code in great detail, as it should look almost identical to the original
`slide-north` function, except where `rock-sorter` and `range-creator` are used. Note that with this new
implementation, we can discard the `northmost-rounded` function, as it the comparison resides within the new
`slide-north` function.

We learn that a "cycle" means sliding the rocks north, west, south, and then east, and we need to compute the
`total-load` after a mere 1 billion cycles. No problem.

```clojure
(defn rock-cycle [platform]
  (-> platform slide-north slide-west slide-south slide-east))

(defn nth-spin [platform target]
  (reduce (fn [[seen n] p] (if-some [loop-start (seen p)]
                             (let [loop-size (- n loop-start)
                                   target-idx (+ loop-start (mod (- target loop-start) loop-size))]
                               (reduced (ffirst (filter (fn [[k v]] (when (= v target-idx) k)) seen))))
                             [(assoc seen p n) (inc n)]))
          [{} 0]
          (iterate rock-cycle platform)))

(defn part2 [input]
  (-> input parse-input (nth-spin 1000000000) total-load))
```

It stands to reason that we're not supposed to calculate all 1 billion rock cycles, as there's bound to be a loop
somewhere in the sequence. `nth-spin` will find and use it. In fact, we're so confident there's a loop that we'll 
cavalierly call `reduce` based on `(iterate rock-cycle platform)`, which is an infinite sequence of feeding
the result of `rock-cycle` into itself, starting with the original `platform` value. The accumulated state for each
reduction will be a vector `[seen n]`. `seen` will be our cache, mapping each platform to the first cycle in which we
found it, and `n` is the current cycle number.

Each time we see a platform, we'll check whether it already exists in `seen`; if not, then place this new platform into
the `seen` cache and increment `n` for the next go-around. (Well, this is Clojure, so of course we're not modifying
`seen` or changing `n`, but that should go without saying by now.) If we do find the platform in `seen`, we've looped
to a previous state so we prepare to call `reduced` and break out of the `reduce` function. The loop doesn't necessarily
start at the first platform, meaning that `loop-start` isn't necessarily going to be zero. The `nth` platform we're
looking for comes from subtracting the start of the loop from the `target` of 1 billion, and then getting the `mod`
remainder by dividing by the size of the loop and adding the `loop-start` back in again. Now `seen` only maps from
platform to index and not vice versa; I could have mapped the values in both directions, but `seen` is so small that
we can just look for the first index where the value of the map (the index) matches what we're seeking.

Once we have that, `part2` is as simple as parsing the input, finding the one-billionth spin cycle, and calculating its
total load.