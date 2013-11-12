(ns clojurewerkz.buffy.util-test
  (:require [clojure.test :refer :all]
            [clojurewerkz.buffy.core :refer :all]
            [clojurewerkz.buffy.util :refer :all]))

(deftest positions-test
  (is (= '(0 4 14 18 22) (positions [(int32-type) (string-type 10) (int32-type) (int32-type) (string-type 16)]))))
