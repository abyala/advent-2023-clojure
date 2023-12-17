(ns advent-2023-clojure.day17
  (:require [abyala.advent-utils-clojure.point :as p]))

(def up [0 -1])
(def down [0 1])
(def right [1 0])
(def left [-1 0])
(defn turn90 [dir] (if (#{up down} dir) [left right] [up down]))
(defn move [p dir] (mapv + p dir))

(defn parse-input [input]
  (let [points (p/parse-to-char-coords-map (comp parse-long str) input)]
    {:points points, :target (-> points keys p/bounding-box second)}))

(defn estimate-step [island p cost-so-far]
  (+ cost-so-far (p/manhattan-distance p (:target island))))

(defn option-of [island p dir cost]
  {:p p, :dir dir, :cost cost, :estimate (estimate-step island p cost)})

(defn move-step-range [min-steps max-steps island option]
  (let [{:keys [p dir cost]} option
        turns (turn90 dir)]
    (first (reduce (fn [[options prev-p prev-c :as acc] step-num]
                              (let [p' (move prev-p dir)]
                                (if-some [cost ((:points island) p')]
                                  (let [cost' (+ prev-c cost)
                                        options' (if (< step-num min-steps)
                                                   options
                                                   (concat options (map #(option-of island p' % cost') turns)))]
                                    [options' p' cost'])
                                  (reduced acc))))
                            [() p cost]
                            (range 1 (inc max-steps))))))

(defn initial-options [island]
  (let [c (fn [opt1 opt2] (compare ((juxt :estimate :cost :p :dir) opt1)
                                   ((juxt :estimate :cost :p :dir) opt2)))]
    (into (sorted-set-by c) (map #(option-of island [0 0] % 0) [right down]))))

(defn solve [min-steps max-steps input]
  (let [island (parse-input input)
        cache-key (fn [{:keys [p dir]}] [p dir])]
    (loop [options (initial-options island), seen #{}]
      (when-let [option (first options)]
        (cond
          (= (:p option) (:target island)) (:cost option)
          (seen (cache-key option)) (recur (disj options option), seen)
          :else (recur (disj (apply conj options (move-step-range min-steps max-steps island option)) option)
                       (conj seen (cache-key option))))))))

(defn part1 [input] (solve 1 3 input))
(defn part2 [input] (solve 4 10 input))
