(ns clojurewerkz.buffy.dynamic-test
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer :all]
            [clojurewerkz.buffy.util :refer :all]
            [clojurewerkz.buffy.core :refer :all]
            [clojurewerkz.buffy.frames :refer :all]
            [clojurewerkz.buffy.types.protocols :refer :all]
            [simple-check.core :as sc]
            [simple-check.generators :as gen]
            [simple-check.properties :as prop]))

(deftest dynamic-roundtrip-test
  (let [string-encoder (defframeencoder [value]
                         length (short-type) (count value)
                         string (string-type (count value)) value)
        string-decoder (defframedecoder [buffer offset]
                         length (short-type)
                         string (string-type (read length buffer offset)))
        b              (dynamic-buffer (frame-type string-encoder string-decoder second)
                                       (frame-type string-encoder string-decoder second))]

    (is (= ["stringaaaasd" "stringbbb"])
        (decompose b
                   (compose b ["stringaaaasd" "stringbbb"])))))
