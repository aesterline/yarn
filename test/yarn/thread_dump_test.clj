(ns yarn.thread-dump-test
  (:require [yarn.thread-dump :as t]
            [protoflex.parse :as p])
  (:use clojure.test))

(def gc-header "\"GC task thread#0 (ParallelGC)\" prio=5 tid=0x00007f809200e000 nid=0x1110f9000 runnable\n")
(def task-header "\"main\" prio=5 tid=0x00007f8092001000 nid=0x10a9a5000 waiting on condition [0x000000010a9a3000]\n")
(def daemon-header "\"Reference Handler\" daemon prio=5 tid=0x00007f8092054800 nid=0x115f06000 in Object.wait() [0x0000000115f05000]\n")

(deftest gc-thread-header
  (let [header (p/parse t/thread-header gc-header :eof false)]
    (is (= "GC task thread#0 (ParallelGC)" (:name header)))
    (is (= "task" (:type header)))
    (is (= 5 (:priority header)))
    (is (= "0x00007f809200e000" (:thread-id header)))
    (is (= "0x1110f9000" (:native-id header)))
    (is (= "runnable" (:state header)))))

(deftest task-thread-header
  (let [header (p/parse t/thread-header task-header :eof false)]
    (is (= "main" (:name header)))
    (is (= "task" (:type header)))
    (is (= 5 (:priority header)))
    (is (= "0x00007f8092001000" (:thread-id header)))
    (is (= "0x10a9a5000" (:native-id header)))
    (is (= "waiting on condition" (:state header)))
    (is (= "0x000000010a9a3000" (:address header)))))

(deftest daemon-thread-header
  (let [header (p/parse t/thread-header daemon-header :eof false)]
    (is (= "Reference Handler" (:name header)))
    (is (= "daemon" (:type header)))
    (is (= 5 (:priority header)))
    (is (= "0x00007f8092054800" (:thread-id header)))
    (is (= "0x115f06000" (:native-id header)))
    (is (= "in Object.wait()" (:state header)))
    (is (= "0x0000000115f05000" (:address header)))))

(def native-method-element "	at java.lang.Object.wait(Native Method)\n")
(def regular-method-element "	at java.lang.ref.Reference$ReferenceHandler.run(Reference.java:133)\n")

(deftest native-stack-trace-element
  (let [element (p/parse t/stack-trace-element native-method-element :eof false)]
    (is (= "java.lang.Object.wait" (:method element)))
    (is (= "Native Method" (:file element)))
    (is (= -1 (:line-number element)))))

(deftest regular-stack-trace-element
  (let [element (p/parse t/stack-trace-element regular-method-element :eof false)]
    (is (= "java.lang.ref.Reference$ReferenceHandler.run" (:method element)))
    (is (= "Reference.java" (:file element)))
    (is (= 133 (:line-number element)))))

(def daemon-thread
"\"Service Thread\" daemon prio=5 tid=0x00007f8092082800 nid=0x116710000 runnable [0x0000000000000000]
   java.lang.Thread.State: RUNNABLE")

(def no-monitor-thread
"\"clojure-agent-send-off-pool-5\" prio=5 tid=0x00007f809212c000 nid=0x117498000 runnable [0x0000000117495000]
   java.lang.Thread.State: RUNNABLE
	at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:603)
	at java.lang.Thread.run(Thread.java:722)")

(deftest gc-stack-trace
  (let [trace (p/parse t/stack-trace gc-header :eof false)]
    (is (= {:header
            {:name "GC task thread#0 (ParallelGC)"
             :type "task"
             :priority 5
             :thread-id "0x00007f809200e000"
             :native-id "0x1110f9000"
             :state "runnable"
             :address nil}
            :elements []} trace))))

(deftest daemon-stack-trace
  (let [trace (p/parse t/stack-trace daemon-thread :eof false)]
    (is (= {:header
            {:name "Service Thread"
             :type "daemon"
             :priority 5
             :thread-id "0x00007f8092082800"
             :native-id "0x116710000"
             :state "runnable"
             :address "0x0000000000000000"}
            :elements []} trace))))

(deftest no-monitor-trace
  (let [trace (p/parse t/stack-trace no-monitor-thread :eof false)]
    (is (= {:header
            {:name "clojure-agent-send-off-pool-5"
             :type "task"
             :priority 5
             :thread-id "0x00007f809212c000"
             :native-id "0x117498000"
             :state "runnable"
             :address "0x0000000117495000"}
            :elements [{:method "java.util.concurrent.ThreadPoolExecutor$Worker.run"
                        :file "ThreadPoolExecutor.java"
                        :line-number 603}
                       {:method "java.lang.Thread.run"
                        :file "Thread.java"
                        :line-number 722}]} trace))))

