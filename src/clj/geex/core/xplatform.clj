(ns geex.core.xplatform
  (:require [geex.core.defs :as defs]
            [clojure.core :as c])
  (:refer-clojure :exclude [get]))

(def platform-map (atom {}))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Interface
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn register
  "Add a map of values"
  [platform-key value-map]
  {:pre [(map? value-map)]}
  (swap!
   platform-map
   (fn [dst]
     (update dst platform-key #(merge % value-map)))))

(defn list-platforms
  "List the available platforms"
  []
  (-> platform-map
      deref
      keys))

(defn get [key]
  (let [platform (defs/get-platform)
        data (deref platform-map)]
    (if (contains? data platform)
      (let [specific (c/get data platform)]
        (if (contains? specific key)
          (c/get specific key)
          (throw (ex-info "No such key"
                          {:key key
                           :platform platform
                           :available-keys (keys specific)}))))
      (throw (ex-info "No such platform"
                      {:non-existing-platform platform
                       :available-platforms (keys data)})))))

(defn call [f & args]
  (apply (get f) args))

(defn caller [f]
  (partial call f))
