(ns yarn.thread-dump
  (:require [clojure.string :as s]))

(def header-regex #"^\"(.+?)\"\s*(daemon)?\s*prio=(\d+) tid=(\S+) nid=(\S+) ([^\[]+)\[?(.+?)?\]?$")
(def element-regex #"^\s+at ([^\(]+)\((.+?)\)$")
(def monitor-regex #"^\s+- ([^<]+)<([^>]+)> \(a ([^\)]+)\)$")
(def time-re #"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}")

(defn parse-thread-header [header]
  (let [[_ name type priority thread-id native-id state address] (re-find header-regex header)]
    {:name name
     :type (if type :daemon :task)
     :priority (Integer/parseInt priority)
     :thread-id thread-id
     :native-id native-id
     :state (s/trim state)
     :address address}))

(defn parse-stack-trace-element [element]
  (let [[_ method location] (re-find element-regex element)
        [file line-number] (s/split location #":")]
    {:method method
     :file file
     :line-number (if line-number (Integer/parseInt line-number) -1)}))

(defn state [text]
  (cond
    (.startsWith text "waiting") :waiting
    (.startsWith text "locked") :locked
    (.startsWith text "parking") :parked))

(defn parse-monitor [text]
  (let [[_ state-str monitor lock] (re-find monitor-regex text)]
    {:state (state state-str)
     :monitor monitor
     :lock lock}))

(defn element->data [element]
  (cond
    (re-matches element-regex element) (parse-stack-trace-element element)
    (re-matches monitor-regex element) (parse-monitor element)))

(defn parse-trace-elements [elements]
  (reduce #(conj %1 (element->data %2)) [] elements))

(defn parse-stack-trace [trace]
  (let [header (parse-thread-header (first trace))
        elements (parse-trace-elements (nnext trace))]
    (assoc header :elements elements)))

(defn parse-thread-dump [dump]
  (let [time (first dump)
        version (s/replace (second dump) #":" "")
        stacks (remove #(s/blank? (first %)) (partition-by s/blank? (nnext dump)))]
    {:timestamp time
     :version version
     :threads (reduce #(conj %1 (parse-stack-trace %2)) [] stacks)}))

(defn next-dump [lines]
  (let [start-dump (drop-while #(not (re-matches time-re %)) lines)]
    (split-with #(not (.startsWith % "JNI global references")) start-dump)))

(defn parse-thread-dump-file [file]
  (with-open [rdr (clojure.java.io/reader file)]
    (loop [dumps []
           [dump rest] (next-dump (line-seq rdr))]
      (if (empty? dump)
        dumps
        (recur (conj dumps (parse-thread-dump dump)) (next-dump rest))))))
