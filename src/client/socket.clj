
(ns client.socket
  (:require (swank.util.concurrent [mbox :as mb]))
  (:use swank.core.server)
  (:import (java.net InetAddress Socket)))

(defn current-thread [] (Thread/currentThread))
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
  ([socket connection-serve options]
     (when-let [announce (options :announce)]
       (announce (.getLocalPort socket)))
     (connection-serve socket)
     socket))

;;depends on socket-serve private function in (ns swank.core.server in server.clj)
(def #^{:private true} private-socket-serve
  (ns-resolve 'swank.core.server 'socket-serve))
(defn setup-client [port announce-fn connection-serve opts]
  (let [socket (make-client-socket
                 {:port    port
                  :host    (opts :host)})]
  (start-swank-client-socket!
    socket
    #(private-socket-serve connection-serve % opts)
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
