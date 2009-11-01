(ns client.swank
 (:require swank.core.connection
   (swank.util.concurrent [mbox :as mb]))
 (:use
   swank.util.concurrent.thread
   client.socket
   [swank.core :exclude (send-to-emacs)]
   swank.core.server))


;;similar functions for server are in server.clj (ns swank.core.server)
(defn simple-client-announce [port]
  (println "Attempting to connect on local port " port))

;;;;;;;;;;;;;;;;;;

;;Equivalent to putting in swank.clj (ns swank.swank)
(defn client-serve [conn f] 
  (let [out *out*
        control
        (dothread-swank
          (binding [*out* out]       
            ;;do not fail silently! For now...
            (.setName (Thread/currentThread) "Client Swank Control Thread")
            (client-control-loop conn f)))
        read
        (dothread-swank 
          (thread-set-name "Client Read Loop Thread")
          (try
            (read-loop conn control)
            (catch Exception e
              ;; This could be put somewhere better
              (.interrupt control)
              (dosync (alter *connections* (partial remove #{conn}))))))]
    (dosync
      (ref-set (conn :control-thread) control)
      (ref-set (conn :read-thread) read))))

(defn start-client
  "Start the server and write the listen port number to
PORT-FILE. This is the entry point for Emacs."
  ([port-file client-func slot-func & opts]
    (let [opts (apply hash-map opts) new-conn (atom nil)]
      (setup-client (get opts :port 0)
        (fn announce-port [port]
          (if port-file
            (announce-port-to-file port-file port))
          (simple-client-announce port))
        (fn [conn]
          (with-connection (merge conn {:handlers (atom nil)} (slot-func conn))
            (reset! new-conn (current-connection))
            (client-serve (current-connection) client-func)))
        opts)
      @new-conn)))


(defn connection-handlers [conn]
  (:handlers conn))

(defmacro alter-handler [conn func & args]
  `(dosync (alter (connection-handlers ~conn) ~func ~@args)))

(defn write-to-connection
  "Given a `writer' (java.io.Writer) and a `message'
(typically an sexp), encode the message according to the slime protocol and
write the message into the writer.
   The protocol itself is simply a 6-character hex string,
representing the message length, followed by a lisp-readable
version of the message itself.
See also `read-swank-message'."
  ([conn message]
    (let [writer #^java.io.Writer (:writer @conn) 
          len (.length message)]
      (doto writer
        (.write (format "%06x" len))
        (.write message)
        (.flush))))
  {:tag String})

(defn send-to-emacs [conn event]
  (mb/send @(:control-thread conn) event))

(defn write-sexp-to-connection [conn message]
  (swank.core.connection/write-to-connection @conn message))

(defn close-connection [conn]
  (.stop @(:control-thread conn))
  (.stop @(:read-thread conn))
  (.close (:socket conn)))




;;UTILS

(defn register-handler [conn k func]
  (alter-handler conn (fn [& args]
                        (apply merge-with concat args))
    {k (list func)}))

(defn register-handlers [conn k & funcs]
  (alter-handler conn (fn [& args]
                        (apply merge-with concat args))
    {k funcs}))

(defn clear-handlers [conn k]
  (alter-handler conn dissoc k))

(defmacro register-all-handlers [conn & args]
  `(do ~@(map (fn [the-args] `(register-handlers ~conn ~@the-args)) args)))

(defmacro register-default-handlers [conn func & the-keys]
  (let [auto-func (gensym)]
    `(let [~auto-func ~func]
       ~@(map (fn [the-key] `(register-handlers ~conn ~the-key
                               ~auto-func))
           the-keys))))

(defn call-handlers
  ([conn event] (call-handlers conn event (first event)))
  ([conn event the-key]
    (let [func-list (@(connection-handlers conn) the-key)]
      (dorun (map #(% conn event) func-list))
      (if func-list true))))

(defmacro with-catch [& bod]
  `(try (do ~@bod)
     (catch Exception e#
       (.printStackTrace e#))))
