(ns parseus.examples
  (:use [parseus.core]))

(def delim
  (p-or
    (p-char \,)
    (p-some
      (p-char \space))))

(def digits (p-some (p-digit)))

(def line (p-seq
            (:= num1 digits)
            delim
            (:= num2 digits)
            delim
            (p-char \()
            (:= n1 digits)
            delim
            (:= n2 digits)
            delim
            (:= n3 digits)
            (p-char \))
            delim
            (:= details (p-some (p-char)))
            (return {:from    (p-num num1)
                     :to      (p-num num2)
                     :total   (+ (p-num n1)
                                 (p-num n2)
                                 (p-num n3))
                     :details (p-str details)})))

(parse line "123,45 (1,2,3),Needs re testing of this")
(parse line "234,45,(1,2,3),some random text")
(parse line "2 233 (23 34 31) some other text")
(parse line "2,233,(23 34 31) some other text")
(parse line "234,45,(1 2 3),some random text")