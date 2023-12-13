(ns advent-2023-clojure.day13
  (:require [clojure.set :as set]
            [abyala.advent-utils-clojure.core :refer :all]))

(defn mirror? [line idx]
  (let [[left right] (split-at idx line)]
    (every? identity (map = (reverse left) right))))

(defn mirror-indexes [lines]
  (let [first-line (first lines)
        possible-indexes (filter #(mirror? first-line %) (range 1 (count first-line)))]
    (filter (fn [idx] (every? #(mirror? % idx) lines)) possible-indexes)))

(defn vertical-mirror-indexes [pattern]
  (mirror-indexes pattern))

(defn horizontal-mirror-indexes [pattern]
  (mirror-indexes (map (fn [idx] (map #(get % idx) pattern)) (range (count (first pattern))))))

(defn points-for [v-indexes h-indexes]
  (apply + (concat v-indexes (map #(* 100 %) h-indexes))))

(defn pattern-points [pattern]
  (points-for (vertical-mirror-indexes pattern) (horizontal-mirror-indexes pattern)))

(defn part1 [input]
  (transduce (map pattern-points) + (split-blank-line-groups input)))

(defn all-smudges [pattern]
  (for [x (range (count pattern))
        y (range (count (first pattern)))]
    (update-in pattern [x y] {\. \#, \# \.})))

(defn smudgy-pattern-points [pattern]
  (let [h (set (horizontal-mirror-indexes pattern))
        v (set (vertical-mirror-indexes pattern))]
    (->> (all-smudges (mapv vec pattern))
         (keep (fn [smudge] (let [h' (-> (horizontal-mirror-indexes smudge) set (set/difference h))
                                  v' (-> (vertical-mirror-indexes smudge) set (set/difference v))]
                              (when (or (seq h') (seq v'))
                                (points-for v' h')))))
         first)))

(defn part2 [input]
  (transduce (map smudgy-pattern-points) + (split-blank-line-groups input)))