(ns clojurewerkz.buffy.core-test
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer :all]
            [clojurewerkz.buffy.util :refer :all]
            [clojurewerkz.buffy.core :refer :all]
            [clojurewerkz.buffy.types.protocols :refer :all]
            [simple-check.core :as sc]
            [simple-check.generators :as gen]
            [simple-check.properties :as prop]))

(def quickcheck-iterations 1000)

(deftest size-test
  (is (= 4 (size (int32-type))))
  (is (= 10 (size (string-type 10)))))

(deftest int-field-write-test
  (let [s (spec :first-field (int32-type)
                :second-field (string-type 10))
        b (compose-buffer s)]
    (set-field b :first-field 101)
    (is (= 101 (get-field b :first-field)))

    (set-field b :first-field 10)
    (is (= 10 (get-field b :first-field)))

    (set-field b :first-field Integer/MAX_VALUE)
    (is (= Integer/MAX_VALUE (get-field b :first-field)))

    (set-field b :first-field (- Integer/MAX_VALUE))
    (is (= (- Integer/MAX_VALUE) (get-field b :first-field)))

    (set-field b :first-field -101)
    (is (= -101 (get-field b :first-field)))))

(deftest boolean-field-write-test
  (let [s (spec :first-field (boolean-type)
                :second-field (string-type 10))
        b (compose-buffer s)]
    (set-field b :first-field true)
    (is (= true (get-field b :first-field)))
    (set-field b :first-field false)
    (is (= false (get-field b :first-field)))
    (set-field b :first-field true)
    (is (= true (get-field b :first-field)))))

(deftest byte-field-write-test
  (let [s (spec :first-field (byte-type)
                :second-field (string-type 10))
        b (compose-buffer s)]
    (set-field b :first-field (byte \0))
    (is (= (byte \0) (get-field b :first-field)))

    (set-field b :first-field (byte 50))
    (is (= (byte 50) (get-field b :first-field)))))

(deftest short-field-write-test
  (let [s (spec :first-field (short-type)
                :second-field (string-type 10))
        b (compose-buffer s)]
    (set-field b :first-field 10)
    (is (= 10 (get-field b :first-field)))

    (set-field b :first-field Short/MAX_VALUE)
    (is (= Short/MAX_VALUE (get-field b :first-field)))

    (set-field b :first-field (- Short/MAX_VALUE))
    (is (= (- Short/MAX_VALUE) (get-field b :first-field)))))

(deftest medium-field-write-test
  (let [s (spec :first-field (medium-type)
                :second-field (string-type 10))
        b    (compose-buffer s)]
    (set-field b :first-field 10)
    (is (= 10 (get-field b :first-field)))

    (set-field b :first-field 8388607)
    (is (= 8388607 (get-field b :first-field)))

    (set-field b :first-field (- 8388607))
    (is (= (- 8388607) (get-field b :first-field)))))

(deftest float-field-write-test
  (let [s (spec :first-field (float-type)
                :second-field (string-type 10))
        b (compose-buffer s)]
    (set-field b :first-field (float 5.34))
    (is (= (float 5.34) (get-field b :first-field)))))

(deftest string-field-write-test
  (let [s (spec :first-field (int32-type)
                :second-field (string-type 10))
        b (compose-buffer s)]
    (set-field b :second-field "abcdef")
    (is (= "abcdef" (get-field b :second-field)))
    (set-field b :second-field "yoyo")
    (is (= "yoyo" (get-field b :second-field)))
    (set-field b :second-field "")
    (is (empty? (get-field b :second-field)))))

(deftest bytes-field-test
  (let [s (spec :first-field  (string-type 10)
                :second-field (bytes-type 10))
        b (compose-buffer s)]
    (set-field b :second-field (.getBytes "abcdef"))
    (is (= "abcdef" (String. (get-field b :second-field))))
    (set-field b :second-field (.getBytes "yoyo"))
    (is (= "yoyo" (String. (get-field b :second-field))))
    (set-field b :second-field (.getBytes ""))
    (is (empty? (String. (get-field b :second-field))))))

