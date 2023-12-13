(ns advent-2023-clojure.day13-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day13 :refer :all]))

(def test-data (slurp "resources/day13-test.txt"))
(def puzzle-data (slurp "resources/day13-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 405
                        puzzle-data 35691))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-data 400
                        puzzle-data 39037))