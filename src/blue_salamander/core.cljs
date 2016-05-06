(ns blue-salamander.core
  (:require-macros [cljs.core.async.macros :as async-macros :refer [go]])
  (:require [blue-salamander.graphics :as gfx]
            [blue-salamander.keypresses :as k]
            [blue-salamander.collision :as coll]
            [blue-salamander.player-movement :as movement]
            [blue-salamander.blockmaze :as maze]
            [blue-salamander.orbs :as orbs]
            [cljs.core.async :as async :refer [chan >! <!]]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;;
;; define your app data so that it doesn't get over-written on reload
;;


(defonce app-state (atom {:player/position (vec3 3 0 0)
                          :player/rotation (js/THREE.Euler. 0 0 0)
                          :screen/width 600
                          :screen/height 400
                          :count 1
                          :blockdata []
                          :orbs []}))


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

(defn move-player
  [state dt]
  (let [playerpos (:player/position state)
        playerrot (:player/rotation state)
        boxes (:blockdata state)
        [dx dy] (player-input)
        desired-position (g/+ playerpos (vec3 0 -0.1 0) (g/*  (vec3 dx 0 dy) dt))
        player-radius 0.70
        corrected-sphere (movement/collide-sphere-with-boxes playerpos desired-position player-radius boxes)
        new-position (:center corrected-sphere)
        new-rotation (if (and (= 0 dx) (= 0 dy)) ;; if not moving keep previous rotation
                             playerrot
                             (js/THREE.Euler. 0 (- Math.PI (js/Math.atan2 dy dx)) 0))]
    (assoc state
           :player/position new-position
           :player/rotation new-rotation)))

(defn game-tick-fn
  [state newtime]
  (-> state
      (move-player 0.02)
      (orbs/update-orbs)))


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

(defn init-level [state]
  (let [maze-size 5
        blocksize 3
        mazedata  (maze/gen-maze maze-size)
        mazeblocks (maze/blocks-for-maze mazedata maze-size blocksize)
        placementspots (maze/placement-spots-seq mazedata maze-size blocksize 5)]
    (-> state
        (assoc :blockdata mazeblocks)
        ;; place player at the first placement spot
        (assoc :player/position (first placementspots))
        (assoc :player/rotation (js/THREE.Euler. 0 0 0))
        ;; place orbs at the 5 following spots
        (orbs/gen-orbs (take 5 (rest placementspots))))))

(defonce initial-load? true)

(if initial-load?
  (do
    (gfx/mount-graphics app-state)
    (k/start-keypress-tracking)
    ;; loaders returns channels. Once assets are loaded they are sent over the channel.
    (let [mesh-chan (gfx/load-meshes ["assets/salamander.json"])
          tex-chan (gfx/load-textures gfx/texture-assets)]
      ;; once assets are loaded, put them into the app-state
      (go (swap! app-state assoc :assets/textures (<! tex-chan))
          (swap! app-state assoc :assets/meshes (<! mesh-chan))
          ;; don't start the ticks until assets are loaded
          (swap! app-state init-level)
          (start-ticking)))
    (set! initial-load? false)))



(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;;(swap! app-state update-in [:__figwheel_counter] inc)
  (gfx/mount-graphics app-state)
  #_(swap! app-state assoc :blockdata (maze/build-block-maze 10 1)))
