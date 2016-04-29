(ns blue-salamander.r3utils)

(def origin (js/THREE.Vector3. 0 0 0))

(defn vec3 [x y z] (js/THREE.Vector3. x y z))
