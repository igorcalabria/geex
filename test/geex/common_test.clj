(ns geex.common-test
  (:require [geex.java :as java :refer [typed-defn] :as java]
            [geex.common :as lib]
            [geex.core :as core]
            [clojure.test :refer :all]
            [bluebell.utils.ebmd :as ebmd]
            [geex.ebmd.type :as gtype]
            [geex.graphviz :as graphviz]))

(typed-defn no-ret-fn [])

(typed-defn void-fn [] (lib/void))

(deftest void-test
  (is (nil? (void-fn)))
  (is (nil? (no-ret-fn))))



(typed-defn unwrapper-fn [java.lang.Object k]
            (lib/unwrap Double/TYPE k))

(typed-defn inc-vector-values

            [clojure.lang.IPersistentVector src]
            
            (lib/transduce
             (lib/map (comp lib/inc (partial lib/unwrap Double/TYPE)))
             lib/conj
             (lib/result-vector)
             src))

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

(typed-defn alength-fn [(lib/array-type Double/TYPE) x]
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

(typed-defn is-it-nil? [(lib/array-type Double/TYPE) x]
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
             (lib/result-vector)
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

(typed-defn more-rem [Long/TYPE a]
            (lib/rem a 3))

(deftest more-rem-test
  (is (= -1 (more-rem -13))))

(typed-defn mod-fun [Long/TYPE a]
            (lib/mod a 3))

(deftest mod-test
  (is (= 2 (mod-fun -13))))

(typed-defn pos-neg-zero-tester [Double/TYPE x]
            [(lib/pos? x)
             (lib/neg? x)
             (lib/zero? x)])

(deftest pos-neg-zero-test
  (is (= [true false false] (pos-neg-zero-tester 3.34)))
  (is (= [false true false] (pos-neg-zero-tester -3.34324)))
  (is (= [false false true] (pos-neg-zero-tester 0.0))))

(def V2 (vec (take 2 (repeat Double/TYPE))))

(typed-defn norm-of-v2 [V2 x]
            (lib/sqrt (apply lib/+ (map lib/sqr x))))

(deftest v2-norm-test
  (is (= (norm-of-v2 [3.0 4.0])
         5.0)))

(typed-defn sliceable-array-size [(lib/array-type Double/TYPE) arr]
            (-> arr
                lib/sliceable-array
                lib/count))

(deftest sliceable-array-size-test
  (is (= 5 (sliceable-array-size (double-array [1 2 3 4 5]))))
  (is (= 6 (sliceable-array-size (double-array [1 2 8 3 4 5])))))

(typed-defn arr-to-it [(lib/array-type Double/TYPE) x]
            (lib/iterable x))

(typed-defn sum-first-two-elements-of-array [(lib/array-type Double/TYPE) x0]
            (let [x (lib/iterable x0)]
              (lib/+ (lib/first x)
                     (lib/first (lib/rest x)))))

(deftest sum-first-two-elements-test
  (is (= 109.0 (sum-first-two-elements-of-array (double-array [9 100])))))

(typed-defn sum-up-array [(lib/array-type Double/TYPE) x]
            (lib/reduce lib/+ (lib/wrap 0.0) x))

(deftest sum-array-test
  (is (= 10.0 (sum-up-array (double-array [1 2 3 4])))))

(typed-defn sum-but-first-two [(lib/array-type Double/TYPE) x]
            (lib/reduce lib/+
                        (lib/wrap 0.0)
                        (lib/slice x 2 (lib/count x))))


(deftest sum-but-2-test
  (is (= (sum-but-first-two (double-array (range 5)))
         (+ 2.0 3.0 4.0))))

(typed-defn sum-with-marg [(lib/array-type Double/TYPE) x
                           Integer/TYPE marg]
            (lib/reduce lib/+ (lib/slice-but (lib/slice-from x marg) marg)))

(deftest sum-with-marg-test
  (is (= 1100.0 (sum-with-marg (double-array [1 2 1000 100 9 9]) 2))))

(typed-defn wrap-into-struct-array [(lib/array-type Double/TYPE) arr]
            (lib/wrap-struct-array #_[(lib/typed-seed Double/TYPE)
                                      (lib/typed-seed Double/TYPE)]
                                   [(lib/typed-seed Double/TYPE)
                                    (lib/typed-seed Double/TYPE)]
                                   arr))

(deftest wrap-structs-test
  (let [result (wrap-into-struct-array (double-array (range 9)))]
    (is (= (select-keys result [:offset :size
                                ::lib/struct-size :type])
           {:offset 0
            :size 4
            ::lib/struct-size 2
            :type :struct-array}))))

(typed-defn pop-and-cast-ab [V2 x]
            (lib/populate-and-cast {:a (lib/typed-seed Integer/TYPE)
                                    :b (lib/typed-seed Float/TYPE)}
                                   x))

(deftest test-pop-and-cast
  (is (= (pop-and-cast-ab [3.4 1.0])
         {:a 3 :b 1.0})))

(typed-defn aget-test
            [(lib/array-type Double/TYPE) xy-pairs
             Integer/TYPE index]
            (lib/aget
             (lib/wrap-struct-array {:x (lib/typed-seed Double/TYPE)
                                     :y (lib/typed-seed Double/TYPE)}
                                    xy-pairs)
             index))

(deftest struct-array-aget-test
  (is (= {:x 2.0 :y 3.0}
         (aget-test (double-array (range 12)) 1))))


(typed-defn two-power [Double/TYPE x]
  (lib/pow 2.0 x))

(deftest two-power-test
  (is (= 8.0 (two-power 3.0)))
  (is (= 32.0 (two-power 5.0))))

(typed-defn my-special-number-test [Double/TYPE x]
            [(lib/finite? x)
             (lib/infinite? x)
             (lib/nan? x)])

(deftest finiteness-test
  (is (= [true false false] (my-special-number-test 3.4)))
  (is (= [false false true] (my-special-number-test Double/NaN)))
  (is (= [false true false] (my-special-number-test Double/POSITIVE_INFINITY))))

(typed-defn some-bit-ops [Long/TYPE a
                          Long/TYPE b]
            [(lib/bit-and a b)
             (lib/bit-or a b)])

(deftest bit-op-test
  (is (= [1 15] (some-bit-ops 11 5))))

(typed-defn some-random-numbers
            []
            [(lib/basic-random)
             (lib/basic-random)
             (lib/basic-random)
             (lib/basic-random)])

(deftest random-test-make-sure-they-are-unique
  (is (= 4 (-> (some-random-numbers)
               set
               count))))

;;;;;;;; Would it be useful to automatically promote
;;;;;;;; chararcters to integers? Don't think so...
#_(typed-defn unary-promotion
            [Character/TYPE x]
            (lib/+ x))

