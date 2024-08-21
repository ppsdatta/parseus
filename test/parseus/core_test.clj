(ns parseus.core-test
  (:require [clojure.test :refer :all]
            [parseus.core :refer :all]))

(deftest expr-tests
  (testing "Test a digit parser."
    (let [p1 (p-digit 9)
          r (parse p1 "9 islands")]
      (is (= (first r) 9))))

  (testing "Test rest of the sequence."
    (let [p1 (p-digit 9)
          r (parse p1 "9 islands")]
      (is (= (second r) " islands"))))

  (testing "Test a char parser."
    (let [p2 (p-char \c)
          r (parse p2 "clojure")]
      (is (= (first r) \c))))

  (testing "Test a char parser failure."
    (let [p2 (p-char \c)
          r (parse p2 "ruby")]
      (is (fail? r)))))

(deftest composition-tests
  (testing "Test some digits."
    (let [p-digits (p-some (p-digit))
          r (parse p-digits "123algol")]
      (is (= (-> r first some-value num-value) 123))))

  (testing "Test some digits failure."
    (let [p-digits (p-some (p-digit))
          r (parse p-digits "fortran123algol")]
      (is (fail? (-> r first some-value)))))

  (testing "Test some chars."
    (let [p-chars (p-some (p-char))
          r (parse p-chars "abc233as")]
      (is (= (-> r first some-value str-value) "abc233as"))))

  (testing "Test some chars. Should pass as some is eqv to *."
    (let [p-chars (p-some (p-char))
          r (parse p-chars "")]
      (is (= (-> r first some-value str-value) "")))))

(deftest or-test
  (testing "Test p-or."
    (let [p1 (p-or (p-digit 0)
                   (p-char \H))
          r1 (parse p1 "0 Code")
          r2 (parse p1 "Hello")
          r3 (parse p1 "#include")]
      (is (= (-> r1 first) 0))
      (is (= (-> r2 first) \H))
      (is (fail? r3)))))

(deftest line-test
  (testing "A line with a fixed format."
    (let [delim (p-or
                  (p-char \,)
                  (p-some
                    (p-char \space)))
          digits (p-some (p-digit))
          line (p-seq
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
                 (return {:from    (num-value num1)
                          :to      (num-value num2)
                          :total   (+ (num-value n1)
                                      (num-value n2)
                                      (num-value n3))
                          :details (str-value details)}))
          r1 (parse line "123,45 (1,2,3),Needs re testing of this")
          r2 (parse line "234,45,(1,2,3),some random text")
          r3 (parse line "2 233 (23 34 31) some other text")
          r4 (parse line "2,233,(23 34 31) some other text")
          r5 (parse line "234,45,(1 2 3),some random text")]
      (is (= r1 [{:details "Needs re testing of this"
                  :from    123
                  :to      45
                  :total   6}
                 ""]))
      (is (= r2 [{:details "some random text"
                  :from    234
                  :to      45
                  :total   6}
                 ""]))
      (is (= r3 [{:details "some other text"
                  :from    2
                  :to      233
                  :total   88}
                 ""]))
      (is (= r4 [{:details "some other text"
                  :from    2
                  :to      233
                  :total   88}
                 ""]))
      (is (= r5 [{:details "some random text"
                  :from    234
                  :to      45
                  :total   6}
                 ""])))))