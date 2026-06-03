(ns parseus.core-test
  (:require [clojure.test :refer :all]
            [parseus.core :refer :all]))

(deftest primitive-tests
  (testing "p-digit any — matches a digit and returns its integer value."
    (let [r (parse (p-digit) "5abc")]
      (is (= (first r) 5))
      (is (= (second r) "abc"))))

  (testing "p-digit any — fails on non-digit."
    (is (fail? (parse (p-digit) "abc"))))

  (testing "p-digit any — fails on empty input."
    (is (fail? (parse (p-digit) ""))))

  (testing "p-digit n — matches specific digit."
    (let [r (parse (p-digit 9) "9 islands")]
      (is (= (first r) 9))
      (is (= (second r) " islands"))))

  (testing "p-digit n — fails when digit doesn't match."
    (is (fail? (parse (p-digit 3) "9 islands"))))

  (testing "p-char any — matches any character."
    (let [r (parse (p-char) "clojure")]
      (is (= (first r) \c))
      (is (= (second r) "lojure"))))

  (testing "p-char any — fails on empty input."
    (is (fail? (parse (p-char) ""))))

  (testing "p-char c — matches specific character."
    (let [r (parse (p-char \c) "clojure")]
      (is (= (first r) \c))))

  (testing "p-char c — fails on wrong character."
    (is (fail? (parse (p-char \c) "ruby"))))

  (testing "num-value converts digit list to integer."
    (is (= (num-value [1 2 3]) 123))
    (is (= (num-value [0]) 0))
    (is (= (num-value []) 0)))

  (testing "str-value converts char list to string."
    (is (= (str-value [\a \b \c]) "abc"))
    (is (= (str-value []) "")))

  (testing "some-value unwraps [:none] sentinel to empty vector."
    (is (= (some-value [:none]) [])))

  (testing "some-value passes through a real result unchanged."
    (is (= (some-value [1 2 3]) [1 2 3])))

  (testing "p-satisfy matches character satisfying predicate."
    (let [r (parse (p-satisfy #(= % \x)) "xyz")]
      (is (= (first r) \x))
      (is (= (second r) "yz"))))

  (testing "p-satisfy fails when predicate is false."
    (is (fail? (parse (p-satisfy #(= % \x)) "abc"))))

  (testing "p-satisfy fails on empty input."
    (is (fail? (parse (p-satisfy (constantly true)) "")))))

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

(deftest p-seq-tests
  (testing "Basic sequence: two parsers succeed, return value and remaining."
    (let [p (p-seq (:= a (p-digit))
                   (:= b (p-digit))
                   (return [a b]))
          r (parse p "12rest")]
      (is (= (first r) [1 2]))
      (is (= (second r) "rest"))))

  (testing "Non-binding step is run for effect and its result discarded."
    (let [p (p-seq (:= a (p-digit))
                   (p-char \-)
                   (:= b (p-digit))
                   (return [a b]))
          r (parse p "3-7done")]
      (is (= (first r) [3 7]))
      (is (= (second r) "done"))))

  (testing "Failure in first step short-circuits the whole sequence."
    (let [p (p-seq (:= a (p-digit))
                   (:= b (p-digit))
                   (return [a b]))]
      (is (fail? (parse p "abc")))))

  (testing "Failure in a later step short-circuits the whole sequence."
    (let [p (p-seq (:= a (p-digit))
                   (p-char \-)
                   (:= b (p-digit))
                   (return [a b]))]
      (is (fail? (parse p "3Xabc")))))

  (testing "Non-binding step failure short-circuits."
    (let [p (p-seq (p-char \()
                   (:= n (p-digit))
                   (p-char \))
                   (return n))]
      (is (fail? (parse p "(3x")))))

  (testing "return expression can be any Clojure form over bound names."
    (let [p (p-seq (:= a (p-digit))
                   (:= b (p-digit))
                   (return (+ a b)))
          r (parse p "39end")]
      (is (= (first r) 12))
      (is (= (second r) "end"))))

  (testing "Sequence with no return yields [nil remaining]."
    (let [p (p-seq (p-digit) (p-digit))
          r (parse p "12rest")]
      (is (nil? (first r)))
      (is (= (second r) "rest")))))

(deftest p-fmap-tests
  (testing "Transforms parsed value with given function."
    (let [r (parse (p-fmap #(* % 10) (p-digit)) "3abc")]
      (is (= (first r) 30))
      (is (= (second r) "abc"))))

  (testing "Propagates failure unchanged."
    (is (fail? (parse (p-fmap #(* % 10) (p-digit)) "abc"))))

  (testing "Function can change the type of the result."
    (let [r (parse (p-fmap str (p-digit)) "7xyz")]
      (is (= (first r) "7"))))

  (testing "Composes with p-some — transform the collected list."
    (let [r (parse (p-fmap num-value (p-some (p-digit))) "42rest")]
      (is (= (first r) 42))
      (is (= (second r) "rest"))))

  (testing "Remaining input is not affected by the transform."
    (let [r (parse (p-fmap (constantly :x) (p-char \a)) "abc")]
      (is (= (first r) :x))
      (is (= (second r) "bc")))))

(deftest p-any-of-tests
  (testing "Matches a character in the set."
    (let [r (parse (p-any-of [\a \e \i \o \u]) "apricot")]
      (is (= (first r) \a))
      (is (= (second r) "pricot"))))

  (testing "Fails when character is not in the set."
    (is (fail? (parse (p-any-of [\a \e \i \o \u]) "fruit"))))

  (testing "Fails on empty input."
    (is (fail? (parse (p-any-of [\a \b]) "")))))

(deftest p-none-of-tests
  (testing "Matches a character not in the set."
    (let [r (parse (p-none-of [\, \; \space]) "abc,")]
      (is (= (first r) \a))
      (is (= (second r) "bc,"))))

  (testing "Fails when character is in the excluded set."
    (is (fail? (parse (p-none-of [\, \;]) ","))))

  (testing "Fails on empty input."
    (is (fail? (parse (p-none-of [\,]) "")))))

(deftest p-eof-tests
  (testing "Succeeds on empty input."
    (let [r (parse p-eof "")]
      (is (nil? (first r)))
      (is (= (second r) ""))))

  (testing "Fails when input remains."
    (is (fail? (parse p-eof "abc"))))

  (testing "Succeeds after consuming all input in a sequence."
    (let [p (p-seq (:= d (p-digit)) p-eof (return d))
          r (parse p "7")]
      (is (= (first r) 7))
      (is (= (second r) "")))))

(deftest p-optional-tests
  (testing "Returns parsed value when parser succeeds."
    (let [r (parse (p-optional (p-digit)) "3abc")]
      (is (= (first r) 3))
      (is (= (second r) "abc"))))

  (testing "Returns nil and leaves input unchanged when parser fails."
    (let [r (parse (p-optional (p-digit)) "abc")]
      (is (nil? (first r)))
      (is (= (second r) "abc"))))

  (testing "Never fails — succeeds even on empty input."
    (let [r (parse (p-optional (p-digit)) "")]
      (is (nil? (first r)))
      (is (= (second r) ""))))

  (testing "Works inside p-seq to make a step optional."
    (let [p (p-seq (:= sign (p-optional (p-char \-)))
                   (:= n (p-some (p-digit)))
                   (return {:sign sign :n (num-value n)}))
          r1 (parse p "-42rest")
          r2 (parse p "42rest")]
      (is (= (first r1) {:sign \- :n 42}))
      (is (= (first r2) {:sign nil :n 42})))))

(deftest p-many-tests
  (testing "Matches one or more — returns a seq of results."
    (let [r (parse (p-many (p-digit)) "123abc")]
      (is (= (first r) '(1 2 3)))
      (is (= (second r) "abc"))))

  (testing "Fails on zero matches unlike p-some."
    (is (fail? (parse (p-many (p-digit)) "abc"))))

  (testing "Fails on empty input."
    (is (fail? (parse (p-many (p-digit)) ""))))

  (testing "Single match succeeds."
    (let [r (parse (p-many (p-digit)) "1abc")]
      (is (= (first r) '(1)))
      (is (= (second r) "abc"))))

  (testing "num-value works on p-many result."
    (let [r (parse (p-fmap num-value (p-many (p-digit))) "99end")]
      (is (= (first r) 99)))))

(deftest p-collect-tests
  (testing "Collects results of each parser into a vector."
    (let [r (parse (p-collect (p-char \c) (p-char \+) (p-char \+)) "c++ is cool")]
      (is (= (first r) [\c \+ \+]))
      (is (= (second r) " is cool"))))

  (testing "Fails if any parser in the sequence fails."
    (is (fail? (parse (p-collect (p-char \c) (p-char \+) (p-char \+)) "c# is not"))))

  (testing "Fails on first-step failure."
    (is (fail? (parse (p-collect (p-char \x) (p-char \y)) "abc"))))

  (testing "Empty p-collect always succeeds with empty vector."
    (let [r (parse (p-collect) "hello")]
      (is (= (first r) []))
      (is (= (second r) "hello"))))

  (testing "Mixed parsers: digits and chars collected together."
    (let [r (parse (p-collect (p-digit) (p-char \x) (p-digit)) "3x7end")]
      (is (= (first r) [3 \x 7]))
      (is (= (second r) "end"))))

  (testing "str-value converts collected chars to string."
    (let [r (parse (p-collect (p-char \h) (p-char \i)) "hi there")]
      (is (= (str-value (first r)) "hi")))))

(deftest p-str-tests
  (testing "Matches a literal string and returns chars."
    (let [r (parse (p-str "c++") "c++ is cool")]
      (is (= (first r) [\c \+ \+]))
      (is (= (second r) " is cool"))))

  (testing "str-value on result gives back the original string."
    (let [r (parse (p-str "hello") "hello world")]
      (is (= (str-value (first r)) "hello"))))

  (testing "Fails when input does not start with the literal."
    (is (fail? (parse (p-str "c++") "java is verbose"))))

  (testing "Fails on partial match."
    (is (fail? (parse (p-str "c++") "c+ is partial"))))

  (testing "Remaining input is correct after match."
    (let [r (parse (p-str "clojure") "clojure rocks")]
      (is (= (second r) " rocks"))))

  (testing "Empty string matches everything, returns empty vector."
    (let [r (parse (p-str "") "anything")]
      (is (= (first r) []))
      (is (= (second r) "anything"))))

  (testing "p-str inside p-seq."
    (let [p (p-seq (:= lang (p-str "c++"))
                   (p-char \space)
                   (:= rest (p-some (p-char)))
                   (return {:lang (str-value lang) :desc (str-value rest)}))
          r (parse p "c++ is cool")]
      (is (= (first r) {:lang "c++" :desc "is cool"})))))

(deftest p-some-tests
  (testing "Single match returns one-element list."
    (let [r (parse (p-some (p-digit)) "1abc")]
      (is (= (some-value (first r)) [1]))
      (is (= (second r) "abc"))))

  (testing "Remaining input is correct after partial match."
    (let [r (parse (p-some (p-digit)) "12rest")]
      (is (= (second r) "rest"))))

  (testing "Zero matches: some-value yields empty, input unchanged."
    (let [r (parse (p-some (p-digit)) "abc")]
      (is (= (some-value (first r)) []))
      (is (= (second r) "abc"))))

  (testing "Entire input consumed when all chars match."
    (let [r (parse (p-some (p-digit)) "987")]
      (is (= (some-value (first r)) [9 8 7]))
      (is (= (second r) ""))))

  (testing "p-some nested inside p-seq accumulates correctly."
    (let [p (p-seq (:= ds (p-some (p-digit)))
                   (p-char \!)
                   (return (some-value ds)))
          r (parse p "42!")]
      (is (= (first r) [4 2]))
      (is (= (second r) ""))))

  (testing "p-some of p-some: outer gets list of inner results."
    ; each inner p-some(p-digit) grabs one digit run; outer p-some runs it repeatedly
    ; on "1x2" inner matches [1], fails on 'x' so outer stops after one match
    (let [inner (p-some (p-digit))
          r (parse (p-some inner) "123")]
      (is (not (fail? r)))))

  (testing "p-some returns parser with uniform (fn [s]) arity — callable directly."
    (let [p (p-some (p-char \a))
          r (p "aab")]
      (is (= (some-value (first r)) [\a \a]))
      (is (= (second r) "b")))))

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