(ns advent-2023-clojure.day06-algebra
  (:require [abyala.advent-utils-clojure.math :as m]
            [clojure.math :refer [ceil]]))

(defn num-winners [total-time previous-best]
  (let [[low-root high-root] (sort (m/quadratic-roots -1 total-time (- previous-best)))
        low (inc (int low-root))
        high (dec (int (ceil high-root)))]
    (inc (- high low))))

(defn part1 [races]
  (transduce (map (fn [[time best]] (num-winners time best))) * races))

