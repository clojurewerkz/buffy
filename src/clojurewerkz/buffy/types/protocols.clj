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

(ns clojurewerkz.buffy.types.protocols
  (:refer-clojure :exclude [read]))

;;
;; Protocol
;;

(defprotocol BuffyType
  (size         [bt] "Return size of the data type")
  (write        [bt buffer idx value] "Relative write")
  (read         [bt buffer idx] "Relative read")
  (rewind-write [bt buffer value] "Writes at current writer position, rewinds writer index to the last written position.")
  (rewind-read  [bt buffer] "Reads value at current reader position, rewinds reader index to the end of current read value.")
  )
