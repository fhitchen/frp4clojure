(ns frp.stream-test
  (:require [clojure.test :refer :all]
            [frp.core :refer :all])
  (:import [nz.sodium])
  (:import [nz.sodium Operational Stream StreamSink StreamLoop CellSink Cell Transaction Tuple2])
  (:import [java.util ArrayList]))

(set! *warn-on-reflection* true)

(deftest test-send-stream
  (testing "send stream"
    (let [e (StreamSink. )
          out (ArrayList. )
          l (.listen e (handler [x] (.add out x)))]
      (.send e 5)
      (.unlisten l)
      (is (= out [5]))
      (.send e 6)
      (is (= out [5])))))

(deftest test-map
  (testing "map"
    (let [e (StreamSink.)
          m (.map e (apply1 [x] (.toString x)))
          out (ArrayList.)
          l (.listen m (handler [x] (.add out x)))]
      (.send e 5)
      (.unlisten l)
      (is (= out ["5"])))))

(deftest test-merge-non-simultaneous
  (testing "merge non simultaneous"
    (let [e1 (StreamSink.)
          e2 (StreamSink.)
          out (ArrayList.)
          l (.listen (.orElse e2 e1) (handler [x] (.add out x)))]
      (.send e1 7)
      (.send e2 9)
      (.send e1 8)
      (.unlisten l)
      (is (= out [ 7 9 8])))))


(deftest test-merge-simultaneous
  (testing "merge simultaneous"
    (let [s1 (StreamSink. (apply2 [l r] r))
          s2 (StreamSink. (apply2 [l r] r))
          out (ArrayList.)
          l (.listen (.orElse s2 s1) (handler [x] (.add out x)))]
      (Transaction/runVoid #(do
                              (.send s1 7)
                              (.send s2 60)))
      (Transaction/runVoid #(do
                              (.send s1 9)))
      (Transaction/runVoid #(do
                              (.send s1 7)
                              (.send s2 60)
                              (.send s2 8)
                              (.send s2 90)))
      (Transaction/runVoid #(do
                              (.send s1 8)
                              (.send s2 90)
                              (.send s1 7)
                              (.send s1 60)))
      (Transaction/runVoid #(do
                              (.send s2 8)
                              (.send s1 7)
                              (.send s2 90)
                              (.send s1 60)))
      (.unlisten l)
      (is (= out [60 9 90 90 90])))))

(deftest test-coalesce
  (testing "coalesce"
    (let [s (StreamSink. (apply2 [a b] (+ a b)))
          out (ArrayList.)
          l (.listen s (handler [x] (.add out x)))]
      (Transaction/runVoid #(.send s 2))
      (Transaction/runVoid #(do
                              (.send s 8)
                              (.send s 40)))
      (.unlisten l)
      (is (= out [2 48])))))

(deftest test-filter
  (testing "filter"
    (let [e (StreamSink. (apply2 [a b] (+ a b)))
          out (ArrayList.)
          l (.listen (.filter e (apply1 [c] (Character/isUpperCase c))) (handler [x] (.add out x)))]
      (.send e \H)
      (.send e \o)
      (.send e \I)
      (.unlisten l)
      (is (= out [\H \I])))))

(deftest test-filter-optional
  (testing "filter optional"
    (let [e (StreamSink.)
          out (ArrayList.)
          l (.listen (Stream/filterOptional e) (handler [s] (.add out s)))]
      (.send e (oo "tomato"))
      (.send e (oe))
      (.send e (oo "peach"))
      (.unlisten l)
      (is (= out ["tomato" "peach"])))))


(deftest test-loop-stream
  (testing "loop stream"
    (let [ea (StreamSink.)
          ec (Transaction/run (apply0 (let [eb (StreamLoop.)
                                            ec_ (.merge (.map ea (apply1 [x] (mod x 10)))
                                                        eb
                                                        (apply2 [x y] (+ x y)))
                                            eb_out (.filter (.map ea (apply1 [x] (int (/ x 10))))
                                                            (apply1 [x] (not= x 0)))]
                                        (.loop eb eb_out)
                                        ec_)))
          out (ArrayList.)
          l (.listen ec (handler [x] (.add out x)))]
      (.send ea 2)
      (.send ea 52)
      (.unlisten l)
      (is (= out [2 7])))))

(deftest test-gate
  (testing "gate"
    (let [ec (StreamSink.)
          epred (CellSink. true)
          out (ArrayList.)
          l (.listen (.gate ec epred) (handler [x] (.add out x)))]
      (.send ec \H)
      (.send epred false)
      (.send ec \O)
      (.send epred true)
      (.send ec \I)
      (.unlisten l)
      (is (= out [\H \I])))))

(deftest test-collect
  (testing "collect"
    (let [ea (StreamSink.)
          out (ArrayList.)
          sum (.collect ea 0 (apply2 [a s] (Tuple2. (+ a s 100) (+ a s))))
          l (.listen sum (handler [x] (.add out x)))]
      (.send ea 5)
      (.send ea 7)
      (.send ea 1)
      (.send ea 2)
      (.send ea 3)
      (.unlisten l)
      (is (= out [105 112 113 115 118])))))

(deftest test-accum
  (testing "accum"
    (let [ea (StreamSink.)
          out (ArrayList.)
          sum (.accum ea 100 (apply2 [a s] (+ a s)))
          l (.listen sum (handler [x] (.add out x)))]
      (.send ea 5)
      (.send ea 7)
      (.send ea 1)
      (.send ea 2)
      (.send ea 3)
      (.unlisten l)
      (is (= out [100 105 112 113 115 118])))))

(deftest test-once
  (testing "once"
    (let [e (StreamSink.)
          out (ArrayList.)
          l (.listen (.once e) (handler [x] (.add out x)))]
      (.send e \A)
      (.send e \B)
      (.send e \C)
      (.unlisten l)
      (is (= out [\A])))))

(deftest test-defer
  (testing "defer"
    (let [e (StreamSink.)
          b (.hold e \space)
          out (ArrayList.)
          l (.listen (.snapshot (Operational/defer e) b) (handler [x] (.add out x)))]
      (.send e \C)
      (.send e \B)
      (.send e \A)
      (.unlisten l)
      (is (= out [\C \B \A])))))




