(ns advent-2023-clojure.day11
  (:require [abyala.advent-utils-clojure.point :as p]))

(defn expand-row [expand-by galaxies row]
  (map (fn [[x y :as coords]] (if (> y row) [x (+ y expand-by)] coords)) galaxies))

(defn expand-column [expand-by galaxies column]
  (map (fn [[x y :as coords]] (if (> x column) [(+ x expand-by) y] coords)) galaxies))

(defn expand-universe [expand-by galaxies [[x0 y0] [x1 y1]]]
  (let [expansion-rows (remove (set (map second galaxies)) (range y1 (dec y0) -1))
        expansion-columns (remove (set (map first galaxies)) (range x1 (dec x0) -1))]
    (as-> galaxies x
          (reduce #(expand-row expand-by %1 %2) x expansion-rows)
          (reduce #(expand-column expand-by %1 %2) x expansion-columns))))

(defn solve [expand-by input]
  (let [points (p/parse-to-char-coords input)
        bounds (p/bounding-box (map first points))
        galaxies (keep (fn [[p c]] (when (= c \#) p)) points)
        expanded-galaxies (expand-universe expand-by galaxies bounds)]
    (apply + (for [g1 expanded-galaxies
                   g2 expanded-galaxies
                   :while (not= g1 g2)]
               (p/manhattan-distance g1 g2)))))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 999999 input))                   ; Not 1,000,000
