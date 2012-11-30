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
    (is (= :task (:type header)))
    (is (= 5 (:priority header)))
    (is (= "0x00007f809200e000" (:thread-id header)))
    (is (= "0x1110f9000" (:native-id header)))
    (is (= "runnable" (:state header)))))

(deftest task-thread-header
  (let [header (p/parse t/thread-header task-header :eof false)]
    (is (= "main" (:name header)))
    (is (= :task (:type header)))
    (is (= 5 (:priority header)))
    (is (= "0x00007f8092001000" (:thread-id header)))
    (is (= "0x10a9a5000" (:native-id header)))
    (is (= "waiting on condition" (:state header)))
    (is (= "0x000000010a9a3000" (:address header)))))

(deftest daemon-thread-header
  (let [header (p/parse t/thread-header daemon-header :eof false)]
    (is (= "Reference Handler" (:name header)))
    (is (= :daemon (:type header)))
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

(def parked-monitor "  - parking to wait for  <0x00000007f97e1358> (a java.util.concurrent.SynchronousQueue$TransferStack)")
(def waiting-monitor "  - waiting on <0x00000007adab0f70> (a java.lang.ref.ReferenceQueue$Lock)")
(def locked-monitor "  - locked <0x00000007f9896030> (a java.io.BufferedInputStream)")

(deftest waiting-stack-monitor
  (let [monitor (p/parse t/monitor waiting-monitor :eof false)]
    (is (= :waiting (:state monitor)))
    (is (= "0x00000007adab0f70" (:monitor monitor)))
    (is (= "java.lang.ref.ReferenceQueue$Lock" (:lock monitor)))))

(deftest locked-stack-monitor
  (let [monitor (p/parse t/monitor locked-monitor :eof false)]
    (is (= :locked (:state monitor)))
    (is (= "0x00000007f9896030" (:monitor monitor)))
    (is (= "java.io.BufferedInputStream" (:lock monitor)))))

(deftest parked-stack-monitor
  (let [monitor (p/parse t/monitor parked-monitor :eof false)]
    (is (= :parked (:state monitor)))
    (is (= "0x00000007f97e1358" (:monitor monitor)))
    (is (= "java.util.concurrent.SynchronousQueue$TransferStack" (:lock monitor)))))

(def daemon-thread
  "\"Service Thread\" daemon prio=5 tid=0x00007f8092082800 nid=0x116710000 runnable [0x0000000000000000]
 java.lang.Thread.State: RUNNABLE")

(def no-monitor-thread
  "\"clojure-agent-send-off-pool-5\" prio=5 tid=0x00007f809212c000 nid=0x117498000 runnable [0x0000000117495000]
   java.lang.Thread.State: RUNNABLE
  at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:603)
  at java.lang.Thread.run(Thread.java:722)")

(def monitor-thread
  "\"Finalizer\" daemon prio=5 tid=0x00007f8092055000 nid=0x116009000 in Object.wait() [0x0000000116008000]
   java.lang.Thread.State: WAITING (on object monitor)
  at java.lang.Object.wait(Native Method)
  - waiting on <0x00000007adab0f70> (a java.lang.ref.ReferenceQueue$Lock)
  at java.lang.ref.ReferenceQueue.remove(ReferenceQueue.java:135)
  - locked <0x00000007adab0f70> (a java.lang.ref.ReferenceQueue$Lock)")

(deftest gc-stack-trace
  (let [trace (p/parse t/stack-trace gc-header :eof false)]
    (is (= {:name "GC task thread#0 (ParallelGC)"
            :type :task
            :priority 5
            :thread-id "0x00007f809200e000"
            :native-id "0x1110f9000"
            :state "runnable"
            :address nil
            :elements []} trace))))

(deftest daemon-stack-trace
  (let [trace (p/parse t/stack-trace daemon-thread :eof false)]
    (is (= {:name "Service Thread"
            :type :daemon
            :priority 5
            :thread-id "0x00007f8092082800"
            :native-id "0x116710000"
            :state "runnable"
            :address "0x0000000000000000"
            :elements []} trace))))

(deftest no-monitor-trace
  (let [trace (p/parse t/stack-trace no-monitor-thread :eof false)]
    (is (= {:name "clojure-agent-send-off-pool-5"
            :type :task
            :priority 5
            :thread-id "0x00007f809212c000"
            :native-id "0x117498000"
            :state "runnable"
            :address "0x0000000117495000"
            :elements [{:method "java.util.concurrent.ThreadPoolExecutor$Worker.run"
                        :file "ThreadPoolExecutor.java"
                        :line-number 603}
                       {:method "java.lang.Thread.run"
                        :file "Thread.java"
                        :line-number 722}]} trace))))

(deftest monitor-trace
  (let [trace (p/parse t/stack-trace monitor-thread :eof false)]
    (is (= {:name "Finalizer"
            :type :daemon
            :priority 5
            :thread-id "0x00007f8092055000"
            :native-id "0x116009000"
            :state "in Object.wait()"
            :address "0x0000000116008000"
            :elements [{:method "java.lang.Object.wait"
                        :file "Native Method"
                        :line-number -1}
                       {:monitor "0x00000007adab0f70"
                        :state :waiting
                        :lock "java.lang.ref.ReferenceQueue$Lock"}
                       {:method "java.lang.ref.ReferenceQueue.remove"
                        :file "ReferenceQueue.java"
                        :line-number 135}
                       {:monitor "0x00000007adab0f70"
                        :state :locked
                        :lock "java.lang.ref.ReferenceQueue$Lock"}]} trace))))

(def small-thread-dump
  "2012-11-25 05:38:05
  Full thread dump Java HotSpot(TM) 64-Bit Server VM (23.0-b17 mixed mode):

  \"VM Thread\" prio=5 tid=0x00007f8092052000 nid=0x115e03000 runnable

  \"GC task thread#0 (ParallelGC)\" prio=5 tid=0x00007f809200e000 nid=0x1110f9000 runnable
  ")

(deftest thread-dump-small
  (let [dump (p/parse t/thread-dump small-thread-dump)]
    (is (= {:timestamp "2012-11-25 05:38:05"
            :version "Full thread dump Java HotSpot(TM) 64-Bit Server VM (23.0-b17 mixed mode)"
            :threads [{:name "VM Thread"
                       :type :task
                       :priority 5
                       :thread-id "0x00007f8092052000"
                       :native-id "0x115e03000"
                       :state "runnable"
                       :address nil
                       :elements []}
                      {:name "GC task thread#0 (ParallelGC)"
                       :type :task
                       :priority 5
                       :thread-id "0x00007f809200e000"
                       :native-id "0x1110f9000"
                       :state "runnable"
                       :address nil
                       :elements []}]} dump))))
