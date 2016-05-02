(ns blue-salamander.collision
  (:require [thi.ng.geom.core :as g]
            [thi.ng.geom.core.vector :as v :refer [vec2 vec3]]))


(defrecord Sphere [center radius])

;;
;; for AABB size is full size on each axis
(defrecord AABB [center size])

(defn collide-sphere-cube
  "Checks if the given sphere and cube collide. Returns nil if no collision,
   or a vec3 of a point where the two overlap. This point is just one of many
possible overlap points, and may not be the most-penetrating overlap point or
anything like that."
  [{sphere-center :center radius :radius :as sphere}
   {aabb-center :center box-size :size :as cube}]
  {:pre [(v/vec3? sphere-center)
         (number? radius)
         (v/vec3? aabb-center)
         (v/vec3? box-size)]}
  (let [
        ;; clamp the seperating vector to be on the aabb edge/corner
        sep-vector (g/- sphere-center aabb-center)
        [dx dy dz] sep-vector
        [ex ey ez] (g/* box-size 0.5 )
        cubeclamp (fn [v mag] (max (- mag) (min mag v)))
        px (cubeclamp dx ex)
        py (cubeclamp dy ey)
        pz (cubeclamp dz ez)
        p-on-cube (vec3 px py pz)

        ;; is this clamped point inside the sphere?
        dist (g/dist p-on-cube sep-vector)]
    (if (< dist radius)
      (g/+ aabb-center p-on-cube)
      nil)))


;;
;; example data for some tests
;;

(def sphere1 (Sphere. (vec3 10 10 10) 10))
(def box1 (AABB. (vec3 -1 -1 -1) (vec3 4 4 4)))
(def box2 (AABB. (vec3 3 3 3) (vec3 4 4 4)))
(def box3 (AABB. (vec3 -1 -1 -1) (vec3 10 10 10)))

(defn test-collision []
  (println "1-1:" (str (collide-sphere-cube sphere1 box1)))
  (println "1-2:" (str (collide-sphere-cube sphere1 box2)))
  (println "1-3:" (str (collide-sphere-cube sphere1 box3))))


