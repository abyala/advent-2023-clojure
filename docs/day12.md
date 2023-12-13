# Day 12: Hot Springs

* [Problem statement](https://adventofcode.com/2023/day/12)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day12.clj)

## Intro

Oof! This puzzle was a doozie, and I'm not really sure why. After spending _multiple_ hours on it, I left it alone
overnight, and then did a complete rewrite from a coffee shop, this time with an abundance of test cases. Lo and
behold, the puzzle turned out not to be all that difficult and, with a little bit of memoization, quite speedy!

## Overall Approach

We are given a list of hot springs and the number of non-contiguous groups of damaged springs. Our goal is to figure
out how many different interpretations of each line can match the groupings of damaged springs. Let's talk about my
approach. For each row, we look at the first grouping, and isolate the "search space" to scan. I define the search
space as the starting characters of the spring, until the first known damaged spring occurs, plus the next `g+1`
characters, where `g` is the size of the group. So for the string `"..??.?##??"` and group `1`, the search space is
`..??.?##`, while the same string with group `2` is `..??.?##?`. The rationale is that only question marks and hash
signs can define a damaged spring, but if all question marks resolved to periods, then the first grouping could start
no later than the first hash sign; if that happens, we need to know that there will be `g` characters that are all
damaged springs, and if we're not at the end of string, then the next value must be resolvable unto a period. I don't
know if that makes any sense, but I've been working on this for hours, so I'm moving on.

Once we have a search string, we'll define all possible ways to realize the grouping, and then substitute them for the
front of the spring string.  We'll then strip away any leading periods from the end (since they don't affect the next
search space), and repeat the process with the remaining groupings.

Let's look at an example.  The first sample row is `???.### 1,1,3"` which we will parse into `["???.###" (1 1 3)]`.
The first grouping is of length 1, so the search space will be `"???.##"` as only the characters in that substring
could possibly make a grouping of 1. There are three ways to do this -- the first three question marks could become
`"#.?"` or `.#.` or `..#`, which would make the entire search string `#.?.##` or `.#..##` or `..#.##`. Any other
combination won't work, because `##.` or `.##` would make a grouping of 2, and `...` would make the result `....##`,
which also fails. Going back to our matches of `("#.?.##", ".#..##", "..#.##")`, we can pull out the group from each
solution (including its trailing period), resulting in `("?.##", "..##", ".##")`. Thinking ahead, when we check each of
these for their next grouping, the leading periods in options 2 and 3 won't impact the solution, so both `"..##"` and
`".##"` are essentially equivalent to `"##"`. We'll need to remember how many reduced solutions came out for the
grouping, so we'll have to remember that stripping away the first grouping of `(1 1 3)` from `"???.###"` results in
one match of `"?.##"` and two of `"##"`. Now if THAT makes sense, we're in good shape.

## Part One

Let's start with parsing. Our goal is to transform each row into a vector of `[s groups]` where `s` is the unchanged
springs string, and `groups` is a numerical sequence of the damaged springs.

```clojure
(defn parse-row [row]
  (let [[s0 s1] (str/split row #" ")]
    [s0 (split-longs s1)]))

(defn parse-input [input] (map parse-row (str/split-lines input)))
```

There's nothing fancy here. `parse-row` splits the input string by the one space, making the first value the string `s`
and the right value the list of groups. We know how to parse that already.

Now let's create `search-space-for`, which takes in a string `s` and the size of the group to find, and returns the
search string we need to inspect.

```clojure
; abyala.advent-utils-clojure.core namespace
(defn subs-to-end
  ([s start] (if (< start (count s)) (subs s start) ""))
  ([s start end] (if (< start (count s))
                   (subs s start (min end (count s)))
                   "")))

(defn search-space-for [s group]
  (if-let [idx (str/index-of s "#")]
    (subs-to-end s 0 (+ idx group 1))
    s))
```

The structure of the search space depends on whether there are any `#` characters in the string. If there are any,
then we want the leading substring of `s`, going up to that character plus the size of the group. I find there are lots
of times when I want "the biggest substring of length 'n'" without throwing a `StringIndexOutOfBoundsException`, so I
made the `subs-to-end` function in my core utils namespace to simplify the code. If the string does not contain any
`#` characters, then it only has `.` and `?` characters, so return the whole string.

Now we'll create `search-space-matches`, which takes in a `search-space` and `group` and returns a map of structure
`{:leftover :count}`, where `:leftover` is the simplified substring of the search space with the match removed, and
`:count` is the number of matches that reduced to that that substring remainder.

```clojure
; abyala.advent-utils-clojure.core namespace
(defn repeat-string [n s] (apply str (repeat n s)))

(defn drop-leading-dots [s] (apply str (drop-while (partial = \.) s)))

(def search-space-matches
  (memoize (fn [search-space group]
             (let [too-many (repeat-string (inc group) "#")]
               (->> (range (- (count search-space) group -1))
                    (remove #(let [replaced-str (str/replace (subs search-space % (+ % group)) "?" "#")
                                   after-replacement (subs-to-end search-space (+ % group) (+ % group 1))
                                   s (str (subs search-space 0 %) replaced-str after-replacement)]
                               (or (str/includes? replaced-str ".")
                                   (str/includes? s too-many))))
                    (map (comp drop-leading-dots #(subs-to-end search-space (+ % group 1))))
                    (frequencies))))))
```

