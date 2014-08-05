(ns clojurewerkz.buffy.core-test
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer :all]
            [clojurewerkz.buffy.util :refer :all]
            [clojurewerkz.buffy.core :refer :all]
            [clojurewerkz.buffy.types.protocols :refer :all]
            [simple-check.core :as sc]
            [simple-check.generators :as gen]
            [simple-check.properties :as prop]))

(def quickcheck-iterations 10)

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
    (set-field b :first-field 0)
    (is (= 0 (get-field b :first-field)))

    (set-field b :first-field 50)
    (is (= 50 (get-field b :first-field)))))

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

(deftest enum-field-write-test-1
  (let [spec {:first-field (int32-type)
              :second-field (enum-type (long-type) {:STARTUP 0x02 :QUERY 0x07})}
        b    (compose-buffer spec)]
    (set-field b :second-field :STARTUP)
    (is (= :STARTUP
           (get-field b :second-field)))
    (set-field b :second-field :QUERY)
    (is (= :QUERY
           (get-field b :second-field)))))

(deftest enum-field-write-test-2
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

    (compose b {:first-field 101
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

(deftest bit-field-write-test
  (let [s (spec :first-field (bit-type 4)
                :second-field (string-type 10))
        b (compose-buffer s)]
    (let [v1 [true  true  false false
              false false false false
              false false false false
              false false false false
              false false false false
              false false false false
              false false false false
              false false false false]]
      (set-field b :first-field v1)
      (is (= v1 (get-field b :first-field))))
    (let [v1 [true true true true
              true true true true
              true true true true
              true true true true
              true true true true
              true true true true
              true true true true
              true true true true]]
      (set-field b :first-field v1)
      (is (= v1 (get-field b :first-field))))))

(deftest bit-map-field-read-write-test
  (let [s                       (spec :payload (bit-map-type :a 1 :b 2 :c 3 :d 4 :e 6))
        b                       (compose-buffer s)
        equivalent-regular-type (bit-type 2)]
    (let [p {:a 2r0,
             :b 2r00,
             :c 2r000,
             :d 2r0000,
             :e 2r000000}]
      (set-field b :payload p)
      (is (= p (get-field b :payload)))
      (is (= [false
              false false
              false false false
              false false false false
              false false false false false false]
             (read equivalent-regular-type (.buf b) 0))))
    (let [p {:a 2r1,
             :b 2r11,
             :c 2r111,
             :d 2r1111,
             :e 2r111111}]
      (set-field b :payload p)
      (is (= p (get-field b :payload)))
      (is (= [true
              true true
              true true true
              true true true true
              true true true true true true]
             (read equivalent-regular-type (.buf b) 0))))
    (let [p {:a 2r1,
             :b 2r11,
             :c 2r101,
             :d 2r1001,
             :e 2r100100}]
      (set-field b :payload p)
      (is (= p (get-field b :payload)))
      (is (= [true
              true true
              true false true
              true false false true
              true false false true false false]
             (read equivalent-regular-type (.buf b) 0))))))

(deftest wrapped-buffer-test
  (let [s (spec :first-field (int32-type)
                :second-field (string-type 10))
        b (compose-buffer s :orig-buffer (java.nio.ByteBuffer/allocate 14))]
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

(deftest to-bit-map-test
  (is (= [0 0 0 0
          0 0 0 0

          0 0 0 0
          0 0 0 0

          0 0 0 0
          0 0 0 0

          0 1 1 0
          0 1 0 1]
         (->> 101 (to-bit-map (int32-type)) to-binary)))

  (is (= [0 0 0 0
          0 0 0 0

          0 0 0 1
          0 0 1 1

          0 0 1 0
          0 0 1 1

          0 1 0 0
          0 1 0 0]
         (->> 1254212 (to-bit-map (int32-type)) to-binary))))

(deftest to-bit-roundtrip-test
  (let [num 101]
    (is (= num
           (->> num
                (to-bit-map (int32-type))
                (from-bit-map (int32-type))))))
  (let [num 1254212]
    (is (= num
           (->> num
                (to-bit-map (int32-type))
                (from-bit-map (int32-type)))))))

(deftest very-bit-bit-set-test
  (let [cap 800
        s   (spec :first-field (bit-type (/ cap 8))
                  :second-field (string-type 10))
        b   (compose-buffer s)]
    (let [v1
          (interleave (repeat (/ cap 2) true) (repeat (/ cap 2) false))]
      (set-field b :first-field v1)
      (is (= v1
             (get-field b :first-field))))))

(deftest rewinding-write-read-tests
  (testing "Rewind-read and write ints"
    (doseq [[type samples] {(int32-type)     [100 1001 10001]
                            (boolean-type)   [true, false]
                            (byte-type)      [10 11 12]
                            (short-type)     [100 1001 10001]
                            (long-type)      [100 1001 10001]
                            (medium-type)    [100 1001 10001]
                            (float-type)     [100.0 1001.0 10001.0]
                            (double-type)    [100.0 1001.0 10001.0]
                            ;; (bytes-type 10)  [(.getBytes "abcdef") (.getBytes "cfdegh")]
                            (string-type 10) ["abcdef" "cfdegh"]
                            }]
      (let [b (direct-buffer (* (size type) (count samples)))]
        (reset-writer-index b)
        (doseq [sample samples]
          (rewind-write type b sample))
        (rewind-until-end b)
        (doseq [sample samples]
          (is (= sample (rewind-read type b))))

        )

      )
    (comment

      (let [int-t    (int32-type)
            b        (direct-buffer 20)]
        (reset-writer-index b)
        (rewind-write int-t b 1)
        (rewind-write int-t b 100)
        (rewind-write int-t b 1000)
        (rewind-until-end b)
        (is (= 1 (rewind-read int-t b)))
        (is (= 100 (rewind-read int-t b)))
        (is (= 1000 (rewind-read int-t b)))
        )

      (testing "Rewind-read and write strings"
        (let [int-t    (int32-type)
              string-t (string-type 10)
              b        (direct-buffer 20)]
          (reset-writer-index b)
          (rewind-write int-t b 1)
          (rewind-write string-t b "abcdefg")
          (rewind-until-end b)
          (is (= 1 (rewind-read int-t b)))
          (is (= "abcdefg" (rewind-read string-t b)))
          )))
    )
)
