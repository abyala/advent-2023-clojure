# Day 15: Lens Library

* [Problem statement](https://adventofcode.com/2023/day/15)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day15.clj)

## Intro

What a delightfully simple and straightforward puzzle today! Thanks, Eric, for a little relief on day 15.

## Part One

We are given a long string of comma separated characters, for which we need to calculate a hash function whose values
we sum together. We can't implement a function called `hash` because that's part of the `clojure.core` namespace,
so we'll call it `HASH` to match the puzzle storyline.

```clojure
(defn HASH [s] (reduce #(-> %1 (+ (int %2)) (* 17) (mod 256)) 0 s))
```

There's not much to it. Because Clojure can treat a string like a sequence of characters, we can use `reduce` with an
input of `s`, initialized to `0` and then calculate through it. `(int %2)` coerces a character into its ASCII int
value, but the rest should be easy enough to read.

Now we can zoom right to the `part1` function.

```clojure
(defn part1 [input] (transduce (map HASH) + (str/split input #",")))
```

There we go. Split the input by commas, use the `(map HASH)` transducer, and add together the results. Yes, the first
part of this puzzle took 2 lines of code.

## Part Two

In this part, we no longer treat each comma-separated substring as just a bunch of characters, but rather an
instruction with two or three parts - the label of a lens, the operation (set/replace or remove), and the focal length
of a lens if the operation is to set/replace. So first, let's parse the input into a sequence of `[op label len?]`.
As usual, I want to work with magic literals as little as possible, so `op` will be either `:set` or `:remove`. Note
also that I reorder the data because after parsing the input, it makes more sense to see the operator first. Good lord,
I've been working with a LISP for too long.

```clojure
(defn parse-instructions [input]
  (map #(let [[_ label op focal-length] (re-matches #"(\w+)([=-])(\d*)" %)]
          [({"-" :remove, "=" :set} op), label, (parse-long focal-length)])
       (str/split input #",")))
```

To start, we `map` over the comma-separated strings, as in part 1. The `let` macro binds the results of using a regex
on each string, where the `label` has only word characters, the `operator` is either a dash or an equals, and `len`
is numeric. Since the first response element of `re-matches` (due to Java's `Matcher` class) is the entire string, we
skip that binding and work with the rest. `({'-' :remove, '=' :set} op)` converts the dash or equals into the keyword
we'll use later, and `parse-long` creates a numeric value from the numeric string; calling `(parse-long "")` returns
`nil`, which is fine for our purposes. I don't mind seeing `[:remove "foo" nil]` enough to force it to be a smaller
vector.

Next, we implement `process-instructions`, which takes in a sequence of instructions and returns the map of the box
number (from 0 to 255) to its vector of lenses, each of form `[label focal-length]`.

```clojure
(defn process-instructions [instructions]
  (reduce (fn [acc [op label len]]
            (let [box (HASH label)
                  idx (index-of-first #(= label (first %)) (acc box))]
              (case [op (some? idx)]
                [:set false] (update acc box conj [label len])
                [:set true] (assoc-in acc [box idx 1] len)
                [:remove false] acc
                [:remove true] (update acc box #(into (subvec % 0 idx) (subvec % (inc idx)))))))
          (into {} (map vector (range 256) (repeat [])))
          instructions))
```

This is a simple `reduce` function over the parsed `instructions`, where the accumulated state begins as a map of each
box ID (0 to 255) to an empty vector.  We destructure each instruction into its `op`, `label`, and optional `len`. Then
we use `HASH` to calculate the right box for the label, and use `index-of-first` to find the index in that box of the
lens with the matching label, if any. To pick the action to apply, I thought it would be fun to use the `case` function,
which is like Java's `switch` expression. In this function, we use `(case [op (some? idx)])` to match against a 
two-element vector of the `operator` and whether a lens with the correct label currently exists in the box. For
`[:set false]`, this is a new lens in the box, so `(update acc box conj [label len])` adds the vector `[label len]` to
the existing collection for the boxFor `[:set true]`, we need to replace the `focal-length` of the existing lens, so
`(assoc-in acc [box idx 1] len)` find the existing lens at index `[box idx]`, and then changes its length (the second
element of `[label focal-lenth]`). For `[:remove false]`, the instruction is to remove a lens that's not within the
box, so we can just return the unchanged `acc`. And for `[:remove true]`, we need to remove the lens from the box's
vector; there's no built-in function in Clojure to do it, so `(into (subvec % 0 idx) (subvec % (inc idx)))` gets the
job done by appending the vector starting after the index being removed onto the vector before that index.

To get the `total-focusing-power` of the boxes, we'll have to make slightly more complex transducer than we normally
do.

```clojure
(defn total-focusing-power [boxes]
  (transduce (mapcat (fn [[box lenses]]
                       (map-indexed (fn [idx [_ len]] (* (inc box) (inc idx) len)) lenses)))
             + boxes))
```

As expected, we're transducing over the `boxes` that come out of `process-instructions`, and we transform each box
into a sequence of each lens's focusing power, combining them together using `+`. Remembering that `boxes` is a map
of the form `{box-id [[label1 len1] [label2 len2]]}`, we can use `mapcat` on `boxes` on each sequence of `box-id`
and internal `lenses`. Then we use `map-indexed` for each lens within that box so that we can multiply together the
`box-id` and lens's index (both incremented to be 1-indexed) with the focal length.

We're ready to finish!

```clojure
(defn part2 [input] (-> input parse-instructions process-instructions total-focusing-power))
```

It's just what we'd expect - take the input, parse it, run the instructions, and get the focusing power.

## Rewrite using vectors

My go-to data structure in Clojure is the almighty map, but we could have also implemented part 2 using vectors. 
Granted, vectors and maps are essentially the same data structure under the hood, but they look different. Or do they?
Let's see how `process-instructions` and `total-focusing-power` look with vectors instead of maps.

```clojure
(defn process-instructions [instructions]
  (reduce (fn [acc [op label len]]
            (let [box (HASH label)
                  idx (index-of-first #(= label (first %)) (acc box))]
              (case [op (some? idx)]
                [:set false] (update acc box conj [label len])
                [:set true] (assoc-in acc [box idx 1] len)
                [:remove false] acc
                [:remove true] (update acc box #(into (subvec % 0 idx) (subvec % (inc idx)))))))
          (vec (repeat 256 []))                                   ; Different initializer
          instructions))

(defn total-focusing-power [boxes]
  ; This function had to change to use reduce-kv, where `box` (the vector's index) is the key
  (reduce-kv (fn [acc box lenses] (transduce (map-indexed (fn [idx [_ len]] (* (inc box) (inc idx) len))) + acc lenses))
             0
             boxes))
```

The only difference in `process-instructions` with a vector is that it gets initialized to a vector of 256 empty
vectors, as opposed to a map of 256 empty vectors. That's it. The rest of the function works exactly the same way. Now
`total-focusing-power` has to switch from using `transduce` to something that lets the code see both the value in each
box as well as its index. `reduce-kv` does this nicely, since a vector is simply a map where the key is just the
vector's index. Since we have to accumulate as we go, it makes sense to add each focusing power to the accumulator; 
lo and behold, using `transduce` _within_ the `reduce` function fits the bill perfectly!

So, which is better? I don't know. I suppose the vector solution is a bit easier to interpret since `reduce-kv` is
a pretty way to get to the vector's index and its values, but we accomplish the same thing with the map version if we
first sorted the `[key value]` values by their keys.

```clojure
; This is the vector version
(defn total-focusing-power [boxes]
  (reduce-kv (fn [acc box lenses] (transduce (map-indexed (fn [idx [_ len]] (* (inc box) (inc idx) len))) + acc lenses))
             0
             boxes))

; This is the map version, identical except for the `(sort-by first boxes)` as the data fed in.
(defn total-focusing-power [boxes]
  (reduce-kv (fn [acc box lenses] (transduce (map-indexed (fn [idx [_ len]] (* (inc box) (inc idx) len))) + acc lenses))
             0
             (sort-by first boxes)))
```

I guess I'll keep the map version since I wrote it first. But this is part of what I love about Clojure's datatypes and
strong focus on immutability - it can be nearly trivial to swap between lists/sequences, vectors, maps, and sets.
Entire algorithms barely get affected by changing types, even when significant features like indexes are critical.