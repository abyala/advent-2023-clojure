(ns advent-2023-clojure.day16-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day16 :refer :all]))

(def test-data (slurp "resources/day16-test.txt"))
(def puzzle-data (slurp "resources/day16-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 46
                        puzzle-data 7046))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-data 51
                        puzzle-data 7313))
