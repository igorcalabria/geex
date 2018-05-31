(ns lime.core.typesystem
  (:require [bluebell.utils.setdispatch :as sd]
            [bluebell.utils.symset :as ss]
            [lime.core.seed :as seed]
            [bluebell.tag.core :as tg]
            [bluebell.utils.debug :as debug]
            [clojure.spec.alpha :as spec]
            [bluebell.utils.party :as party]))

(sd/def-system system)

(defn tag-as-seed [x]
  [:seed x])

(defn tagged-as-seed? [x]
  (tg/tagged? :seed x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Specs
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(spec/def ::prefixed (spec/cat :prefix keyword?
                               :value any?))

(spec/def ::suffixed (spec/cat :value any?
                               :suffix keyword?))

(spec/def ::platform (spec/cat :prefix #{:platform}
                               :platform any?))

(spec/def ::type any?)
(spec/def ::typed-map (spec/keys :req-un [::type]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Indicators
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;


(defn basic-indicator [x]
  (cond
    (seed/seed? x) #{(tag-as-seed (seed/datatype x))}
    (map? x) #{:map}
    (set? x) #{:set}
    (vector? x) #{:vector}
    (seq? x) #{:seq}
    :default #{(class x)}))

(def prefix-indicator (sd/spec-indicator ::prefixed (fn [x] #{[:prefix (:prefix x)]})))
(def suffix-indicator (sd/spec-indicator ::suffixed (fn [x] #{[:suffix (:suffix x)]})))
(def typed-map-indicator (sd/spec-indicator ::typed-map (fn [x] #{ [:map-type (:type x)]})))

(def platform-indicator (sd/spec-indicator ::platform (fn [x] #{[:platform (:platform x)]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Superset generators
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn seed-supersets [set-reg x]
  (if (tagged-as-seed? x)
    (let [supersets (ss/direct-supersets-of set-reg (tg/value x))]
      (conj (set (map tag-as-seed supersets))
            :seed))))

(defn class-supersets [set-reg x]
  (if (class? x)
    (conj (set (supers x))
          :class)))

(defn tagged-generator [tag superset]
  (fn [set-reg x]
    (if (tg/tagged? tag x)
      #{superset}
      #{})))

(def prefix-generator (tagged-generator :prefix :prefixed))
(def primitive-array-generator (tagged-generator :java-primitive-array :java-primitive-array))
(def suffix-generator (tagged-generator :suffix :suffixed))
(def typed-map-generator (tagged-generator :map-type :typed-map))
(def platform-generator (tagged-generator :platform :platform))

(sd/register-superset-generator system seed-supersets)
(sd/register-superset-generator system class-supersets)
(sd/register-superset-generator system prefix-generator)
(sd/register-superset-generator system suffix-generator)
(sd/register-superset-generator system typed-map-generator)
(sd/register-superset-generator system primitive-array-generator)
(sd/register-superset-generator system platform-generator)

(defn primitive-array-class [element-class]
  (class (make-array element-class 0)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Static relations
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
(sd/subset-of system :map :associative)
(sd/subset-of system :seq :sequential)
(sd/subset-of system :vector :sequential)
(sd/subset-of system :associative :coll)
(sd/subset-of system :sequential :coll)
(sd/subset-of system :coll :any)
(sd/subset-of system :prefixed :vector)
(sd/subset-of system :suffixed :vector)
(sd/subset-of system :typed-map :map)
(sd/subset-of system [:seed java.lang.Object] :any)
(sd/subset-of system :seed :any)
(sd/subset-of system java.lang.Object :class)
(sd/subset-of system :class :any)

(sd/subset-of system java.lang.Double :floating-point)
(sd/subset-of system java.lang.Float :floating-point)
(sd/subset-of system java.lang.Byte :integer)
(sd/subset-of system java.lang.Short :integer)
(sd/subset-of system java.lang.Integer :integer)
(sd/subset-of system java.lang.Long :integer)
(sd/subset-of system clojure.lang.BigInt :integer)
(sd/subset-of system :floating-point :real-number)
(sd/subset-of system :integer :real-number)
(sd/subset-of system clojure.lang.Ratio :real-number)
(sd/subset-of system :real-number :general-number)
(sd/subset-of system :general-number :any)


(sd/subset-of system java.lang.Double/TYPE :floating-point)
(sd/subset-of system java.lang.Float/TYPE :floating-point)
(sd/subset-of system java.lang.Byte/TYPE :integer)
(sd/subset-of system java.lang.Short/TYPE :integer)
(sd/subset-of system java.lang.Integer/TYPE :integer)
(sd/subset-of system java.lang.Long :integer)
(sd/subset-of system java.lang.Boolean/TYPE :java-primitive)
(sd/subset-of system java.lang.Character/TYPE :java-primitive)
(sd/subset-of system java.lang.Void/TYPE :java-primitive)
(sd/subset-of system :java-primitive :any)

(sd/subset-of system clojure.lang.Keyword :keyword)
(sd/subset-of system clojure.lang.Symbol :symbol)
(sd/subset-of system java.lang.String :string)

(sd/subset-of system :keyword :any)
(sd/subset-of system :symbol :any)
(sd/subset-of system :string :any)



(sd/subset-of system :java-primitive-array :java-array)
(sd/subset-of system (primitive-array-class Double/TYPE) [:java-primitive-array :floating-point])
(sd/subset-of system (primitive-array-class Float/TYPE) [:java-primitive-array :floating-point])
(sd/subset-of system (primitive-array-class Byte/TYPE) [:java-primitive-array :integer])
(sd/subset-of system (primitive-array-class Short/TYPE) [:java-primitive-array :integer])
(sd/subset-of system (primitive-array-class Integer/TYPE) [:java-primitive-array :integer])
(sd/subset-of system (primitive-array-class Long/TYPE) [:java-primitive-array :integer])
(sd/subset-of system (primitive-array-class Boolean/TYPE) [:java-primitive-array :any])
(sd/subset-of system (primitive-array-class Character/TYPE) [:java-primitive-array :any])

(sd/def-feature feature
  basic-indicator
  prefix-indicator
  suffix-indicator
  typed-map-indicator
  platform-indicator)


