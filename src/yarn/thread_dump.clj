(ns yarn.thread-dump
  (:require [protoflex.parse :as p]
            [clojure.string :as s]))

(defn thread-address []
  (p/sq-brackets #(p/regex #"\w+")))

(defn thread-header []
  (let [name (p/regex #"\"(.+?)\"" 1)
        type (p/opt #(p/string "daemon") "task")
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

(defn stack-trace []
  (let [header (thread-header)
        thread-state (p/attempt #(p/read-to-re #"\r?\n"))
        elements (p/opt #(p/multi+ stack-trace-element) [])]
    {:header header
     :elements elements}))