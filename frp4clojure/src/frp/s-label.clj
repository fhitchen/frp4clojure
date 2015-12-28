(ns frp.s-label
  (:require [frp.core :refer :all])
  (:require [clojure.string :as s])
  (:import [nz.sodium Cell Listener Stream StreamSink Operational Transaction Unit])
  (:import [javax.swing JButton JFrame JLabel JTextField SwingUtilities])
  (:import [java.awt FlowLayout])
  (:import [java.awt.event ActionListener])
  (:import [javax.swing.event DocumentEvent DocumentListener]))

(defn update-text [source sink]
  (let [text (.getText source)]
    (SwingUtilities/invokeLater (fn []
                                  (if (not= text nil)
                                    (do
                                      (.send sink text)))))))
                                          

(defn listen [source sink]
  (.. source getDocument
      (addDocumentListener
       (proxy [DocumentListener] []
         (insertUpdate[e] (update-text source sink))
         (removeUpdate [e] (update-text source sink))
         (changedUpdate [e] (update-text source sink))))))

(defn s-button
  ([label] (s-button label (Cell. true)))
  ([label enabled]
   (let [s-clicked (StreamSink.)
         l (atom 0)
         button (proxy [JButton] [label]
                  (removeNotify []
                    (.unlisten @l)
                    (proxy-super removeNotify)))
         act (proxy [ActionListener] []
               (actionPerformed [event] #( [] (println "in actionPerformed")
                                          (.send s-clicked Unit/UNIT))))]
     (.addActionListener button act)
     (Transaction/post (fn []
                         (.setEnabled button (.sample enabled))))
     (reset! l (.listen (Operational/updates enabled) (handler [ena]
                                                               (if (SwingUtilities/isEventDispatchThread)
                                                                 (.setEnabled button ena)
                                                                 (SwingUtilities/invokeLater (fn []
                                                                                              (.setEnabled button ena)))))))
     {:jbutton button :s-clicked s-clicked})))
     
         

(defn s-label
  [c-text]
  (let [l (atom false)
        label (proxy [JLabel] [""]
                (removeNotify []
                  (.unlisten @l)
                  (proxy-super removeNofify)))]
    (reset! l (.listen (Operational/updates c-text)
                       (handler [t] (if (SwingUtilities/isEventDispatchThread)
                                      (.setText label t)
                                      (SwingUtilities/invokeLater (fn [t] (.setText label ) t))))))
    (Transaction/post (fn [] (SwingUtilities/invokeLater (fn []
                                                         (.setText label (.sample c-text))))))
    label))

(defn s-text-field
  ([init-text] (s-text-field (Stream.) init-text 15))
  ;([^String init-text ^Long width] (s-text-field (Stream.) init-text width))
  ([^Stream s-text ^String init-text] (s-text-field s-text init-text 15))
  ([^Stream s-text ^String init-text ^long width] (s-text-field s-text init-text width (Cell. true)))
  ([^Stream s-text ^String init-text ^long width ^Cell enabled]
   (let [s-decrement (StreamSink.)
         allow (.map
                (.accum
                 (.orElse
                  (.map s-text (apply1 [u] 1))
                  s-decrement)
                 0
                 (apply2 [d b] (+ b d)))
                (apply1 [b] (== b 0)))
         l (atom 0)
         s-user-changes (StreamSink.)
         text (.hold (.orElse (.gate s-user-changes allow) s-text) init-text)
         s-text-field (proxy [JTextField] [init-text width]
                        (removeNotify []
                          (.unlisten @l)
                          (proxy-super removeNotify)))
         s-text (Stream.)
         dl (listen s-text-field s-user-changes)]
     (println (str "dl: " dl))
     (Transaction/post (fn [] (.setEnabled s-text-field (.sample enabled))))
     (reset! l (.append (.listen s-text (handler [text] (SwingUtilities/invokeLater
                                                         (fn []
                                                           (println "in remove document listener")
                                                           (.removeDocumentListener (.getDocument dl))
                                                           (.setText s-text-field text)
                                                           (.addDocumentListener (.getDocument dl))
                                                           (.send s-decrement -1)))))
                        (.listen (Operational/updates enabled)
                                 (handler [ena] (if (SwingUtilities/isEventDispatchThread)
                                                  (do
                                                    (println "dispatch thread")
                                                    (.setEnabled s-text-field ena))
                                                  (do
                                                    (println "dispatch else")
                                                    (SwingUtilities/invokeLater (fn []
                                                                                (.setEnabled s-text-field ena)))))))))
     {:jtext s-text-field :cell text })))
  

  
(let [frame (JFrame. "label")
      msg (s-text-field "Hello")
      label (s-label (:cell msg))]
  (doto frame
    (.setLayout (FlowLayout.))
    ;(.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.add (:jtext msg))
    (.add label)
    (.setSize 400 160)
    (.setVisible true)))

(let [frame (JFrame. "reverse")
      msg (s-text-field "Hello")
      reversed (.map (:cell msg) (apply1 [t]
                                   (s/reverse t)))
      label (s-label reversed)]
  (doto frame
    (.setLayout (FlowLayout.))
    ;(.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.add (:jtext msg))
    (.add label)
    (.setSize 400 160)
    (.setVisible true)))

(let [frame (JFrame. "gamechat")
      onegai (s-button "Onegai shimasu")
      thanks (s-button "Thank you")
      s-onegai (.map (:s-clicked onegai) (apply1 [u] (str "Onegai shimasu")))
      s-thanks (.map (:s-clicked thanks) (apply1 [u] (str "Thank you")))
      foo (println (class s-thanks))
      s-canned (.orElse s-onegai s-thanks)
      text (s-text-field s-canned "")]
  (println (class (:cell text)))
  (doto frame
    (.setLayout (FlowLayout.))
    ;(.setDefaultCloseOperation JFrame/EXIT_ON_CLOSE)
    (.add (:jtext text))
    (.add (:jbutton onegai))
    (.add (:jbutton thanks))
    (.setSize 400 160)
    (.setVisible true)))




