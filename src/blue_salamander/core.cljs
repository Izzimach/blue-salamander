(ns blue-salamander.core
  (:require [cljsjs.react]
            [goog.dom :as gdom]
            [om.next :as om :refer-macros [defui]]
            [om.dom :as dom]
            [blue-salamander.r3elements :as r3]))

(enable-console-print!)

(println "Edits to this text should show up in your developer console.")

;; define your app data so that it doesn't get over-written on reload

(defonce app-state (atom {:text "Hello world!"}))
(def boxgeometry (js/THREE.BoxGeometry. 100 100 100))
(def cupcaketexture (js/THREE.ImageUtils.loadTexture "assets/lollipopGreen.png"))
(def cupcakematerial (js/THREE.MeshBasicMaterial. #js {:map cupcaketexture}))
(def maincameraelement (r3/perspectivecamera {:name "maincamera"
                                              :key 'camera'
                                              :fov 75
                                              :aspect 1.33
                                              :near 1
                                              :far 5000
                                              :position (js/THREE.Vector3. 400 200 400)
                                              :lookat (js/THREE.Vector3. 0 0 0)}))





(defui ThreeScene
  Object
  (render [this]
          (r3/renderer {:width 600 :height 400 :background 0x8080a0}
                       (r3/scene {:width 600 :height 400 :camera "maincamera"}
                                 [maincameraelement
                                  (r3/mesh {:key 'box'
                                            :position (js/THREE.Vector3. 0 0 0)
                                            :geometry boxgeometry
                                            :material cupcakematerial})]))))

(defui Counter
  Object
  (render [this]
          (let [{:keys [count]} (om/props this)]
            (dom/div nil
                     (dom/span nil (str "Count: " count))
                     (dom/button #js {:onClick
                                      (fn [e]
                                        (swap! app-state update-in [:count] inc))}
                                 "Click me")))))

(def reconciler
  (om/reconciler {:state app-state
                  :root-render js/ReactTHREE.render
                  :root-unmount js/ReactTHREE.unmountComponentAtNode}))

(om/add-root! reconciler
              ThreeScene
              (gdom/getElement "app"))

(defui HelloWorld
  Object
  (render [this]
          (dom/div nil "Hello, world!")))

(def hello (om/factory HelloWorld))

(defn on-js-reload []
  ;; optionally touch your app-state to force rerendering depending on
  ;; your application
  (swap! app-state update-in [:__figwheel_counter] inc)
  )
