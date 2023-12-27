(ns advent-2023-clojure.day24
  (:require [abyala.advent-utils-clojure.core :refer :all]
            [abyala.advent-utils-clojure.math :refer [signum]]
            [clojure.string :as str]))

(defn parse-line [line]
  (let [[px py pz dx dy dz] (map parse-long (re-seq #"-?\d+" line))
        slope (/ dy dx)]
    {:px    px, :py py, :pz pz, :dx dx, :dy dy, :dz dz,
     :slope slope :y-intercept (- py (* px slope))}))

(defn parse-input [input]
  (map parse-line (str/split-lines input)))

(defn line-intersections [{m0 :slope, b0 :y-intercept} {m1 :slope, b1 :y-intercept}]
  (when (not= m0 m1)
    (let [x (/ (- b1 b0) (- m0 m1))
          y (+ (* m0 x) b0)]
      [x y])))

(defn cross-in-boundary? [line1 line2 low high]
  (when-let [[x y] (line-intersections line1 line2)]
    (and (<= low x high)
         (<= low y high)
         (= (signum (:dx line1)) (signum (- x (:px line1))))
         (= (signum (:dx line2)) (signum (- x (:px line2)))))))

(defn part1 [low high input]
  (count-when (fn [[line1 line2]] (cross-in-boundary? line1 line2 low high))
              (unique-combinations (parse-input input))))

(defn part2 [input]
  (println "var('t0', 't1', 't2', 'x', 'y', 'z', 'vx', 'vy', 'vz')\na = solve([")
  (println (->> (take 3 (parse-input input))
                (map-indexed (fn [idx {:keys [px py pz dx dy dz]}]
                               (str px " + " dx "*t" idx " == x + vx*t" idx ",\n"
                                    py " + " dy "*t" idx " == y + vy*t" idx ",\n"
                                    pz " + " dz "*t" idx " == z + vz*t" idx)))
                (str/join ",\n")))
  (println "], x, vx, y, vy, z, vz, t0, t1, t2,\nsolution_dict=True)\nprint(a[0][x] + a[0][y] + a[0][z])"))