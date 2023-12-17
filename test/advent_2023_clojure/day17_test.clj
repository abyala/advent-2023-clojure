(ns advent-2023-clojure.day17-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day17 :refer :all]))

(def test-data (slurp "resources/day17-test.txt"))
(def puzzle-data (slurp "resources/day17-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 102
                        puzzle-data 742))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-data 94
                        puzzle-data 918))
