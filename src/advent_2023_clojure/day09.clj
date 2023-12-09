(ns advent-2023-clojure.day09
  (:require [clojure.string :as str]))

(defn parse-line [line] (map parse-long (str/split line #" ")))
(defn parse-input [input] (map parse-line (str/split-lines input)))

(defn diff-seq [coll]
  (when (not-every? zero? coll)
    (cons coll (lazy-seq (diff-seq (map (fn [[a b]] (- b a))
                                        (partition 2 1 coll)))))))

(defn extrapolate [next-value? differences]
  (let [[op dir] (if next-value? [+ last] [- first])]
    (reduce (fn [child sibling] (op sibling child))
            0
            (map dir (reverse differences)))))

(defn solve [next-value? input]
  (transduce (map (comp (partial extrapolate next-value?) diff-seq)) + (parse-input input)))

(defn part1 [input] (solve true input))
(defn part2 [input] (solve false input))