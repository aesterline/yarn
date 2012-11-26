(ns yarn.thread-dump)

;"Reference Handler" daemon prio=5 tid=0x00007f8092054800 nid=0x115f06000 in Object.wait() [0x0000000115f05000]
;"clojure-agent-send-off-pool-3" prio=5 tid=0x00007f8091281800 nid=0x116ccb000 waiting on condition [0x0000000116cca000]
(def thread-header-pattern #"\"([^\"]+)\" ?(daemon)? ?prio=(\S+) tid=(\S+) nid=(\S+) ([^\[]+) \[(\S+)\]")

;at java.net.PlainSocketImpl.socketAccept(Native Method)
;at java.net.AbstractPlainSocketImpl.accept(AbstractPlainSocketImpl.java:398)
(def stack-trace-element-pattern #"\s*at ([^\(]+)\(([^:]+):?(\d+)?\)")

(defn parse-thread-header [line]
  (when-let [[_ name type priority thread-id native-id state address] (re-find thread-header-pattern line)]
    {:name name
     :type type
     :priority priority
     :thread-id thread-id
     :native-id native-id
     :state state
     :address address}))

(defn parse-stack-trace-element [line]
  (when-let [[_ method file line-number] (re-find stack-trace-element-pattern line)]
    {:method method
     :file file
     :line-number (if line-number (Integer/parseInt line-number) -1)}))

(defn parse-stack-trace [lines]
  (let [[line & rest] lines
        header (parse-thread-header line)
        elements (take-while (complement clojure.string/blank?) rest)]
    )
  (when-let [[header-str & elements] lines]
    (when-let [header (parse-thread-header header-str)]
      ))
  (when-let [header (parse-thread-header (first lines))]))

(defn parse [lines])