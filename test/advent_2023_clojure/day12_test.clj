(ns advent-2023-clojure.day12-test
  (:require [clojure.test :refer :all]
            [advent-2023-clojure.day12 :refer :all]))

(def test-data (slurp "resources/day12-test.txt"))
(def puzzle-data (slurp "resources/day12-puzzle.txt"))

(deftest search-space-for-test
  (are [s group expected] (= (search-space-for s group) expected)
                          "#.#.###" 1 "#."
                          "#.#.###" 2 "#.#"
                          ".#.#.###" 1 ".#."
                          ".#.#.###" 2 ".#.#"
                          "?.###." 1 "?.##"
                          "?.###." 2 "?.###"
                          "?.###." 3 "?.###."
                          "##" 3 "##"
                          "?.?.?" 1 "?.?.?") )

(deftest drop-leading-dots-test
  (are [s expected] (= (drop-leading-dots s) expected)
                    "#?#.?#?" "#?#.?#?"
                    "###" "###"
                    ".###" "###"
                    "...###" "###"
                    "...#..." "#..."
                    "" ""))

(deftest search-space-matches-test
  (are [s g expected] (= (search-space-matches s g) expected)
                      "????" 1 {"??" 1, "?" 1, "" 2}
                      "???.?#?" 2 {"?#?" 2, "" 2}
                      "?" 1 {"" 1}
                      "" 1 {}
                      "?" 2 {}
                      "###" 2 {}))

(deftest replace-next-search-space-test
  (are [s groups expected] (= (replace-next-search-space s groups) expected)
                           "???.###" (list 1 1 3) {"?.###" 1, "###" 2}
                           "?.###" (list 1 3) {"###" 1}
                           "????##" (list 2) {"?##" 1, "##" 1, "" 1}
                           "?#?" (list 2) {"" 2}
                           ".?." (list 2) {}))

(deftest dead-end?-test
  (are [s g] (true? (dead-end? s g))
             "#" ()
             "?#?" ())
  (are [s g] (false? (dead-end? s g))
             "" ()
             ".?" ()
             "#" (list 1)
             "." (list 1)
             "" (list 1)))

(deftest success?-test
  (are [s g] (true? (success? s g))
             "" ()
             "." ()
             "?" ()
             "?..??..?" ())
  (are [s g] (false? (success? s g))
             "#" ()
             "#.?" ()
             "#" (list 1)
             "." (list 1)
             "" (list 1)))

(deftest num-arrangements-test
  (are [s g expected] (= (num-arrangements s g) expected)
                      "???.###" (list 1 1 3) 1
                      ".??..??...?##." (list 1 1 3) 4
                      "?#?#?#?#?#?#?#?" (list 1 3 1 6) 1
                      "????.#...#..." (list 4 1 1) 1
                      "????.######..#####." (list 1 6 5) 4
                      "?###????????" (list 3 2 1) 10))

(deftest unfold-test
  (are [n line expected] (= (unfold n (parse-row line)) expected)
                         1 ".# 1" [".#" (list 1)]
                         5 ".# 1" [".#?.#?.#?.#?.#" (list 1 1 1 1 1)]
                         1 "???.### 1,1,3" ["???.###" (list 1 1 3)]
                         5 "???.### 1,1,3" ["???.###????.###????.###????.###????.###"
                                            (list 1,1,3,1,1,3,1,1,3,1,1,3,1,1,3)]))

(deftest part1-test
  (are [input expected] (time (= expected (part1 input)))
                        test-data 21
                        puzzle-data 7204))

(deftest part2-test
  (are [input expected] (time (= expected (part2 input)))
                        test-data 525152
                        puzzle-data 1672318386674))