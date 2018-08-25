(ns geex.lib-test
  (:require [geex.java :as java :refer [typed-defn] :as java]
            [geex.core :as core]
            [bluebell.utils.setdispatch :as setdispatch]
            [geex.lib :as lib]
            [bluebell.utils.symset :as ss]
            [clojure.test :refer :all]
            [geex.visualize :as viz]
            [geex.debug :as gdb]))

(gdb/set-expr-map-inspector viz/plot-expr-map)

(typed-defn add-3 [Double/TYPE a
                   Double/TYPE b
                   Double/TYPE c]
            (lib/+ a b c))

(deftest add-3-test
  (is (= (add-3 9.0 4.1 1.15)
         14.25)))

(typed-defn and-3
            [Boolean/TYPE a
             Boolean/TYPE b
             Boolean/TYPE c
             ]
            (lib/and a b c))

(defn gen-combos [n]
  (if (= n 1)
    [[true] [false]]
    (transduce
     (comp (map (fn [combo]
                  [(conj combo false) (conj combo true)]))
           cat)
     conj
     []
     (gen-combos (dec n)))))

(defn true-and-3 [a b c]
  (and a b c))

(deftest and-3-test
  (doseq [combo (gen-combos 3)]
    (let [actual (apply and-3 combo)
          expected (apply true-and-3 combo)]
      (is (= actual expected))
      (when (not= actual expected)
        (println "Combo" combo)
        (println "Actual" actual)
        (println "Expected" expected)))))


(typed-defn or-3
            [Boolean/TYPE a
             Boolean/TYPE b
             Boolean/TYPE c]
            (lib/or a b c))

(defn true-or-3 [a b c]
  (or a b c))

(deftest test-or-3
  (doseq [combo (gen-combos 3)]
    (is (= (apply or-3 combo)
           (apply true-or-3 combo)))))

(typed-defn add-with-constant
            [Long/TYPE x]
            (lib/+ x 119))

(deftest add-with-constant-test
  (is (= 121 (add-with-constant 2))))

(typed-defn my-negate [Double/TYPE x]
            (lib/- x))

(deftest my-negate-test
  (is (= -3.0 (my-negate 3))))

(typed-defn my-sub [Double/TYPE a
                    Double/TYPE b]
            (lib/- a b))

(deftest my-sub-test
  (is (= -314.0
         (my-sub 10 324))))

(typed-defn my-sub-3 [Double/TYPE a
                      Double/TYPE b
                      Double/TYPE c]
            (lib/- a b c))

(deftest sub-3-test
  (is (= -12.0
         (my-sub-3 1 4 9))))

(typed-defn my-not [Boolean/TYPE x]
            (lib/not x))

(deftest not-test
  (is (= false (my-not true)))
  (is (= true (my-not false))))

(typed-defn my-implies [Boolean/TYPE a
                        Boolean/TYPE b]
            (lib/implies a b))

(deftest implies-test
  (is (true? (my-implies false false)))
  (is (true? (my-implies false true)))
  (is (false? (my-implies true false)))
  (is (true? (my-implies true true))))

(typed-defn my-raw-eq-test [Long/TYPE a
                            Long/TYPE b]
            (lib/== a b))

(deftest raw-eq-test
  (is (my-raw-eq-test 9 9))
  (is (not (my-raw-eq-test 9 8))))

(typed-defn in-interval? [Long/TYPE a]
            (lib/and (lib/<= 4 a)
                     (lib/<= a 9)))

(deftest in-interval-test
  (is (in-interval? 4))
  (is (not (in-interval? 0))))

(typed-defn compare-against-119 [Long/TYPE x]
            [(lib/== 119 x)
             (lib/<= 119 x)
             (lib/>= 119 x)
             (lib/< 119 x)
             (lib/> 119 x)
             (lib/!= 119 x)
             (lib/= 119 x)
             (lib/not= 119 x)
             ])

(deftest compare-agains-119-test
  (is (= [true true true false false false true false]
         (compare-against-119 119)))
  (is (= [false false true false true true false true]
         (compare-against-119 118)))
  (is (= [false true false true false true false true]
         (compare-against-119 120))))


;; TODO: Comparison of general objects.
(typed-defn eq-ops [clojure.lang.IPersistentVector a
                    clojure.lang.IPersistentVector b]
            (lib/== a b))

(deftest test-common-eq
  (let [x [:a :b :c]]
    (is (eq-ops x x))
    (is (not (eq-ops x [:a :b])))))

(typed-defn mixed-add [Double/TYPE a
                       Long/TYPE b]
            (lib/+ a b))



(deftest mixed-add-test
  (is (= 7.0 (mixed-add 3 4))))


(typed-defn fn-returning-nil [Long/TYPE x]
            (core/If (lib/< x 9)
                     (lib/wrap "Less than 9")
                     (lib/nil-of java.lang.String)))

(deftest check-nil-ret
  (is (= "Less than 9" (fn-returning-nil 1)))
  (is (nil? (fn-returning-nil 19))))

