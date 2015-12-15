(defproject frp "0.1.0-SNAPSHOT"
  :description "Hacking FRP Sodium"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [local/sodium "1.0.0"]]
  :main ^:skip-aot frp.core
  :target-path "target/%s"
  :profiles {:uberjar {:aot :all}})
