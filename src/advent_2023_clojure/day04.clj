(ns advent-2023-clojure.day04
  (:require [clojure.string :as str]
            [clojure.set :as set]))

(defn line-wins [line]
  (let [parse-numbers (fn [s] (->> (re-seq #"\d+" s) (map parse-long) set))
        [_ winners mine] (re-seq #"[^:\|]+" line)]
    (count (set/intersection (parse-numbers winners) (parse-numbers mine)))))

(defn line-points [line]
  (let [wins (line-wins line)]
    (if (zero? wins) 0 (long (Math/pow 2 (dec wins))))))

(defn part1 [input]
  (transduce (map line-points) + (str/split-lines input)))

(defn part2 [input]
  (last (reduce (fn [[cards row acc] line]
                  (let [wins (line-wins line)
                        num-cards-here (inc (get cards row 0))
                        future-cards (reduce #(assoc %1 (+ %2 row) num-cards-here) {} (range 1 (inc wins)))]
                    [(merge-with + cards future-cards) (inc row) (+ acc num-cards-here)]))
                [{} 1 0]
                (str/split-lines input))))