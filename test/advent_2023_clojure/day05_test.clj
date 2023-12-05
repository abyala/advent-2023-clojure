(ns advent-2023-clojure.day05-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day05 :refer :all]))

(def test-data (slurp "resources/day05-test.txt"))
(def puzzle-data (slurp "resources/day05-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 35
                        puzzle-data 1181555926))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-data 46
                        puzzle-data 37806486))
