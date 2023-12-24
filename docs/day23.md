# Day 23: A Long Walk

* [Problem statement](https://adventofcode.com/2023/day/23)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day23.clj)

## Intro

Whew! I wrote and rewrote this problem multiple times until I finally found an algorithm that would return sometime
before we all turn to dust. I'm happy with my final data structure, although I know I'm doing something inefficient
with the searching approach, but I'm much too tired to try and optimize it further. Maybe someday in the future when I
fondly look back on this year of Advent, I'll figure out where I'm doing extra work. Still, this was an interesting
puzzle and I enjoyed the challenge.

## Part One

We're given a map with multiple paths we can follow down a hiking trail, and we need to find the longest non-repeating
path from the start to the end. It's unusual to get longest-path algorithms, since I believe those are of NP
complexity, but even though this was complicated, it wasn't complex.

The general approach we will take is to go through the path twice, once to simplify the trail and a second time to
find the longest path. In the simplification phase, we'll start walking from the starting position until we reach a
junction, meaning a space that connects to the previous space and more than one more. Once we reach such a junction,
we will consider the previous segment to be complete, and then start looking at future junctions, until the entire
trail becomes a simple set of weighted path segments. As we're mapping, if we reach a spot we cannot reach because of
the slope of the snow, we abandon that path.

### Parsing

First, we're going to parse the input into our usual coordinate map, filtering out any impassible forests. Because we
know that in part 2 we'll need to make modifications to the input characters, the `parse-trail` function will take in
a transformation function to use, which we'll start off with `identity` for part 1.

```clojure
(defn parse-trail [parse-xf input]
  (reduce-kv (fn [m p c] (if (not= c \#) (assoc m p (parse-xf c)) m))
             {}
             (p/parse-to-char-coords-map input)))
```

We'll call our usual `parse-to-char-coords-map` on the input, and pass the resulting key-value pair of `[coords c]` to
a `reduce-kv` function call. It filters out the forests with `(not= c \#)`, and if it's an acceptable space, it
associates the coordinates to the result of calling the transformation function `parse-xf` on the input character.

Next, we'll knock out two utility functions -- identifying the start and end positions of the trail, and determining
whether we can reach a point on the trail _from a given direction_.

```clojure
(defn start-and-end [trail] ((juxt first last) (sort-by second (keys trail))))

(defn accessible? [trail p approaching-dir]
  (when-let [c (trail p)]
    (not= c ({east \< west \> south \^ north \v} approaching-dir))))
```

`start-and-end` is a simple enough one. Given the keys of the trail, where the keys are the coordinates, sort them by
their `y` values, and extract the first and last elements of the sorted list using `juxt`.

`accessible?` isn't all that bad. Given the parsed `trail`, the point `p`, and the `approaching-dir` taken to reach
that point, it returns whether the point is accessible or not. First `(trail p)` checks if the point is even in the
trail, and then the `(not= ...)` function makes sure any slope is compatible with the direction. They should all make
sense; traveling `east` would not work if the slope points `<`, and traveling `south` would not work if the slop points
`^`. Flat paths of type `.` always pass this function if they're on the trail.

### Simplifying the trail

The bulk of the complexity comes from simplifying the trail from a grid to a map of `{from {to distance}}` map of maps.
To start, we'll implement `next-steps`, which takes in a parsed trail, the starting position for the segment to map,
and the direction to travel. It will return a map of the last step in the segment, the distance traveled, and all
other directions to attempt to travel from the end of the path segment.

```clojure
(defn next-steps [trail segment-start dir]
  (loop [p (p/move segment-start dir), dir dir, n 1]
    (let [[{:keys [next-p next-dir]} :as next-steps]
          (keep #(let [p' (p/move p %)]
                   (when (and (not= % (reverse-dir dir)) (accessible? trail p' %)) {:next-p p' :next-dir %}))
                [north south east west])]
      (if (= 1 (count next-steps))
        (recur next-p next-dir (inc n))
        {:last-step p, :dist n, :next-dirs (map :next-dir next-steps)}))))
```

The function is a loop of point `p`, the `dir` currently being traveled (since the segment can include turns), and the
distance traveled in the form `n`. At each step, the function looks at `[north south east west]` and keeps only those
directions that (1) don't match the direction already taken (to avoid stepping back and forth between two points), and
which result in an accessible point on the trail. If there's only one such step to be taken, then the path is continuing
and we simply `recur` from the next point and direction, incrementing the distance traveled. If there's any other
number, then the segment is complete, and we return the last value for `p` and `n`, as well as the other directions
we will need to inspect in future path segments that start from `p`. Note that we expect to eventually end up at the
trail's end, so we can't abandon path segments that dead end.

With that in place, it's time to build `all-paths`, which takes in a `trail` and returns the map we discussed above.

```clojure
(defn all-paths [trail]
  (let [[start] (start-and-end trail)]
    (loop [options [{:p start :dir south}], seen #{}, segments {}]
      (if (seq options)
        (let [{:keys [p dir]} (first options)]
          (if (seen [p dir])
            (recur (rest options) seen segments)
            (let [{:keys [last-step dist next-dirs]} (next-steps trail p dir)]
              (recur (apply conj (rest options) (map #(hash-map :p last-step :dir %) next-dirs))
                     (conj seen [p dir])
                     (map-conj segments p [last-step dist])))))
        segments))))
```

`all-paths` is another `loop-recur` function, which looks at every segment start we still need to examine before
returning the set of all completed path segments. It also holds on to a cache of `[p dir]` paths we already examined, 
to avoid infinite loops. For each option, which itself is a `[p dir]` tuple, it calls `next-steps` to determine how the
segment starting at that position and direction ends. Then it recursively calls itself by adding new options from the
`next-steps` results, filling the cache, and storing the completed segments. The `map-conj` function is one I wrote a
few years ago and is now part of the `abyala.advent-utils-clojure.core` namespace. It makes sure that
`(get segments p)` contains a map, initializing it if it was previously `nil`, and then associates `p` to
`[last-step dist]`.

Alright, we're ready to write the `part1` function!

```clojure
(defn part1 [input]
  (let [trail (parse-trail identity input)
        [start end] (start-and-end trail)
        paths (all-paths trail)]
    (loop [options [[start #{} 0]], best 0]
      (if (seq options)
        (let [[[p seen n] & x-options] options]
          (cond (= p end) (recur x-options (max best n))
                (seen p) (recur x-options best)
                :else (recur (apply conj x-options (map (fn [[p' dist']] [p' (conj seen p) (+ n dist')])
                                                        (paths p))) best)))
        best))))
```

This function is a little messy, but it gets the job done. We parse the trail using the `identity` function, grab the
`start` and `end` points of the trail, and then calculate `all-paths`. Then we start our `loop-recur` to find the
longest path to the `end`, starting with a single option that starts at `start`, has seen no intersections, and has a
total distance traveled of 0. For each option, if it has reached the `end`, then keep the greater of its distance or
the longest distance already found. If the path has already seen the point, skip that option because it looped. 
Otherwise, check `paths` for all segments that are accessible from this point, and add them to the `options`
collection, incrementing their distances by that already traveled along the trail so far. When we're all done, just
return the `best` distance found among the options.

## Part Two

It _looks_ I'm trivializing the problem because of how I structured the code, and forgetting the fact that it takes
about 40 seconds to run on my computer. But the algorithm for part 1 should work for part 2 if we replace all slopes
with flat paths. Then we're just looking again for the longest path to the end. So the only real work to do is to 
build our `solve` function, rip out most of the code from `part1`, and implement `part2`.

```clojure
(defn solve [parse-xf input]
  (let [trail (parse-trail parse-xf input)
        [start end] (start-and-end trail)
        paths (all-paths trail)]
    (loop [options [[start #{} 0]], best 0]
      (if (seq options)
        (let [[[p seen n] & x-options] options]
          (cond (= p end) (recur x-options (max best n))
                (seen p) (recur x-options best)
                :else (recur (apply conj x-options (map (fn [[p' dist']] [p' (conj seen p) (+ n dist')])
                                                        (paths p))) best)))
        best))))

(defn replace-slopes [c] (if (#{\> \< \^ \v} c) \. c))
(defn part1 [input] (solve identity input))
(defn part2 [input] (solve replace-slopes input))
```

The `solve` function is identity to the previous `part1` function, except that it takes in the `parse-xf` function
argument, which it uses to call `parse-trail`. That's it. Then `part1` calls `solve` using the `identity` function, as
before.  For `part2`, the parsing transformation function is `replace-slopes`, which replaces any of the four slope
characters with a `.` path character. That's it!

So the `all-paths` logic is lightning fast but there's something about my `solve` function that's inefficient. Maybe
I'll figure it out after reading other people's solutions. But for now, this is relatively succinct, so I'll take my
two stars and move on!
