(ns attendify-test-app.test-runner
  (:require
   [doo.runner :refer-macros [doo-tests]]
   [attendify-test-app.core-test]
   [attendify-test-app.common-test]))

(enable-console-print!)

(doo-tests 'attendify-test-app.core-test
           'attendify-test-app.common-test)
