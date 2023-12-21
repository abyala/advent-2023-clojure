# Day 20: Pulse Propagation

* [Problem statement](https://adventofcode.com/2023/day/20)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day20.clj)

## Intro

Boooooooo!!! There's always one problem every Advent season that you can only solve by looking at your dataset instead
of the problem set, and this was it. As always, I went to Reddit instead of wasting my time, but it bugs me every year.
Part 1 is a reasonable and fun puzzle, before they ruined it with part 2. Still, let's do it.

## Part One

We're given a list of connections from one communication module to others, and instructions on how different module
types convert one signal (low or high) into another. Our goal is to determine how many low and high pulses are sent
when the module state gets kicked off by 1000 button pushes.

### Parsing

Let's figure out our parsing strategy. We'll want to create a map of `{module-name module}`, where each module has
at least a `:name`, `:type`, sequence of `:targets` to which it sends signals, and optionally some other data. The
broadcaster module just sends the same signal it receives, so it has no more data. Flip-flop modules retain an on-off
state, such that it switches state whenever it receives a low pulse, so it will have an `:on?` field. And conjunction
modules contains a `:memories` map of the last signal each connected input last sent it, defaulting all to `false` to
represent a low signal.

```clojure
(def broadcaster "broadcaster")

(defn parse-module [s]
  (let [[name targets] (str/split s #" -> ")]
    (assoc (cond
             (= name broadcaster) {:type :broadcaster :name name}
             (str/starts-with? name "%") {:type :flip-flop :name (subs name 1) :on? false}
             (str/starts-with? name "&") {:type :conjunction :name (subs name 1) :memories {}})
      :targets (str/split targets #", "))))
```

`parse-module` is the first step we'll apply to each input line, and it's of the form `name -> t1, t2, ...`. We split
the string by the ` -> ` arrow with spaces, destructuring into the name and the single string of potentially 
comma-separated targets. Then we create a map with the split target names, which we configure based on a `cond`
expression. The `:broadcaster` just holds on to its full name. The `:flip-flop` types set their `:name` to the 
substring that comes after the `%` prefix, and sets `:on?` to `false`. And the `:conjunction` type does the same thing
with its `:name`, and initializes an empty map for the `:memories` since it depends on _other_ configuration lines'
target outputs to determine its inputs. So how do we initialize the `:memories` to be all `false` (low)?

```clojure
(defn register-conjunction-inputs [module-map]
  (reduce (fn [acc [target source]] (if (= :conjunction (:type (module-map target)))
                                      (update-in acc [target :memories] assoc source false)
                                      acc))
          module-map
          (mapcat (fn [{:keys [name targets]}] (map #(vector % name) targets)) (vals module-map))))
```

Given a `module-map`, the `register-conjunction-inputs` solves that problem for us. It uses a `reduce` with the input
being a sequence of every `[target source]` pair, which it determines using `mapcat` on the values of the
originally-parsed `module-map`. Then for each dependency, if the target type is a conjunction module, the reducing
function updates that module's `:memories` map to set the source module to `false`.

Then it's a matter of connecting those two functions together for the combined `parse-input` function.

```clojure
(defn parse-input [input]
  (let [modules (map parse-module (str/split-lines input))]
    (register-conjunction-inputs (zipmap (map :name modules) modules))))
```

This function calls `parse-module` on each line of the input, then uses `zipmap` to map the name of each module to the
module itself, and then calls `register-conjunction-inputs` to finish its initialization.

### Sending and receiving signals

Whenever we send a signal to a module, two things may happen - the module may change state, and the module may send out
new signals. We'll prepare two functions to support this, before implementing it for each module type.

```clojure

(defn- send-signals [module-map from high?]
  (map #(hash-map :from from :to % :high? high?) (get-in module-map [from :targets])))

(defmulti receive-signal (fn [module-map from to high?] (get-in module-map [to :type])))
```

`send-signals` is a private method that takes in a `module-map` and the name of the module that intends to send
messages, along with whether the signals it sends will be high. The function gets the list of `:targets` from the
`module-map` and constructs a signal message, being a map with keys `:from`, `:to`, and `:high?`. This is a
convenience method that we'll use soon.

Then `receive-signal` is a multi-method, the first I've used in this year's Advent puzzles but which I do enjoy when
they make sense. This is Clojure's form of polymorphism without depending on inheritance. It says that we will have
a function called `receive-signal`, which takes in a `module-map`, the signal sender (`from`), the signal receiver
(`to`), and the signal strength (`high?`), and returns a vector of `[new-module-map output-signals]`, where both values
can be `nil`. It then also defines its dispatch function, which determines which instance of the function to execute
based on its inputs; in our case, the module type as found in `(get-in module-map [to :type])`.

Now we can create four implementations of `receive-signal` based on the three known module types and a default
implementation. It just so happens in part 1 that there is a missing module called `rx`, which receives a signal but
doesn't do anything with it, so that module will route to the default implementation.

```clojure
(defmethod receive-signal :broadcaster [module-map _ to high?]
  [nil (send-signals module-map to high?)])

(defmethod receive-signal :flip-flop [module-map _ to high?]
  (when-not high?
    (let [module-map' (update-in module-map [to :on?] not)]
      [module-map' (send-signals module-map' to (get-in module-map' [to :on?]))])))

(defmethod receive-signal :conjunction [module-map from to high?]
  (let [module-map' (assoc-in module-map [to :memories from] high?)
        all-high? (every? true? (vals (get-in module-map' [to :memories])))]
    [module-map' (send-signals module-map' to (not all-high?))]))

(defmethod receive-signal :default [module-map from to high?])
```

The implementation of a multi-method is a `defmethod` macro, which is declared with the multi-method name of
`receive-signal` and the dispatch value, followed by the function arguments and implementation. Let's look at each
instance.

The `broadcaster` module doesn't make any real decisions of importance; its state doesn't change and it just calls
`send-signals` with whatever signal type it receives.

The `flip-flop` modules doesn't do anything with a high signal, so its `(when-not high? ...)` simply returns `nil` if
there's nothing to do. Clojure's very flexible with how it handles `nil`, so `nil` and `[]` and `[nil nil]` can often
be treated the same way. If the signal is low, then `(update-in module-map [to :on?] not)` changes the value of the
on/off signal by calling `(not old-value)` on it. Then the function returns the new `module-map'` and sends signals to
its targets, based on the new `:on?` value.

The `conjunction` module first associates in the new memory value for the input signal it's receiving, by calling
`(assoc-in module-map [to :memories from] high?)` on the incoming data. Then `all-high?` checks if every memory the
module now contains are all high, or `true`. It only outputs a low pulse if all memories are high, so `send-signals`
will transmit `(not all-high?)`.

And the default implementation does nothing, so its function declaration doesn't even need to have a body for it to
effectively return `nil`.

### Pushing buttons

We're going to need a function that sends off an initial signal by pushing a button that's connected to the
`broadcaster` module. We'll let it cascade its downstream signals until the system quiets before it returns, and the
function will return a vector `[module-map' {false m, true n}]`. The `module-map'` is the resting state of the modules,
while `{false m, true n}` is the count of low and high signals that were sent throughout the venture.

```clojure
(defn push-button
  [module-map button-target]
  (loop [signals [{:from "button" :to button-target :high? false}], state module-map, receive-stats {false 0, true 0}]
    (if (seq signals)
      (let [{:keys [from to high?]} (first signals)
            [returned-state signals'] (receive-signal state from to high?)
            state' (or returned-state state)]
        (recur (apply conj (subvec signals 1) signals'), state', (update receive-stats high? inc)))
      [state receive-stats])))
```

The function does a standard `loop-recur`, initialized with a starting signal from `"button"` going to a function
argument that we assume will be `"broadcaster"`. It loops with the current state of the module map, as well as the
accumulated `received-stats` map that defaults to `{false 0, true 0}`. While there are still signals to process, it
calls `receive-signal` and handles `nil` values until it has the new `state'` and `signals'` values it needs. Finally,
it recurses with the new signals added to the end of the signal queue, the revised state, and calling `inc` on either
the `true` or `false` values of the `receive-stats`, based on what signal type it received. Note that we don't call
`(conj (rest signals) signals')` because `signals` is a vector, and we need that to process signals in order. `rest`
returns a sequence, but `subvec` keeps the vector intact.

Now we can build an infinite sequence of the results of calling `push-button-seq` that we can use to finish `part1`.

```clojure
(defn push-button-seq [module-map button-target]
  (rest (iterate #(push-button (first %) button-target) [module-map])))

(defn part1 [input]
  (->> (push-button-seq (parse-input input) broadcaster)
       (take 1000)
       (map second)
       (apply (partial merge-with +))
       vals
       (apply *)))
```

`push-button-seq` takes in the starting `module-map` and the `button-target`, which we again assume will be
`"broadcaster"`, and returns a sequence of `[module-map' results]`. I originally implemented this as a recursive call
with `lazy-seq`, but decided I could simplify it with `iterate`. That function takes in a function and an initial input,
and returns a lazy sequence of calling the same function of the output of the previous instance. `push-button` takes in
only the `module-map` while it returns a tuple of `[module-map results]`, so we initialize `iterate` with the vector of
`[module-map]` and the function extracts the first value out of it each time it runs. We also call `rest` on the
returned lazy sequence since `iterate` always returns the initial input as its first result, and we don't want that to
skew the output.

Finally, `part1` is rather procedural. First, create the sequence of 1000 outputs and call `(map second)` to grab the
`{true false}` results instead of the resulting `module-map`s. Then we call `(merge-with +)` on each output to combine
each map together, adding together the results at each key. Finally, since we need to multiply together the total
number of high and low signals, we call `vals` on the combined map and `(apply *)` to get our result.

## Part Two

This is the puzzle where you need to analyze the puzzle data instead of solving the problem as stated. At the
suggestion of someone on Reddit, I fed my input file into Graphviz and got [this output image](day20-graphviz.jpg).
The suggestion is that each of the four outputs from `broadcaster` is an independent sub-problem which itself loops
without an initial offset, and they together feed the output `rx`. If that's true, then finding out how long it takes
for each sub-problem to loop and calculating the LCM of those loop lengths should tell us how long it will take to
trigger `rx`.

First, we need to support a new `rx` module, which starts in an off state until it receives a low pulse; this means
that `rx` is a flip-flop module, so `add-rx-node` takes in a parsed `module-map` and adds that new node to it.

```clojure
(defn add-rx-node [module-map]
  (assoc module-map "rx" {:type :flip-flop :name "rx" :on? false}))
```

Then we need to determine how long it takes for a sub-puzzle to loop. We'll implement `loop-length` for this, but with
a kicker - since we only care about each sub-puzzle in isolation, we'll trigger the button press to trigger the first
module within each puzzle, rather than the broadcaster.

```clojure
(defn loop-length [module-map button-target]
  (let [[initial & other-states] (push-button-seq module-map button-target)]
    (count (take-until #(= % initial) other-states))))
```

The function calls `push-button-seq` on the `module-map` and the input `button-target` argument, binding `initial` to
the first state and `other-states` to the subsequent ones. We call `take-until` to find the first state within
`other-states` that matches the initial state, and then count them up. Note that this solution wouldn't work if we
had the button push the broadcaster, because when the sub-puzzle looped, the other sub-puzzles might be different. So
this approach isolates each sub-puzzle.

Now we're ready to implement `part2`.

```clojure
(defn part2 [input]
  (let [module-map (add-rx-node (parse-input input))]
    (reduce #(math/lcm (loop-length module-map %2) %1) 1 (get-in module-map [broadcaster :targets]))))
```

We start with parsing the input and calling `add-rx-node` to establish our enhanced map. Then we call `reduce` on the
four targets of `broadcaster` as the input values. For each of those target modules, we call `loop-length` and fold the
result into `math/lcm`, initialized to a 1.

So yeah, the code wasn't awful, but hopefully that is the last "find the tricky" problem for this year.
