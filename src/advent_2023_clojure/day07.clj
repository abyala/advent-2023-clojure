(ns advent-2023-clojure.day07
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :refer :all]))

(defn- map-by-index [coll] (into {} (map-indexed #(vector %2 %1) coll)))

(def joker \J)
(def hand-freq-points (map-by-index [[1 1 1 1 1], [2 1 1 1], [2 2 1], [3 1 1], [3 2], [4 1], [5]]))
(def scoring-map-1 (map-by-index "23456789TJQKA"))
(def scoring-map-2 (map-by-index "J23456789TQKA"))

(defn parse-line [line]
  (let [[hand bid] (str/split line #" ")] {:hand hand, :bid (parse-long bid)}))

(defn parse-input [input]
  (map parse-line (str/split-lines input)))

(defn hand-points [hand]
  (->> hand frequencies vals (sort-by -) hand-freq-points))

(defn card-points [scoring-map hand]
  (mapv scoring-map hand))

(defn apply-jokers [hand]
  (if-some [most-common-card (as-> (frequencies hand) x
                                   (dissoc x joker)
                                   (sort-by (comp - second) x)
                                   (ffirst x))]
    (str/replace hand joker most-common-card)
    hand))

(defn sort-hands [joker-fn scoring-map hands]
  (sort-by (fn [{hand :hand}] [(-> hand joker-fn hand-points)
                               (card-points scoring-map hand)])
           hands))

(defn winnings [rank hand] (* (inc rank) (:bid hand)))

(defn solve [joker-fn scoring-map input]
  (transduce (map-indexed winnings) + (sort-hands joker-fn scoring-map (parse-input input))))

(defn part1 [input] (solve identity scoring-map-1 input))
(defn part2 [input] (solve apply-jokers scoring-map-2 input))