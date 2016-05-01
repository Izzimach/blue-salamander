(ns blue-salamander.core
  (:require [cljsjs.react]
            [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [blue-salamander.r3elements :as r3 :include-macros]
            [blue-salamander.keypresses :as k]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;;
;; key-pressed is a atom containing a set of the
;; currently pressed key codes.
;;

(defonce keys-pressed (atom #{}))
(k/track-keypresses-in-atom keys-pressed)


;;
;; define your app data so that it doesn't get over-written on reload
;;

(def origin (vec3 0 0 0))
(def unity (vec3 1 1 1))

(defonce app-state (atom {:player/position (vec3 300 0 0)
                          :screen/width 600
                          :screen/height 400
                          :count 1
                          :blockdata [
                                      {:position origin :size unity}
                                      {:position (vec3 0 0 100) :size unity}
                                      {:position (vec3 100 0 100) :size unity}
                                      {:position (vec3 0 0 200) :size unity}
                                      ]}))

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



(def boxgeometry (js/THREE.BoxGeometry. 100 100 100))
(def cupcaketexture (js/THREE.ImageUtils.loadTexture "assets/cupCake.png"))
(def cupcakematerial (js/THREE.MeshBasicMaterial. #js {:map cupcaketexture}))



(def playercamera-defaultprops {:name "playercamera"
                                :key 'camera'
                                :fov 75
                                :near 1
                                :far 5000
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
                camerapos (g/+ playerpos (vec3 -200 400 -200))
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
                      :material cupcakematerial}))))
(def playercharacter (om/factory PlayerCharacter))

(defn blockdata->meshprops [block]
  (let [{:keys [position size]} block
        [sx sy sz] size
        scale (js/THREE.Vector3 (/ sx 100) (/ sy 100) (/ sz 100))]
    {:position (r3/vec3->Vector3 position)
     :geometry boxgeometry
     :material cupcakematerial
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

(om/add-root! reconciler
              GameScreen
              (gdom/getElement "app"))

(defn player-input []
  (let [curkeys @keys-pressed
        left (contains? curkeys :left-arrow)
        right (contains? curkeys :right-arrow)
        up (contains? curkeys :up-arrow)
        down (contains? curkeys :down-arrow)
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
        [dx dy] (player-input)
        newposition (g/+ playerpos (vec3 dx 0 dy))]
    (assoc state :player/position newposition)))

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

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  (swap! app-state update-in [:__figwheel_counter] inc)
  )
