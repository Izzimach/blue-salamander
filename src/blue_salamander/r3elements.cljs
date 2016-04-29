(ns blue-salamander.r3elements
  (:require-macros [blue-salamander.r3elements :as abbrev])
  (:require [goog.events :as events]
            [clojure.string :as string]))

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
