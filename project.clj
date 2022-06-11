(defproject clojurewerkz/buffy "1.2.0-SNAPSHOT"
  :description  "Clojure library for working with binary data"
  :url          "http://github.com/clojurewerkz/buffy"

  :license      {:name "Eclipse Public License"
                 :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure   "1.10.1"]
                 [io.netty/netty-buffer "4.0.51.Final"]]

;  :jvm-opts     ["-noverify"
;                 "-javaagent:/Users/ifesdjeen/hackage/introspect/target/introspect-1.0.0-SNAPSHOT-standalone.jar=clojurewerkz.buffy.core"
;]

  :profiles     {:1.7    {:dependencies [[org.clojure/clojure "1.7.0"]]}
                 :1.8    {:dependencies [[org.clojure/clojure "1.8.0"]]}
                 :1.9    {:dependencies [[org.clojure/clojure "1.9.0"]]}
                 :master {:dependencies [[org.clojure/clojure "1.11.0-master-SNAPSHOT"]]}
                 :dev    {:dependencies [[org.clojure/test.check  "0.6.2"]
                                         [criterium "0.4.4"]]
                          :resource-paths ["test/resources"]
                          :plugins [[codox "0.8.10"]]
                          :codox {:sources ["src/clojure"]
                                  :output-dir "doc/api"}}}

  :repositories {"sonatype"           {:url "https://oss.sonatype.org/content/repositories/releases"
                                       :snapshots false
                                       :releases {:checksum :fail :update :always}}
                 "sonatype-snapshots" {:url "https://oss.sonatype.org/content/repositories/snapshots"
                                       :snapshots true
                                       :releases {:checksum :fail :update :always}}}

  :global-vars {*warn-on-reflection* true}

  :aliases      {"all" ["with-profile" "dev:dev,1.7:dev,1.8:dev,1.9:dev,master"]})
