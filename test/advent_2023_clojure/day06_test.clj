(ns advent-2023-clojure.day06-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day06 :as d6]
            [advent-2023-clojure.day06-algebra :as d6alg]))

(deftest part1-test
  (are [input expected] (= (d6/part1 input) expected)
                        [[7 9] [15 40] [30 200]] 288
                        [[58 434] [81 1041] [96 2219] [76 1218]] 1159152))

(deftest part2-test
  (are [t d expected] (= (d6/num-winners t d) expected)
                      71530 940200 71503
                      58819676 434104122191218 41513103))

(deftest part1-algebra-test
  (are [input expected] (= (d6alg/part1 input) expected)
                        [[7 9] [15 40] [30 200]] 288
                        [[58 434] [81 1041] [96 2219] [76 1218]] 1159152))

(deftest part2-algebra-test
  (are [t d expected] (= (d6alg/num-winners t d) expected)
                      71530 940200 71503
                      58819676 434104122191218 41513103))
