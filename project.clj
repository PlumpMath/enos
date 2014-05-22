(defproject enos "0.1.3-SNAPSHOT"
  
  :description "Clojure core.async utilities"
  :url "http://github.com/grammati/enos"
  :license "MIT License"

  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]]

  :profiles
  {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]
                        [org.clojure/tools.trace "0.7.6"]]
         :source-paths ["src" "dev"]
         }}
  )
