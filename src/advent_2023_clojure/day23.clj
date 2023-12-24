(ns advent-2023-clojure.day23
  (:require [abyala.advent-utils-clojure.core :refer [map-conj]]
            [abyala.advent-utils-clojure.point :as p]))

(def north [0 -1])
(def south [0 1])
(def east [1 0])
(def west [-1 0])
(defn reverse-dir [dir] ({north south, south north, east west, west east} dir))

(defn replace-slopes [c] (if (#{\> \< \^ \v} c) \. c))

(defn parse-trail [parse-xf input]
  (reduce-kv (fn [m p c] (if (not= c \#) (assoc m p (parse-xf c)) m))
             {}
             (p/parse-to-char-coords-map input)))

(defn start-and-end [trail] ((juxt first last) (sort-by second (keys trail))))

(defn accessible? [trail p approaching-dir]
  (when-let [c (trail p)]
    (not= c ({east \< west \> south \^ north \v} approaching-dir))))

(defn next-steps [trail segment-start dir]
  (loop [p (p/move segment-start dir), dir dir, n 1]
    (let [[{:keys [next-p next-dir]} :as next-steps]
          (keep #(let [p' (p/move p %)]
                   (when (and (not= % (reverse-dir dir)) (accessible? trail p' %)) {:next-p p' :next-dir %}))
                [north south east west])]
      (if (= 1 (count next-steps))
        (recur next-p next-dir (inc n))
        {:last-step p, :dist n, :next-dirs (map :next-dir next-steps)}))))

(defn all-paths [trail]
  (let [[start] (start-and-end trail)]
    (loop [options [{:p start :dir south}], seen #{}, segments {}]
      (if (seq options)
        (let [{:keys [p dir]} (first options)]
          (if (seen [p dir])
            (recur (rest options) seen segments)
            (let [{:keys [last-step dist next-dirs]} (next-steps trail p dir)]
              (recur (apply conj (rest options) (map #(hash-map :p last-step :dir %) next-dirs))
                     (conj seen [p dir])
                     (map-conj segments p [last-step dist])))))
        segments))))

(defn solve [parse-xf input]
  (let [trail (parse-trail parse-xf input)
        [start end] (start-and-end trail)
        paths (all-paths trail)]
    (loop [options [[start #{} 0]], best 0]
      (if (seq options)
        (let [[[p seen n] & x-options] options]
          (cond (= p end) (recur x-options (max best n))
                (seen p) (recur x-options best)
                :else (recur (apply conj x-options (map (fn [[p' dist']] [p' (conj seen p) (+ n dist')])
                                                        (paths p))) best)))
        best))))

(defn part1 [input] (solve identity input))
(defn part2 [input] (solve replace-slopes input))