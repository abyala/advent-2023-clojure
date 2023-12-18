(ns advent-2023-clojure.day18
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.point :as p]))

(defn parse-line [line]
  (let [[_ dir n _] (re-matches #"(\w{1}) (\d+) .*" line)]
    [({"U" [0 -1], "D" [0 1], "L" [-1 0], "R" [1 0]} dir) (parse-long n)]))

(defn parse-line2 [line]
  (let [[_ dist-hex dir-hex] (re-matches #"\w \d+ \(\#(\w{5})(\w)\)" line)]
    [([[1 0] [0 1] [-1 0] [0 -1]] (parse-long dir-hex)) (Integer/parseInt dist-hex 16)]))

(defn parse-instructions [line-parser input]
  (map line-parser (str/split-lines input)))

(defn move-seq [p dir]
  (let [p' (mapv + p dir)]
    (cons p' (lazy-seq (move-seq p' dir)))))

(defn all-turns [instructions]
  (reduce (fn [acc [dir dist]] (conj acc (mapv + (last acc)
                                                 (mapv * dir (repeat dist)))))
          [p/origin]
          instructions))

(defn solve [parse-fn input]
  (->> (parse-instructions parse-fn input) all-turns p/polygon-total-point-count))

(defn part1 [input] (solve parse-line input))
(defn part2 [input] (solve parse-line2 input))
