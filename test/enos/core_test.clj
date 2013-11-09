(ns enos.core-test
  (:require [clojure.test :refer :all]
            [enos.core :as enos :refer [dochan! dochan!! pmap<]]
            [clojure.core.async :as async :refer [go thread <!! >!! close!]]))


(defn slow [f ms]
  (fn [& args]
    (Thread/sleep ms)
    (apply f args)))

(defn arange
  ([n]
     (arange n nil))
  ([n ms]
     (let [ch (async/chan)]
       (thread
        (dotimes [i n]
          (when ms
            (Thread/sleep ms))
          (>!! ch i))
        (close! ch))
       ch)))

(deftest test-dochan
  (let [a (atom 0)]
    (dochan!! [i (arange 10 10)]
      (swap! a + i))
    (is (= 45 @a))))

(deftest test-pmap
  (let [c-in (arange 20 100)
        c-out (pmap< (slow identity 200) c-in)]
    (let [result (time (<!! (async/into [] c-out)))]
      (is (= (range 20) result)))))