(defmacro get-set-generative
  [generator b f]
  `(:result
    (sc/quick-check quickcheck-iterations
                    (prop/for-all* [~generator]
                                   (fn [i#]
                                     (set-field ~b ~f i#)
                                     (is (= i# (get-field ~b ~f))))))))

(deftest ^:generative int-field-write-test-generative-test
  (let [s (spec :first-field (int32-type)
                :second-field (string-type 10))
        b (compose-buffer s)]
    (is (= true (get-set-generative gen/int b :first-field)))))

(deftest ^:generative byte-field-write-test-generative-test
  (let [s (spec :first-field (byte-type)
                :second-field (string-type 10))
        b (compose-buffer s)]
    (is (= true (get-set-generative gen/byte b :first-field)))))

(def gen-float (gen/fmap float gen/ratio))

(deftest ^:generative float-field-write-test-generative-test
  (let [s (spec :first-field (float-type)
                :second-field (string-type 10))
        b (compose-buffer s)]

    (is (= true (get-set-generative gen-float b :first-field)))))

(deftest ^:generative string-field-write-test-generative-test
  (let [s (spec :first-field (int32-type)
                :second-field (string-type 10))
        b (compose-buffer s)
        g (gen/such-that #(< (count %1) 10) gen/string-ascii)]

    (is (= true (get-set-generative g b :second-field)))))


(deftest ^:generative concurrent-write-read-test
  (let [s    (spec :first-field (int32-type)
                   :second-field (string-type 10))
        b    (compose-buffer s)
        g    (gen/such-that #(< (count %1) 10) gen/string-ascii)

        res1 (future (get-set-generative gen/int b :first-field))

        res2 (future (get-set-generative g b :second-field))

        res1 (deref res1)
        res2 (deref res2)]

    (is (= true res1))
    (is (= true res2))))


(deftest composite-field-write-test
  (let [s (spec :first-field (int32-type)
                :second-field (composite-type (int32-type) (string-type 10)))
        b (compose-buffer s)]
    (set-field b :second-field [100 "abcdef"])
    (is (= [100 "abcdef"] (get-field b :second-field)))))

(deftest repeated-field-write-test
  (let [s (spec :first-field  (int32-type)
                :second-field (repeated-type (string-type 10) 5))
        b (compose-buffer s)]
    (set-field b :second-field ["abcde1" "abcde2" "abcde3" "abcde4" "abcde5"])
    (is (= ["abcde1" "abcde2" "abcde3" "abcde4" "abcde5"]
           (get-field b :second-field)))))

(deftest repeated-composite-write-test
  (let [s (spec :first-field (int32-type)
                :second-field (repeated-type (composite-type (int32-type) (string-type 10)) 5))
        b (compose-buffer s)]
    (set-field b :second-field [[1 "abcde1"] [2 "abcde2"] [3 "abcde3"] [4 "abcde4"] [5 "abcde5"]])
    (is (= [[1 "abcde1"] [2 "abcde2"] [3 "abcde3"] [4 "abcde4"] [5 "abcde5"]]
           (get-field b :second-field)))))

(deftest enum-field-write-test
  (let [spec {:first-field (int32-type)
              :second-field (enum-type (long-type) {:STARTUP 0x02 :QUERY 0x07})}
        b    (compose-buffer spec)]
    (set-field b :second-field :STARTUP)
    (is (= :STARTUP
           (get-field b :second-field)))
    (set-field b :second-field :QUERY)
    (is (= :QUERY
           (get-field b :second-field)))))

(deftest enum-field-write-test
  (let [s (spec :first-field (int32-type)
                :second-field (enum-type (repeated-type (composite-type (int32-type) (string-type 10)) 5)
                                         {:FIRST [[1 "abcde1"] [2 "abcde2"] [3 "abcde3"] [4 "abcde4"] [5 "abcde5"]]
                                          :SECOND [[6 "abcde6"] [7 "abcde7"] [8 "abcde8"] [9 "abcde9"] [10 "abcde10"]]}))
        b (compose-buffer s)]
    (set-field b :second-field :FIRST)
    (is (= :FIRST
           (get-field b :second-field)))
    (set-field b :second-field :SECOND)
    (is (= :SECOND
           (get-field b :second-field)))))


(deftest complete-access-test
  (let [s (spec :first-field  (int32-type)
                :second-field (string-type 10)
                :third-field  (boolean-type))
        b (compose-buffer s)]

    (set-fields b {:first-field 101
                   :second-field "string"
                   :third-field true})
    (is (= {:third-field true :second-field "string" :first-field 101}
           (decompose b)))))


(deftest long-payload-test
  (let [s (spec :first-field (int32-type)
                :second-field (string-type 10))
        b (compose-buffer s)]

    (set-field b :first-field 101)
    (is (= 101 (get-field b :first-field)))

    (set-field b :first-field 10)
    (is (= 10 (get-field b :first-field)))

    (set-field b :first-field Integer/MAX_VALUE)
    (is (= Integer/MAX_VALUE (get-field b :first-field)))

    (set-field b :first-field (- Integer/MAX_VALUE))
    (is (= (- Integer/MAX_VALUE) (get-field b :first-field)))

    (set-field b :first-field -101)
    (is (= -101 (get-field b :first-field)))))

(deftest array-map-payload
  (let [field #(keyword (format "field-%02d" %))
        s     {:field-00 (int32-type)
               :field-01 (string-type 10)
               :field-02 (int32-type)
               :field-03 (string-type 10)
               :field-04 (int32-type)
               :field-05 (string-type 10)}
        b     (compose-buffer s)]
    (doseq [i (range 0 5)]
      (if (even? i)
        (set-field b (field i) i)
        (set-field b (field i) (str i))))

    (doseq [i (range 0 5)]
      (if (even? i)
        (is (= i (get-field b (field i))))
        (is (= (str i) (get-field b (field i))))))))

(deftest over-32-field-payload
  (let [field #(keyword (format "field-%02d" %))
        spec  (apply spec
                     (interleave
                      (map
                       field
                       (range 0 35))
                      (interleave (repeatedly int32-type) (repeatedly #(string-type 10)))))
        b     (compose-buffer spec)]

    (doseq [i (range 0 35)]
      (if (even? i)
        (set-field b (field i) i)
        (set-field b (field i) (str i))))

    (doseq [i (range 0 35)]
      (if (even? i)
        (is (= i (get-field b (field i))))
        (is (= (str i) (get-field b (field i))))))))
