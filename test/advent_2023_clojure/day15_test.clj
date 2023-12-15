(ns advent-2023-clojure.day15-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day15 :refer :all]))

(def test-data "rn=1,cm-,qp=3,cm=2,qp-,pc=4,ot=9,ab=5,pc-,pc=6,ot=7")
(def puzzle-data (slurp "resources/day15-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-data 1320
                        puzzle-data 511416))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-data 145
                        puzzle-data 290779))