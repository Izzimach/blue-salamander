(ns blue-salamander.core
  (:require [cljsjs.react]
            [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [blue-salamander.r3elements :as r3 :include-macros]
            [blue-salamander.keypresses :as k]
            [blue-salamander.collision :as coll]
            [blue-salamander.player-movement :as movement]
            [blue-salamander.blockmaze :as maze]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;;
;; key-pressed is a atom containing a set of the
;; currently pressed key codes.
;;

;;
;; define your app data so that it doesn't get over-written on reload
;;

(def origin (vec3 0 0 0))
(def unity (vec3 1 1 1))


(defonce app-state (atom {:player/position (vec3 3 0 0)
                          :screen/width 600
                          :screen/height 400
                          :count 1
                          :blockdata (maze/build-block-maze 3 3)}))

(defn read-fn
  [{:keys [state] :as env} key params]
  (let [st @state]
    (if-let [[_ v] (find st key)]
      {:value v}
      {:value :not-found})))

(defn mutate-fn
  [{:keys [state] :as env} key params]
  (if (= 'increment key)
    {:value {:keys [:count]}
     :action #(swap! state update-in [:count] inc)}
    {:value :not-found}))

(def game-parser (om.next/parser {:read read-fn :mutate mutate-fn}))

(def boxgeometry (js/THREE.BoxGeometry. 1 1 1))

(def cupcaketexture (js/THREE.ImageUtils.loadTexture "assets/cupCake.png"))
(def smiletexture (js/THREE.ImageUtils.loadTexture "assets/smileImage.png"))
(def lollitexture (js/THREE.ImageUtils.loadTexture "assets/lollipopGreen.png"))

(def cupcakematerial (js/THREE.MeshBasicMaterial. #js {:map cupcaketexture}))
(def smilematerial (js/THREE.MeshBasicMaterial. #js {:map smiletexture}))
(def lollimaterial (js/THREE.MeshBasicMaterial. #js {:map lollitexture}))
(def block-materials
  {:smile smilematerial
   :cupcake cupcakematerial
   :lollipop lollimaterial})



(def playercamera-defaultprops {:name "playercamera"
                                :key 'camera'
                                :fov 75
                                :near 0.1
                                :far 500
                                })
(defui PlayerCamera
  static om/IQuery
  (query [this]
         [:screen/width :screen/height :player/position])
  Object
  (render [this]
          (let [{width :screen/width height :screen/height [px py pz] :player/position :as props} (om/props this)
                ;; om.next destructures thi.ng vec3's for some reason
                playerpos (vec3 px py pz)
                aspect (/ width height)
                lookat playerpos
                camerapos (g/+ playerpos (vec3 -3 8 -3))
                curprops (assoc playercamera-defaultprops
                                :aspect aspect
                                :position (r3/vec3->Vector3 camerapos)
                                :lookat (r3/vec3->Vector3 lookat))]
            (r3/perspectivecamera curprops))))
(def playercamera (om/factory PlayerCamera))

(defui PlayerCharacter
  static om/IQuery
  (query [this]
         [:player/position])
  Object
  (render [this]
          (let [{playerpos :player/position :as props} (om/props this)]
            (r3/mesh {:position (r3/vec3->Vector3  playerpos)
                      :geometry boxgeometry
                      :material smilematerial}))))
(def playercharacter (om/factory PlayerCharacter))

(defn blockdata->meshprops [block]
  (let [{{:keys [center size]} :geometry materialname :material} block
        [sx sy sz] size
        scale (js/THREE.Vector3. sx sy sz)]
    {:position (r3/vec3->Vector3 center)
     :geometry boxgeometry
     :material (get block-materials materialname)
     :scale scale}))

(defui LavaLand
  Object
  (render [this]
          (let [{:keys [blockdata]} (om/props this)
                datatomesh (comp r3/mesh blockdata->meshprops)]
            (r3/object3d {}
                         (mapv datatomesh blockdata)))))
(def lava-land (om/factory LavaLand))

(defui GameScreen
  static om/IQuery

  (query [this]
         [:screen/width :screen/height :player/position :blockdata])
  Object
  (render [this]
          (let [props (om/props this)
                {width :screen/width height :screen/height playerpos :player/position} props
                rendererprops {:width width :height height}
                sceneprops (assoc rendererprops :camera "playercamera")]
            (r3/renderer rendererprops
                         (r3/scene sceneprops
                                   [(playercamera (assoc props :key 'playercamera'))
                                    (playercharacter {:key 'playercharacter'
                                                      :player/position playerpos})
                                    (lava-land props)])))))



(def reconciler
  (om/reconciler {:state app-state
                  :parser game-parser
                  :root-render js/ReactTHREE.render
                  :root-unmount js/ReactTHREE.unmountComponentAtNode}))

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
(def queued-swap-fn nil)

(defn tick-fn [highrestime]
  (do 
    #_(js/console.log "tick")
    (set! tickID (js/requestAnimationFrame tick-fn))
    (if queued-swap-fn
      (do 
        (swap! app-state queued-swap-fn)
        (set! queued-swap-fn nil)))
    (swap! app-state game-tick-fn highrestime)))

(defn start-ticking []
  (if (= tickID :off)
    (set! tickID (js/requestAnimationFrame tick-fn))))

(defn stop-ticking []
  (if-not (= tickID :off)
    (do
      (js/cancelAnimationFrame tickID)
      (set! tickID :off))))

(defonce initial-load? false)

(om/add-root! reconciler
              GameScreen
              (gdom/getElement "app"))

(if-not initial-load?
  (do
    (k/start-keypress-tracking)
    (start-ticking)
    (set! initial-load? true)))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  ;;(swap! app-state update-in [:__figwheel_counter] inc)
  #_(swap! app-state assoc :blockdata (maze/build-block-maze 10 1))
  (set! queued-swap-fn #(assoc % :blockdata (maze/build-block-maze 3 3)))
  )
