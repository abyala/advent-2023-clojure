(ns advent-2023-clojure.day15
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :refer [index-of-first]]))

(defn HASH [s] (reduce #(-> %1 (+ (int %2)) (* 17) (mod 256)) 0 s))

(defn parse-instructions [input]
  (map #(let [[_ label op focal-length] (re-matches #"(\w+)([=-])(\d*)" %)]
          [({"-" :remove, "=" :set} op), label, (parse-long focal-length)])
       (str/split input #",")))

(defn process-instructions [instructions]
  (reduce (fn [acc [op label len]]
            (let [box (HASH label)
                  idx (index-of-first #(= label (first %)) (acc box))]
              (case [op (some? idx)]
                [:set false] (update acc box conj [label len])
                [:set true] (assoc-in acc [box idx 1] len)
                [:remove false] acc
                [:remove true] (update acc box #(into (subvec % 0 idx) (subvec % (inc idx)))))))
          (into {} (map vector (range 256) (repeat [])))
          instructions))

(defn total-focusing-power [boxes]
  (transduce (mapcat (fn [[box lenses]]
                       (map-indexed (fn [idx [_ len]] (* (inc box) (inc idx) len)) lenses)))
             + boxes))

(defn part1 [input] (transduce (map HASH) + (str/split input #",")))
(defn part2 [input] (-> input parse-instructions process-instructions total-focusing-power))
