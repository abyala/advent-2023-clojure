(ns advent-2023-clojure.day23-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day23 :refer :all]))

(def test-data (slurp "resources/day23-test.txt"))
(def puzzle-data (slurp "resources/day23-puzzle.txt"))

(deftest part1-test
    (are [input expected] (time (= (part1 input) expected))
                          test-data 94
                          puzzle-data 2194))

(deftest part2-test
  (are [input expected] (time (= (part2 input) expected))
                        test-data 154
                        puzzle-data 6410))
