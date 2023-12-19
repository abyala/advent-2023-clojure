(ns advent-2023-clojure.day18
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.point :as p :refer [up down left right origin]]))

(defn parse-line [line]
  (let [[_ dir n _] (re-matches #"(\w{1}) (\d+) .*" line)]
    [({"U" up, "D" down, "L" left, "R" right} dir) (parse-long n)]))

(defn parse-line2 [line]
  (let [[_ dist-hex dir-hex] (re-matches #"\w \d+ \(\#(\w{5})(\w)\)" line)]
    [([right up left down] (parse-long dir-hex)) (Integer/parseInt dist-hex 16)]))

(defn parse-instructions [line-parser input]
  (map line-parser (str/split-lines input)))

(defn all-turns [instructions]
  (reduce (fn [acc [dir dist]] (conj acc (p/move (last acc) dir dist)))
          [origin]
          instructions))

(defn solve [parse-fn input]
  (->> (parse-instructions parse-fn input) all-turns p/polygon-total-point-count))

(defn part1 [input] (solve parse-line input))
(defn part2 [input] (solve parse-line2 input))
