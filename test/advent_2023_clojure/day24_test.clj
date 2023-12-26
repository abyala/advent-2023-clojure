(ns advent-2023-clojure.day24-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day24 :refer :all]))

(def test-data (slurp "resources/day24-test.txt"))
(def puzzle-data (slurp "resources/day24-puzzle.txt"))

(deftest part1-test
    (are [input low high expected] (time (= (part1 low high input) expected))
                          test-data 7 27 2
                          puzzle-data 200000000000000 400000000000000 28174))
;
;(deftest part2-test
;  (are [input expected] (time (= (part2 input) expected))
;                        test-data 154
;                        puzzle-data 6410))
;