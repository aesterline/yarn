# yarn

yarn is a JVM thread dump parser written in Clojure. yarn's main goal is to turn JVM thread dumps
into Clojure data.

[![Build Status](https://secure.travis-ci.org/aesterline/yarn.png)](http://travis-ci.org/aesterline/yarn)

## Installation

`yarn` is available as a Maven artifact from [Clojars](http://clojars.org/yarn):

```clojure
[yarn "0.0.1"]
```

## Usage

```clojure
(use 'yarn.parser)

(parse-thread-dump-file "path/to/thread/dump")

;; The above statement returns a clojure vector

[{:timestamp "2012-11-25 05:38:05"
            :version "Full thread dump Java HotSpot(TM) 64-Bit Server VM (23.0-b17 mixed mode)"
            :threads [{:name "Finalizer"
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
                                   :lock "java.lang.ref.ReferenceQueue$Lock"}]}
                      {:name "GC task thread#0 (ParallelGC)"
                       :type :task
                       :priority 5
                       :thread-id "0x00007f809200e000"
                       :native-id "0x1110f9000"
                       :state "runnable"
                       :address nil
                       :elements []}]}]
```

## Development

To run the tests:

    $ lein test

## License

Distributed under the Eclipse Public License, the same as Clojure. <http://opensource.org/licenses/eclipse-1.0.php>