First off, there are two simple functions. `repeat-string` is new in the `core` library, and it simply repeats an 
input value `n` times and returns it as a string. Then `drop-leading-dots` is another simple function that removes all
periods from the front of a string; we'll need this to normalize the substrings after removing their matches. The other
thing to note with `search-space-matches` is that we memoize the function, meaning that Clojure will cache the return
value for each unique set of input arguments. This cut the running time of the solution by about one third, which was
a little over a second.

The memoized function `search-space-matches` starts by creating `bad-pattern`, a regex that represents having too many
broken spring in the sequence. If `group` is `2`, then `bad-pattern` would be the regex `"#{3}"`. Then the function
runs a thread-last macro, starting with the range of all indexes where we could successfully replace any `?` characters
with `#`. Then we `remove` any indexes that don't do what we want -- either the substring starting from the index and
having a length of `group` already includes a `.`, or by replacing all `?` with `#`, we end up with too many
consecutive `#` characters. Then, knowing that we only have valid indexes, we pull out the substring of the
`search-space` starting one place after the end of the grouping (to leave room for the trailing `.` that must follow),
and we drop the leading dots to normalize the result. Finally, `frequencies` turns the sequence of simplified
substrings to the map of each substring to its count.

Now let's create `replace-next-search-space`, which takes in a string `s` and the remaining `groups` to match, and
returns another map `{:leftover :count}`, similar to that we got from `search-space-matches`. But this time, after
calculating the next matches, it returns a map of the entire leftover string (both the leftovers from the search space
and the portion of `s` after the search space) to the number of matches that create those leftovers.

```clojure
(defn replace-next-search-space [s groups]
  (let [g (first groups)
        search (search-space-for s g)
        after-search (subs-to-end s (count search))]
    (update-keys (search-space-matches search g)
                 #(str % after-search))))
```

`search` is the search space we'll use, and `after-search` is the portion of `s` after it. Then we use the built-in
function `update-keys`, which takes in a map and a 1-arity function, and returns a new map by applying the function to
each key from the input map. In this case, we want to replace the search string leftovers with the result of appending
them with `after-search`.

Note that memoizing this function is _not_ a good idea, as it _increased_ the running time by about 25%. So no
premature optimizations, people!

We're nearing the end, I promise. Now that we can take a string and its broken spring groups, and return the results of
solving the next search space, it's time to implement `num-arrangements`, which takes in `s` and `groups` again, and
returns the total number of arrangements for _all_ groups.

```clojure
(defn dead-end? [s groups] (and (empty? groups) (str/includes? s "#")))
(defn success? [s groups] (and (empty? groups) (not (str/includes? s "#"))))

(def num-arrangements
  (memoize (fn [s groups]
             (cond (success? s groups) 1
                   (dead-end? s groups) 0
                   :else (transduce (map (fn [[leftover n]] (* n (num-arrangements leftover (rest groups)))))
                                    +
                                    (replace-next-search-space s groups))))))
```

First off, we'll make two helper functions, `dead-end?` and `success?`, both of which take in both `s` and `groups`.
If the `groups` collection is empty and there are still `#` characters remaining, then the path we took to get to `s`
is invalid, because there's no way to make those characters disappear. Likewise, if there are no more groups
remaining and no `#` characters remaining, then the path worked, and we've found a good solution.

Once again, `num-arrangements` is memoized, as that cuts down an enormous amount of running time. First it checks if
either `success?` or `dead-end?` has occurred, as the former means the number of arrangements is `1` and the latter is
`0`. Otherwise, we've got more work to do, and we'll do it using a depth-first search. We'll call
`replace-next-search-space` to find the possible leftover strings after removing the next group, along with the number
of ways we got there. For each solution, we multiply the number of paths to it by the result of calling
`num-arrangements` on the leftover, using the rest of the `groups` after the one just calculated. Then we `transduce`
that mapping by summing up the number of arrangements for each path. Isn't a depth-first search slow? Well not
necessarily, but remember that we `memoize` the results, so it's quite performant.

Ok, let's finish up part 1!

```clojure
(defn part1 [input]
  (transduce (map (partial apply num-arrangements)) + (parse-input input)))
```

Typical `transduce` solution here -- parse the input into the sequence of `[s g]`, call `num-arrangements` on each one,
and add up the results.

## Part Two

Given the strong performance of the part 1 solution and the huge value of memoizing the key function calls, part 2 is
actually quite simple. A semi-brute force solution is perfectly fine.

```clojure
(defn unfold [n [s g]]
  [(str/join "?" (repeat n s)) (apply concat (repeat n g))])
```

The `unfold` function converts a parsed row of `[s g]` into the result of repeating both values `n` times, where 
`n` is 5 for part 2 (and you can probably already see that `n` is 1 for part 1). To amplify the string `s`, we call
`(repeat n s)` to create a sequence of length `n` of strings `s`, and call `(str/join "?")` on them to smoosh them
together using `?` as the separator character. To amplify the groups `g`, we'll again use `repeat` on them, and then
use `(apply concat)` to combine them into a single sequence.

Let's bring it on home.

```clojure
(defn solve [n input]
  (transduce (map (comp (partial apply num-arrangements) (partial unfold n)))
             +
             (parse-input input)))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 5 input))
```

The `solve` function is again a `transduce` call, where each parsed row gets unfolded either 1 or 5 times, and then
sent to `num-arrangements` before adding the results together. That's it. We don't need to do anything fancy to handle
the very large strings, and part2 on my puzzle data takes about 1 second to complete. Let's call it a win!