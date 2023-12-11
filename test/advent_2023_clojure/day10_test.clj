(ns advent-2023-clojure.day10-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day10 :refer :all]))

(def simple-test-data (slurp "resources/day10-simple-test.txt"))
(def complex-test-data (slurp "resources/day10-complex-test.txt"))
(def part2-test-1-data "...........\n.S-------7.\n.|F-----7|.\n.||.....||.\n.||.....||.\n.|L-7.F-J|.\n.|..|.|..|.\n.L--J.L--J.\n...........")
(def part2-test-2-data ".F----7F7F7F7F-7....\n.|F--7||||||||FJ....\n.||.FJ||||||||L7....\nFJL7L7LJLJ||LJ.L-7..\nL--J.L7...LJS7F-7L7.\n....F-J..F7FJ|L7L7L7\n....L7.F7||L7|.L7L7|\n.....|FJLJ|FJ|F7|.LJ\n....FJL-7.||.||||...\n....L---J.LJ.LJLJ...")
(def part2-test-3-data "FF7FSF7F7F7F7F7F---7\nL|LJ||||||||||||F--J\nFL-7LJLJ||||||LJL-77\nF--JF--7||LJLJ7F7FJ-\nL---JF-JLJ.||-FJLJJ7\n|F|F-JF---7F7-L7L|7|\n|FFJF7L7F-JF7|JL---7\n7-L-JL7||F7|L7F-7F7|\nL.L7LFJ|||||FJL7||LJ\nL7JLJL-JLJLJL--JLJ.L")
(def puzzle-data (slurp "resources/day10-puzzle.txt"))

(deftest part1-test
  (are [input expected] (= (part1 input) expected)
                        simple-test-data 4
                        complex-test-data 8
                        puzzle-data 7005))

(deftest part2-test
  (are [input expected] (= (part2 input) expected)
                        simple-test-data 1
                        complex-test-data 1
                        part2-test-1-data 4
                        part2-test-2-data 8
                        part2-test-3-data 10
                        puzzle-data 417))
