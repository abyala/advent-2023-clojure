(ns advent-2023-clojure.day25-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day25 :refer :all]))

(def puzzle-data (slurp "resources/day25-puzzle.txt"))

(deftest part1-test
  (is (= (part1 puzzle-data) 582590)))
