(ns buffy.core
  (:refer-clojure :exclude [read])
  (:import [io.netty.buffer UnpooledByteBufAllocator ByteBufAllocator]))

(def ^ByteBufAllocator allocator UnpooledByteBufAllocator/DEFAULT)

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

(defn direct-buffer
  ([]
     (.directBuffer allocator))
  ([^long initial-capacity]
     (.directBuffer allocator initial-capacity initial-capacity))
  ([^long initial-capacity ^long max-capacity]
     (.directBuffer allocator initial-capacity max-capacity)))

(defprotocol BuffyType
  (size [bt])
  (write [bt buffer idx value])
  (read [bt buffer idx]))

(deftype Int32Type []
  BuffyType
  (size [_] 4)
  (write [bt buffer idx value]
    (.setInt buffer idx value))
  (read [by buffer idx]
    (.getInt buffer idx)))

(deftype BooleanType []
  BuffyType
  (size [_] 1)
  (write [bt buffer idx value]
    (.setBoolean buffer idx value))
  (read [by buffer idx]
    (.getBoolean buffer idx)))

(deftype ByteType []
  BuffyType
  (size [_] 1)
  (write [bt buffer idx value]
    (.setByte buffer idx value))
  (read [by buffer idx]
    (.getByte buffer idx)))

(deftype ShortType []
  BuffyType
  (size [_] 2)
  (write [bt buffer idx value]
    (.setShort buffer idx value))
  (read [by buffer idx]
    (.getShort buffer idx)))

(deftype MediumType []
  BuffyType
  (size [_] 3)
  (write [bt buffer idx value]
    (.setMedium buffer idx value))
  (read [by buffer idx]
    (.getMedium buffer idx)))

(deftype FloatType []
  BuffyType
  (size [_] 4)
  (write [bt buffer idx value]
    (.setFloat buffer idx value))
  (read [by buffer idx]
    (.getFloat buffer idx)))

(deftype LongType []
  BuffyType
  (size [_] 4)
  (write [bt buffer idx value]
    (.setLong buffer idx value))
  (read [by buffer idx]
    (.getLong buffer idx)))



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

(deftype BytesType [size]
  BuffyType
  (size [_] size)
  (write [bt buffer idx value]
    (.setBytes buffer idx value)
    (zero-fill-till-end buffer idx (count value) (.size bt)))
  (read [bt buffer idx]
    (read-nonempty-bytes buffer idx (.size bt))))

(deftype StringType [size]
  BuffyType
  (size [_] size)
  (write [bt buffer idx value]
    ;; TODO assert
    (.setBytes buffer idx (.getBytes value))
    (zero-fill-till-end buffer idx (count value) size))
  (read [bt buffer idx]
    (String.
     (read-nonempty-bytes buffer idx (.size bt)))))

(deftype CompositeType [types]
  BuffyType
  (size [_] (reduce + (map #(.size %) types)))
  (write [bt buffer idx values]
    (assert (= (count types) (count values)) "Number of values passed to composite should equal number of types")
    (doseq [[type value position] (map vector types values (positions types))]
      (.write type buffer (+ idx position) value)))
  (read [bt buffer idx]
    (into []
          (for [[type position] (map vector types (positions types))]
            (.read type buffer (+ idx position))))))

(deftype RepeatedType [type times]
  BuffyType
  (size [_] (* (.size type) times))
  (write [bt buffer idx values]
    (doseq [[value position] (map vector values (positions (repeat (count values) type)))]
      (.write type buffer (+ idx position) value))
    ;; buffer idx size expected-size
    (zero-fill-till-end buffer idx (dec (* (.size type) (-> values count inc))) (.size bt)))

  (read [bt buffer idx]
    (into []
          (for [position (positions (repeat times type))]
            (.read type buffer (+ idx position))))))

(def int32-type (memoize #(Int32Type.)))
(def boolean-type (memoize #(BooleanType.)))
(def byte-type (memoize #(ByteType.)))
(def short-type (memoize #(ShortType.)))
(def medium-type (memoize #(MediumType.)))
;; TODO: Add unsigned types: byte, int, medium, short
(def float-type (memoize #(FloatType.)))
(def long-type (memoize #(LongType.)))

(defn composite-type
  [& types]
  (CompositeType. types))

(defn repeated-type
  [type times]
  (RepeatedType. type times))

(def string-type (memoize (fn [length] (StringType. length))))

(defn bytes-type
  [length]
  (BytesType. length))

(defn spec
  [& {:keys [] :as conf}]
  (let [indexes (map first conf)]
    (fn [buf field]

      )))



(defprotocol IBuffyBuf
  (buffer [this])
  (slices [this])

  (get-field [this field-name])
  (get-field-idx [this field-idx])

  (set-field [this field-name value])
  (set-field-idx [this field-name value]))


(deftype BuffyBuf [buf indexes types positions]
  IBuffyBuf
  (buffer [b] buf)

  (set-field [b field-name value]
    (let [idx (get (.indexes b) field-name)]
      (write (nth (.types b) idx)
             (.buf b)
             (nth (.positions b) idx)
             value))
    b)

  (get-field [b field-name]
    (let [idx (get (.indexes b) field-name)]
      (read (nth (.types b) idx)
            (.buf b)
            (nth (.positions b) idx))))

  )

(defn compose-buffer
  [spec]
  (let [indexes    (zipmap (map first spec)
                           (iterate inc 0))
        types      (map second spec)
        total-size (reduce + (map size types))
        buffer     (direct-buffer total-size)
        positions  (positions types)]
    (BuffyBuf. buffer indexes types positions)))
