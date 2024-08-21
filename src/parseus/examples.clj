(ns parseus.examples
  (:use [parseus.core]))

(def whitespace
  (p-some
    (p-char \space)))

(def digits (p-some (p-digit)))

(def factor (p-seq (:= d1 digits)
                   (:= part (p-some
                              (p-seq
                                whitespace
                                (:= op (p-or (p-char \*)
                                             (p-char \/)))
                                whitespace
                                (:= d2 factor)
                                (return {:op op :num2 d2}))))
                   (return (if (fail? (some-value part))
                             (-> d1 some-value num-value)
                             {:op   (-> part first :op)
                              :args [(num-value d1)
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
(parse factor "23 * 334 / 3 * 12")
(parse term "")
