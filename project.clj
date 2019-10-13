(defproject shadow-sente "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [org.clojure/clojurescript "1.10.520"
                  :exclusions [com.google.javascript/closure-compiler-unshaded
                               org.clojure/google-closure-library]]
                 [org.clojure/core.async "0.4.500"]
                 [clj-time "0.15.2"]
                 [thheller/shadow-cljs "2.8.62"]
                 [com.taoensso/sente "1.14.0-RC2"]
                 [http-kit "2.3.0"]
                 [hiccup "1.0.5"]
                 [reagent "0.8.1"]
                 [re-frame "0.10.9"]
                 [re-com "2.6.0"]
                 [compojure "1.6.1"]
                 [yogthos/config "1.1.5"]
                 [ring "1.7.1"]
                 [ring/ring-defaults "0.3.2"]]

  :plugins []

  :min-lein-version "2.5.3"

  :jvm-opts ["-Xmx1G"]

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]


  :aliases {"dev"  ["with-profile" "dev" "run" "-m" "shadow.cljs.devtools.cli" "watch" "app"]
            "prod" ["with-profile" "prod" "run" "-m" "shadow.cljs.devtools.cli" "release" "app"]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.10"]
                   [day8.re-frame/re-frame-10x "0.4.3"]
                   [day8.re-frame/tracing "0.5.3"]]}

   :prod { :dependencies [[day8.re-frame/tracing-stubs "0.5.3"]]}

   :uberjar {:source-paths ["env/prod/clj"]
             :dependencies [[day8.re-frame/tracing-stubs "0.5.3"]]
             :omit-source  true
             :main         shadow-sente.server
             :aot          [shadow-sente.server]
             :uberjar-name "shadow-sente.jar"
             :prep-tasks   ["compile" ["prod"]]}
   })
