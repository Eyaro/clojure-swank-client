(comment
(ns client.example
  ;(:require )
  (:use client.swank)
  ;(:import )
  )

(defclient client-decide [event conn]
  (try
    (do
      (println event))
    (catch Exception e (println "EXCEPTION"))))

  (def *c*  (start-client
              nil
              #'client-decide
              :host "127.0.0.1" :port 4040))

  (defn send-emacs [event] (send-to-emacs *c* event))
  (defn send-one [] (send-emacs "(:emacs-rex 'hor)"))
  ;;write to it and receive an error! Cool!
  (write-to-connection *c* "(:emacs-rexs '(print 'hi))")
  ;;or use (write-sexp-to-connection *c* '(:emacs-rex (print 'hi))) 
  )

