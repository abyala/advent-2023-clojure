(ns advent-2023-clojure.day16
  (:require [abyala.advent-utils-clojure.point :as p]))

(def up [0 -1])
(def down [0 1])
(def right [1 0])
(def left [-1 0])
(defn move [p dir] [(p/move p dir) dir])

(defn next-steps [cave p dir]
  (let [target (cave p)]
    (filter #(cave (first %)) (case target
                                \. [(move p dir)]
                                \/ [(move p ({up right, down left, left down, right up} dir))]
                                \\ [(move p ({up left, down right, left up, right down} dir))]
                                \| (if (#{up down} dir) [(move p dir)]
                                                        [(move p up) (move p down)])
                                \- (if (#{left right} dir) [(move p dir)]
                                                           [(move p left) (move p right)])))))

(defn energized-tiles [cave starting-point starting-dir]
  (loop [beams (list [starting-point starting-dir]), seen #{}]
    (if-some [[p dir :as beam] (first beams)]
      (if (seen beam)
        (recur (rest beams) seen)
        (recur (apply conj (rest beams) (next-steps cave p dir)) (conj seen beam)))
      (-> (map first seen) set count))))

(defn starting-options [cave]
  (let [[_ [max-x max-y]] (p/bounding-box (map first cave))
        point-dir-range (fn [[range-x range-y dir]] (map vector (map vector range-x range-y) (repeat dir)))]
    (apply concat (map point-dir-range [[(repeat 0) (range 0 (inc max-y)) right]
                                        [(repeat max-x) (range 0 (inc max-y)) left]
                                        [(range 0 (inc max-x)) (repeat 0) down]
                                        [(range 0 (inc max-x)) (repeat max-y) up]]))))

(defn solve [f input]
  (let [cave (p/parse-to-char-coords-map input)]
    (transduce (map (fn [[p dir]] (energized-tiles cave p dir))) max 0 (f cave))))

(defn part1 [input] (solve (fn [_] [[p/origin right]]) input))
(defn part2 [input] (solve starting-options input))