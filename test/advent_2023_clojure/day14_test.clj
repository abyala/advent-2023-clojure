(ns advent-2023-clojure.day14-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day14 :refer :all]))

(def test-data (slurp "resources/day14-test.txt"))
(def puzzle-data (slurp "resources/day14-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 136
                        puzzle-data 108935))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-data 64
                        puzzle-data 100876))