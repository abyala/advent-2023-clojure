(ns advent-2023-clojure.day09-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day09 :refer :all]))

(def test-data (slurp "resources/day09-test.txt"))
(def puzzle-data (slurp "resources/day09-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 114
                        puzzle-data 1916822650))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-data 2
                        puzzle-data 966))
