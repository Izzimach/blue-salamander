(ns blue-salamander.core
  (:require [blue-salamander.graphics :as gfx]
            [blue-salamander.keypresses :as k]
            [blue-salamander.collision :as coll]
            [blue-salamander.player-movement :as movement]
            [blue-salamander.blockmaze :as maze]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;;
;; define your app data so that it doesn't get over-written on reload
;;


(defonce app-state (atom {:player/position (vec3 3 0 0)
                          :screen/width 600
                          :screen/height 400
                          :count 1
                          :blockdata (maze/build-block-maze 5 3)}))


(defn player-input []
  (let [left (k/is-key-pressed? :left-arrow)
        right (k/is-key-pressed? :right-arrow)
        up (k/is-key-pressed? :up-arrow)
        down (k/is-key-pressed? :down-arrow)
        dx (cond
             left 1
             right -1
             :else 0)
        dy (cond
             up 1
             down -1
             :else 0)]
    [(+ dx dy) (- dy dx)]))

(defn game-tick-fn
  [state newtime]
  (let [playerpos (:player/position state)
        boxes (:blockdata state)
        [dx dy] (player-input)
        desired-position (g/+ playerpos (vec3 0 -0.1 0) (g/*  (vec3 dx 0 dy) 0.02))
        player-radius 0.55
        corrected-sphere (movement/collide-sphere-with-boxes playerpos desired-position player-radius boxes)
        new-position (:center corrected-sphere)]
    (assoc state :player/position new-position)))


(def tickID :off)

(defn tick-fn [highrestime]
  (do 
    #_(js/console.log "tick")
    (set! tickID (js/requestAnimationFrame tick-fn))
    (swap! app-state game-tick-fn highrestime)))

(defn start-ticking []
  (if (= tickID :off)
    (set! tickID (js/requestAnimationFrame tick-fn))))

(defn stop-ticking []
  (if-not (= tickID :off)
    (do
      (js/cancelAnimationFrame tickID)
      (set! tickID :off))))

(defonce initial-load? true)

(if initial-load?
  (do
    (gfx/mount-graphics app-state)
    (k/start-keypress-tracking)
    (start-ticking)
    (set! initial-load? false)))



(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;;(swap! app-state update-in [:__figwheel_counter] inc)
  #_(swap! app-state assoc :blockdata (maze/build-block-maze 10 1))
  )
