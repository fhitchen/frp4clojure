(ns frp.core
  (:import [nz.sodium])
  (:import [nz.sodium Operational Stream StreamSink StreamLoop Cell CellSink Transaction Node])
  (:import [java.util ArrayList Optional])
  (:gen-class))


(set! *warn-on-reflection* true)

(defmacro apply0
  [& body]
  `(proxy [nz.sodium.Lambda0] []
     (apply []
       ~@body)))

(defmacro apply-0
  [& body]
  (let [targs '[this] ]
    `(reify nz.sodium.Lambda0
       (apply ~targs
         ~@body))))

(defmacro apply-0
  [& body]
  `(proxy [nz.sodium.Lambda0] []
     (apply []
       ~@body)))

(defmacro apply1
  [args & body]
  (let [targs (into '[this] args)]
  `(reify nz.sodium.Lambda1
     (apply ~targs
       ~@body))))

(defmacro apply2
  [args & body]
  `(proxy [nz.sodium.Lambda2] []
     (apply ~args
       ~@body)))

(defmacro handler
  [args & body]
  (let [targs (into '[this] args)]
  `(reify nz.sodium.Handler
     (^void run ~targs
       ~@body))))

(defmacro run
  [args & body]
  (let [targs (into '[this] args)]
  `(reify java.lang.Runnable
     (^void run ~targs
       ~@body))))


(defmacro transaction-handler
  [args & body]
  (let [targs (into '[this] args)]
  `(reify nz.sodium.TransactionHandler
     (run ~targs
       ~@body))))
                                                                           
(defmacro oo
  [a]
  `(Optional/of ~a))

(defmacro oe
  []
  `(Optional/empty))


(def ea (StreamSink.))

(def ec (Transaction/run (apply0 (let [eb (StreamLoop.)
                                            ec_ (.merge (.map ea (apply1 [x] (mod x 10)))
                                                        eb
                                                         (apply2 [x y] (+ x y)))
                                            eb_out (.filter (.map ea (apply1 [x] (/ x 10)))
                                                            (apply1 [x] (not= x 0)))]
                                   (.loop eb eb_out)
          ec_))))

(pr (class ec))

(def out (ArrayList.))
(def l (.listen ec (handler [x] (.add out x))))
   (pr l)


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
