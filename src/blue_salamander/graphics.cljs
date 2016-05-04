(ns blue-salamander.graphics
  (:require-macros [cljs.core.async.macros :as async-macros :refer [go]])
  (:require [cljsjs.react]
            [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [cljs.core.async :as async :refer [chan >! <!]]
            [blue-salamander.r3elements :as r3 :include-macros]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))

(def origin (vec3 0 0 0))
(def unity (vec3 1 1 1))

;;
;; assets
;;

(def boxgeometry (js/THREE.BoxGeometry. 1 1 1))


(defn- load-mesh [loader filepath]
  "Load the mesh through a mesh loader and put the result onto a channel. 
The channel is returned. Once the mesh is loaded, a map with keys 
{:filepath :geometry :materials} is put onto the channel and the channel is closed."
  (let [results-channel (chan 1)]
    (.load loader filepath (fn [geom materials]
                             (async/put! results-channel {:filepath filepath
                                                          :geometry geom
                                                          :materials materials})
                             (async/close! results-channel)))
    results-channel))


(defn load-meshes [meshlist]
  "Loads mesh data asynchronously. Pass in a vector of paths pointing to 
JSON files to load. Returns a channel that you can take from. Once all mesges are
loaded, the channel produces a single value containing a map keys as the file path
and values as [geometry materials]"
  (let [mesh-loader (js/THREE.JSONLoader.)]
    (.setTexturePath mesh-loader "assets/")
    (->> meshlist
         (map (fn [mesh-filepath] (load-mesh mesh-loader mesh-filepath)))
         (async/merge)
         (async/reduce
          (fn [a {:keys [filepath geometry materials]}] (assoc a filepath [geometry (js/THREE.MultiMaterial. materials)]))
          {}))))

(defn- load-texture [loader filepath]
  "Load the texture through a texture loader and put the result onto a channel.
The channel is returned. Once the texture is loaded, a map with keys
{:filepath :texture} is put onto the channel and the channel is closed."
  (let [results-channel (chan 1)]
    (.load loader filepath (fn [texture]
                             (async/put! results-channel {:filepath filepath
                                                          :texture texture})
                             (async/close! results-channel)))
    results-channel))

(defn load-textures [texturelist]
  "Load textures. Pass in a vector of paths pointing to textures to load.
Returns a channel that you can take from. Once all textures are loaded,
then channel produces a single value containing a map with keys as the file path
and values as the texture."
  (let [texture-loader (js/THREE.TextureLoader.)]
    (->> texturelist
         (map (fn [texture-filepath] (load-texture texture-loader texture-filepath)))
         (async/merge)
         (async/reduce (fn [a {:keys [filepath texture]}] (assoc a filepath texture)) {}))))

(def texture-assets ["assets/cupCake.png"
                     "assets/smileImage.png"
                     "assets/lollipopGreen.png"
                     "assets/red-rocks.png"])

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

(defn load-assets []
  (let [result-channel (chan 1)]
    result-channel))

(defn textures->materials [textures]
  (into {} (map (fn [filepath tex] [filepath (js.THREE.MeshBasicMaterial #js {:map tex})]) textures)))

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
                                :key "camera"
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
                ;; om.next destructures thi.ng vec3's for some reason - dammit
                playerpos (vec3 px py pz)
                aspect (/ width height)
                lookat playerpos
                camerapos (g/+ playerpos (vec3 -3 8 -3))
                curprops (assoc playercamera-defaultprops
                                :aspect aspect
                                :position (r3/vec3->Vector3 camerapos)
                                :lookat (r3/vec3->Vector3 lookat))]
            (r3/perspectivecamera curprops))))
(def playercamera (om/factory PlayerCamera {:keyfn :key}))

(def playermaterial (js/THREE.MeshPhongMaterial.))

(defui PlayerCharacter
  static om/IQuery
  (query [this]
         [:player/position :assets/meshes])
  Object
  (render [this]
          (let [{playerpos :player/position :as props} (om/props this)
                [geometry material] (get-in props [:assets/meshes "assets/salamander.json"])]
            (if (nil? geometry)
              (r3/object3d {:key "playermesh"})
              (r3/mesh {:key "playermesh"
                        :position (r3/vec3->Vector3  playerpos)
                        :geometry geometry
                        :material material
                        :scale 0.15})))))
(def playercharacter (om/factory PlayerCharacter {:keyfn :key}))

(defn blockdata->meshprops [react-key block]
  (let [{{:keys [center size]} :geometry materialname :material} block
        [sx sy sz] size
        scale (js/THREE.Vector3. sx sy sz)]
    {:key react-key
     :position (r3/vec3->Vector3 center)
     :geometry boxgeometry
     :material (get block-materials materialname)
     :scale scale}))

(defui LavaLand
  Object
  (render [this]
          (let [{:keys [blockdata]} (om/props this)
                datatomesh (fn [block index] (->> block
                                                  (blockdata->meshprops index)
                                                  r3/mesh))]
            (r3/object3d {:key "blocks"}
                         (mapv datatomesh blockdata (iterate inc 1))))))
(def lava-land (om/factory LavaLand {:keyfn :key}))

(defui SceneLighting
  Object
  (render [this]
          ;; use both a hemisphere and directional light
          (r3/object3d {:key "light_container"}
                       (r3/ambientlight {:key "overall_light" :color 0xffffff
                                            :groundColor 0xff8080
                                            :intensity 0.1})
                       [(r3/directionallight {:key "sun" :color 0xa0ffff :intensity 10})])))
(def lights (om/factory SceneLighting {:keyfn :key}))

(defui GameScreen
  static om/IQuery
  (query [this]
         [:screen/width :screen/height :player/position :blockdata :assets/meshes :assets/textures])
  Object
  (render [this]
          (let [props (om/props this)
                {width :screen/width
                 height :screen/height
                 playerpos :player/position
                 texture-assets :assets/textures
                 mesh-assets :assets/meshes} props
                rendererprops {:width width :height height}
                sceneprops (assoc rendererprops :camera "playercamera")]
            ;; until assets are loaded, just say "loading..."
            (if (or (nil? texture-assets)
                    (nil? mesh-assets))
              (dom/div "Loading...")
              ;; assets are all loaded
              (r3/renderer rendererprops
                           (r3/scene sceneprops
                                     [
                                      (playercamera (assoc props :key "playercamera"))
                                      (playercharacter {:key "playercharacter"
                                                        :player/position playerpos
                                                        :assets/meshes mesh-assets})
                                      (lava-land (assoc props :key "level"))
                                      (lights {:key "lights"})
                                      ]))))))


(defn mount-graphics [app-state]
  (let [reconciler (om/reconciler {:state app-state
                                   :parser game-parser
                                   :root-render js/ReactTHREE.render
                                   :root-unmount js/ReactTHREE.unmountComponentAtNode})]
    (om/add-root! reconciler
                  GameScreen
                  (gdom/getElement "app"))))


