(ns blue-salamander.r3elements
  (:require-macros [blue-salamander.r3elements :as abbrev])
  (:require [goog.events :as events]
            [clojure.string :as string]))

;;
;; provides convenient definitions for threejs nodes. Instead of going:
;;
;; (js/React.createElement (js/ReactTHREE.Scene #js {} ....))
;;
;; you go:
;;
;; < require this ns as 'r3' >
;;
;; (r3/scene {} ...)
;;


(defn element-args [opts children]
  (cond
    (nil? opts) [nil children]
    (map? opts) [(clj->js opts) children]
    (object? opts) [opts children]))

(abbrev/defn-r3-element Renderer)
(abbrev/defn-r3-element Scene)
(abbrev/defn-r3-element Mesh)
(abbrev/defn-r3-element Object3D)
(abbrev/defn-r3-element PerspectiveCamera)
(abbrev/defn-r3-element HemisphereLight)
(abbrev/defn-r3-element DirectionalLight)
(abbrev/defn-r3-element AmbientLight)

(def Origin (js/THREE.Vector3. 0 0 0))

(defn Vector3 [x y z] (js/THREE.Vector3. x y z))

(defn vec3->Vector3
  "Convert a thi.ng vec3 into a threejs Vector3"
  [[x y z]]
  (js/THREE.Vector3. x y z))
