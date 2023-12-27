# Day 25: Snowverload

* [Problem statement](https://adventofcode.com/2023/day/25)
* [Solution code](https://github.com/abyala/advent-2023-clojure/blob/master/src/advent_2023_clojure/day25.clj)

## Intro

I won't hide my disappointment with how Advent is ending this year, as there is yet another unintuitive puzzle that
you can either solve knowing one of several searching algorithms, including Karger's Algorithm, Kruskal's Minimum
Spanning Tree Algorithm, or Stoer-Wagner's Algorithm. Some put the data into Graphviz and tried to figure it out from
the visualization. Many Python programmers all had to use the library `networkx`, or Kotlin users `jgrapht`. This was
again very much not my kind of puzzle, as clearly it's not the kind of thing you can just figure out, but need to come
to the table already knowing.  Bummer.  I did find some joy in leveraging some of my `advent-utils-clojure` utility
code, so there's that.

## Part One

We're given an input of lines of text representing which components connect to each other. Our goal is to find out
which three connections we can severa to break the map of connected components into two separate groups. Let's start
with parsing the input into a map of `{from-component #{connected-component-set}}`.

```clojure
(defn parse-input [input]
  (reduce (fn [acc line]
            (let [[from & to] (re-seq #"\w+" line)]
              (merge-with set/union acc {from (set to)} (zipmap to (repeat (hash-set from))))))
          {}
          (str/split-lines input)))
```

Rather than implementing this in two functions, this time we'll do it in one. For each line of input, we'll run a
regex to split apart all words, treating the first as the `from` component and the rest as the sequence of `to`
components. Because connections are bidirectional and we don't know which side of the connection will appear as the
`from` or the `to`, we'll store both sides of each connection in the output map. As we `reduce` over each line of the
input, we'll merge the new connect ions into the existing map. `(merge-with set/union m1 m2 m3...)` combines multiple
maps together, and when they have a map key collision, it uses `set/union` to combine the hash sets together. In this
case, `m1` is the accumulated map we're reducing over.  Then `{from (set to)}` creates a second map of the outbound
connections from `from` to all of the `to` components. Finally, `(zipmap to (repeat (hash-set from)))` creates a third
map of the reverse connections, from each `to` component to `#{from}`.

Now for reasons I didn't quite understand, I read that one approach was to randomly select a few nodes, and determine
the shortest path from them to each other node in the graph. Then, we find the most common paths and check to see
whether that cuts the nodes into two groups. It turns out that even though the instructions say to make 3 cuts, I found
I could get the correct answer quickly if I picked 10 nodes to calculate shortest paths, but then make 35 cuts. Go
figure.

First, let's implement `shortest-path-to`. Before this season of Advent started, I created some utility functions for
doing breadth-first and depth-first searches, and I haven't had a chance to use them! So this time, we'll leverage the
`breadth-first-stateful` function.

```clojure
(defn shortest-path-to [nodes from to]
  (breadth-first-stateful #{}
                          [(list from)]
                          (fn [seen path] (let [n (first path)]
                                            (cond (= n to) (done-searching path)
                                                  (seen n) (keep-searching seen)
                                                  :else (keep-searching (conj seen n)
                                                                        (map #(conj path %) (nodes n))))))))
```

The `breadth-first-stateful` function takes in three arguments - the initial state to use throughout the search, the
initial values to search, and a function that processe each search value with the accumulated state. In this case, the
state is the set of nodes already seen, so we don't create a loop, and the initial search is a vector with a single
value, being the list of the starting node, representing the path to that point. The processing function checks if the
most recently-stored node is the target; if so, it uses `done-searching` to exit the search, similar to how `reduced`
short-circuits a `reduce` call. If the function finds a node already seen, it calls the 1-arity `keep-searching`
function with the revised state, which in this case doesn't change at all. And if it's a new node, then it returns
the 2-arity `keep-searching` with the new state (adding the node to the set of nodes already seen) and new values to
add to the search space (all paths constructed from adding this node's connections to the existing path). I don't know
if I love this way of doing a search, but it is rather declarative so I think it's a win.

The next function, `cut`, takes in the parsed nodes and the list of connections to sever, and returns the updated
nodes without those connections.

```clojure
(defn cut [nodes connections-to-sever]
  (reduce (fn [acc [node1 node2]] (-> acc (update node1 disj node2) (update node2 disj node1)))
          nodes
          connections-to-sever))
```

We `reduce` over the `nodes`, and for each one we call `update` on the accumulated map. For each node pair being
severed, we disjoint the other node from its set of connected nodes.

Now assuming we've made our cuts, we'll implement `group-sizes` to see how many groups of connect nodes exist in total.
If it returns a single-element list, then the nodes were not partitioned as desired. If it returns a multi-element list,
then we're all set.

```clojure
(defn group-sizes [nodes]
  (let [all-keys (set (keys nodes))
        first-key (first all-keys)]
    (loop [checking #{first-key}, in-group #{first-key}, out-group (disj all-keys first-key)]
      (if-some [k (first checking)]
        (let [removing (filter out-group (nodes k))]
          (recur (apply conj (disj checking k) removing)
                 (apply conj in-group removing)
                 (set/difference out-group (nodes k))))
        (keep #(when (seq %) (count %)) [in-group out-group])))))
```

This function does a `loop-recur` by selecting an initial key to check, putting it into the `in-group`, and leaving
all other nodes in the `out-group`. Then for each node it sees, it removes it from the `out-group` and inserts it into
the `in-group`, adding all other connected nodes to the search space. When the list of nodes to check is complete, it
returns the sizes of the in- and out-groups.

Finally, we can implement the `part1` function. As I don't honestly understand what's going on, I didn't try to make
this all that pretty.

```clojure
(defn part1 [input]
  (let [nodes (parse-input input)]
    (->> (unique-combinations (take 10 (keys nodes)))
         (map (fn [[a b]] (shortest-path-to nodes a b)))
         (mapcat (fn [path] (map #(sort-by first %) (partition 2 1 path))))
         (frequencies)
         (sort-by (comp - second))
         (take 35)
         (map first)
         (cut nodes)
         (group-sizes)
         (apply *))))
```

Let's just walk through what's going on. After parsing the input nodes, we pick 10 random nodes from 
`(take 10 (keys nodes))` and search for the unique combinations of values; this is a utility function I created which
returns all unique combinations of n-length (defaults to 2) from an input collection. Then for each pair of nodes, it
finds the shortest path between them, and then `mapcat`s each component from the path by calling `(partition 2 1 path)`,
sorting each pair alphabetically so that the segment `(a b)` and `(b a)` get counted as the same. Now that we have
every path segment for all tested connections, `frequencies` tells us how often each segment was used. We sort them in
decreasing order of each frequency, take the 35 most common, and use `(map first)` to keep the connections and throw
away the frequencies. Then `(cut nodes connections)` severs those connections from the nodes, `group-sizes` counts how
large each group is, and `(apply *)` multiplies them together to get our answer.

It's an awful solution for a difficult last puzzle for the season, but it's done and that's 50 stars.
