(ns blue-salamander.r3elements)

;;
;; abbreviated react-three component definitions, similar to those used in om-tools
;;


(defmacro defn-r3-element [tag]
  (let [jsname  (symbol "js" (str "ReactTHREE." (name tag)))
        cljname (symbol (clojure.string/lower-case (name tag)))]
    `(defn ~cljname [opts# & children#]
       (let [[opts# children#] (element-args opts# children#)]
         (apply React.createElement
                ~jsname
                (cljs.core/into-array (cons opts# children#)))))))
