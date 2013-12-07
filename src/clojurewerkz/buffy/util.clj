(ns clojurewerkz.buffy.util
  (:refer-clojure :exclude [read])
  (:require [clojurewerkz.buffy.types.protocols :refer :all])
  (:import [io.netty.buffer ByteBufUtil]))

(defn positions
  "Returns a lazy sequence containing positions of elements"
  [types]
  ((fn rpositions [[t & more] current-pos]
     (when t
       (cons current-pos (lazy-seq (rpositions more (+ current-pos (size t)))))))
   types 0))

(defn zero-fill-till-end
  [buffer idx size expected-size]
  (when (< size expected-size)
    (.setZero buffer (+ idx size) (- expected-size size))))

(defn read-nonempty-bytes
  [buffer idx size]
  (let [first-non-empty (or
                         (->> (range idx (+ idx size))
                              reverse
                              (filter #(not (= 0 (.getByte buffer %))))
                              first)
                         0)]
    (if (> first-non-empty 0)
      (let [ba (byte-array (- (inc first-non-empty) idx))]
        (.getBytes buffer idx ba)
        ba)
      (byte-array 0))))

;;
;; Hexdumps
;;

(def j #(clojure.string/join "" %))
(def jnl #(clojure.string/join \newline %))

(defn- hexdump-bytes
  [bytes]
  (j
   (for [b bytes]
     ;; only take lower bytes
     (let [b (mod b 256)]
       (cond
        (<= 0 b 16) (format "%02x" b)
        (< 0 b 256) (Integer/toHexString b))))))


(defn- to-ascii
  "Converts character to it's ascii representation"
  [c]
  (let [c (mod c 256)]
    (if (<= 0x1f c 0x7f)
      (char c)
      \.)))

(defn- chardump-bytes
  "Returns chardump of bytes"
  [bytes]
  (->> bytes
       (map to-ascii)
       j
       (partition-all 16)
       (map #(format "%16s" (j %)))))

(defn- format-hexdump
  "Formats hexsump"
  [dump]
  (->> dump
       (partition-all 32)
       (map (fn [line]
              (->> line
                   (interleave (take (count dump)
                                     (for [i (iterate inc 0)]
                                       (cond
                                        (= 0 (mod i 32))       ""
                                        (and (= 1 (quot i 16))
                                             (= 0 (mod i 16))) "  "
                                        (= 0 (mod i 2))        " "
                                        :else                  ""))))
                   flatten
                   j)))))

(def header1   "            +--------------------------------------------------+\n")
(def header2   "            | 0  1  2  3  4  5  6  7   8  9  a  b  c  d  e  f  |\n")
(def header3   " +----------+--------------------------------------------------+------------------+\n")
(def footer  "\n +----------+--------------------------------------------------+------------------+\n")

(defn hex-dump
  "Prints a hex representation of "
  [b & {:keys [print] :or {print true}}]
  (let [total        (.capacity b)
        total-padded (* 32 (inc (quot total 32)))
        bytes        (byte-array total-padded)
        _            (.getBytes b 0 bytes 0 total)
        formatted    (-> bytes hexdump-bytes format-hexdump)
        offsets      (map #(format "%08x" %) (map #(* 16 %) (range)))
        line         (repeat " | ")
        chardump     (chardump-bytes bytes)
        res          (j [header1 header2 header3
                         (->> (map vector line offsets line formatted line chardump line)
                              (map #(apply str %))
                              jnl)
                         footer])]
    (if print
      (println res)
      res)))

(defn seqable?
  "Returns true if (seq x) will succeed, false otherwise."
  [x]
  (or (seq? x)
      (instance? clojure.lang.Seqable x)
      (nil? x)
      (instance? Iterable x)
      (-> x .getClass .isArray)
      (string? x)
      (instance? java.util.Map x)))

(defn bits-on-at
  "Given a sequence of indexes (0-based), returns a sequence of 32 booleans where indexes
   on given positions are set to `true`"
  [positions]
  (assert (seqable? positions) "Positions for `bits-on-at` should be a sequence")
  (mapv (fn [i]
          (some #(= i %) positions))
        (range 0 32)))

(defn bits-off-at
  "Given a sequence of indexes (0-based), returns a sequence of 32 booleans where indexes
   on given positions are set to `false`"
  [positions]
  (assert (seqable? positions) "Positions for `bits-off-at` should be a sequence")
  (mapv (fn [i]
          (not (some #(= i %) positions)))
        (range 0 32)))

(defn on-bits-indexes
  "Returns indexes of the bits that are 'on'"
  [val]
  (assert (seqable? val) "Value should be sequence")
  (assert (= (count val) 32) "Value should be 32 bits long")
  (reduce (fn [acc [val i]]
            (if val
              (conj acc i)
              acc))
          []
          (map vector val (iterate inc 0))))

(defn off-bits-indexes
  "Returns indexes of the bits that are 'off'"
  [val]
  (assert (seqable? val) "Value should be sequence")
  (assert (= (count val) 32) "Value should be 32 bits long")
  (reduce (fn [acc [val i]]
            (if-not val
              (conj acc i)
              acc))
          []
          (map vector val (iterate inc 0))))
