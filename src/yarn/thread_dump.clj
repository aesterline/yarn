(ns yarn.thread-dump
  (:require [protoflex.parse :as p]
            [clojure.string :as s]))

(defn thread-address []
  (p/sq-brackets #(p/regex #"\w+")))

(defn thread-header []
  (let [name (p/regex #"\"(.+?)\"" 1)
        type (keyword (p/opt #(p/string "daemon") "task"))
        priority (do (p/string "prio=") (p/integer))
        thread-id (p/regex #"tid=(\S+)" 1)
        native-id (p/regex #"nid=(\S+)" 1)
        state (p/read-to-re #" \[|\r?\n")
        address (p/attempt thread-address)]
    {:name name
     :type type
     :priority priority
     :thread-id thread-id
     :native-id native-id
     :state state
     :address address}))

(defn stack-trace-element []
  (let [method (do (p/skip-over "at") (p/read-to "("))
        location (p/regex #"\((.+?)\)" 1)
        [file line-number] (s/split location #":")]
    {:method method
     :file file
     :line-number (if line-number (Integer/parseInt line-number) -1)}))

(defn state [text]
  (cond
    (.startsWith text "waiting") :waiting
    (.startsWith text "locked") :locked
    (.startsWith text "parking") :parked))

(defn monitor []
  (let [state-str (do (p/skip-over "-") (p/read-to "<"))
        monitor (p/regex #"<(.+?)>" 1)
        lock (p/regex #"\(a (.+?)\)" 1)]
    {:state (state state-str)
     :monitor monitor
     :lock lock}))

(defn trace-elements []
  (p/look-ahead ["at" stack-trace-element
                 "-" monitor]))

(defn stack-trace []
  (let [header (thread-header)
        thread-state (p/attempt #(p/skip-over-re #"java.lang.Thread.State:.+?\r?\n"))
        elements (p/opt #(p/multi+ trace-elements) [])]
    (assoc header :elements elements)))

;2012-11-25 05:38:05
(defn timestamp []
  (let [time-re #"\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}"]
    (p/read-to-re time-re)
    (p/regex time-re)))

(defn thread-dump []
  (let [time (timestamp)
        version (p/regex #"(.+?):\r?\n" 1)
        threads (p/multi+ stack-trace)]
    {:timestamp time
     :version version
     :threads threads}))

(defn parse-thread-dump [dump]
  (p/parse thread-dump dump :eof false))