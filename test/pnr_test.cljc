(ns pnr-test
  (:require [clojure.test :refer [deftest is testing]]
            [pnr]))
(deftest namespace-loads
  (testing "the restored CLJC namespace loads"
    (is (some? pnr))))