#_(deftest promotion-test
  (is (= 65 (unary-promotion \A))))

#_(typed-defn negate-char
            [Character/TYPE x]
            (lib/- x))

#_(deftest negate-char-test
  (is (= -65 (negate-char \A))))


(typed-defn nth-fibonacci
            [Long/TYPE n]
            (let [[_ b _] (lib/iterate-while
                           [(lib/wrap 0) (lib/wrap 1) (lib/wrap 0)]
                           (fn [[a b counter]]
                             [b (lib/+ a b) (lib/inc counter)])
                           (fn [[_ _ counter]]
                             (lib/< counter n)))]
              b))

(deftest fibonacci-test
  (is (= [1 1 2 3 5 8 13 21]
         (mapv nth-fibonacci (range 8)))))

(typed-defn range-properties [Long/TYPE lower
                              Long/TYPE upper
                              Long/TYPE step]
            (let [rng (lib/range lower upper step)]
              {:range rng
               :count (lib/count rng)
               :first (lib/first rng)
               :rest (lib/rest rng)
               :iterable (lib/iterable rng)
               :empty? (lib/empty? rng)
               :aget (lib/aget rng 2)
               :slice (lib/slice rng 1 3)
               }))

(deftest range-test
  (let [result (range-properties 0 8 2)]
    (is (= (:count result) 4))
    (is (= 0 (:first result)))
    (is (= 2 (-> result
                 :rest
                 :offset)))
    (is (= (:iterable result)
           (:range result)))
    (is (not (:empty? result)))
    (is (= 4 (:aget result)))
    (is (= (-> result
               :slice
               :size)
           2))))

