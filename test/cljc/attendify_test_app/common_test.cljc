(ns attendify-test-app.common-test
  #? (:cljs (:require-macros [cljs.test :refer (is deftest testing)]))
  (:require [attendify-test-app.common :as sut]
            #?(:clj [clojure.test :refer :all]
               :cljs [cljs.test])))

(deftest example-passing-test-cljc
  (is (= 1 1)))
