(ns advent-2023-clojure.day02
  (:require [clojure.string :as str]))

(defn parse-game [line]
  (let [[_ game-num & boxes] (re-seq #"\w+" line)]
    {:game-num (parse-long game-num)
     :cubes    (reduce (fn [acc [n cube]] (update acc (keyword cube) max (parse-long n)))
                       {:red 0, :green 0, :blue 0}
                       (partition 2 boxes))}))

(defn playable? [{:keys [cubes]}]
  (= cubes (merge-with min cubes {:red 12, :green 13, :blue 14})))

(defn part1 [input]
  (transduce (comp (map parse-game) (filter playable?) (map :game-num)) + (str/split-lines input)))

(defn cube-power [game]
  (->> game :cubes vals (apply *)))

(defn part2 [input]
  (transduce (map (comp cube-power parse-game)) + (str/split-lines input)))
