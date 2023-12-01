(ns advent-2023-clojure.day01-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day01 :refer :all]))

(def test1-data "1abc2\npqr3stu8vwx\na1b2c3d4e5f\ntreb7uchet")
(def test2-data "two1nine\neightwothree\nabcone2threexyz\nxtwone3four\n4nineeightseven2\nzoneight234\n7pqrstsixteen")
(def puzzle-data (slurp "resources/day01-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= expected (part1 input))
                        test1-data 142
                        puzzle-data 54597))

(deftest part2-test
  (are [input expected] (= expected (part2 input))
                        test2-data 281
                        puzzle-data 54504))