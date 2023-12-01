(ns advent-2023-clojure.day01-indexes
  (:require [abyala.advent-utils-clojure.core :refer :all]
            [clojure.string :as str]))

(def numbers (map vector (map str (range 10)) (range 10)))
(def numbers-and-names (into numbers (map vector ["one" "two" "three" "four" "five" "six" "seven" "eight" "nine"]
                                          (range 1 10))))

(defn find-digit [str-fn seq-fn s search]
  (->> search
       (keep (fn [[n v]] (when-let [idx (str-fn s n)] [idx v])))
       sort
       seq-fn
       second))

(def find-first (partial find-digit str/index-of first))
(def find-last (partial find-digit str/last-index-of last))
(defn calibration-value [search s]
  (+ (* 10 (find-first s search)) (find-last s search)))

(defn solve [search input]
  (transduce (map #(calibration-value search %)) + (str/split-lines input)))
(def part1 (partial solve numbers))
(def part2 (partial solve numbers-and-names))
