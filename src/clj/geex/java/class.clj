(ns geex.java.class
  (:require [clojure.spec.alpha :as spec]
            [clojure.reflect :as r]
            [geex.core :as core]))

(spec/def ::name string?)
(spec/def ::visibility #{:public :private :protected})
(spec/def ::static? boolean?)
(spec/def ::fn fn?)
(spec/def ::class class?)

(spec/def ::type any?) ;; anything that we can call type-sig on 
(spec/def ::arg-type ::type)
(spec/def ::ret ::type)
(spec/def ::arg-types (spec/* ::arg-type))
(spec/def ::init any?)
(spec/def ::final? boolean?)

(spec/def ::method (spec/keys
                    :req-un [::name
                             ::arg-types]
                    :opt-un [::fn
                             ::visibility
                             ::static?
                             ::ret
                             ::final?]))

(defn abstract-method? [x]
  (not (contains? x :fn)))

(spec/def ::abstract-method abstract-method?)

(defn dynamic? [x]
  (not (:static? x)))

(spec/def ::dynamic dynamic?)

(spec/def ::interface-method
  (spec/and
   ::abstract-method
   ::dynamic
   (spec/keys :req-un [::name
                       ::arg-types
                       ::ret])))

(spec/def ::constructor
  (spec/keys :req-un [::arg-types
                      ::fn]
             :opt-un [::visibility]))

(spec/def ::interface-methods (spec/* ::interface-method))

(spec/def ::variable (spec/keys
                      :req-un [::name]
                      :opt-un [::visibility
                               ::type
                               ::static?
                               ::init
                               ::final?]))

(spec/def ::method-map (spec/map-of string? ::method))
(spec/def ::variable-map (spec/map-of string? ::variable))
(spec/def ::methods (spec/* ::method))
(spec/def ::variables (spec/* ::variable))
(spec/def ::classes (spec/* ::class))
(spec/def ::extends ::class)
(spec/def ::implements ::classes)
(spec/def ::super ::class)
(spec/def ::package string?)
(spec/def ::key string?)
(spec/def ::flags (spec/* core/valid-flags))
(spec/def ::private-stub class?)
(spec/def ::public-stub class?)
(spec/def ::interface? boolean?)
(spec/def ::constructors (spec/* ::constructor))
(spec/def ::local-classes (spec/* ::class-def))
(spec/def ::format? boolean?)

(spec/def ::class-def (spec/keys :opt-un [::name
                                          ::constructors
                                          ::flags
                                          ::visibility
                                          ::methods
                                          ::variables
                                          ::extends
                                          ::implements
                                          ::super
                                          ::final?
                                          ::key
                                          ::method-map
                                          ::variable-name
                                          ::package
                                          ::private-stub
                                          ::public-stub
                                          ::local-classes
                                          ::interface?
                                          ::format?]))

(defn make-map-from-named [coll]
  (transduce
   (map (fn [x] [(:name x) x]))
   conj
   {}
   coll))

(defn add-kv-pair-non-dup [msg m [k v]]
  (when (contains? m k)
    (throw (ex-info msg
                    {:key k
                     :value-a (get m k)
                     :value-b v})))
  (assoc m k v))

(defn check-non-dup [msg kv-pairs]
  (reduce
   (partial add-kv-pair-non-dup msg) 
   {}
   kv-pairs))

(defn method-signature [method]
  (select-keys method [:name :arg-types]))

(defn var-signature [method]
  (select-keys method [:name]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Interface
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def class-def? (partial spec/valid? ::class-def))

(defn anonymous? [x]
  (contains? x :super))

(defn visibility [x]
  (or (:visibility x)
      :public))

(defn visibility-str [v]
  {:pre [(spec/valid? ::visibility v)]}
  (name v))

(defn static? [x]
  (:static? x))

(defn valid? [x]
  (and (map? x)
       (::valid? x)))

(defn abstract? [class-def]
  (if (valid? class-def)
    (:abstract? class-def)
    (some abstract-method? (:methods class-def))))


(defn validate-class-def [class-def]
  (if (valid? class-def)
    class-def
    (do
      
      (when (not (spec/valid? ::class-def class-def))
        (throw (ex-info
                (str "Class-def does not conform with spec: "
                     (spec/explain-str ::class-def class-def))
                {})))  
      (check-non-dup
       "Duplicate method"
       (mapv (fn [method]
               [(method-signature method) method])
             (:methods class-def)))
      (check-non-dup
       "Duplicate variable"
       (mapv (fn [v]
               [(var-signature v) v])
             (:variables class-def)))

      (when (:interface? class-def)
        (let [cl (:local-classes class-def)]
          (when (not (empty? cl))
            (throw (ex-info "An interface cannot have local classes"
                            {:local-classes cl}))))
        (let [ext (:extends class-def)]
          (when (not (empty? ext))
            (throw (ex-info "An interface cannot extend classes"
                            {:extends ext}))))
        
        (doseq [method (:methods class-def)]
          (do (when (not (spec/valid? ::interface-method method))
                (throw
                 (ex-info
                  (str
                   "Invalid interface method: "
                   (spec/explain-str ::interface-method method))
                  {:method method})))))

        (when (not (empty? (:variables class-def)))
          (throw (ex-info
                  "An interface does not have variables"
                  {:variables (:variables class-def)})))
        (let [cst (:constructors class-def)]
          (if (not (empty? cst))
            (throw (ex-info "An interface does not have constructors"
                            {:constructors cst})))))
      
      (when
          (and (contains? class-def :super)
               (or (not (empty? (:extends class-def)))
                   (not (empty? (:implements class-def)))))
        (throw (ex-info "I don't think you are allowed to create an anonymous class that inherits or extends other classes")))
      (merge class-def {::valid? true
                        :method-map (make-map-from-named
                                     (:methods class-def))
                        :variable-map (make-map-from-named
                                       (:variables class-def))
                        :abstract? (abstract? class-def)}))))

(defn named? [x]
  (contains? x :name))

(defn implements-code [class-def]
  (let [classes (:implements class-def)]
    (if (empty? classes)
      []
      (vec
       (butlast
        (reduce
         into ["implements"]
         (map (fn [x]
                [(r/typename x) ", "])
              classes)))))))

(defn extends-code [class-def]
  (if-let [e (:extends class-def)]
    ["extends" (r/typename e)]
    []))

(defn has-key? [x]
  (contains? x :key))

(defn full-java-class-name
  ([package-name class-name]
   (if (nil? package-name)
     class-name
     (str package-name
          "."
          class-name)))
  ([class-def]
   {:pre [(valid? class-def)
          (named? class-def)]}
   (full-java-class-name (:package class-def) (:name class-def))))

(defn has-stubs? [x]
  (and (class? (:private-stub x))
       (class? (:public-stub x))))

(defn interface? [x]
  (:interface? x))

(defn format? [x]
  (:format? x))
