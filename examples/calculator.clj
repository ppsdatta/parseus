;; Integer expression calculator — parseus example
;; Evaluate in a REPL: load this file after adding parseus to your deps.
;;
;; Grammar (informal BNF):
;;   factor := '(' term ')' | digits | digits ('*' | '/') factor
;;   term   := factor | factor ('+' | '-') term

(ns parseus.examples.calculator
  (:use [parseus.core]))

;; --- primitives ---

(def whitespace
  (p-some (p-char \space)))

(def digits (p-some (p-digit)))

;; --- grammar ---

(declare term)

(def factor
  (p-seq (:= d1 (p-or
                  (p-seq
                    (p-char \()
                    whitespace
                    (:= t1 term)
                    whitespace
                    (p-char \))
                    (return t1))
                  (p-seq
                    (:= d1 digits)
                    (return (-> d1 some-value num-value)))))
         (:= part (p-some
                    (p-seq
                      whitespace
                      (:= op (p-or (p-char \*)
                                   (p-char \/)))
                      whitespace
                      (:= d2 factor)
                      (return {:op op :num2 d2}))))
         (return (if (fail? (some-value part))
                   d1
                   {:op   (-> part first :op)
                    :args [d1 (-> part first :num2)]}))))

(def term
  (p-seq
    (:= f1 factor)
    (:= part (p-some
               (p-seq
                 whitespace
                 (:= op (p-or (p-char \+)
                              (p-char \-)))
                 whitespace
                 (:= f2 term)
                 (return {:op op :f2 f2}))))
    (return (if (fail? (some-value part))
              f1
              {:op   (-> part first :op)
               :args [f1 (-> part first :f2)]}))))

;; --- evaluator ---

(def op-map {\+ + \- - \* * \/ /})

(defn term-eval [t]
  (if (number? t)
    t
    (apply (op-map (:op t)) (map term-eval (:args t)))))

(defn calc [s]
  (-> (parse term s) first term-eval))

;; --- try it ---

(comment
  (parse term "2 * 3 + 3 * 4 - 10")
  (parse term "2*102 + 32 * 3 - 34")
  (parse term "(2 + 3 * 99 ) *(5 - (11 / 11) )")
  (calc "2 * 3 + 3 * 4 - 10")
  (calc "23 * 3 / (4 - 21)")
  (calc "(2 + 3 * 99 ) *(5 - (11 / 11) )")
  ;; => 1196
  )
