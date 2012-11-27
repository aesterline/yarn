(ns yarn.thread-dump
  (:require [protoflex.parse :as p]
            [clojure.string :as s]))

;"Reference Handler" daemon prio=5 tid=0x00007f8092054800 nid=0x115f06000 in Object.wait() [0x0000000115f05000]
;"clojure-agent-send-off-pool-3" prio=5 tid=0x00007f8091281800 nid=0x116ccb000 waiting on condition [0x0000000116cca000]
(defn thread-header []
  (let [name (p/dq-str)
        type (p/opt #(p/string "daemon") "task")
        priority (do (p/string "prio=") (p/integer))
        thread-id (p/regex #"tid=(\S+)" 1)
        native-id (p/regex #"nid=(\S+)" 1)
        state (p/read-to " [")
        address (p/sq-brackets #(p/regex #"\w+"))]
    {:name name
     :type type
     :priority priority
     :thread-id thread-id
     :native-id native-id
     :state state
     :address address}))

;at java.net.PlainSocketImpl.socketAccept(Native Method)
;at java.net.AbstractPlainSocketImpl.accept(AbstractPlainSocketImpl.java:398)
(defn stack-trace-element []
  (let [method (do (p/skip-over "at") (p/read-to "("))
        location (p/regex #"\((.+?)\)" 1)
        [file line-number] (s/split location #":")]
    {:method method
     :file file
     :line-number (if line-number (Integer/parseInt line-number) -1)}))

(defn stack-trace []
  (let [header (thread-header)
        thread-state (p/read-to-re #"\r?\n")
        elements (p/opt #(p/multi+ stack-trace-element) [])]
    {:header header
     :elements elements}))