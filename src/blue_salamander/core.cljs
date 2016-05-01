(ns blue-salamander.core
  (:require [cljsjs.react]
            [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [blue-salamander.r3elements :as r3 :include-macros]
            [blue-salamander.keypresses :as keypress]
            [blue-salamander.r3utils :as r3u :include-macros]
            [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;;
;; key-pressed is a atom containing a set of the
;; currently pressed key codes.
;;

(defonce keys-pressed (atom #{}))
(keypress/track-keypresses-in-atom keys-pressed)


;;
;; define your app data so that it doesn't get over-written on reload
;;

(defonce app-state (atom {:camerapos (vec3 400 200 400)
                          :player/position (vec3 0 0 0)
                          :screen/width 600
                          :screen/height 400
                          :count 1}))

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
(def cupcaketexture (js/THREE.ImageUtils.loadTexture "assets/lollipopGreen.png"))
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
          (let [{width :screen/width height :screen/height playerpos :player/position} (om/props this)
                aspect (/ width height)
                lookat playerpos
                camerapos  (g/+ (vec3 1 1 1) (vec3 400 200 200))
                curprops (assoc playercamera-defaultprops
                                :aspect aspect
                                :position (r3/vec3->Vector3 camerapos)
                                :lookat (r3/vec3->Vector3 lookat))]
            (r3/perspectivecamera curprops))))
(def playercamera (om/factory PlayerCamera {:keyfn :key}))

(defui GameScreen
  static om/IQuery
  (query [this]
         [:screen/width :screen/height :player/position])
  Object
  (render [this]
          (let [props (om/props this)
                {width :screen/width height :screen/height} props
                rendererprops {:width width :height height}
                sceneprops (assoc rendererprops :camera "playercamera")]
            (r3/renderer rendererprops
                         (r3/scene sceneprops
                                   [(playercamera props)
                                    (r3/mesh {:key 'box'
                                              :position r3/Origin
                                              :geometry boxgeometry
                                              :material cupcakematerial})])))))



(def reconciler
  (om/reconciler {:state app-state
                  :parser game-parser
                  :root-render js/ReactTHREE.render
                  :root-unmount js/ReactTHREE.unmountComponentAtNode}))

(om/add-root! reconciler
              GameScreen
              (gdom/getElement "app"))


(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  (swap! app-state update-in [:__figwheel_counter] inc)
  )
