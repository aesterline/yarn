(defproject yarn "0.0.1"
  :description "A clojure JVM thread dump parser."
  :url "http://github.com/aesterline/yarn"
  :license {:name "Eclipse Public License 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :profiles {:dev {:resource-paths ["thread_dumps"]}}
  :plugins [[quickie "0.1.0-SNAPSHOT"]]
  :test-matcher #"yarn.*")
