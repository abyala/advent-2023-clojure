(ns advent-2023-clojure.day08
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :refer :all]
            [clojure.math.numeric-tower :as math]))

(defn parse-input [input]
  (let [[instructions _ & mappings] (str/split-lines input)]
    {:instructions instructions
     :paths        (reduce (fn [acc line]
                             (let [[src dest1 dest2] (re-seq #"\w{3}" line)]
                               (assoc acc [src \L] dest1 [src \R] dest2)))
                           {}
                           mappings)}))

(defn all-steps [instructions paths start-loc]
  (letfn [(next-step [loc turns] (let [[turn & turns'] turns
                                       loc' (paths [loc turn])]
                                   (cons loc (lazy-seq (next-step loc' turns')))))]
    (next-step start-loc (cycle instructions))))

(defn steps-to-end [end-loc? instructions paths start-loc]
  (index-of-first #(end-loc? %)
                  (all-steps instructions paths start-loc)))

(defn solve [start-locs-fn end-loc? input]
  (let [{:keys [instructions paths]} (parse-input input)]
    (reduce math/lcm (map #(steps-to-end end-loc? instructions paths %)
                          (start-locs-fn paths)))))

(defn single-start [_] ["AAA"])
(defn multi-start [paths] (->> (map ffirst paths)
                               (filter #(str/ends-with? % "A"))
                               set))
(defn part1 [input]
  (solve single-start #(= % "ZZZ") input))

(defn part2 [input]
  (solve multi-start #(str/ends-with? % "Z") input))