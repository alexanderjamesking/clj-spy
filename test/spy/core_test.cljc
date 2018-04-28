(ns spy.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [spy.core :as s]))

(deftest spy-that-returns-nil
  (testing "calling spy with no arguments gives a spy that returns nil"
    (let [f (s/spy)]
      (is (nil? (f)))
      (is (s/called? f))))
  (testing "calling stub with no arguments gives a spy that returns nil"
    (let [f (s/stub)]
      (is (nil? (f)))
      (is (s/called? f)))))

(deftest stub-call-counts
  (testing "call counts"
    (let [f (s/stub 42)]
      (is (s/not-called? f))
      (f)
      (is (s/called? f))
      (is (s/called-once? f))
      (f)
      (f)
      (is (s/called-n? 3 f)))))

(deftest called-at-least
  (testing "called at least once"
    (let [f (s/stub 42)]
      (is (false? (s/called-at-least? 1 f)))
      (f)
      (is (s/called-at-least? 1 f))
      (is (s/called-at-least-once? f))

      (doall (repeatedly 42 f))

      (is  (s/called-at-least? 42 f)))))

(deftest called-at-most
  (testing "called at most once"
    (let [f (s/stub 42)]
      (is (s/called-at-most-n? 1 f))
      (is (s/called-no-more-than-once? f))
      (f)
      (f)

      (is (false? (s/called-at-most-n? 1 f)))
      (f)

      (is (false? (s/called-no-more-than-once? f)))

      (doall (repeatedly 42 f))

      (is  (s/called-at-most-n? 50 f)))))

(deftest reset-spy
  (testing "resetting the call count for a spy"
    (let [f (s/stub 1863)]
      (doall (repeatedly 3 f))
      (is (s/called-n? 3 f))
      (s/reset-spy! f)
      (is (s/not-called? f))
      (is (nil? (s/first-response f))))))

(deftest spy-call-counts
  (testing "call counts"
    (let [f (s/spy (fn [x y] (+ x y)))]
      (is (s/not-called? f))
      (is (= 3 (f 1 2)))
      (is (s/called-once? f))))

  (testing "call counts when the spy is wrapped with partial"
    (let [spy (s/spy (fn [x y] (+ x y)))
          pf (partial spy 5)]
      (is (s/not-called? spy))
      (is (= 8 (pf 3)))
      (is (s/called-once? spy)))))

(deftest called-with-test
  (testing "called with"
    (let [f (s/spy +)]
      (f 1 2)
      (is (s/called-with? f 1 2))
      (f 1 2 3)
      (is (s/called-with? f 1 2 3))
      (is (false? (s/called-with? f 4 5 6)))
      (is (s/not-called-with? f 7 8)))))

(deftest called-once-with-test
  (let [f (s/spy str)]
    (testing "called with exactly"
      (f "hello world!")
      (is (s/called-once-with? f "hello world!")))

    (testing "returns false if there were other calls"
      (f "foo bar")
      (is (false? (s/called-once-with? f "foo bar")))
      (is (false? (s/called-once-with? f "hello" "world" "!"))))))

(deftest nth-call
  (testing "nth call"
    (let [f (s/spy keyword)]
      (f "bingo")
      (is (= ["bingo"] (s/nth-call 0 f)))
      (is (= ["bingo"] (s/first-call f)))

      (f "foo")
      (f "bar")

      (is (= ["bar"] (s/last-call f)))

      (is (= nil (s/nth-call 42 f)))))

  (testing "error cases"
    (testing "returns nil when there are no calls"
      (is (nil? (s/nth-call 5 (s/spy str)))))
    (testing "returns nil when the function passed is not a spy"
      (is (nil? (s/nth-call 5 str))))))

(deftest function-with-no-args
  (let [f (s/spy (fn [] "I have no arguments"))]
    (f)
    (is (s/called-once? f))))

(deftest response-values
  (testing "first response"
    (let [f (s/stub 42)]
      (f)
      (is (= 42 (s/first-response f)))))

  (testing "nth response"
    (let [f (s/spy (fn [x] (+ x 42)))]
      (f 1)
      (f 2)
      (f 3)
      (f 4)
      (is (= 43 (s/first-response f)))
      (is (= 43 (s/nth-response 0 f)))
      (is (= 44 (s/nth-response 1 f)))
      (is (= 45 (s/nth-response 2 f)))
      (is (= 46 (s/nth-response 3 f)))
      (is (= 46 (s/last-response f)))
      (is (= nil (s/nth-response 99 f))))))

(deftest mock-test
  (testing "a mock is just a spy of a function with some behaviour"
    (let [f (s/mock (fn [x] (if (= 1 x)
                              :one
                              :something-else)))]
      (is (= :one (f 1)))
      (is (s/called-once? f))
      (is (= :something-else (f 42))))))
