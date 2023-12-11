(ns advent-2023-clojure.day10
  (:require [clojure.set :as set]
            [abyala.advent-utils-clojure.core :refer :all]
            [abyala.advent-utils-clojure.point :as p]))

(def north [0 -1])
(def south [0 1])
(def east [1 0])
(def west [-1 0])
(def connecting-dirs {\| #{north south}, \- #{east west}, \L #{north east},
                      \J #{north west}, \7 #{south west}, \F #{south east}
                      \S #{north south east west}})
(defn reverse-dir [dir] ({north south, south north, east west, west east} dir))

(defn parse-maze [input]
  (into {} (remove #(= \. (second %)) (p/parse-to-char-coords input))))

(defn maze-start [maze] (first (first-when #(= \S (second %)) maze)))

(defn connected-steps [maze p]
  (->> (maze p)
       connecting-dirs
       (keep (fn [dir-taken] (let [p' (mapv + p dir-taken)
                                   c' (maze p')]
                               (when (and c' ((connecting-dirs c') (reverse-dir dir-taken)))
                                 p'))))))

(defn loop-path [maze]
  (let [start (maze-start maze)]
    (letfn [(next-step [p previous]
              (when (not (and previous (= p start)))
                (cons p (lazy-seq (next-step (first-when #(not= % previous)
                                                         (connected-steps maze p))
                                             p)))))]
      (next-step start nil))))

(defn part1 [input] (-> (parse-maze input) loop-path count (quot 2)))

(defn rebind-maze-start [maze]
  (let [start (maze-start maze)]
    (assoc maze start (->> [north south east west]
                           (filter #(maze (map + start %)))
                           set
                           ((set/map-invert connecting-dirs))))))

(defn num-enclosed-by-line [maze points min-x max-x y]
  (let [flip (fn [v] (if (= v :outside) :inside :outside))]
    (first (reduce (fn [[n loc :as acc] p]
                     (let [c (maze p)]
                       (cond (or (nil? c) (not (points p))) [(if (= loc :inside) (inc n) n) loc]
                             (#{\L \J \|} c) [n (flip loc)]
                             :else acc)))
                   [0 :outside]
                   (map vector (range min-x max-x) (repeat y))))))

(defn part2 [input]
  (let [maze (parse-maze input)
        points (set (loop-path maze))
        [[x0 y0] [x1 y1]] (p/bounding-box points)
        maze' (rebind-maze-start maze)]
    (transduce (map #(num-enclosed-by-line maze' points x0 x1 %)) + (range y0 y1))))
