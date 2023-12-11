(ns advent-2023-clojure.day11-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day11 :refer :all]))

(def test-data (slurp "resources/day11-test.txt"))
(def puzzle-data (slurp "resources/day11-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 374
                        puzzle-data 10313550))

(deftest solve-test
  (are [input n expected] (= (solve n input) expected)
                          test-data 9 1030
                          test-data 99 8410))

(deftest part2-test
  (is (= (part2 puzzle-data) 611998089572)))
