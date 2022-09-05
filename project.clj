(defproject todo "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :min-lein-version "2.0.0"

  :dependencies [[org.clojure/clojure "1.11.1"]
                 [duct/core "0.8.0"]
                 [ring "1.9.5"]
                 [ring/ring-json "0.5.1"]
                 [compojure "1.6.3"]
                 [com.github.seancorfield/next.jdbc "1.2.796"]
                 [org.xerial/sqlite-jdbc "3.39.2.1"]]

  :plugins [[duct/lein-duct "0.12.3"]]
  :middleware [lein-duct.plugin/middleware]
  :prep-tasks ["javac" "compile" ["run" ":duct/compiler"]]

  :main ^:skip-aot todo.main
  :resource-paths ["resources" "target/resources"]

  :profiles
  {:dev          [:project/dev :profiles/dev]
   :repl         {:prep-tasks   ^:replace ["javac" "compile"]
                  :repl-options {:init-ns user}}
   :uberjar      {:aot :all}
   :profiles/dev {}
   :project/dev  {:source-paths   ["dev/src"]
                  :resource-paths ["dev/resources"]
                  :dependencies   [[integrant/repl "0.3.2"]
                                   [hawk "0.2.11"]
                                   [eftest "0.5.9"]]}})
