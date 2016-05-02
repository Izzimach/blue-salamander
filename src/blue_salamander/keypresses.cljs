(ns blue-salamander.keypresses)

;;
;; All this code does is track which keys are currently pressed by listening
;; for keyup/keydown events and putting the currently pressed keys into a
;; set. You pass in an atom that contains the (initially empty set) and
;; as keys are pressed/released their keycodes are swap!'d into and out of the set

;; record keys as something more readable than numbers

(def keypress-map
  {:left-arrow 37
   :up-arrow 38
   :right-arrow 39
   :down-arrow 40})

;;
;; the keys-pressed atom contains currently pressed keys, stored
;; as key codes.
;;
(def keys-pressed (atom #{}))

(defn is-key-pressed?
  "Returns true if the current key is held down. Can be one of the keywords listed in keypress-map or just a raw keycode if you know it"
  [key]
  (if (keyword? key)
    (contains? @keys-pressed (get keypress-map key)) ;; map from keyword to keycode and check
    (contains? @keys-pressed key))) ;; just use the raw keycode

;; KeyboardEvent.keyCode is deprecated but .key doesn't seem
;; to be supported everywhere

(defn- process-key-down [keys-pressed-atom key-event]
  (let [key (.-keyCode key-event)]
    (swap! keys-pressed-atom #(conj % key))
    #_(js/console.log (str "key down: " key))))

(defn- process-key-up [keys-pressed-atom key-event]
  (let [key (.-keyCode key-event)]
    (swap! keys-pressed-atom #(disj % key))
    #_(js/console.log (str "key up: " key))))

(defn- track-keypresses-in-atom
  [keytrack-atom]
  (let []
    (aset js/window "onkeydown" #(process-key-down keytrack-atom %))
    (aset js/window "onkeyup" #(process-key-up keytrack-atom %))))

(defn start-keypress-tracking
  []
  (track-keypresses-in-atom keys-pressed))




