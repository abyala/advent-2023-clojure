# Day 06: Wait For It

* [Problem statement](https://adventofcode.com/2023/day/6)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day06.clj)
* [Algebraic solution code](https://github.com/abyala/advent-2023-clojure/blob/main/src/advent_2023_clojure/day06_algebra.clj)

## Intro

Today's puzzle felt trivial to me, which was well-timed since I had a friend looking over my shoulder while I was
working the puzzle! I'm sure that part 2 is supposed to be a performance challenge with large numbers, but either
because my code was really optimized (doubt it) or Clojure does some clever things (more likely), I didn't _need_
to refactor anything.

## Part One

We're given a list of boat races, with their total times and the greatest distance ever recorded for that race in
the past, and we need to calculate the product of all hold times we can set such that we'll beat the record. Because
the data set is small and simple, I didn't write any parse logic, and instead just passed in a vector of the races as
2-element subvectors of `[time best-distance]`.

First, we'll calculate the distance moved in a race, given the `total-time` allocated and the `hold-time` chosen. The
velocity will simply be the `hold-time`, which we multiply with the remaining seconds not spent holding, thus
`(- total-time hold-time)`.

```clojure
(defn distance-moved [total-time hold-time]
  (* hold-time (- total-time hold-time)))
```

Then we'll return the collection of all possible distances we could have for the given `total-time`. This just means
mapping `distance-moved` over all reasonable `hold-times`, being everything from 1 to 1 less than the `total-time`.

```clojure
(defn all-distances [total-time]
  (map #(distance-moved total-time %) (range 1 total-time)))
```

Finally, we calculate the number of winning options for the given race, meaning all of the calculated distances that
are greater (further) than the previous best.

```clojure
(defn num-winners [total-time previous-best]
  (c/count-when #(> % previous-best) (all-distances total-time)))
```

Now we can implement the simple `part1` function, which as usual is a transducer. We take all of the `races` passed in,
calculate `num-winners` for each, and multiply the results together to get the answer.

```clojure
(defn part1 [races]
  (transduce (map (fn [[time best]] (num-winners time best))) * races))
```

## Part Two

For part 2, we combine the values from the input to make a single race to run, with very large input values. My
`previous-best` distance measured in the mid-trillions, so I expect that naively using the part1 code would cause
overflows or at least take too long. Not so with my solution in Clojure. All I did was call `num-winners` with the
large inputs, and the correct answer came out in about 1.8 seconds. Victory!

## Performance Optimization

My friend asked if there was anything reasonable we could do to optimize the running time to get it to be under 1
second. My thought after looking at some data sets was that the distances would increase until hitting a peak or
plateau (hereafter referred to as the peak), and then the values would mirror the previously-increasing values. In 
other words, if we can measure the peak, we can stop calculating `distance-moved` once we measure a distance lower than
the previously-calculated one.

Imagine we draw our hold times on a horizontal lines, where `o` means a loser, `>` is a winner with a larger distance
than the previous one, `-` is a winner with the same distance as the previous one, and `<` is a winnder with a smaller
distance than the previous one. We'll start counting the number of winners from the first one until the end of the
peak, while also recording the size of the peak itself.  Once we hit a decreasing value, we know the total number of 
winners, being twice the size of `num-winners`, minus the size of `peak` since we don't want to count it twice. Here
is a visual to show this:

````
            |-peak-|
ooo>>>>>>>>>--------<<<<<<<ooo
   |--num-winners--|
````

Given that, we'll discard the `all-distances` function and rewrite `num-winners`.

```clojure
(defn num-winners [total-time previous-best]
  (reduce (fn [[num-winners last-dist count-at-top] hold-time]
            (let [dist' (distance-moved total-time hold-time)
                  num-winners' (+ num-winners (if (> dist' previous-best) 1 0))]
              (cond
                (< last-dist dist') [num-winners' dist' 1]
                (= last-dist dist') [num-winners' dist' (inc count-at-top)]
                :else (reduced (+ num-winners num-winners (- count-at-top))))))
          [0 0 1]
          (range 1 total-time)))
```

We're reducing over all reasonable hold times in `(range 1 total-time)` again, but we plan to short-circuit the 
reduction. The accumulator is a 3-tuple of the number of winners found (default to 0), the distance covered by the last
hold time (default to 0), and the count of hold times at the peak (default to 1). Then for each hold time, we
calculate the `distance-moved` and possibly increment the next `num-winners'`. Finally, we compare the previous
distance to the current one. If we're incrementing, the new accumulator uses the new `num-winners'`, the new `dist'`,
and keeps the `count-at-top` at 1 since there's a new value. If the distance didn't change, we're on the peak, so
use `num-winners'` and `dist`, but increment `count-at-top` since there's another value at the max distance.
Otherwise, we've left the peak, so we can use `reduced` to short-circuit our way out of the `reduce` function. As shown
in the diagram above, the new value is twice `num-winners` minus `count-at-top`.

Making this change drops the running time of the large dataset in part 2 by about two-thirds, and a <500 ms response
time is just lovely to me.

## Rewrite using algebra

When my friend and I were working on the problem, I had written down on paper that there may be another way to express
`distance-moved`, since if `h=hold-time` and `t=total-time`, then my function as math would be `h*(t-h)` which could
be represented as `th-h^2`. I thought "huh, that's interesting but harder to read," and I left it alone.

Before going to bed, I was thinking more about the data and the fact that it was parabolic in nature, increasing to a
certain level before dropping back down again, but again I didn't figure out what to do about it.

Then this morning, I realized that we were so close, and that if I looked at the function with `p=previous-best` as
`th-h^2>p` or `-h^2+th-p>0` then we had the quadratic formula! All I'd need to do is calculate the quadratic factors of
the equation to find the lowest and highest values that solved the equation, and manipulate them slightly to work with
integers instead of doubles. So let's do that!

```clojure
; abyala.advent-utils-clojure.math namespace
(defn quadratic-roots [a b c]
  (map (fn [op] (/ (op (- b) (m/sqrt (- (* b b) (* 4 a c)))) (* 2 a)))
       [+ -]))
```

The factors of a quadratic equation are `(-b +- sqrt(b^2-4ac))/2a`. So I mapped `+` and `-` to the rest of the function,
which returns our values, or the "y-intercepts" in the algebraic sense. I put this function into my common
`advent-utils-clojure` repository within the `math` namespace.

Armed with this and our equation `-h^2+th-p>0`, rewriting `num-winners` is really quite simple.

```clojure
(defn num-winners [total-time previous-best]
  (let [[low-root high-root] (sort (m/quadratic-roots -1 total-time (- previous-best)))
        low (inc (int low-root))
        high (dec (int (ceil high-root)))]
    (inc (- high low))))
```

We calculate the quadratic roots and sort them, binding the two values of type `double` into `low-root` and `high-root`.
These values represent the values that _equal_ zero, but we only want hold times that `exceed` the previous best time.
So the lowest acceptable hold time is one greater than the integer value of `low-root`, and the highest acceptable hold
time is one lower than the integer value of the ceiling of `high-root`; if the `high-root` were 5.6, then 5 would be the
highest hold time. Then to count the number of winners, it's the difference between `high` and `low`, but we increment
the value again since both values are inclusive.

The result is a tiny and lightning fast solution. Hooray for math!