(ns advent-2023-clojure.day07-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day07 :refer :all]))

(def test-data (slurp "resources/day07-test.txt"))
(def puzzle-data (slurp "resources/day07-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 6440
                        puzzle-data 251106089))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-data 5905
                        puzzle-data 249620106))
