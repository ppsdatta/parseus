(ns parseus.examples
  (:use [parseus.core]))

(def whitespace
  (p-some
    (p-char \space)))

(def digits (p-some (p-digit)))

(def factor (p-seq (:= d1 (p-or
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
                              :args [d1
                                     (-> part first :num2)]}))))

(def term (p-seq
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

(parse term "2*102 + 32 * 3 - 34")
(parse term "23 * 334 / 3 * 12")
(parse term "")
(parse term "(2 + 3 * 99 ) *(5 - (11 / 11) )")
(parse term "42")

(def op-map {\+ +
             \- -
             \* *
             \/ /})

(defn term-eval [term]
  (if (number? term)
    term
    (let [op (:op term)
          axs (map term-eval (:args term))]
      (apply (op-map op) axs))))

(defn calc [s]
  (->> s
       (parse term)
       first
       term-eval))

(calc "23 * 3 / (4 - 21)")
(calc "23 * 3 + 4 / 2")
(calc "(2 + 3 * 99 ) *(5 - (11 / 11) )")

