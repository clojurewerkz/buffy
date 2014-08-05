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
  (let [string-encoder (frame-encoder [value]
                                      length (short-type) (count value)
                                      string (string-type (count value)) value)
        string-decoder (frame-decoder [buffer offset]
                                      length (short-type)
                                      string (string-type (read length buffer offset)))
        b              (dynamic-buffer (frame-type string-encoder string-decoder second)
                                       (frame-type string-encoder string-decoder second))]

    (is (= ["super-duper-random-string" "long-ans-senseless-stringyoyoyo"]
           (decompose b
                      (compose b ["super-duper-random-string" "long-ans-senseless-stringyoyoyo"]))))))

(deftest encoding-size-test
  (let [string-encoder (frame-encoder [value]
                                      length (short-type) (count value)
                                      string (string-type (count value)) value)
        string-decoder (frame-decoder [buffer offset]
                                      length (short-type)
                                      string (string-type (read length buffer offset)))
        string-frame (frame-type string-encoder string-decoder second)]

    (is (= 8 (encoding-size string-frame "abcdef")))))

(deftest dynamic-stringmap-test
  (let [string-encoder (frame-encoder [value]
                                      length (short-type) (count value)
                                      string (string-type (count value)) value)
        string-decoder (frame-decoder [buffer offset]
                                      length (short-type)
                                      string (string-type (read length buffer offset)))
        b              (dynamic-buffer
                        (composite-frame
                         (frame-type string-encoder string-decoder second)
                         (frame-type string-encoder string-decoder second)))]

    (is (= [["stringaaaasd" "stringbbb"]]
           (decompose b
                      (compose b [["stringaaaasd" "stringbbb"]]))))))

(deftest complex-encoding-decoding
  (let [string-encoder   (frame-encoder [value]
                                        length (short-type) (count value)
                                        string (string-type (count value))
                                        value)
        string-decoder   (frame-decoder [buffer offset]
                                        length (short-type)
                                        string (string-type (read length buffer offset)))
        string-frame     (frame-type string-encoder string-decoder second)
        kvp-frame        (composite-frame
                          (frame-type string-encoder string-decoder second)
                          (frame-type string-encoder string-decoder second))
        map-encoder      (frame-encoder [value]
                                        length (short-type) (count value)
                                        map (repeated-frame kvp-frame (count value)) value)
        map-decoder      (frame-decoder [buffer offset]
                                        length (short-type)
                                        map    (repeated-frame kvp-frame (read length buffer offset)))
        b                (dynamic-buffer (frame-type map-encoder
                                                     map-decoder
                                                     second))]

    (is (= [[["12" "34"] ["56" "78"] ["90" "12243"]]]
           (decompose b
                      (compose b
                               [[["12" "34"] ["56" "78"] ["90" "12243"]]]))))))
