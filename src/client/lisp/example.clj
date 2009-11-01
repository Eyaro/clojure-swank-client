
(ns client.example
  ;(:require )
  (:use 
    client.swank  ;client.threadmap
    client.lisp.core client.lisp.utils
    clojure.contrib.def clojure.contrib.pprint)
  ;(:import )
  )

(defn client-decide [event conn]
  (with-catch
    (comment
      (if-not (call-handlers conn event)
      (cl-format true
        "!!! Warning: No handler found for event ~A~%" event)))
    ;(find-and-start-thread thread-id)
    (print-swank-in "~A" event)))

(def *swank*
  (start-lisp-client
    nil
    #'client-decide
    :host "127.0.0.1" :port 4040))

(comment
  (emacs-rex *swank* "(cl::print 'hi)")
  (defn send-emacs [event] (send-to-emacs *swank* event))
  (defn send-one [] (send-emacs "(:emacs-rex 'hor)"))
  (register-all-handlers *client*
    (:read-string )
    (:presentation-start )
    (:presentation-end )
    (:write-string )
    (:indentation-update )
    (:ping )
    (:debug-activate )
    (:debug )
    (:debug-return ))

  (register-default-handlers *client* f
    :return :reader-error :new-features :new-package)


  )



