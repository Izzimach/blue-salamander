(ns blue-salamander.collision
  (:require [thi.ng.geom.core.vector :as v :refer [vec2 vec3]])
  )


(defrecord Sphere [center radius])

(defrecord OBB [center x-axis y-axis z-axis])

(defn collide-sphere-cube
  "Checks if the given sphere and cube collide. Returns a map "
  [{sphere-center :center :keys [radius] :as sphere}
   {obb-center :center :keys [x-axis y-axis z-axis] :as cube}]
  (let [diff-vec ( )]))
