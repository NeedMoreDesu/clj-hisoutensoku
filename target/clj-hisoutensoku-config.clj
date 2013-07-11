;;; window regex pattern.
"Touhou Hisoutensoku ver"

;;; new config section
[:end     ; new A key
 :+       ; new B key
 :return  ; new C key
 :numpad5 ; new D key
 :numpad1
 :numpad2
 :numpad3
 :numpad4
 ;; there is no numpad5 direction
 :numpad6
 :numpad7
 :numpad8
 :numpad9
 :home   ; power-system key
 :*      ; card choose
 :-      ; card use
 ]
;;; old config section
;; must match your character config in soku
;; beware, arrow keys don't work. Blame java.awt.robot class for that
[:z  ; old A key
 :x  ; old B key
 :c  ; old C key
 :v  ; old D key
 :b  ; old card choose
 :n  ; old card use
 :s  ; old 2 key
 :d  ; old 6 key
 :w  ; old 8 key
 :a  ; old 4 key
 ]

;;; key-replacement section
(first-match
 [key-1 key-6] ;; 1+6 == 66
 fns-66
 [key-9 key-4] ;; 3+4 == 66
 fns-66
 [key-3 key-4] ;; 3+4 == 44
 fns-44
 [key-7 key-6] ;; 7+6 == 44
 fns-44
 [key-power]                            ; heavy artillery
 (first-match
  [key-7] fns-421b [key-8] fns-22b [key-9] fns-623b
  [key-4] fns-421c [key-d] fns-22b [key-6] fns-623c
  [key-1] fns-421b [key-2] fns-22c [key-3] fns-623b
  [] fns-22b)
 [key-c key-a]
 (first-match
  [key-1] fns-421c
  [key-3] fns-623c
  [] (fns :key-hold #(press-release 2 [key-old-c])))
 [key-b key-a]
 (first-match
  [key-1] fns-421b
  [key-3] fns-623b
  [] (fns :key-hold #(press-release 2 [key-old-b])))
 [key-b]                         ; same key as for B bullets
 (first-match
  ;; [key-7] fns-421c                    [key-9] fns-623c
  [key-1] fns-214b                    [key-3] fns-236b)
 [key-c]                         ; same key as for C bullets
 (first-match
  ;; [key-7] fns-421b                    [key-9] fns-623b
  [key-1] fns-214c                    [key-3] fns-236c)
 []
 (all-matches
  []
  (binding [*reset* false]            ; reset crashes blocks
   (all-matches      ; Uses guarded version of key-sequence.
    ;; [key-old-2 [key-2 key-3]] means s key won't be
    ;; keyuped while either numpad2 or numpad3 is pressed
    ;; Also, [key-old-2 [[[key-2] [key-3]] key-4]] means
    ;; that even if key-2 is down, if key-3 is down, we
    ;; will do keyup
    [key-1] (key-sequence [[key-old-2
                            [[[key-1] [key-6]]
                             key-2
                             [[key-3] [key-4]]]]
                           [key-old-4
                            [[[key-1] [key-6]]
                             [[key-4] [key-9 key-3]]
                             [[key-7] [key-6]]]]])
    [key-2] (key-sequence [[key-old-2
                            [[[key-1] [key-6]]
                             key-2
                             [[key-3] [key-4]]]]])
    [key-3] (key-sequence [[key-old-2
                            [[[key-1] [key-6]]
                             key-2
                             [[key-3] [key-4]]]]
                           [key-old-6
                            [[[key-3] [key-4]]
                             [[key-6] [key-7 key-1]]
                             [[key-9] [key-4]]]]])
    [key-4] (key-sequence [[key-old-4
                            [[[key-1] [key-6]]
                             [[key-4] [key-9 key-3]]
                             [[key-7] [key-6]]]]])
    [key-6] (key-sequence [[key-old-6
                            [[[key-3] [key-4]]
                             [[key-6] [key-7 key-1]]
                             [[key-9] [key-4]]]]])
    [key-7] (key-sequence [[key-old-4
                            [[[key-1] [key-6]]
                             [[key-4] [key-9 key-3]]
                             [[key-7] [key-6]]]]
                           [key-old-8
                            [[[key-7] [key-6]]
                             key-8
                             [[key-9] [key-4]]]]])
    [key-8] (key-sequence [[key-old-8
                            [[[key-7] [key-6]]
                             key-8
                             [[key-9] [key-4]]]]])
    [key-9] (key-sequence [[key-old-6
                            [[[key-3] [key-4]]
                             [[key-6] [key-7 key-1]]
                             [[key-9] [key-4]]]]
                           [key-old-8
                            [[[key-7] [key-6]]
                             key-8
                             [[key-9] [key-4]]]]])
    [key-d] (key-sequence [key-old-d])))
  []
  (first-match
   [key-a] (tk-cancel-tree key-old-a)
   [key-b] (tk-cancel-tree key-old-b)
   [key-c] (tk-cancel-tree key-old-c)
   [key-card-choose] (key-sequence [key-old-card-choose])
   [key-card-use] (key-sequence [key-old-card-use])
   [key-3 key-9] (triangle fn29 [key-old-2 key-old-6])
   [key-2 key-9] (triangle fn29 [key-old-2])
   [key-1 key-9] (triangle fn29 [key-old-2 key-old-4])
   [key-3 key-8] (triangle fn28 [key-old-2 key-old-6])
   [key-2 key-8] (triangle fn28 [key-old-2])
   [key-1 key-8] (triangle fn28 [key-old-2 key-old-4])
   [key-3 key-7] (triangle fn27 [key-old-2 key-old-6])
   [key-2 key-7] (triangle fn27 [key-old-2])
   [key-1 key-7] (triangle fn27 [key-old-2 key-old-4]))))
