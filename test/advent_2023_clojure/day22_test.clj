(ns advent-2023-clojure.day22-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day22 :refer :all]))

(def test-data (slurp "resources/day22-test.txt"))
(def puzzle-data (slurp "resources/day22-puzzle.txt"))

(deftest part1-test
  (are [input expected] (time (= (part1 input) expected))
                        test-data 5
                        puzzle-data 473))

(deftest part2-test
  (are [input expected] (time (= (part2 input) expected))
                        test-data 7
                        puzzle-data 61045))
