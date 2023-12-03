(ns advent-2023-clojure.day03-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day03 :refer :all]))

(def test-data (slurp "resources/day03-test.txt"))
(def puzzle-data (slurp "resources/day03-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 4361
                        puzzle-data 556057))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-data 467835
                        puzzle-data 82824352))