(typed-defn div-120 [Double/TYPE x]
            (lib// 120.0 x))

(deftest div-test
  (is (= 40.0 (div-120 3))))

(typed-defn mul-120 [Double/TYPE x]
            (lib/* 120.0 x))

(deftest mul-test
  (is (= 1200.0 (mul-120 10))))

(typed-defn aset-test-fn [Integer/TYPE size
                       Integer/TYPE magic-pos]
            (let [dst (lib/make-array Double/TYPE size)]
              (lib/aset dst magic-pos 119.0)
              dst))

(deftest aset-test0
  (is (= [0.0 0.0 0.0 119.0 0.0 0.0 0.0 0.0 0.0 0.0]
         (vec (aset-test-fn 10 3)))))

(typed-defn make-magic-array [Integer/TYPE m]
            (let [dst (lib/make-array Double/TYPE 10)]
              (lib/aset dst 3 12.0)
              (lib/aset dst 4 14.0)
              (lib/aset dst 5 (lib/aget dst m))
              dst))

(deftest magic-array-test
  (is (= [0.0 0.0 0.0 12.0 14.0 12.0 0.0 0.0 0.0 0.0]
         (vec (make-magic-array 3)))))

(typed-defn alength-fn [(lib/array-class Double/TYPE) x]
            (lib/alength x))

(deftest alength-test
  (is (= (alength-fn (make-array Double/TYPE 4)) 4)))


(typed-defn some-collections []
            [(lib/wrap {:a 3})
             (lib/wrap [])
             (lib/wrap '())
             (lib/wrap #{:a})])

(deftest some-coll-test
  (is (= (some-collections)
         [{:a 3} [] () #{:a}])))

(typed-defn conj-some-values []
            (lib/conj
             (lib/conj
              (lib/wrap [])
              3) :a))

(deftest test-conj
  (is (= [3 :a]
         (conj-some-values))))

(typed-defn is-it-nil? [(lib/array-class Double/TYPE) x]
            (lib/nil? x))

(deftest is-it-nil-test
  (is (false? (is-it-nil? (make-array Double/TYPE 3))))
  (is (true? (is-it-nil? nil))))

(typed-defn is-it-empty? [clojure.lang.IPersistentVector x]
            (lib/empty? x))

(deftest emptiness-test
  (is (true? (is-it-empty? [])))
  (is (true? (is-it-empty? nil)))
  (is (false? (is-it-empty? [:a]))))

(typed-defn get-the-first [clojure.lang.IPersistentVector v]
            (lib/first v))

(deftest first-test
  (is (= (get-the-first [119 2 3])
         119)))

(typed-defn get-the-count [clojure.lang.IPersistentVector v]
            {:the-count-is (lib/count v)})

(deftest count-test
  (is (= {:the-count-is 3}
         (get-the-count [1 2 4]))))

(typed-defn get-the-first-of-rest [clojure.lang.IPersistentVector v]
            (-> v
                lib/rest
                lib/first))

(deftest rest-test
  (is (= 119
         (get-the-first-of-rest [120 119]))))


(typed-defn first-two-of-obj [java.lang.Object x]
            [(lib/first x)
             (-> x lib/rest lib/first)])

(deftest first-obj-test
  (is (= [3 4]
         (first-two-of-obj [3 4 9 4]))))

(defn squared-norm-impl [src-coll]
  (lib/reduce
   (fn [result input-obj]
     (lib/+ result
            (lib/sqr (lib/unwrap Double/TYPE input-obj))))
   (lib/wrap 0.0)
   src-coll))

(typed-defn squared-norm [java.lang.Object src-coll]
            (squared-norm-impl src-coll))

(deftest squared-norm-test
  (is (= 25.0 (squared-norm '(3.0 4.0)))))

(typed-defn basic-wrapping [Double/TYPE x]
            [x x x])


(deftest basic-wrapping-test
  (is (= [4.0 4.0 4.0]
         (basic-wrapping 4.0))))

(typed-defn squared-norm-v [clojure.lang.IPersistentVector src-coll]
            (squared-norm-impl src-coll))

(deftest squared-norm-v-test
  (is (= 25.0 (squared-norm-v [3.0 4.0]))))

(typed-defn inc-vector-values [clojure.lang.IPersistentVector src]
            (lib/transduce
             (lib/map (comp lib/inc (partial lib/unwrap Double/TYPE)))
             lib/conj
             (lib/cast clojure.lang.IPersistentCollection (lib/wrap []))
             src))

(deftest transducer-test
  (is (= (inc-vector-values [1.0 2.0 3.0])
         [2.0 3.0 4.0])))

(defn small-value? [x]
  (lib/<= x 3.0))


(defn inc-twice [x]
  (-> x
      lib/inc
      lib/inc))

(typed-defn inc2-f [Double/TYPE x]
            (inc-twice x))

(deftest inc-twice-test
  (is (= 3.0 (inc2-f 1.0))))


(typed-defn inc-twice-loop [clojure.lang.IPersistentVector src]
  (lib/reduce
   (fn [result x]
     (lib/conj result (lib/inc (lib/inc (lib/unwrap Double/TYPE x)))))
   (lib/cast clojure.lang.IPersistentCollection (lib/wrap []))
   src))

(def the-transducer (comp (lib/map (comp lib/inc lib/inc (partial lib/unwrap Double/TYPE)))
                          ))

(typed-defn inc-and-keep-small [clojure.lang.IPersistentVector v]
            (lib/transduce
             the-transducer
             lib/conj
             (lib/cast clojure.lang.IPersistentCollection (lib/wrap []))
             v))


(deftest inc-twice-test
  (is (= [3.0 4.0 5.0]
         (inc-and-keep-small [1.0 2.0 3.0]))))


(typed-defn dec-and-keep-positive [clojure.lang.IPersistentVector src]
            (lib/transduce
             (comp (lib/map (comp #(lib/- % 4) (partial lib/unwrap Double/TYPE)))
                   (lib/filter #(lib/<= 0.0 %))
                   (lib/map (partial lib/* 2.0)))
             lib/conj
             (lib/cast clojure.lang.IPersistentCollection (lib/wrap []))
             src))

(deftest try-complex-transducer
  (is (= [0.0 2.0 4.0 6.0 8.0]
         (dec-and-keep-positive (vec (map double (range 9)))))))


(typed-defn quot-with-3 [Long/TYPE a]
            [(lib/quot a 3)
             (lib/rem a 3)])

(deftest quot-with-3-test
  (is (= (quot-with-3 11) [3 2]))
  (is (= (quot-with-3 8) [2 2])))

(def V2 (vec (take 2 (repeat Double/TYPE))))

(typed-defn norm-of-v2 [V2 x]
            (lib/sqrt (apply lib/+ (map lib/sqr x))))

(deftest v2-norm-test
  (is (= (norm-of-v2 [3.0 4.0])
         5.0)))

(typed-defn sliceable-array-size [(lib/array-class Double/TYPE) arr]
            (-> arr
                lib/sliceable-array
                lib/count))

(deftest sliceable-array-size-test
  (is (= 5 (sliceable-array-size (double-array [1 2 3 4 5]))))
  (is (= 6 (sliceable-array-size (double-array [1 2 8 3 4 5])))))

(typed-defn arr-to-it [(lib/array-class Double/TYPE) x]
            (lib/iterable x))

(typed-defn sum-first-two-elements-of-array [(lib/array-class Double/TYPE) x0]
            (let [x (lib/iterable x0)]
              (lib/+ (lib/first x)
                     (lib/first (lib/rest x)))))

(deftest sum-first-two-elements-test
  (is (= 109.0 (sum-first-two-elements-of-array (double-array [9 100])))))

(typed-defn sum-up-array [(lib/array-class Double/TYPE) x]
            (lib/reduce lib/+ (lib/wrap 0.0) x))

(deftest sum-array-test
  (is (= 10.0 (sum-up-array (double-array [1 2 3 4])))))

(typed-defn sum-but-first-two [(lib/array-class Double/TYPE) x]
            (lib/reduce lib/+
                        (lib/wrap 0.0)
                        (lib/slice x 2 (lib/count x))))


(deftest sum-but-2-test
  (is (= (sum-but-first-two (double-array (range 5)))
         (+ 2.0 3.0 4.0))))

(typed-defn sum-with-marg [(lib/array-class Double/TYPE) x
                           Integer/TYPE marg]
            (lib/reduce lib/+ (lib/slice-but (lib/slice-from x marg) marg)))

(deftest sum-with-marg-test
  (is (= 1100.0 (sum-with-marg (double-array [1 2 1000 100 9 9]) 2))))

(typed-defn wrap-into-struct-array [(lib/array-class Double/TYPE) arr]
            (lib/wrap-struct-array #_[(lib/typed-seed Double/TYPE)
                                      (lib/typed-seed Double/TYPE)]
                                   [(lib/typed-seed Double/TYPE)
                                    (lib/typed-seed Double/TYPE)]
                                   arr))

(deftest wrap-structs-test
  (let [result (wrap-into-struct-array (double-array (range 9)))]
    (is (= (select-keys result [:offset :size :struct-size :type])
           {:offset 0
            :size 4
            :struct-size 2
            :type :struct-array}))))

(typed-defn pop-and-cast-ab [V2 x]
            (lib/populate-and-cast {:a (lib/typed-seed Integer/TYPE)
                                    :b (lib/typed-seed Float/TYPE)}
                                   x))

(deftest test-pop-and-cast
  (is (= (pop-and-cast-ab [3.4 1.0])
         {:a 3 :b 1.0})))

(typed-defn aget-test
            [(lib/array-class Double/TYPE) xy-pairs
             Integer/TYPE index]
            (lib/aget
             (lib/wrap-struct-array {:x (lib/typed-seed Double/TYPE)
                                     :y (lib/typed-seed Double/TYPE)}
                                    xy-pairs)
             index))

(deftest struct-array-aget-test
  (is (= {:x 2.0 :y 3.0}
         (aget-test (double-array (range 12)) 1))))
