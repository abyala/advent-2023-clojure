(ns advent-2023-clojure.day05
  (:require [abyala.advent-utils-clojure.core :refer [split-blank-line-groups]]))

(defn parse-seeds [line]
  (map parse-long (re-seq #"\d+" (first line))))

(defn parse-rule [line]
  (let [[dest src len] (map parse-long (re-seq #"\d+" line))]
    {:low src, :dest dest, :len len}))

(defn parse-ruleset [lines]
  (map parse-rule (rest lines)))

(defn parse-input [input]
  (let [[seeds-str & rules-str] (split-blank-line-groups input)]
    {:seeds    (parse-seeds seeds-str)
     :rulesets (map parse-ruleset rules-str)}))

(defn target-range [rule seed-range]
  (let [{:keys [low dest len]} rule
        [seed-low seed-len] seed-range]
    (when (<= low seed-low (+ low len -1))
      [(+ dest (- seed-low low))
       (min seed-len (+ low len (- seed-low)))])))

(defn convert-range [ruleset seed-ranges]
  (when-some [[seed-low seed-len :as seed-range] (first seed-ranges)]
    (if-some [[_ target-len :as target] (first (keep #(target-range % seed-range) ruleset))]
      (let [next-seed-ranges (if (= target-len seed-len) (rest seed-ranges)
                                                         (cons [(+ seed-low target-len) (- seed-len target-len)]
                                                               (rest seed-ranges)))]
        (cons target (lazy-seq (convert-range ruleset next-seed-ranges))))
      (cons seed-range (lazy-seq (convert-range ruleset (rest seed-ranges)))))))

(defn solve [seed-fn input]
  (let [{:keys [:seeds :rulesets]} (parse-input input)]
    (->> (seed-fn seeds)
         (mapcat (fn [seed-ranges] (reduce #(convert-range %2 %1) (list seed-ranges) rulesets)))
         (map first)
         (apply min))))

(defn part1 [input]
  (solve (partial map #(vector % 1)) input))

(defn part2 [input]
  (solve #(partition 2 %) input))