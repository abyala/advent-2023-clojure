(ns advent-2023-clojure.day10
  (:require [abyala.advent-utils-clojure.core :refer :all]
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
(defn part2 [input] (-> (parse-maze input) loop-path p/polygon-interior-point-count))