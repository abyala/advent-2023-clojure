(ns advent-2023-clojure.day20
  (:require [clojure.string :as str]
            [abyala.advent-utils-clojure.core :refer [take-until]]
            [clojure.math.numeric-tower :as math]))

(def broadcaster "broadcaster")

(defn parse-module [s]
  (let [[name targets] (str/split s #" -> ")]
    (assoc (cond
             (= name broadcaster) {:type :broadcaster :name name}
             (str/starts-with? name "%") {:type :flip-flop :name (subs name 1) :on? false}
             (str/starts-with? name "&") {:type :conjunction :name (subs name 1) :memories {}})
      :targets (str/split targets #", "))))

(defn register-conjunction-inputs [module-map]
  (reduce (fn [acc [target source]] (if (= :conjunction (:type (module-map target)))
                                      (update-in acc [target :memories] assoc source false)
                                      acc))
          module-map
          (mapcat (fn [{:keys [name targets]}] (map #(vector % name) targets)) (vals module-map))))

(defn parse-input [input]
  (let [modules (map parse-module (str/split-lines input))]
    (register-conjunction-inputs (zipmap (map :name modules) modules))))

(defn- send-signals [module-map from high?]
  (map #(hash-map :from from :to % :high? high?) (get-in module-map [from :targets])))

(defmulti receive-signal (fn [module-map from to high?] (get-in module-map [to :type])))
(defmethod receive-signal :broadcaster [module-map _ to high?]
  [nil (send-signals module-map to high?)])
(defmethod receive-signal :flip-flop [module-map _ to high?]
  (when-not high?
    (let [module-map' (update-in module-map [to :on?] not)]
      [module-map' (send-signals module-map' to (get-in module-map' [to :on?]))])))
(defmethod receive-signal :conjunction [module-map from to high?]
  (let [module-map' (assoc-in module-map [to :memories from] high?)
        all-high? (every? true? (vals (get-in module-map' [to :memories])))]
    [module-map' (send-signals module-map' to (not all-high?))]))
(defmethod receive-signal :default [module-map from to high?])

(defn push-button
  [module-map button-target]
  (loop [signals [{:from "button" :to button-target :high? false}], state module-map, receive-stats {false 0, true 0}]
    (if (seq signals)
      (let [{:keys [from to high?]} (first signals)
            [returned-state signals'] (receive-signal state from to high?)
            state' (or returned-state state)]
        (recur (apply conj (subvec signals 1) signals'), state', (update receive-stats high? inc)))
      [state receive-stats])))

(defn push-button-seq [module-map button-target]
  (rest (iterate #(push-button (first %) button-target) [module-map])))

(defn part1 [input]
  (->> (push-button-seq (parse-input input) broadcaster)
       (take 1000)
       (map second)
       (apply (partial merge-with +))
       vals
       (apply *)))

(defn add-rx-node [module-map]
  (assoc module-map "rx" {:type :flip-flop :name "rx" :on? false}))

(defn loop-length [module-map button-target]
  (let [[initial & other-states] (push-button-seq module-map button-target)]
    (count (take-until #(= % initial) other-states))))

(defn part2 [input]
  (let [module-map (add-rx-node (parse-input input))]
    (reduce #(math/lcm (loop-length module-map %2) %1) 1 (get-in module-map [broadcaster :targets]))))
