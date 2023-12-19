# Day 19: Aplenty

* [Problem statement](https://adventofcode.com/2023/day/19)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day19.clj)
* [Solution code with consolidated logic](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day19_consolidated.clj)

## Intro

Cute puzzle - part 2 seems much more difficult than it turned out to be, and overall I enjoyed today's challenge quite
a bit.

## Part One

Our input is a series of workflows and a series of parts. Each part has numeric values of four ratings named `x`, `m`,
`a`, and `s`. Each workflow has one or more rules that check if a part's ratings are within bounds, and directs it to
either a next workflow for further processing, or to be automatically accepted or rejected as a part. Our goal is to
find all parts that are accepted, add together their components, and sum them all up.

There isn't much code but it compartmentalizes well, so I'll put in some text separators to keep concepts distinct.

### Parsing

Let's start with parsing. From the input, we'll want to call `parse-input`, which will return a vector of the parsed
workflows and parsed parts, so let's start from the workflow side. A workflow looks like `px{a<2006:qkq,m>2090:A,rfg}`,
which means it has a name (`px`), and one or more rules. A rule always has a target rule name, and may also have three
components for rating, operator, and amount. In the above example, the first rule should be parsed as
`{:target "qkq", :rating "a", :op "<", :amount 2006}` while the last rule should be `{:target "rfg"}`. Below I will 
refer to the former as a "comparison rule" and the latter as a "default rule." Ok, here we go. 

```clojure
(defn parse-rule [s]
  (let [[_ v0 v1 v2 v3] (re-find #"(\w+)([<>])?(\d+)?\:?(\w+)?" s)]
    (if v1
      {:target v3, :op v1, :rating v0, :amount (parse-long v2)}
      {:target v0})))

(defn parse-workflow [line]
  (let [[name & rules] (str/split line #"[\{\,\}]")]
    [name (map parse-rule rules)]))
```

First we'll look at `parse-workflow`, which splits the line of text by `{`, `,`, and `}`. The name always comes before
the `{`, and then every rule ends either at a `,` before the next rule, or before `}` for the end of the workflow. We
destructure this as `[[name & rules]]` to bind the first split substring to `name`, and the rest to a sequence called
`rules`, each of which we then map to `parse-rule`. The `parse-rule` function notes that both comparison and default
rules start with a "word" string, but comparison rules also has the other three components, so we can use a single 
regular expression that expects a word but then optionally has the other components. We can then check if the second
argument, `v1` exists. If so, return the mapping for the comparison rule, and if not, the default rule.

Now we'll write `parse-part` and `parse-input`.

```clojure
(defn parse-part [line] (update-keys (edn/read-string (str/replace line "=" " ")) str))

(defn parse-input [input]
  (let [[workflow-str parts-str] (split-by-blank-lines input)]
    [(into {} (mapv parse-workflow (str/split-lines workflow-str))) (map parse-part (str/split-lines parts-str))]))
```

We'll take a shortcut with `parse-part`, since the text for a part looks so close to EDN syntax for a map. We can use
`edn/read-string` so long as we replace the equals signs with spaces; if we don't, then EDN would interpret `{a=5,b=6}`
to be a map with a single key of `a=5` and a single value of `b=6`, and thus would look like `{"a=5" "b=6"}`. After
calling `read-string`, we then also call `(update-keys m str)`, since without double-quotes in the string, EDN would
read each rating name as a symbol instead of a string.

`parse-input` is nothing special - use `split-by-blank-lines` to separate the input into two groups of lines. It then
returns a vector of form `[{name workflow} (parts)]`.

### Processing rules and workflows

Our next task is to apply a workflow to a rule to see where it goes next. For this, we'll implement both `run-rule` and
`run-workflow`. `run-rule` will either return the name of the next workflow to use, or else `nil` if the rule doesn't
apply; `run-workflow` will return the name of the first workflow for which a rule applied.

```clojure
(defn run-rule [rule part]
  (let [{:keys [target op rating amount]} rule]
    (if op
      (when (({">" > "<" <} op) (get part rating) amount) target)
      target)))

(defn run-workflow [workflow part]
  (first (keep #(run-rule % part) workflow)))
```

Once again, let's start at the bottom with `run-workflow`, remembering that a workflow is nothing more than a
sequence of rules; the `name` of the workflow is entirely contained in the map that observes it.  We'll call `run-rule`
on each rule against the given `part`, calling `keep` to discard the `nil` results, and `first` to stop processing once
we have an answer.

`run-rule` destructures the rule into its components of `target` and optional `op`, `rating`, and `amount`. If this is
a comparison rule (`op` is not `nil`), then see if the rule passes. We'll want to run either `<` or `>` as the function,
comparing the rating of the part against the rule's `amount`. When that resolves to `true`, return the `target`. Or
if the `op` doesn't exist and this is a default rule, return the `target`. The function only returns `nil` if it is
a comparison rule and the `when` test is falsey.

### Completing Part 1

Now we can finish it up by implementing `accept-part?` and then `part1`.

```clojure
(def start-rule "in")
(def accepted "A")
(def rejected "R")

(defn accept-part?
  ([workflows part] (accept-part? workflows part start-rule))
  ([workflows part workflow-name] (condp = workflow-name
                                    accepted true
                                    rejected false
                                    (recur workflows part (run-workflow (get workflows workflow-name) part)))))
```

`accept-part?` takes in two arities for convenience - one with the map of `workflows` and the `part` to be inspected,
and the other with the name of the workflow to inspect. The 2-arity function just calls the 3-arity function with 
the starting rule named `"in"`. The 3-arity rule checks the name of the workflow. If it is the `accepted` rule with
name `"A"`, then the function returns `true` for accepting the rule. Similarly, if it is the `rejected` rule with
name `"R"`, then it returns `false`. Otherwise, it calls `run-workflow` on the `workflow-name` and loops back on itself.

Why do we use `(condp = workflow-name ...)` instead of the simpler `(case workflow-name ...)`? This one has tripped me
up in the past. `case` only accepts literal values, nothing calculated even if they're as simple as `def` literals like
`accepted` and `rejected`. So we could have used `(case workflow-name "A" ... "R" ...)` just fine, but we can't do the
same with the symbol `accepted`. `condp` is a funny little macro that applies the predicate function (`=`) to each test
expression (`accepted` or `rejected`) and the `condp` expression (`workflow-name`). It's just cleaner than saying
`(cond (= accepted workflow-name) ... (= rejected workflow-name) ...)`.

Ok, we can take a part and run it all the way through the workflows to determine whether or not it's accepted, so we
can write `part1`. Will we use a transducer?

```clojure
(defn part1 [input]
  (let [[workflows parts] (parse-input input)]
    (transduce (comp (filter (partial accept-part? workflows))
                     (map #(apply + (vals %))))
               + parts)))
```

Of course we use a transducer! We feed in the `parts` and aggregate them with `+`. This time, the transformation
of each part goes through a filter to see if it's accepted, and if so then a mapping that adds together all of its
rating values.

## Part Two

In this part, we need to find all possible parts, where each rating can have a value between 1 and 4000 inclusive, such
that the part is accepted. We know from the sample input that the answer will be in the trillions, so brute force isn't
going to cut it. I thought about two approaches. First, we could work from the `start-rule` and a construct that
represents all 4000 values of each rating, applying each rule and splitting that construct to go down each `if` and
`else` branch. Alternatively, we could start from all `accepted` states and work our way backwards. The former seemed
much simpler and turned out to be quite fast, so that's what we'll do.

The bulk of the code depends on two functions - `split-parts-by-workflow`, which in turn calls `split-parts-by-rule`.
This is now the third time in this puzzle when I've described the "higher" function before the "lower" one, from a
granularity perspective, and I'll do it again this time.

```clojure
(defn split-parts-by-workflow [workflow parts]
  (->> workflow
       (reduce (fn [[outputs leftovers] rule]
                 (let [[good target bad] (split-parts-by-rule rule leftovers)]
                   [(if good (conj outputs [target good]) outputs) bad]))
               [() parts])
       first))
```

This function takes in a single `workflow` and a `parts` construct, where the latter is a map of the four rating names
(`"x"`, `"m"`, `"a"`, and `"s"`) to a vector of `[low high]` values, both inclusive. The output of the function is a
sequence of tuples in the form `([workflow-name parts'])`, where each tuple connects the target workflow from a rule to
the `parts` to be run through it. We feed the `workflow` (again being a sequence of rules) into a `reduce` function,
where the accumulator is a sequence of results and the leftover parts to run through the next rule. After assessing
each rule through `split-parts-by-rule`, we expect a 3-tuple to be returned of the form `[good-parts target bad-parts]`.
If anything passed the rule, then `good-parts` is the `parts` that passed it, and `target` is where that `parts` goes.
If anything failed the rule, then `bad-parts` is what's leftover. Thus we'll `conj` any `[target good]` to the
accumulated `outputs`, and any `bad` as the `leftovers` to run through the following rules. When `reduce` is over, we
no longer need the leftover `parts` (hopefully there won't be any!) and we return the `outputs` using `first`.

Ok, now we can build `split-parts-by-rule`, which we'll do twice.

```clojure
; split-parts-by-rule with nested conditionals
(defn split-parts-by-rule [rule parts]
  (let [{:keys [target op rating amount]} rule
        [low high] (get parts rating)]
    (case op
      nil [parts target nil]
      "<" (cond
            (<= amount low) [nil nil parts]
            (> amount high) [parts target nil]
            :else [(assoc-in parts [rating 1] (dec amount)) target (assoc-in parts [rating 0] amount)])
      ">" (cond
            (>= amount high) [nil nil parts]
            (< amount low) [parts target nil]
            :else [(assoc-in parts [rating 0] (inc amount)) target (assoc-in parts [rating 1] amount)]))))
```

As described above, this function takes in a `rule` and the `parts` to consider, and returns a 3-tuple of form
`[good target bad]`, where either the first two values or the last one can be `nil`. After destructuring both the
`rule` and the rating from `parts`, we run `case` on the operator. If there is no `op`, then this is a default rule,
so all of `parts` are considered "good" and transition to the `target`. Otherwise, we compare the operator against the
`parts`' existing rating to put together the tuple. I won't go through each and every line for every condition.

Now while I was working on this problem, it occurred to me that nested conditions tend to be ugly, and this was no
exception. For the sake of readability, I reimplemented the function to use pattern matching, and I rather like the
result.

```clojure
(defn split-parts-by-rule [rule parts]
  (let [{:keys [target op rating amount]} rule
        [low high] (get parts rating)]
    (match [op (when op (signum (- amount low))) (when op (signum (- amount high)))]
           [nil _ _]          [parts target nil]
           ["<" (:or 0 -1) _] [nil nil parts]
           ["<" _ 1]          [parts target nil]
           ["<" _ _]          [(assoc-in parts [rating 1] (dec amount)) target (assoc-in parts [rating 0] amount)]
           [">" _ (:or 0 1)]  [nil nil parts]
           [">" -1 _]         [parts target nil]
           [">" _ _]          [(assoc-in parts [rating 0] (inc amount)) target (assoc-in parts [rating 1] amount)])))
```

We again destructure the `rule` and rating, and then we create a tuple of `[op sig-low sig-high]`, where the latter
applies the `<` or `>` operator to the rule `amount` and the rating's `low` or `high` value, calling `signum` to
express the comparison as a `-1`, `0`, or `1`. Then we look at that tuple across match rules, which I think are quite
clean to read. An `_` underscore means you can ignore that part of the tuple because it doesn't matter, and the
`(:or ...)` syntax means that any following expression matches. So now it's easy to read! If the operator is `nil`,
we have the `[parts target nil]` result. Then if it's either `<` or `>`, the sign of the differences between the amount
and the rating determines how to put together the result tuple. It's exactly the same code as above, but flattened and
hopefully easier to read.

We're almost done.

```clojure
(defn num-combos [parts]
  (transduce (map (fn [[low high]] (- (inc high) low))) * (vals parts)))

(defn part2 [input]
  (let [workflows (first (parse-input input))]
    (loop [options [[start-rule (zipmap ["x" "m" "a" "s"] (repeat [1 4000]))]], n 0]
      (if-some [opt (first options)]
        (let [[name parts] opt]
          (condp = name
            accepted (recur (rest options) (+ n (num-combos parts)))
            rejected (recur (rest options) n)
            (recur (concat (rest options) (split-parts-by-workflow (workflows name) parts)) n)))
        n))))
```

First we'll make a simple function `num-combos` that takes a `parts` that has been accepted and returns the number of
combinations contained within it. For this, we don't care about which internal `[low high]` range applies to which
rating, so we'll `transduce` across the `(vals parts)`, map each range to the number of values contained within 
with `(- (inc high) low)`, and then multiply the values together.

Finally, we build `part2`. After parsing the input, we call `first` to keep the `workflows` and discard the `parts`.
Then we'll `loop` over all possible `[rule parts]` options, which initially is the `start-rule` and the result of
mapping each rating to the tuple `[1 4000]` using `zipmap`. (I seldom remember `zipmap` but I love that function.)
Then, as we did in the original `accept-part?` function, we call `condp` to see if the workflow is either `accepted`
or `rejected`. If it's `accepted`, then we add to the accumulated `n` value the `num-combos` for the `parts` which made
it to the workflow. If it's `rejected`, we just drop it and resume looping. Otherwise, we call `split-parts-by-workflow`
on the named `workflow` and the `parts`, and concatenate them to the remaining options being considered as a 
depth-first processing algorithm. When we're all out of options to consider, we return `n`.

## Refactoring to consolidate

This is one of those days when we _can_ consolidate the algorithms, but I don't think it's worth it. Still, I did it in
a separate namespace just for kicks. All we have to do is to convert the `workflows` into a sequence of matching parts
ranges, like we did in part 2, and then feed in a function that looks at the matching parts and the input parts. I
don't feel like explaining the code, so you can read the raw text if you really want, but here's the general view:

```clojure
(defn solve [f input]
  (let [[workflows parts] (parse-input input)]
    (f (matching-parts workflows) parts)))

;Pseudocode
(defn part1 [input]
  (solve #(-> filter-parts-by-those-within-some-matching-parts-range add-together-values-within-each-part)) input)
(defn part2 [input]
  (solve #(transduce-num-combos-on-matches) input))
```

Doing so lets us remove some unneeded functions, like `run-rule`, `run-workflow`, and `accept-part?`. Personally, I
don't like this consolidation as much as the original solution.