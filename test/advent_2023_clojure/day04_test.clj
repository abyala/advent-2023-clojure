(ns advent-2023-clojure.day04-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day04 :refer :all]))

(def test-data (slurp "resources/day04-test.txt"))
(def puzzle-data (slurp "resources/day04-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 13
                        puzzle-data 24175))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-data 30
                        puzzle-data 18846301))