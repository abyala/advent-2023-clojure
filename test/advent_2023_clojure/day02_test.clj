(ns advent-2023-clojure.day02-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day02 :refer :all]))

(def test-data (slurp "resources/day02-test.txt"))
(def puzzle-data (slurp "resources/day02-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= expected (part1 input))
                        test-data 8
                        puzzle-data 2541))

(deftest part2-test
  (are [input expected] (= expected (part2 input))
                        test-data 2286
                        puzzle-data 66016))