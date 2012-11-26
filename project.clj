(defproject yarn "0.0.1"
  :description "A smart Java thread dump analyzer."
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [parse-ez "0.3.1"]]

  :profiles {:dev {:resource-paths ["thread_dumps"]}})
