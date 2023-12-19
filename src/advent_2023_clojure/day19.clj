(ns advent-2023-clojure.day19
  (:require [clojure.string :as str]
            [clojure.edn :as edn]
            [clojure.core.match :refer [match]]
            [abyala.advent-utils-clojure.core :refer :all]
            [abyala.advent-utils-clojure.math :refer [signum]]))

(def start-rule "in")
(def accepted "A")
(def rejected "R")

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

(defn run-rule [rule part]
  (let [{:keys [target op rating amount]} rule]
    (if op
      (when (({">" > "<" <} op) (get part rating) amount) target)
      target)))

(defn run-workflow [workflow part]
  (first (keep #(run-rule % part) workflow)))

(defn accept-part?
  ([workflows part] (accept-part? workflows part start-rule))
  ([workflows part workflow-name] (condp = workflow-name
                                    accepted true
                                    rejected false
                                    (recur workflows part (run-workflow (get workflows workflow-name) part)))))

(defn part1 [input]
  (let [[workflows parts] (parse-input input)]
    (transduce (comp (filter (partial accept-part? workflows))
                     (map #(apply + (vals %))))
               + parts)))

(defn split-parts-by-rule [rule parts]
  (let [{:keys [target op rating amount]} rule
        [low high] (get parts rating)]
    (match [op (when op (signum (- amount low))) (when op (signum (- amount high)))]
           [nil _ _]          [parts target nil]
           ["<" (:or 0 -1) _] [nil nil parts]
           ["<" _ 1]          [parts target nil]
           ["<" _ _]          [(assoc-in parts [rating 1] (dec amount)) target (assoc-in parts [rating 0] amount)]
           [">" _ (:or 0 1)]  [nil nil parts]
           [">" -1 _]         [parts target nil]
           [">" _ _]          [(assoc-in parts [rating 0] (inc amount)) target (assoc-in parts [rating 1] amount)])))

(defn split-parts-by-workflow [workflow parts]
  (->> workflow
       (reduce (fn [[outputs leftovers] rule]
                 (let [[good target bad] (split-parts-by-rule rule leftovers)]
                   [(if good (conj outputs [target good]) outputs) bad]))
               [() parts])
       first))

(defn num-combos [parts]
  (transduce (map (fn [[low high]] (- (inc high) low))) * (vals parts)))

(defn part2 [input]
  (let [workflows (first (parse-input input))]
    (loop [options [[start-rule (zipmap ["x" "m" "a" "s"] (repeat [1 4000]))]], n 0]
      (if-some [opt (first options)]
        (let [[name parts] opt]
          (condp = name
            accepted (recur (rest options) (+ n (num-combos parts)))
            rejected (recur (rest options) n)
            (recur (concat (rest options) (split-parts-by-workflow (workflows name) parts)) n)))
        n))))