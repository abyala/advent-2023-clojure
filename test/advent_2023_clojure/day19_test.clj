(ns advent-2023-clojure.day19-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day19 :as d19]
            [advent-2023-clojure.day19-consolidated :as d19c]))

(def test-data (slurp "resources/day19-test.txt"))
(def puzzle-data (slurp "resources/day19-puzzle.txt"))

(deftest part1-test
  (are [input expected] (time (= (d19/part1 input) expected))
                        test-data 19114
                        puzzle-data 492702)
  (are [input expected] (time (= (d19c/part1 input) expected))
                        test-data 19114
                        puzzle-data 492702))

(deftest part2-test
  (are [input expected] (time (= (d19/part2 input) expected))
                        test-data 167409079868000
                        puzzle-data 138616621185978)
  (are [input expected] (time (= (d19c/part2 input) expected))
                        test-data 167409079868000
                        puzzle-data 138616621185978))
