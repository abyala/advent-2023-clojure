(ns advent-2023-clojure.day22
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [abyala.advent-utils-clojure.core :refer [count-when split-longs]]))

(defn parse-brick-points [line]
  (let [[x0 y0 z0 x1 y1 z1] (split-longs line)]
    (set (for [x (range x0 (inc x1))
               y (range y0 (inc y1))
               z (range z0 (inc z1))]
           [x y z]))))

(defn parse-input [input] (mapv parse-brick-points (str/split-lines input)))

(defn grounded? [brick] (some #(= (% 2) 1) brick))

(defn drop-brick [brick] (set (map #(update % 2 dec) brick)))

(defn drop-bricks-to-ground [bricks]
  (loop [state bricks]
    (let [state' (first (reduce (fn [[acc acc-union] brick-id]
                                  (let [brick (acc brick-id)
                                        brick' (drop-brick brick)
                                        other-bricks (set/difference acc-union brick)]
                                    (if (or (grounded? brick) (some other-bricks brick'))
                                      [acc acc-union]
                                      [(assoc acc brick-id brick') (set/union brick' other-bricks)])))
                                [state (apply set/union state)]
                                (range (count bricks))))]
      (if (= state state') state (recur state')))))

(defn supporting-brick-ids [bricks dead-brick-id]
  (->> (drop-brick (bricks dead-brick-id))
       (mapcat (fn [cube] (keep-indexed (fn [brick-id brick] (when (and (not= brick-id dead-brick-id) (brick cube))
                                                               brick-id))
                                        bricks)))
       set))

(defn critical-brick-ids [bricks]
  (set (keep (fn [brick-id] (let [supporters (supporting-brick-ids bricks brick-id)]
                           (when (= (count supporters) 1)
                             (first supporters))))
              (range (count bricks)))))

(defn num-cascading-bricks [bricks dead-brick-id]
  (let [start-state (apply conj (subvec bricks 0 dead-brick-id) (subvec bricks (inc dead-brick-id)))
        end-state (drop-bricks-to-ground start-state)]
    (count-when false? (map = start-state end-state))))

(defn solve [f input]
  (let [bricks (drop-bricks-to-ground (parse-input input))
        critical (critical-brick-ids bricks)]
    (f bricks critical)))

(def part1 (partial solve (fn [bricks critical-ids] (- (count bricks) (count critical-ids)))))
(def part2 (partial solve (fn [bricks critical-ids]
                            (transduce (map #(num-cascading-bricks bricks %)) + critical-ids))))