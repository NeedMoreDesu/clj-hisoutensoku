(ns clj-hisoutensoku.core
 (:gen-class)
 (:use clj-hisoutensoku.tables)
 (:require clojure.set))

(defmacro jna-call [lib func ret & args]
 "Thanks nakkaya!"
 `(let [library#  (name ~lib)
        function# (com.sun.jna.Function/getFunction library# ~func)]
   (.invoke function# ~ret (to-array [~@args]))))
(defmacro jna-malloc [size]
 "Thanks nakkaya!"
 `(let [buffer# (java.nio.ByteBuffer/allocateDirect ~size)
        pointer# (com.sun.jna.Native/getDirectBufferPointer buffer#)]
   (.order buffer# java.nio.ByteOrder/LITTLE_ENDIAN)
   {:pointer pointer# :buffer buffer#}))

(defn get-current-window-title []
 (let [str-pointer (jna-malloc 512)
       size (jna-call :User32 "GetWindowTextA" Integer
             (jna-call :User32 "GetForegroundWindow" Integer)
             (:pointer str-pointer)
             512)]
  (apply str (map
              (fn [arg] (char (if (< arg 0) (+ arg 256) arg)))
              (repeatedly size #(.get (:buffer str-pointer)))))))
(defn touhou-window? []
 (re-find #"Touhou Hisoutensoku ver" (get-current-window-title)))

(defn key-down? [key & [before-latest-check]]
 (let [gcs (jna-call :User32 "GetAsyncKeyState" Integer (get vk-key-table key))
       current (not= 0 (bit-and gcs 0x8000))
       before (not= 0 (bit-and gcs 0x1))]
  (if before-latest-check
   (or current before)
   current)))
(defn keys-down? [& keys]
 (every? key-down? keys))

(def robot (java.awt.Robot.))
(def ^:dynamic *delay* 30)
(def sleep (fn
            ([] (Thread/sleep *delay*))
            ([arg] (Thread/sleep arg))))

(letfn
 [(send-key-press [key]
   (.keyPress robot (robot-key-table key)))]
 (defn press
  ([keys]
   (doall (map send-key-press keys)))
  ([delay keys]
   (let [key (first keys)
         keys (next keys)]
    (if key
     (do
      (send-key-press key)
      (doall
       (map
        (fn [arg]
         (sleep delay)
         (send-key-press arg))
        keys))))))))
(letfn [(send-key-release [key]
         (.keyRelease robot (robot-key-table key)))]
 (defn release
  ([keys]
   (doall (map send-key-release keys)))
  ([delay keys]
   (let [key (first keys)
         keys (next keys)]
    (if key
     (do
      (send-key-release key)
      (doall
       (map
        (fn [arg]
         (sleep delay)
         (send-key-release arg))
        keys))))))))
(defn press-release
 ([keys]
  (press keys)
  (sleep)
  (release keys))
 ([delay keys]
  (press delay keys)
  (sleep delay)
  (release delay keys)))

(defn fns
 [& {:keys [key-pressed key-released key-hold]
     :or {key-pressed (fn [])
          key-released (fn [])
          key-hold (fn [])}}]
 (fn [pressed-keys last-sequences sequence & [sequence-failed]]
  (if (not sequence-failed)
   {:last-sequences
    (if (some #(= % sequence) last-sequences)
     (do
      (key-hold)
      last-sequences)
     (do
      (key-pressed)
      (conj last-sequences sequence)))
    :succeed? true}
   {:last-sequences
    (if (some #(= % sequence) last-sequences)
     (do
      (key-released)
      (disj last-sequences sequence))
     last-sequences)
    :succeed?
    false})))
(def ^:dynamic *reset* true)
(defn key-sequence
 [sequence & [delay]]
 (fns
  :key-pressed
  (let [sequence (map (fn [arg] (if (keyword? arg) arg (first arg))) sequence)]
   (if *reset*
    #(do
      (release sequence)
      (sleep *delay*)
      (if delay
       (press delay sequence)
       (press sequence)))
    #(press sequence)))
  :key-released
  #(doall
    (map
     (fn [arg]
      (if (keyword? arg)
       (release [arg])
       (if (some keys-down? (second arg)) ; the only fn that directly checks key state.
                                          ; Hope that this wont make bugs.
        nil
        (release [(first arg)]))))
     sequence))))
(defn all-matches
 [& matches]
 (fn [pressed-keys last-sequences sequence & [sequence-failed]]
  (reduce
   (fn [{:keys [last-sequences succeed?]}
        [keys function]]
    (let [new-sequence
          (vec (concat sequence keys))
          seq-failed?
          (not (clojure.set/subset? (set keys) pressed-keys))
          {new-last-sequences :last-sequences
           new-succeed? :succeed?}
          (function
           pressed-keys
           last-sequences
           new-sequence
           (or sequence-failed seq-failed?))]
     (zipmap
      [:last-sequences
       :succeed?]
      [new-last-sequences
       (or succeed? new-succeed?)])))
   {:last-sequences last-sequences
    :succeed? false}
   (partition 2 matches))))
(defn first-match
 [& matches]
 (fn [pressed-keys last-sequences sequence & [sequence-failed]]
  (reduce
   (fn [{:keys [last-sequences matched succeed?]}
        [keys function]]
    (let [new-sequence
          (vec (concat sequence keys))
          seq-failed?
          (not (clojure.set/subset? (set keys) pressed-keys))
          {new-last-sequences :last-sequences
           new-succeed? :succeed?}
          (function
           pressed-keys
           last-sequences
           new-sequence
           (or matched seq-failed?))]
     (zipmap
      [:last-sequences
       :matched
       :succeed?]
      [new-last-sequences
      (or matched new-succeed?)
      (or succeed? new-succeed?)])))
   {:last-sequences last-sequences
    :matched sequence-failed
    :succeed? false}
   (partition 2 matches))))

(def fns-623b (fns
               :key-pressed
               (fn []
                (release [:w :a :s :d])
                (sleep)
                (press-release *delay* [:d])
                (press-release *delay* [:s :d])
                (press [:x])
                (sleep))
               :key-released
               #(release [:x])))
(def fns-623c (fns
               :key-pressed
               (fn []
                (release [:w :a :s :d])
                (sleep)
                (press-release *delay* [:d])
                (press-release *delay* [:s :d])
                (press [:c])
                (sleep))
               :key-released
               #(release [:c])))
(def fns-421b (fns
               :key-pressed
               (fn []
                (release [:w :a :s :d])
                (sleep)
                (press-release *delay* [:a])
                (press-release *delay* [:s :a])
                (press [:x])
                (sleep))
               :key-released
               #(release [:x])))
(def fns-421c (fns
               :key-pressed
               (fn []
                (release [:w :a :s :d])
                (sleep)
                (press-release *delay* [:a])
                (press-release *delay* [:s :a])
                (press [:c])
                (sleep))
               :key-released
               #(release [:c])))
(def fns-236b (fns
               :key-pressed
               (fn []
                (release [:w :a :s :d])
                (sleep)
                (press-release *delay* [:s :d])
                (press [:x])
                (sleep))
               :key-released
               #(release [:x])))
(def fns-236c (fns
               :key-pressed
               (fn []
                (release [:w :a :s :d])
                (sleep)
                (press-release *delay* [:s :d])
                (press [:c])
                (sleep))
               :key-released
               #(release [:c])))
(def fns-214b (fns
               :key-pressed
               (fn []
                (release [:w :a :s :d])
                (sleep)
                (press-release *delay* [:s :a])
                (press [:x])
                (sleep))
               :key-released
               #(release [:x])))
(def fns-214c (fns
               :key-pressed
               (fn []
                (release [:w :a :s :d])
                (sleep)
                (press-release *delay* [:s :a])
                (press [:c])
                (sleep))
               :key-released
               #(release [:c])))
(def fns-22b (fns
               :key-pressed
               (fn []
                (release [:w :a :s :d])
                (sleep)
                (press-release [:s])
                (sleep)
                (press-release [:s])
                (press [:x])
                (sleep))
               :key-released
               #(release [:x])))
(def fns-22c (fns
               :key-pressed
               (fn []
                (release [:w :a :s :d])
                (sleep)
                (press-release [:s])
                (sleep)
                (press-release [:s])
                (press [:c])
                (sleep))
               :key-released
               #(release [:c])))
(def fns-tk-j2a (fns
                 :key-pressed
                 #(press [:w :d])
                 :key-hold
                 #(do
                   (press [:w]) (sleep 15)
                   (release [:w]) (press [:s]) (sleep 15)
                   (press [:z]) (sleep 15)
                   (release 5 [:z :s]) (sleep 15))
                 :key-released
                 #(release [:w :d :z])))
(def fns-tk-j6a (fns
                 :key-pressed
                 #(press [:w :d])
                 :key-hold
                 #(do
                   (press [:w]) (sleep 15)
                   (release [:w]) (sleep 15)
                   (press [:z]) (sleep 15)
                   (release [:z]) (sleep 15))
                 :key-released
                 #(release [:w :d])))
(def fns-tk-j5a (fns
                 :key-pressed
                 #(press [:w :d])
                 :key-hold
                 #(do
                   (press [:z]) (sleep 15)
                   (release [:z]) (sleep 15))
                 :key-released
                 #(release [:w :d])))
(def fns-tk-j2a-rev (fns
                     :key-pressed
                     #(press [:w :a])
                     :key-hold
                     #(do
                       (press [:w]) (sleep 15)
                       (release [:w]) (press [:s]) (sleep 15)
                       (press [:z]) (sleep 15)
                       (release 5 [:z :s]) (sleep 15))
                     :key-released
                     #(release [:w :a :z])))
(def fns-tk-j6a-rev (fns
                     :key-pressed
                     #(press [:w :a])
                     :key-hold
                     #(do
                       (press [:w]) (sleep 15)
                       (release [:w]) (sleep 15)
                       (press [:z]) (sleep 15)
                       (release [:z]) (sleep 15))
                     :key-released
                     #(release [:w :a])))
(def fns-tk-j5a-rev (fns
                     :key-pressed
                     #(press [:w :a])
                     :key-hold
                     #(do
                       (press [:z]) (sleep 15)
                       (release [:z]) (sleep 15))
                     :key-released
                     #(release [:w :a])))

(def input-list
 (first-match
  [:numpad1 :numpad6] ;; 1+6 == 66
  (first-match
   [:end]
   (fns
    :key-hold
    #(do
      (press [:d]) (sleep 15)
      (press [:z]) (sleep 15)
      (release [:z]) (sleep 15)
      (release [:d]) (sleep 15)))
   [:return]
   (fns
    :key-hold
    #(do
      (press [:d]) (sleep 15)
      (release [:d]) (sleep 15)
      (press [:c]) (sleep 15)
      (release [:c]) (sleep 15)))
   []
   (fns
    :key-hold
    #(do (press [:d]) (sleep 10) (release [:d]) (sleep 10))))
  [:numpad3 :numpad4] ;; 3+4 == 44
  (first-match
   [:end]
   (fns
    :key-hold
    #(do
      (press [:a]) (sleep 15)
      (press [:z]) (sleep 15)
      (release [:z]) (sleep 15)
      (release [:a]) (sleep 15)))
   [:return]
   (fns
    :key-hold
    #(do
      (press [:a]) (sleep 15)
      (release [:a]) (sleep 15)
      (press [:c]) (sleep 15)
      (release [:c]) (sleep 15)))
   []
   (fns
    :key-hold
    #(do (press [:a]) (sleep 10) (release [:a]) (sleep 10))))
  [:home] ; heavy artillery
  (first-match
   [:numpad7] fns-421c [:numpad8] fns-22c [:numpad9] fns-623b
   [:numpad4] fns-421b [:numpad5] fns-22b [:numpad6] fns-623b
   [:numpad1] fns-421c [:numpad2] fns-22c [:numpad3] fns-623c)
  [:+] ; same key as for B bullets
  (first-match
   [:numpad7] fns-421c                    [:numpad9] fns-623c
   [:numpad1] fns-214c                    [:numpad3] fns-236c)
  [:return] ; same key as for C bullets
  (first-match
   [:numpad7] fns-421b                    [:numpad9] fns-623b
   [:numpad1] fns-214b                    [:numpad3] fns-236b)
  [] (all-matches
      [] (binding [*reset* false] ; reset crashes blocks
          (all-matches ; Uses guarded version of key-sequence.
           ;; [:s [:numpad2 :numpad3]] means s key won't be
           ;; keyuped while either numpad2 or numpad3 is
           ;; pressed
           [:numpad1] (key-sequence [[:s [:numpad2 :numpad3]] [:a [:numpad4 :numpad7]]])
           [:numpad2] (key-sequence [[:s [:numpad1 :numpad3]]])
           [:numpad3] (key-sequence [[:s [:numpad1 :numpad2]] [:d [:numpad6 :numpad9]]])
           [:numpad4] (key-sequence [[:a [:numpad1 :numpad7]]])
           [:numpad6] (key-sequence [[:d [:numpad3 :numpad9]]])
           [:numpad7] (key-sequence [[:a [:numpad1 :numpad4]] [:w [:numpad8 :numpad9]]])
           [:numpad8] (key-sequence [[:w [:numpad7 :numpad9]]])
           [:numpad9] (key-sequence [[:d [:numpad3 :numpad6]] [:w [:numpad7 :numpad8]]])))
      [:numpad5] (key-sequence [:v])  ; D
      [:end] (first-match
              [:numpad6 :numpad9] ; 6+end+9 == instant tkj6a
              fns-tk-j6a
              [:numpad3 :numpad9] ; 3+end+9 == instant tkj3a
              fns-tk-j2a
              [:numpad9]          ; end+9 == instant tkj5a
              fns-tk-j5a
              [:numpad4 :numpad7] ; same, but reversed
              fns-tk-j6a-rev
              [:numpad1 :numpad7]
              fns-tk-j2a-rev
              [:numpad7]
              fns-tk-j5a-rev
              []                  ; else it's simply A
              (key-sequence [:z]))    ; A
      [:+] (key-sequence [:x])        ; B
      [:return] (key-sequence [:c])   ; C
      [:*] (key-sequence [:b])        ; choose spellcard
      [:-] (key-sequence [:n])        ; activate spellcard
      )))

(defn -main [& args]
 (loop [prev #{}]
  (if (touhou-window?)
   (do
    (Thread/sleep 1)
    (println prev)
    (recur (:last-sequences (input-list
                             (set (keep (fn[[name _]] (if (key-down? name) name)) vk-key-table))
                             prev
                             []))))
   (do
    (Thread/sleep 700)
    (recur prev)))))
