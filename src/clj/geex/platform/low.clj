(ns geex.platform.low

  "Platform specific code needed by the compiler"
  
  (:require [bluebell.utils.defmultiple :refer [defmultiple defmultiple-extra]]
            [bluebell.utils.core :as utils]
            [geex.core.defs :as defs]
            [bluebell.utils.setdispatch :as sd]
            [geex.core.seed :as seed]
            [clojure.reflect :as r]
            [geex.core.typesystem :as ts]
            [bluebell.utils.symset :as ss]
            [geex.core.stringutils :as su]
            [clojure.string :as cljstr]
            ))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;   Code generators
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmultiple compile-static-value defs/platform-dispatch
  (defs/clojure-platform [value] value))

(sd/def-dispatch get-type-signature ts/system ts/feature)

(sd/def-set-method get-type-signature
  "A seed with a general Java class"
  [[:platform p]
   [(ss/difference [:seed :class]
                   [:seed :java-primitive]) x]]
  (seed/datatype x))

(sd/def-set-method get-type-signature
  "A seed with a Java primitive"
  [[:platform p]
   [[:seed :java-primitive] x]]
  (seed/datatype x))

(sd/def-set-method get-type-signature
  "A vector"
  [[:platform p]
   [:vector x]]
  clojure.lang.IPersistentVector)

(sd/def-set-method get-type-signature
  "A map"
  [[:platform p]
   [:map x]]
  clojure.lang.IPersistentMap)

(sd/def-set-method get-type-signature
  "A set"
  [[:platform p]
   [:set x]]
  clojure.lang.IPersistentSet)

(sd/def-set-method get-type-signature
  "Anything else"
  [[:platform p]
   [:any x]]
  java.lang.Object)


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Identifiers on Java
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defn str-to-java-identifier [& args]
  (-> (cljstr/join "_" args)
      (cljstr/replace "_" "__")
      (cljstr/replace "-" "_d")
      (cljstr/replace ":" "_c")
      (cljstr/replace "/" "_s")
      (cljstr/replace "." "_p")
      (cljstr/replace "?" "_q")))


(sd/def-dispatch to-java-identifier ts/system ts/feature)

(sd/def-set-method to-java-identifier [[:symbol x]]
  (str-to-java-identifier (name x)))

(sd/def-set-method to-java-identifier [[:string x]]
  (str-to-java-identifier x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;   Compile a return value
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(sd/def-dispatch compile-return-value ts/system ts/feature)

(sd/def-set-method compile-return-value [[[:platform :java] p]
                                         [:any datatype]
                                         [:any expr]]
  [{:prefix " "
    :step ""}
   "return " expr ";"])

(sd/def-set-method compile-return-value [[[:platform :java] p]
                                         [:nil datatype]
                                         [:any expr]]
  [{:prefix " "
    :step ""}
   "return /*nil datatype*/;"])


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Variable names
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(sd/def-dispatch to-variable-name ts/system ts/feature)

(sd/def-set-method to-variable-name
  [[[:platform :java] p]
   [(ss/union :symbol :string) x]]
  (to-java-identifier x))

(sd/def-set-method to-variable-name
  [[[:platform :clojure] p]
   [(ss/union :symbol :string) x]]
  (symbol x))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Binding names
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(sd/def-dispatch compile-bind-name ts/system ts/feature)

(sd/def-set-method compile-bind-name [
                                      [[:platform :java] p]
                                      [:any x]
                                      ]
  (to-java-identifier x))




;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Bindings
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(defmultiple render-bindings utils/first-arg
  ([:platform :clojure] [p tail body]
   `(let ~(reduce into [] (map (fn [x]
                                 [(:name x) (:result x)])
                               tail))
      ~body))
  ([:platform :java] [p tail body]
   [
    (mapv (fn [x]
            [su/compact
             (let [dt (seed/datatype (:seed x))]
               (if (nil? dt)
                 []
                 (str (r/typename dt)
                      " "
                      (:name x)
                      " = ")))
             (:result x)
             ";"])
          tail)
    body
    ]))
