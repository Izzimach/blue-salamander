(ns blue-salamander.graphics
  (:require [cljsjs.react]
            [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [blue-salamander.r3elements :as r3 :include-macros]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))

(def origin (vec3 0 0 0))
(def unity (vec3 1 1 1))

;;
;; assets
;;

(def boxgeometry (js/THREE.BoxGeometry. 1 1 1))

(def cupcaketexture (js/THREE.ImageUtils.loadTexture "assets/cupCake.png"))
(def smiletexture (js/THREE.ImageUtils.loadTexture "assets/smileImage.png"))
(def lollitexture (js/THREE.ImageUtils.loadTexture "assets/lollipopGreen.png"))
(def lavarocktexture (js/THREE.ImageUtils.loadTexture "assets/red-rocks.png"))

(def cupcakematerial (js/THREE.MeshBasicMaterial. #js {:map cupcaketexture}))
(def smilematerial (js/THREE.MeshBasicMaterial. #js {:map smiletexture}))
(def lollimaterial (js/THREE.MeshBasicMaterial. #js {:map lollitexture}))
(def lavarockmaterial (js/THREE.MeshBasicMaterial. #js {:map lavarocktexture}))
(def block-materials
  {:smile smilematerial
   :cupcake cupcakematerial
   :lollipop lollimaterial
   :lavarock lavarockmaterial})

;;
;; om/next plumbing
;;

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



(defn mount-graphics [app-state]
  (let [reconciler (om/reconciler {:state app-state
                                   :parser game-parser
                                   :root-render js/ReactTHREE.render
                                   :root-unmount js/ReactTHREE.unmountComponentAtNode})]
    (om/add-root! reconciler
                  GameScreen
                  (gdom/getElement "app"))))

