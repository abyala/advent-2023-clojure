(ns advent-2023-clojure.day01
  (:require [clojure.string :as str]))

(defn calibration-value [s]
  (->> (re-seq #"\d" s)
       ((juxt first last))
       (apply str)
       parse-long))

(defn replace-numeric-strings [s]
  (reduce-kv (fn [acc idx name] (str/replace acc name (str name (inc idx) name)))
             s
             ["one" "two" "three" "four" "five" "six" "seven" "eight" "nine"]))

(defn solve [f input]
  (transduce (map (comp calibration-value f)) + (str/split-lines input)))

(def part1 (partial solve identity))
(def part2 (partial solve replace-numeric-strings))