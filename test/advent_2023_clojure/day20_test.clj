(ns advent-2023-clojure.day20-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day20 :refer :all]))

(def simple-test-data "broadcaster -> a, b, c\n%a -> b\n%b -> c\n%c -> inv\n&inv -> a")
(def puzzle-data (slurp "resources/day20-puzzle.txt"))

(deftest part1-test
    (are [input expected] (time (= (part1 input) expected))
                        simple-test-data 32000000
                        puzzle-data 684125385))

(deftest part2-test
  (are [input expected] (time (= (part2 input) expected))
                        puzzle-data 225872806380073))