(typed-defn populate-array
            [Integer/TYPE size]
            (let [result (lib/make-array Long/TYPE size)]
              (lib/doseq [i (lib/range size)]
                (lib/aset result i (lib/sqr i)))
              result))


(deftest populate-array-test
  (is (= (vec (populate-array 10))
         (mapv #(* % %) (range 10)))))


(typed-defn string-to-int [java.lang.String s]
            (lib/call-method :static "valueOf" Integer s))

(typed-defn remove-prefix
            [java.lang.String s]
            (lib/call-method "substring" s
                             (int 2)
                             (lib/call-method "length" s)))

(deftest call-method-test
  (is (= 119 (string-to-int "119")))
  (is (= "kattskit" (remove-prefix "--kattskit"))))



(typed-defn struct-array-properties [(lib/array-type Double/TYPE) input-array]
            (let [arr (lib/wrap-struct-array
                       [(lib/typed-seed Double/TYPE)
                        (lib/typed-seed Double/TYPE)]
                       input-array)]
              {:count (lib/count arr)
               :first (lib/first arr)
               :rest (lib/rest arr)
               :iterable (lib/iterable arr)
               :empty? (lib/empty? arr)
               :slice (lib/slice arr 1 2)}))


(deftest struct-array-prop-test
  (let [result (struct-array-properties (double-array (range 6)))]
    (is (= 3 (:count result)))
    (is (= [0.0 1.0] (:first result)))
    (is (= 2 (:size (:rest result))))
    (is (= 3 (:size (:iterable result))))
    (is (not (:empty? result)))
    (is (= 1 (:size (:slice result))))
    (is (= 2 (:offset (:slice result))))))

(typed-defn populate-struct-array
            []
            (let [dt Double/TYPE
                  E (lib/typed-seed dt)
                  arr (lib/make-struct-array
                       [E E]
                       dt
                       4)]
              (lib/aset arr 2 [(lib/wrap 9)
                               (lib/wrap 7)])
              arr))

(typed-defn make-some-su []
            (lib/make-struct-array
             [(lib/typed-seed Double/TYPE)(lib/typed-seed Double/TYPE)]
             Double/TYPE
             4))

(deftest make-struct-array-test
  (is (= 8 (-> (make-some-su)
               :data
               alength)))
  (let [result (populate-struct-array)]
    (= (-> result :data vec)
       [0.0 0.0
        0.0 0.0
        9.0 7.0
        0.0 0.0])))

(typed-defn expect-true [Boolean/TYPE x]
            (lib/check x "X must be true!!!"))

(deftest test-check
  (is (thrown? Throwable (expect-true false)))
  (is (any? (expect-true true))))




;;;------- Promotions and EBMD -------
(ebmd/declare-poly add-0)
(ebmd/def-poly add-0 [::gtype/double-seed a
                      ::gtype/double-seed b]
  [:double (lib/+ a b)])

(java/typed-defn add-0-function [Long/TYPE a
                                 Long/TYPE b]
                 (add-0 (lib/wrap a)
                        (lib/wrap b)))

(deftest add-0-test
  (is (= [:double 119.0]
         (add-0-function 7 112))))


(ebmd/declare-poly add-1)

(ebmd/def-poly add-1 [::gtype/real-seed a
                      ::gtype/real-seed b]
  [:real-sum (lib/+ a b)])

(ebmd/def-poly add-1 [::gtype/double-seed a
                      ::gtype/double-seed b]
  [:double-sum (lib/+ a b)])

(java/typed-defn add-1-function []
                 [(add-1 (lib/wrap 1.0) (lib/wrap 3.0))
                  (add-1 (lib/wrap 1) (lib/wrap 3.0))])

(deftest real-seed-test
  (is (= (add-1-function)
         [[:double-sum 4.0] [:real-sum 4.0]])))


(ebmd/declare-poly add-2)

(ebmd/def-poly add-2 [::gtype/double a
                      ::gtype/double b]
  [:double (lib/+ a b)])

(ebmd/def-poly add-2 [::gtype/long a
                      ::gtype/long b]
  [:long (lib/+ a b)])

(java/typed-defn add-2-fn [Double/TYPE x
                           Long/TYPE y]
                 [(add-2 x 1.0)
                  (add-2 y 3)])

(deftest add-2-test
  (is (= (add-2-fn 3.0 4)
         [[:double 4.0]
          [:long 7]])))


(java/typed-defn conj-into-vec [clojure.lang.IPersistentVector dst
                                Double/TYPE x]
                 (reduce lib/conj
                         dst
                         [x 1 2 3]))

(deftest conj-into-vec-test
  (is (= [119.0 1 2 3]
         (conj-into-vec [] 119.0))))

(java/typed-defn make-vector [Long/TYPE n]
                 (core/Loop [dst (core/wrap [])
                             i 0]
                            (core/If (lib/< i n)
                                     (core/Recur
                                      (lib/conj dst i)
                                      (lib/inc i))
                                     dst)))

(deftest make-vector-test
  (let [result (make-vector 4)]
    (is (vector? result))
    (is (= [0 1 2 3]
           result))))

(java/typed-defn make-square-map [Long/TYPE n]
                 (core/Loop [dst (core/wrap {})
                             i 0]
                            (core/If
                             (lib/< i n)
                             (core/Recur
                              (lib/conj dst [i (lib/* i i)])
                              (lib/inc i))
                             dst)))

(deftest make-square-map-test
  (is (= (make-square-map 4)
         {0 0, 1 1, 2 4, 3 9})))

(java/typed-defn make-set [Long/TYPE n]
                 (core/Loop [dst (core/wrap #{})
                             i 0]
                            (core/If (lib/< i n)
                                     (core/Recur
                                      (lib/conj dst i)
                                      (lib/inc i))
                                     dst)))

(deftest make-set-test
  (is (= #{0 1 2 3}
         (make-set 4))))

(java/typed-defn populate-collection
                 [clojure.lang.IPersistentCollection dst
                  Long/TYPE n]
                 (core/Loop [dst dst
                             i 0]
                            (core/If (lib/< i n)
                                     (core/Recur
                                      (lib/conj dst i)
                                      (lib/inc i))
                                     dst)))

(deftest pop-coll-test
  (is (= [0 1 2] (populate-collection [] 3)))
  (is #{0 1 2} (populate-collection #{} 3)))


(java/typed-defn add-with-fancy-type-sig [{:a ::gtype/double
                                           :b ::gtype/double} x]
                 (lib/+ (:a x)
                        (:b x)))

(deftest fancy-type-signature-test
  (is (= 119.0 (add-with-fancy-type-sig {:a 112.0
                                         :b 7.0}))))


(deftest generalized-functions
  (is (= 5 (lib/+ 3 2))))

(deftest ordinary-array-ops
  (is (= (lib/aget (int-array [9 7 11]) 1)
         7))
  (let [arr (int-array [9 7 11])]
    (lib/aset arr 1 119)
    (is (= 119 (aget arr 1)))
    (is (= 3 (lib/alength arr)))))

(deftest ordinary-collection-fns
  (is (= [1 2 3] (lib/conj [1 2] 3)))
  (is (= '([:a 3]) (lib/seq {:a 3})))
  (is (lib/empty? []))
  (is (not (lib/empty? [1 2 3])))
  (is (= 9 (lib/first '(9 1 3))))
  (is (= '(1 3) (lib/rest '(9 1 3))))
  (is (= 3 (lib/count (int-array [1 4 9]))))
  (is (= 2 (lib/count {:a 9
                       :b 20}))))

(deftest ordinary-nil-test
  (is (lib/nil? nil))
  (is (not (lib/nil? 9))))

(deftest ordinary-numeric
  (is (= 7 (lib/+ 3 4)))
  (is (= 3 (lib/+ 3)))
  (is (= -1 (lib/- 3 4)))
  (is (= -3 (lib/- 3)))
  (is (= 0 (lib// 3 4)))
  (is (= -3 (lib// -13 4)))
  (is (= 0 (lib// 4)))
  (is (= 12 (lib/* 3 4)))
  (is (= 1 (lib/quot 7 4)))
  (is (= 3 (lib/rem 7 4))))

(deftest ordinary-bit-ops
  (doseq [[x y] [[9 4] [3 15] [7324 234]]]
    (is (= (bit-not x)
           (lib/bit-not x)))
    (is (= (bit-shift-left x y)
           (lib/bit-shift-left x y)))
    (is (= (bit-shift-right x y)
           (lib/bit-shift-right x y)))    
    (is (= (unsigned-bit-shift-right x y)
           (lib/unsigned-bit-shift-right x y)))
    (is (= (bit-flip x y)
           (lib/bit-flip x y)))
    (is (= (bit-and x y)
           (lib/bit-and x y)))
    (is (= (bit-or x y)
           (lib/bit-or x y)))))

(deftest ordinary-comparison-ops
  (doseq [[x y] [[9 4] [3 15] [7324 234] [3 3] [-4 -4]]]
    (is (= (<= x y)
           (lib/<= x y)))
    (is (= (>= x y)
           (lib/>= x y)))
    (is (= (< x y)
           (lib/< x y)))
    (is (= (> x y)
           (lib/> x y)))
    (is (= (not= x y)
           (lib/!= x y)))
    (is (= (= x y)
           (lib/== x y)))))

(deftest ordinary-not
  (is (false? (lib/not true)))
  (is (true? (lib/not false))))

(deftest ordinary-if-and-and
  (is (= 3 (core/If true 3 4))))

(deftest ordinary-equality
  (is (lib/= {:a 3}
             {:a 3}))
  (is (not (lib/= {:a 3}
                  {:a 4}))))

(java/typed-defn map-eq [Double/TYPE x
                         Double/TYPE y]
                 ;(core/set-flag! :disp)
                 (lib/= {:a (lib/wrap 3.0)
                         :b (lib/wrap 4.0)}
                        {:a x
                         :b y}))

(deftest small-eq-test
  (is (map-eq 3.0 4.0))
  (is (not (map-eq 3.0 4.1)))
  (is (not (map-eq 3.1 4.0)))
  (is (not (map-eq 3.1 4.1))))


(ebmd/def-arg-spec ::my-num-type {:pred (fn [x]
                                          (and (map? x)
                                               (contains?
                                                x :my-number)))
                                  :pos [{:my-number 3}]
                                  :neg [{:not-mine 9}
                                        :a
                                        13]})

(ebmd/def-poly lib/sqrt [::my-num-type x]
  {:my-number
   (lib/sqrt (:my-number x))})

(java/typed-defn my-sqrt-num [{:my-number Double/TYPE} x]
                 (lib/sqrt x))

(deftest polymorphic-num
  (is (= {:my-number 3.0}
         (my-sqrt-num {:my-number 9.0})))
  (is (= 3.0 (lib/sqrt 9.0))))



(comment
  
  (java/typed-defn graphviz-square [Double/TYPE x]
                   (java/add-build-callback
                    (graphviz/callback {:lower 5
                                        :upper 9
                                        :disp? true
                                        :fontname
                                        "Manjari"
                                        :out-pdf "../pres-clojured/latex/images/squaregraph.pdf"}))
                   (lib/* x x))

  )

(java/typed-defn get-nth-char [String s
                               Integer/TYPE i]
                 (lib/nth s i))

(deftest nth-char-test
  (is (= (get-nth-char "abcdefg" 3)
         \d)))

(java/typed-defn str-len [String s]
                 (lib/count s))


(deftest nth-char-test
  (is (= 7 (str-len "abcdefg"))))

(java/typed-defn starts-with-x [String s]
                 (lib/= \x (lib/nth s (int 0))))

(deftest starts-with-x-test
  (is (starts-with-x "xasdf"))
  (is (not (starts-with-x "mjao"))))

(java/typed-defn precedes [Character/TYPE a
                           Character/TYPE b]
                 (lib/< a b))

(deftest char-precedes
  (is (precedes \a \b))
  (is (not (precedes \c \b))))

(java/typed-defn check-is-magical-number [Long/TYPE input]
                 (lib/check (lib/= input 119) "It must be magical!!!")
                 (lib/quot input 7))

(deftest check-test
  (is (= 17 (check-is-magical-number 119)))
  (is (thrown? Exception (check-is-magical-number 118))))


(deftest min-max-test
  (is (= 2 (lib/min 9 2 3)))
  (is (= 9 (lib/max 9 2 3))))
