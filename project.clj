(defproject blue-salamander "0.1.0-SNAPSHOT"
  :description "Exploration/maze game"
  :url "http://example.com/FIXME"
  :license {:name "MIT License"
            :url "https://opensource.org/licenses/MIT"}

  :min-lein-version "2.6.1"
  
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [org.clojure/clojurescript "1.7.228"]

                 ;; we're going to use our own react library (react-three)
                 ;; in place of the default React lib. So we have to make
                 ;; sure om doesn't pull in the default lib
                 [org.omcljs/om "1.0.0-alpha24" :exclusions [cljsjs/react]]
                 ;; here's our custom react lib that target threejs
                 [org.clojars.haussman/react-three "0.9.2"]

                 [thi.ng/geom "0.0.1062"]
                 [thi.ng/geom-core "0.0.908"]
                 
                 [org.clojure/core.async "0.2.374"
                  :exclusions [org.clojure/tools.reader]]]
  
  :plugins [[lein-figwheel "0.5.2"]
            [lein-cljsbuild "1.1.3" :exclusions [[org.clojure/clojure]]]]

  :source-paths ["src"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src"]

                ;; If no code is to be run, set :figwheel true for continued automagical reloading
                :figwheel {:on-jsload "blue-salamander.core/on-js-reload"}

                :compiler {:main blue-salamander.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/blue_salamander.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}
               ;; This next build is an compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src"]
                :compiler {:output-to "resources/public/js/compiled/blue_salamander.js"
                           :main blue-salamander.core
                           :optimizations :advanced
                           :pretty-print false}}]}

  :figwheel {;; :http-server-root "public" ;; default and assumes "resources"
             ;; :server-port 3449 ;; default
             ;; :server-ip "127.0.0.1"

             :css-dirs ["resources/public/css"] ;; watch and update CSS

             ;; Start an nREPL server into the running figwheel process
             ;; :nrepl-port 7888

             ;; Server Ring Handler (optional)
             ;; if you want to embed a ring handler into the figwheel http-kit
             ;; server, this is for simple ring servers, if this
             ;; doesn't work for you just run your own server :)
             ;; :ring-handler hello_world.server/handler

             ;; To be able to open files in your editor from the heads up display
             ;; you will need to put a script on your path.
             ;; that script will have to take a file path and a line number
             ;; ie. in  ~/bin/myfile-opener
             ;; #! /bin/sh
             ;; emacsclient -n +$2 $1
             ;;
             ;; :open-file-command "myfile-opener"

             ;; if you want to disable the REPL
             ;; :repl false

             ;; to configure a different figwheel logfile path
             ;; :server-logfile "tmp/logs/figwheel-logfile.log"
             })
