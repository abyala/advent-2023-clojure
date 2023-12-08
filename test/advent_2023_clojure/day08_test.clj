(ns advent-2023-clojure.day08-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day08 :refer :all]))

(def test-1-data "RL\n\nAAA = (BBB, CCC)\nBBB = (DDD, EEE)\nCCC = (ZZZ, GGG)\nDDD = (DDD, DDD)\nEEE = (EEE, EEE)\nGGG = (GGG, GGG)\nZZZ = (ZZZ, ZZZ)")
(def test-2-data "LLR\n\nAAA = (BBB, BBB)\nBBB = (AAA, ZZZ)\nZZZ = (ZZZ, ZZZ)")
(def test-3-data "LR\n\n11A = (11B, XXX)\n11B = (XXX, 11Z)\n11Z = (11B, XXX)\n22A = (22B, XXX)\n22B = (22C, 22C)\n22C = (22Z, 22Z)\n22Z = (22B, 22B)\nXXX = (XXX, XXX)\n")
(def puzzle-data (slurp "resources/day08-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        test-1-data 2
                        test-2-data 6
                        puzzle-data 21389))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        test-3-data 6
                        puzzle-data 21083806112641))
