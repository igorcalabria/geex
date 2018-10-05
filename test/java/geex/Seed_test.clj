(ns geex.Seed-test
  (:import [geex Seed DynamicSeed SeedParameters Mode])
  (:require [clojure.test :refer :all]
            [bluebell.utils.wip.java :as java :refer [set-field]]))

(deftest seed-test
  (let [p0 (doto (SeedParameters.)
             (java/set-field id 119)
             (java/set-field type Double/TYPE)
             (set-field mode Mode/Pure))
        p1 (doto (SeedParameters.)
             (java/set-field id 119)
             (java/set-field type Double/TYPE)
             (set-field mode Mode/Pure))


        s0 (DynamicSeed. p0)
        s1 (DynamicSeed. p1)

        q0 (DynamicSeed.
            (doto (SeedParameters.)
              (java/set-field id 120)
              (java/set-field type Double/TYPE)
              (set-field mode Mode/Pure)))
        r0 (DynamicSeed.
            (doto (SeedParameters.)
              (java/set-field id 119)
              (java/set-field type Integer/TYPE)
              (set-field mode Mode/Pure)))]
    (is (.equals s0 s1))
    (is (= s0 s1))
    (is (= s1 s0))
    (is (not= p0 q0))
    (is (not= p0 r0))
    (is (not= r0 q0))
    (is (not= s0 nil))
    (is (= {s0 2 r0 3}
           {s1 2 r0 3}))
    (is (not= {s0 3 r0 3}
              {s1 2 r0 3}))
    (is (= 1 (count (conj #{s0} s1))))
    (is (= 2 (count (conj #{s0} q0))))

    (do
      (.addDep (.deps s0) :kattskit r0)
      (.setData s0 :kattskit)
      (is (= :kattskit (.getData s0))))

    
    #_(is (= (DynamicSeed. p0)
           (DynamicSeed. p0)
           
           ))))
