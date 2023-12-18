(ns advent-2023-clojure.day18-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day18 :refer :all]))

(def test-data (slurp "resources/day18-test.txt"))
(def puzzle-data (slurp "resources/day18-puzzle.txt"))

(deftest part1-test
  (are [input expected] (time (= (part1 input) expected))
                        test-data 62
                        puzzle-data 53844))

(deftest part2-test
  (are [input expected] (time (= (part2 input) expected))
                        test-data 952408144115
                        puzzle-data 42708339569950))
