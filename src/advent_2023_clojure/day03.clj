(ns advent-2023-clojure.day03
  (:require [abyala.advent-utils-clojure.core :refer [digit?]]
            [abyala.advent-utils-clojure.point :as p]
            [clojure.string :as str]))

(defn space? [c] (= c \.))
(defn engine-symbol? [c] (not (or (digit? c) (space? c))))
(defn gear-symbol [c] (= c \*))

(defn parse-numbers
  ([input] (->> (str/split-lines input)
                (map-indexed #(parse-numbers %2 %1))
                (apply concat)))
  ([line y] (let [m (re-matcher #"\d+" line)]
              (loop [acc ()]
                (if (.find m)
                  (recur (conj acc {:value  (parse-long (.group m))
                                    :points (set (map #(vector % y) (range (.start m) (.end m))))}))
                  acc)))))

(defn parse-symbols [input]
  (keep (fn [[p v]] (when (engine-symbol? v) {:value v :point p}))
        (p/parse-to-char-coords-map input)))

(defn symbol-adjacencies [symbols numbers]
  (map (fn [{:keys [point] :as m}]
         (let [surr (p/surrounding point)]
           (assoc m :adjacent-numbers (filter #(some (:points %) surr) numbers))))
       symbols))

(defn part1 [input]
  (->> (symbol-adjacencies (parse-symbols input) (parse-numbers input))
       (mapcat :adjacent-numbers)
       (set)
       (map :value)
       (apply +)))

(defn part2 [input]
  (->> (symbol-adjacencies (parse-symbols input) (parse-numbers input))
       (keep (fn [{:keys [value adjacent-numbers]}] (when (and (gear-symbol value)
                                                               (= (count adjacent-numbers) 2))
                                                      (transduce (map :value) * adjacent-numbers))))
       (apply +)))
