(ns advent-2023-clojure.day25
  (:require [abyala.advent-utils-clojure.core :refer [unique-combinations]]
            [abyala.advent-utils-clojure.search :refer [breadth-first-stateful done-searching keep-searching]]
            [clojure.set :as set]
            [clojure.string :as str]))

(defn parse-input [input]
  (reduce (fn [acc line]
            (let [[from & to] (re-seq #"\w+" line)]
              (merge-with set/union acc {from (set to)} (zipmap to (repeat (hash-set from))))))
          {}
          (str/split-lines input)))

(defn shortest-path-to [nodes from to]
  (breadth-first-stateful #{}
                          [(list from)]
                          (fn [seen path] (let [n (first path)]
                                            (cond (= n to) (done-searching path)
                                                  (seen n) (keep-searching seen)
                                                  :else (keep-searching (conj seen n)
                                                                        (map #(conj path %) (nodes n))))))))

(defn cut [nodes connections-to-sever]
  (reduce (fn [acc [node1 node2]] (-> acc (update node1 disj node2) (update node2 disj node1)))
          nodes
          connections-to-sever))

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

(defn part1 [input]
  (let [nodes (parse-input input)
        proposed-cuts (->> (unique-combinations (take 10 (keys nodes)))
                           (map (fn [[a b]] (shortest-path-to nodes a b)))
                           (mapcat #(partition 2 1 %))
                           frequencies
                           (sort-by (comp - second))
                           (take 35)
                           (map first))
        groups (group-sizes (cut nodes proposed-cuts))]
    (apply * groups)))

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