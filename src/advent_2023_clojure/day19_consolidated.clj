(ns advent-2023-clojure.day19-consolidated
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.core.match :refer [match]]
            [abyala.advent-utils-clojure.core :refer :all]
            [abyala.advent-utils-clojure.math :refer [signum]]))

(def start-rule "in")
(def accepted "A")
(def rejected "R")
(def rating-names ["x" "m" "a" "s"])

(defn parse-rule [s]
  (let [[_ v0 v1 v2 v3] (re-find #"(\w+)([<>])?(\d+)?\:?(\w+)?" s)]
    (if v1
      {:target v3, :op v1, :rating v0, :amount (parse-long v2)}
      {:target v0})))

(defn parse-workflow [line]
  (let [[name & rules] (str/split line #"[\{\,\}]")]
    [name (map parse-rule rules)]))

(defn parse-part [line] (update-keys (edn/read-string (str/replace line "=" " ")) str))

(defn parse-input [input]
  (let [[workflow-str parts-str] (split-by-blank-lines input)]
    [(into {} (mapv parse-workflow (str/split-lines workflow-str))) (map parse-part (str/split-lines parts-str))]))

(defn split-parts-by-rule [rule parts]
  (let [{:keys [target op rating amount]} rule
        [low high] (get parts rating)]
    (match [op (when op (signum (- amount low))) (when op (signum (- amount high)))]
           [nil _ _] [parts target nil]
           ["<" (:or 0 -1) _] [nil nil parts]
           ["<" _ 1] [parts target nil]
           ["<" _ _] [(assoc-in parts [rating 1] (dec amount)) target (assoc-in parts [rating 0] amount)]
           [">" _ (:or 0 1)] [nil nil parts]
           [">" -1 _] [parts target nil]
           [">" _ _] [(assoc-in parts [rating 0] (inc amount)) target (assoc-in parts [rating 1] amount)])))

(defn split-parts-by-workflow [workflow parts]
  (->> workflow
       (reduce (fn [[outputs leftovers] rule]
                 (let [[good target bad] (split-parts-by-rule rule leftovers)]
                   [(if good (conj outputs [target good]) outputs) bad]))
               [() parts])
       first))

(defn num-combos [parts]
  (transduce (map (fn [[low high]] (- (inc high) low))) * (vals parts)))

(defn matching-parts [workflows]
  (loop [options [[start-rule (zipmap rating-names (repeat [1 4000]))]], matches ()]
    (if-some [opt (first options)]
      (let [[name parts] opt]
        (condp = name
          accepted (recur (rest options) (conj matches parts))
          rejected (recur (rest options) matches)
          (recur (concat (rest options) (split-parts-by-workflow (workflows name) parts)) matches)))
      matches)))

(defn solve [f input]
  (let [[workflows parts] (parse-input input)]
    (f (matching-parts workflows) parts)))

(defn in-range? [matches p]
  (every? #(let [[[low high] v] ((juxt matches p) %)]
             (<= low v high))
          rating-names))

(defn part1 [input]
  (solve (fn [matches parts] (transduce (comp (filter (fn [p] (some #(in-range? % p) matches)))
                                              (map #(apply + (vals %))))
                                        + parts))
         input))

(defn part2 [input]
  (solve (fn [matches _] (transduce (map num-combos) + matches)) input))