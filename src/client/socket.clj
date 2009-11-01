
(ns client.socket
  (:require (swank.util.concurrent [mbox :as mb])
   )
  (:use swank.core.server
    swank.util.concurrent.thread
    [swank.core.connection :exclude (with-connection)]
    swank.util.net.sockets)
  (:import (java.net InetAddress Socket)))


;;;;;;;;;;;;;;;;;;
;;;;;;;;;;;;;;;;;;
;;Equivalent to putting in server.clj (ns swank.core.server)

(defn make-client-socket
  ([] (Socket.))
  ([options]
    (Socket.
      (when-let [host (options :host)]
        (if (instance? InetAddress host)
          host
          (InetAddress/getByName host)))
      (options :port 0))))


(defn start-swank-client-socket!
  "Starts and returns the socket server as a swank host. Takes an
   optional set of options as a map:

    :announce - an fn that will be called and provided with the
      listening port of the newly established server. Default: none
    :dont-close - will accept multiple connections if true. Default: false."
  ([socket connection-serve] (start-swank-client-socket! socket connection-serve {}))
  ([socket connection-serve options] (println "START SWANK CLIENT SOCKET!")
     (when-let [announce (options :announce)]
       (announce (.getLocalPort socket)))
     (connection-serve socket)
     socket))




(defn- accept-authenticated-connection ;; rename to authenticate-socket, takes in a connection
  "Accepts and returns new connection if it is from an authenticated
   machine. Otherwise, return nil.

   Authentication depends on the contents of a slime-secret file on
   both the server (swank) and the client (emacs slime). If no
   slime-secret file is provided on the server side, all connections
   are accepted.

   See also: `slime-secret'"
  ([#^Socket socket opts] (println socket) (println "SCOK")
     (let [conn (make-connection socket (get opts :encoding *default-encoding*))]

       (println conn)
       (if-let [secret (#'swank.core.server/slime-secret)]
         (when-not (= (read-from-connection conn) secret)
           (close-socket! socket))) conn)))

(defmacro with-connection [conn & body]
  `(binding [swank.core.connection/*current-connection* ~conn] ~@body))
(defn current-connection []
  swank.core.connection/*current-connection*)

(defn- socket-serve [connection-serve socket opts] 
  (with-connection (accept-authenticated-connection socket opts)
    (dosync (ref-set (swank.core.connection/*current-connection* :writer-redir) *out*))
    (dosync (alter swank.core.server/*connections* conj swank.core.connection/*current-connection*))
    (connection-serve swank.core.connection/*current-connection*)
    (not dont-close)))

(defn setup-client [port announce-fn connection-serve opts]
  (let [socket (make-client-socket
                 {:port    port
                  :host    (opts :host)})]
  (start-swank-client-socket!
    socket
    #(socket-serve connection-serve % opts)
    {:announce announce-fn
     :dont-close (opts :dont-close)})))

(defn client-control-loop
  "A loop that reads from the mbox queue and runs dispatch-event on
it (will block if no mbox control message is available). This is
intended to only be run on the control thread."
  ([conn f]
    (binding [*1 nil, *2 nil, *3 nil, *e nil]
        (loop []
          (f (mb/receive (current-thread)) conn)
          (recur)))))
