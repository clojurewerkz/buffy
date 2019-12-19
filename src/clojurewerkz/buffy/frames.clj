;; Copyright (c) 2013-2014 Alex Petrov, Michael S. Klishin, and the ClojureWerkz Team
;;
;; Licensed under the Apache License, Version 2.0 (the "License");
;; you may not use this file except in compliance with the License.
;; You may obtain a copy of the License at
;;
;;     http://www.apache.org/licenses/LICENSE-2.0
;;
;; Unless required by applicable law or agreed to in writing, software
;; distributed under the License is distributed on an "AS IS" BASIS,
;; WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
;; See the License for the specific language governing permissions and
;; limitations under the License.

(ns clojurewerkz.buffy.frames
  (:require [clojurewerkz.buffy.types :as t]
            [clojurewerkz.buffy.types.protocols :as p]
            [clojurewerkz.buffy.util :as util]))

(set! *warn-on-reflection* true)

(declare composite-frame)
(declare encoding-size*)
(declare decoding-size*)

(defn- make-encoder-bindings
  [triplets]
  (reduce (fn [acc [a1 a2 _]] (-> acc (conj a1) (conj a2)))
          []
          triplets))

(defn- types
  [triplets]
  (map second triplets))

(defn third [[_ _ x]] x)

(defmacro frame-encoder
  [binding & triplets]
  (let [triplets (partition 3 triplets)]
    `(fn* ~binding
          (let ~(make-encoder-bindings triplets)
            [(apply composite-frame [~@(types triplets)])
             ~(mapv third triplets)]))))


(defn make-decoder-bindings
  ([tuples]
     (make-decoder-bindings tuples (map first tuples)))
  ([tuples types]
     (let [[[f s] & more] tuples]
       (if (not (empty? more))
         (list 'let [f s] (make-decoder-bindings more types))
         (list 'let [f s] (cons `composite-frame types))))))

(defmacro frame-decoder
  [binding & tuples]
  (let [buffer (first binding)
        tuples (partition 2 tuples)]
    `(fn* ~binding
          ~(make-decoder-bindings tuples))))

(defprotocol Frame
  (encoder-for [this value])
  (decoder-for [this buffer idx])

  (encoding-size [this value])
  (decoding-size [this buffer idx]))

(deftype FrameType [encoder decoder value-formatter]
  p/BuffyType
  (size [_] (throw (RuntimeException. "Can't determine size of the frame")))

  (write [bt buffer idx value]
    (let [[type values] (encoder value)]
      (p/write type
               buffer
               idx
               values)))
  (read [by buffer idx]
    (value-formatter
     (p/read (decoder buffer idx)
             buffer
             idx)))

  Frame
  (encoder-for [this value]
    (encoder value))

  (decoder-for [this buffer idx]
    (decoder buffer idx))

  (encoding-size [_ value]
    (->> value
         encoder
         (partition 2)
         (map (fn [[a b]] (encoding-size* a b)))
         (reduce +)))

  (decoding-size [_ buffer idx]
    (decoding-size* (decoder buffer idx) buffer idx))

  Object
  (toString [_]
    "Frame"))

(defn composite-frame-sizes
  [subframes values]
  (->> values
       (interleave subframes)
       (partition 2)
       (map (fn [[t v]] (encoding-size* t v)))))

(deftype CompositeFrame [subframes]
  p/BuffyType
  (size [_] (throw (RuntimeException. "Can't determine size of composite frame")))

  (write [_ buffer idx values]
    (loop [[[frame value] & more] (partition 2 (interleave subframes values))
           [size & more-sizes]    (composite-frame-sizes subframes values)
           idx                    idx]
      (p/write frame buffer idx value)
      (when (not (empty? more))
        (recur more more-sizes (+ idx size))))
    buffer)

  (read [_ buffer idx]
    (loop [[frame & more] subframes
           idx            idx
           acc            []]
      (if (nil? frame)
        acc
        (let [size  (decoding-size* frame buffer idx)
              value (p/read frame buffer idx)]
          (recur more (+ idx size) (conj acc value))))))

  Frame
  (encoding-size [_ values]
    (reduce + (composite-frame-sizes subframes values)))

  (decoding-size [_ buffer idx]
    (loop [size 0
           idx  idx
           [frame & more] subframes]
      (if (nil? frame)
        size
        (let [^long frame-size (decoding-size* frame buffer idx)]
          (recur (+ size frame-size) (+ idx frame-size) more)))))

  Object
  (toString [_]
    (str "Composite Frame of:" (vec subframes))))

(defn encoding-size*
  [frame-or-type value]
  (if (instance? (:on-interface Frame) frame-or-type)
    (encoding-size frame-or-type value)
    (p/size frame-or-type)))

(defn decoding-size*
  [frame-or-type buffer idx]
  (if (instance? (:on-interface Frame) frame-or-type)
    (decoding-size frame-or-type buffer idx)
    (p/size frame-or-type)))

(defn frame-type
  ([encoder decoder]
     (FrameType. encoder decoder identity))
  ([encoder decoder value-formatter]
     (FrameType. encoder decoder value-formatter)))

(defn composite-frame
  [& subframes]
  (CompositeFrame. subframes))

(defn repeated-frame
  [subframe count]
  (CompositeFrame. (repeat count subframe)))
