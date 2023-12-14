(ns advent-2023-clojure.day14-protocols
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.point :as p]))

(defn parse-input [input]
  (let [flipped (->> input str/split-lines reverse (str/join "\n"))
        all-points (update-keys (p/parse-to-char-coords-map flipped) #(update % 1 inc))
        rounded-rocks (set (keep (fn [[p c]] (when (= c \O) p)) all-points))
        cube-shaped-rocks (set (keep (fn [[p c]] (when (= c \#) p)) all-points))
        max-x (transduce (map first) max 0 (keys all-points))
        max-y (transduce (map second) max 0 (keys all-points))]
    {:rounded rounded-rocks, :cube cube-shaped-rocks, :max-x max-x, :max-y max-y}))

(defn occupied? [platform p]
  ((some-fn (:rounded platform) (:cube platform)) p))

(defprotocol SlideStrategy
  (rock-sorter [this])
  (range-creator [this platform p]))

(defn- slide [strategy platform]
  (let [slide-rock (fn [acc p] (let [acc' (update acc :rounded disj p)]
                                 (if-some [p' (->> (range-creator strategy acc p)
                                                   (take-while #(not (occupied? acc' %)))
                                                   last)]
                                   (update acc' :rounded conj p')
                                   acc)))]
    (reduce (partial slide-rock) platform (sort-by (rock-sorter strategy) (:rounded platform)))))

(def slide-north
  (partial slide (reify SlideStrategy
                   (rock-sorter [_] (comp - second))
                   (range-creator [_ {:keys [max-y]} [x y]] (map vector (repeat x) (range (inc y) (inc max-y)))))))

(def slide-west
  (partial slide (reify SlideStrategy
                   (rock-sorter [_] first)
                   (range-creator [_ _ [x y]] (map vector (range (dec x) -1 -1) (repeat y))))))

(def slide-south
  (partial slide (reify SlideStrategy
                   (rock-sorter [_] second)
                   (range-creator [_ _ [x y]] (map vector (repeat x) (range (dec y) 0 -1))))))

(def slide-east
  (partial slide (reify SlideStrategy
                   (rock-sorter [_] (comp - first))
                   (range-creator [_ {:keys [max-x]} [x y]] (map vector (range (inc x) (inc max-x)) (repeat y))))))

(defn rock-cycle [platform]
  (-> platform slide-north slide-west slide-south slide-east))

(defn total-load [platform]
  (transduce (map second) + (:rounded platform)))

(defn part1 [input]
  (->> input parse-input slide-north total-load))

(defn nth-spin [platform target]
  (reduce (fn [[seen n] p] (if-some [loop-start (seen p)]
                             (let [loop-size (- n loop-start)
                                   target-idx (+ loop-start (mod (- target loop-start) loop-size))]
                               (reduced (ffirst (filter (fn [[k v]] (when (= v target-idx) k)) seen))))
                             [(assoc seen p n) (inc n)]))
          [{} 0]
          (iterate rock-cycle platform)))

(defn part2 [input]
  (-> input parse-input (nth-spin 1000000000) total-load))