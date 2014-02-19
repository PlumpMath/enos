(defproject enos "0.1.2"
  
  :description "Clojure core.async utilities"
  :url "http://github.com/grammati/enos"
  :license "MIT License"

  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]]

  :profiles
  {:dev {:dependencies [[org.clojure/tools.namespace "0.2.4"]
                        [org.clojure/tools.trace "0.7.6"]]
         :source-paths ["src" "dev"]
         }}
  )
