
(ns client.lisp.utils
  ;(:require )
  (:use clojure.contrib.pprint)
  ;(:import )
  )

(defn format-code [#^String code]
  (.replace
    (.replace code "\\" "\\\\")
    "\"" "\\\""))

(defmacro print-swank-out [st & args]
  `(~'cl-format true ~(str "~&>>]  " st "~%") ~@args))
(defmacro print-swank-in [st & args]
  `(~'cl-format true ~(str "~&[<<  " st "~%") ~@args))



