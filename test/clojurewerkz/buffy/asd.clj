(ns my-binary-project.core
  (:require [clojurewerkz.buffy.core :refer :all]))

(let [spec {:int-field (int32-type)
            :string-field (string-type 10)}
      buf  (compose-buffer spec)]

  (set-field buf :int-field 101)
  (get-field buf :int-field)
  ;; => 101

  (set-field buf :string-field "stringie")
  (get-field buf :string-field)
  ;; => "stringie"
  )
