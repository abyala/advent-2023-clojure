(ns advent-2023-clojure.day06-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day06 :refer :all]))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        [[7 9] [15 40] [30 200]] 288
                        [[58 434] [81 1041] [96 2219] [76 1218]] 1159152))

(deftest part2-test
  (are [t d expected] (= (num-winners t d) expected)
                      71530 940200 71503
                      58819676 434104122191218 41513103))
