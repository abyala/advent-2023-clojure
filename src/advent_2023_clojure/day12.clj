(ns advent-2023-clojure.day12
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :refer :all]))

(defn dead-end? [s groups] (and (empty? groups) (str/includes? s "#")))
(defn success? [s groups] (and (empty? groups) (not (str/includes? s "#"))))

(defn parse-row [row]
  (let [[s0 s1] (str/split row #" ")]
    [s0 (split-longs s1)]))

(defn parse-input [input] (map parse-row (str/split-lines input)))

(defn search-space-for [s group]
  (if-let [idx (str/index-of s "#")]
    (subs-to-end s 0 (+ idx group 1))
    s))

(defn drop-leading-dots [s] (apply str (drop-while (partial = \.) s)))

(def search-space-matches
  (memoize (fn [search-space group]
             (let [bad-pattern (re-pattern (str "#{" (inc group) "}"))]
               (->> (range (- (count search-space) group -1))
                    (remove #(let [replaced-str (str/replace (subs search-space % (+ % group)) "?" "#")
                                   s (str (subs search-space 0 %) replaced-str (subs-to-end search-space (+ % group) (inc (+ % group))))]
                               (or (str/includes? replaced-str ".")
                                   (re-find bad-pattern s))))
                    (map (comp drop-leading-dots #(subs-to-end search-space (+ % group 1))))
                    (frequencies))))))

(defn replace-next-search-space [s groups]
  (let [g (first groups)
        search (search-space-for s g)
        after-search (subs-to-end s (count search))]
    (update-keys (search-space-matches search g)
                 #(str % after-search))))

(def num-arrangements
  (memoize (fn [s groups]
             (cond
               (success? s groups) 1
               (dead-end? s groups) 0
               :else (transduce (map (fn [[leftover n]] (* n (num-arrangements leftover (rest groups)))))
                                +
                                (replace-next-search-space s groups))))))

(defn part1 [input]
  (transduce (map (partial apply num-arrangements)) + (parse-input input)))

(defn unfold [n [s g]]
  [(str/join "?" (repeat n s)) (apply concat (repeat n g))])

(defn solve [n input]
  (transduce (map (comp (partial apply num-arrangements) (partial unfold n)))
             +
             (parse-input input)))

(defn part1 [input] (solve 1 input))
(defn part2 [input] (solve 5 input))