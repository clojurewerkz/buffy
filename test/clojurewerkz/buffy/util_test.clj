(ns clojurewerkz.buffy.util-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.buffy.core :refer :all]
            [clojurewerkz.buffy.util :refer :all]))

(deftest positions-test
  (is (= '(0 4 14 18 22) (positions [(int32-type) (string-type 10) (int32-type) (int32-type) (string-type 16)]))))

(deftest bits-on-at-test
  (is (= [true true true false
          false false false false
          false false false false
          false false false false
          false false false false
          false false false false
          false false false false
          false false false false])
      (bits-on-at [0 1 2])))

(deftest bits-off-at-test
  (is (= [false false false true
          true true true true
          true true true true
          true true true true
          true true true true
          true true true true
          true true true true
          true true true true])
      (bits-on-at [0 1 2])))

(deftest bits-on-indexes-test
  (is (= [0 1 2] (on-bits-indexes (bits-on-at [0 1 2]))))
  (is (= [5 15 31] (on-bits-indexes (bits-on-at [5 15 31])))))

(deftest bits-off-indexes-test
  (is (= [0 1 2] (off-bits-indexes (bits-off-at [0 1 2]))))
  (is (= [5 15 31] (off-bits-indexes (bits-off-at [5 15 31])))))
