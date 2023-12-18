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

(defn shoelace-area
  "Assumes that the list of points has the same first and last value, proving the loop is closed. This uses the
  \"Showlace formula\" for its calculation. "
  ([points] (/ (transduce (map (partial apply shoelace-area)) + (partition 2 1 points)) 2))
  ([[x1 y1] [x2 y2]] (- (* x1 y2) (* x2 y1))))

(defn total-points-within-path
  "Calculates the total number of points enclosed by a sequence of points, expecting the first and last to be the same
  to close the loop. It returns the total number of points, both defined by the perimeter and all enclosed points."
  [path]
  (let [area (shoelace-area path)
        perimeter (transduce (map (partial apply p/manhattan-distance)) + (partition 2 1 path))
        interior (- (inc area) (/ perimeter 2))]
    (+ perimeter interior)))

(defn solve [parse-fn input]
  (->> (parse-instructions parse-fn input) all-turns total-points-within-path))

(defn part1 [input] (solve parse-line input))
(defn part2 [input] (solve parse-line2 input))
