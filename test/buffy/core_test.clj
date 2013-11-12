(ns buffy.core-test2
  (:refer-clojure :exclude [read])
  (:require [clojure.test :refer :all]
            [buffy.core :refer :all]
            [simple-check.core :as sc]
            [simple-check.generators :as gen]
            [simple-check.properties :as prop]))

(def quickcheck-iterations 1000)

(deftest size-test
  (is (= 4 (size (int32-type))))
  (is (= 10 (size (string-type 10)))))

(deftest positions-test
  (is (= '(0 4 14 18 22) (positions [(int32-type) (string-type 10) (int32-type) (int32-type) (string-type 16)]))))

(deftest int-field-write-test
  (let [spec {:first-field (int32-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)]
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
  (let [spec {:first-field (boolean-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)]
    (set-field b :first-field true)
    (is (= true (get-field b :first-field)))
    (set-field b :first-field false)
    (is (= false (get-field b :first-field)))
    (set-field b :first-field true)
    (is (= true (get-field b :first-field)))))

(deftest byte-field-write-test
  (let [spec {:first-field (byte-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)]
    (set-field b :first-field (byte \0))
    (is (= (byte \0) (get-field b :first-field)))

    (set-field b :first-field (byte 50))
    (is (= (byte 50) (get-field b :first-field)))))

(deftest short-field-write-test
  (let [spec {:first-field (short-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)]
    (set-field b :first-field 10)
    (is (= 10 (get-field b :first-field)))

    (set-field b :first-field Short/MAX_VALUE)
    (is (= Short/MAX_VALUE (get-field b :first-field)))

    (set-field b :first-field (- Short/MAX_VALUE))
    (is (= (- Short/MAX_VALUE) (get-field b :first-field)))))

(deftest medium-field-write-test
  (let [spec {:first-field (medium-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)]
    (set-field b :first-field 10)
    (is (= 10 (get-field b :first-field)))

    (set-field b :first-field 8388607)
    (is (= 8388607 (get-field b :first-field)))

    (set-field b :first-field (- 8388607))
    (is (= (- 8388607) (get-field b :first-field)))))

(deftest float-field-write-test
  (let [spec {:first-field (float-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)]
    (set-field b :first-field (float 5.34))
    (is (= (float 5.34) (get-field b :first-field)))))

(deftest string-field-write-test
  (let [spec {:first-field (int32-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)]
    (set-field b :second-field "abcdef")
    (is (= "abcdef" (get-field b :second-field)))
    (set-field b :second-field "yoyo")
    (is (= "yoyo" (get-field b :second-field)))
    (set-field b :second-field "")
    (is (empty? (get-field b :second-field)))))

(deftest bytes-field-test
  (let [spec {:first-field  (string-type 10)
              :second-field (bytes-type 10)}
        b    (compose-buffer spec)]
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
  (let [spec {:first-field (int32-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)]
    (is (= true (get-set-generative gen/int b :first-field)))))

(deftest ^:generative byte-field-write-test-generative-test
  (let [spec {:first-field (byte-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)]
    (is (= true (get-set-generative gen/byte b :first-field)))))

(def gen-float
  "Generates natural numbers, starting at zero. Shrinks to zero."
  (gen/fmap (fn [[a b]] (float (+ a (* 0.01 b)))) (gen/vector gen/int 2)))

(deftest ^:generative float-field-write-test-generative-test
  (let [spec {:first-field (float-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)]

    (is (= true (get-set-generative gen-float b :first-field)))))

(deftest ^:generative string-field-write-test-generative-test
  (let [spec {:first-field (int32-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)
        g    (gen/such-that #(< (count %1) 10) gen/string-ascii)]

    (is (= true (get-set-generative g b :second-field)))))


(deftest ^:generative concurrent-write-read-test
  (let [spec {:first-field (int32-type)
              :second-field (string-type 10)}
        b    (compose-buffer spec)
        g    (gen/such-that #(< (count %1) 10) gen/string-ascii)

        res1 (future (get-set-generative gen/int b :first-field))

        res2 (future (get-set-generative g b :second-field))

        res1 (deref res1)
        res2 (deref res2)]

    (is (= true res1))
    (is (= true res2))))


(deftest composite-field-write-test
  (let [spec {:first-field (int32-type)
              :second-field (composite-type (int32-type) (string-type 10))}
        b    (compose-buffer spec)]
    (set-field b :second-field [100 "abcdef"])
    (is (= [100 "abcdef"] (get-field b :second-field)))))

(deftest repeated-field-write-test
  (let [spec {:first-field (int32-type)
              :second-field (repeated-type (int32-type) 10)}
        b    (compose-buffer spec)]
    (set-field b :second-field [1 2 3 4 5 6 7 8 9 10])
    (is (= [1 2 3 4 5 6 7 8 9 10] (get-field b :second-field)))


    (set-field b :second-field [5 6 7 8 9])
    (is (= [5 6 7 8 9 0 0 0 0 0] (get-field b :second-field)))))
