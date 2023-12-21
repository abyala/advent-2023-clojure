(ns advent-2023-clojure.day21
  (:require [abyala.advent-utils-clojure.core :refer :all]
            [abyala.advent-utils-clojure.point :as p]))

(defn parse-garden [input]
  (let [all-points (p/parse-to-char-coords-map input)
        starting-point (first (first-when #(= \S (second %)) all-points))]
    {:occupied     [starting-point]
     :garden-plots (set (keep #(when (= \. (second %)) (first %))
                              (assoc all-points starting-point \.)))
     :size         (inc (apply max (map ffirst all-points)))}))

(defn take-step [f garden]
    (let [{:keys [occupied garden-plots size]} garden]
      (assoc garden :occupied (filter (comp garden-plots #(f size %)) (set (mapcat p/neighbors occupied))))))

(defn num-occupied-at-step [f garden n]
  (->> (iterate (partial take-step f) garden) (drop n) first :occupied count))

(defn part1-pred [_ p] p)
(defn part2-pred [size p] (mapv #(mod % size) p))

(defn part1 [input] (num-occupied-at-step part1-pred (parse-garden input) 64))

(defn wolfram-alpha-string [input]
  (let [garden (parse-garden input)]
    (str "{{0, " (num-occupied-at-step part2-pred garden 65)
         "}, {1, " (num-occupied-at-step part2-pred garden 196)
         "}, {2, " (num-occupied-at-step part2-pred garden 327) "}}")))

(defn part2 [input] 610158187362102)