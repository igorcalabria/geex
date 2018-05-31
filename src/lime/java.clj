(ns lime.java

  "Generation of Java backed code"

  (:require [lime.java.defs :as jdefs]
            [lime.core :as lime]
            [lime.platform.low :as low]
            [clojure.spec.alpha :as spec]
            [lime.core.seed :as seed]
            [lime.core :as core]
            [bluebell.utils.specutils :as specutils]
            [bluebell.utils.core :as utils]
            [clojure.string :as cljstr])
  (:import [org.codehaus.janino SimpleCompiler]))

(def platform-tag [:platform :java])

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Implementation
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn janino-cook-and-load-class [class-name source-code]
  "Dynamically compile and load Java code as a class"
  [class-name source-code]
  (try
    (let [sc (SimpleCompiler.)]
      (.cook sc source-code)
      (.loadClass (.getClassLoader sc) class-name))
    (catch Throwable e
      (throw (ex-info "Failed to compile code"
                      {:code source-code
                       :exception e})))))

(defn janino-cook-and-load-object  [class-name source-code]
  (.newInstance (janino-cook-and-load-class
                 class-name
                 source-code)))

(defn parse-typed-defn-args [args0]
  (specutils/force-conform ::jdefs/defn-args args0))

(defn java-class-name [parsed-args]
  (-> parsed-args
      :name
      name
      low/str-to-java-identifier))



(defn java-package-name [parsed-args]
  (-> parsed-args
      :ns
      low/str-to-java-identifier))

(defn full-java-class-name [parsed-args]
  (str (java-package-name parsed-args)
       "."
       (java-class-name parsed-args)))



(defn quote-arg-name [arg]
  (assert (map? arg))
  (merge arg
         {:name `(quote ~(:name arg))}))

(defn make-arg-decl [parsed-arg]
  [{:prefix " "
    :step ""}
   (low/java-type-name-primitive (low/get-type-signature platform-tag (:type parsed-arg)))
   (low/to-variable-name platform-tag (:name parsed-arg))
   ])

(defn join-args
  ([]
   nil)
  ([c0 c1]
   (if (nil? c0)
     c1
     (into [] [c0 [", "] c1]))))

(defn make-arg-list [parsed-args]
  (reduce join-args (map make-arg-decl parsed-args)))


;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;;
;;;  Interface
;;;
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

;; (supers (class (fn [x] (* x x))))
;; #{java.lang.Runnable java.util.Comparator java.util.concurrent.Callable clojure.lang.IObj java.io.Serializable clojure.lang.AFunction clojure.lang.Fn clojure.lang.IFn clojure.lang.AFn java.lang.Object clojure.lang.IMeta}

(defn to-binding [quoted-arg]
  (let [t (low/get-type-signature platform-tag
                                  (:type quoted-arg))]
    (println "The type is" t)

    ;;; TODO: Get the type, depending on what...
    (core/bind-name t (:name quoted-arg))))


(defn generate-typed-defn [args]
  (let [arglist (:arglist args)
        quoted-args (mapv quote-arg-name arglist)]
    `(let [[top# code#] (lime/top-and-code
                         [{:platform :java}]
                         (core/return-value (apply
                                             (fn [~@(map :name arglist)]
                                               ~@(:body args))
                                             (map to-binding ~quoted-args))))]
       (utils/indent-nested
        [[{:prefix " "
           :step ""}
          "package " ~(java-package-name args) ";"]
         ~(str "public class " (java-class-name args) " {")
         ["public " (low/java-type-name-primitive
                     (low/get-type-signature platform-tag top#))
          " apply("
          (make-arg-list ~quoted-args)
          ") {"
          code#
          "}"]
         "}"]))))

(defmacro typed-defn [& args0]
  (let [args (merge (parse-typed-defn-args args0)
                    {:ns (str *ns*)})
        code (generate-typed-defn args)
        arg-names (mapv :name (:arglist args))]
    `(let [obj# (janino-cook-and-load-object ~(full-java-class-name args)
                                             ~code)]
       (defn ~(:name args) [~@arg-names]
         (.apply obj# ~@arg-names)))))

(defmacro disp-ns []
  (let [k# *ns*]
    k#))

(comment
  (do

    (typed-defn return-primitive-number [(seed/typed-seed java.lang.Double) x]
                1)

    
        
    )


  )
