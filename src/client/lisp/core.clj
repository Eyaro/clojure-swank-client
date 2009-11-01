
(ns client.lisp.core
  ;(:require )
  (:use
    clojure.contrib.def clojure.contrib.pprint
    client.swank client.threadmap
    client.lisp.utils)
  ;(:import )
  )

(defn start-lisp-client [log func & others]
  (apply start-client log func (constantly {:default-package "COMMON-LISP-USER"})
    others))

(defn emacs-rex
  ([swank message] (emacs-rex-with-thread swank message 't
                     (:default-package swank)))
  ([swank message pkg] (emacs-rex-with-thread swank message :repl-thread pkg)))

(defn *emacs-rex
  ([swank callback message]
   (thread-map-id callback) (emacs-rex swank message))
  ([swank callback message pkg]
   (thread-map-id callback)
   (emacs-rex swank callback message pkg)))

;(:emacs-rex message pkg [nil] threadID [t] messageNum)
(defn emacs-rex-with-thread
  ([swank message thread-id]
    (emacs-rex-with-thread swank message thread-id nil ))
  ([swank message thread-id pkg]
    (let [formatted-message
          (cl-format nil "(:emacs-rex ~A ~A ~A ~A)"
            message pkg thread-id
            (thread-map-id nil))]
      (print-swank-out "~A" formatted-message)
      (write-to-connection swank formatted-message))))

(defn send-eval
  ([swank message pkg]
    (emacs-rex swank
      (str "(swank:listener-eval \""
        (format-code message) "\")") pkg))
  ([swank message] (send-eval swank message *default-package*)))



