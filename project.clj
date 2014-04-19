(defproject clojurewerkz/buffy "1.0.0-beta4-SNAPSHOT"
  :description "Clojure library for working with binary data"
  :url "http://github.com/clojurewerkz/buffy"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [io.netty/netty-buffer "4.0.18.Final"]
                 [reiddraper/simple-check "0.5.3"]]
  :profiles {:1.4 {:dependencies [[org.clojure/clojure "1.4.0"]]}
             :1.6 {:dependencies [[org.clojure/clojure "1.6.0"]]}
             :dev {:resource-paths ["test/resources"]
                   :plugins [[codox "0.6.6"]]
                   :codox {:sources ["src/clojure"]
                           :output-dir "doc/api"}}}
  :aliases {"all" ["with-profile" "dev:dev,1.4:dev,1.6:dev"]})
