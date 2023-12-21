(ns advent-2023-clojure.day21-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day21 :refer :all]))

(def puzzle-data (slurp "resources/day21-puzzle.txt"))

(deftest part1-test
    (are [input expected] (time (= (part1 input) expected))
                        puzzle-data 3689))

(deftest part2-test
  (are [input expected] (time (= (part2 input) expected))
                        puzzle-data 610158187362102))
