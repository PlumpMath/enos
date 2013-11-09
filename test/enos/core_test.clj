(ns enos.core-test
  (:require [clojure.test :refer :all]
            [enos.core :as enos :refer [dochan! dochan!! pmap< chan->seq pause!!]]
            [clojure.core.async :as async :refer [go thread <!! >!! close!]]))


(defn slow
  "... and low, that is the tempo"
  [f ms]
  (if (and ms (pos? ms))
    (fn [& args]
      (pause!! ms)
      (apply f args))
    f))

(defn arange
  ([n]
     (arange n nil))
  ([n ms]
     (let [ch (async/chan 10)
           f  (slow identity ms)]
       (thread
        (dotimes [i n]
          (>!! ch (f i)))
        (close! ch))
       ch)))

(deftest test-dochan
  (testing "double-bang dochan executes synchronously"
    (let [a (atom 0)]
      (dochan!! [i (arange 10 10)]
        (swap! a + i))
      (is (= 45 @a))))
  
  (testing "single-bang dochan executes asyncronously"
    (let [a (atom 0)]
      (dochan! [i (arange 10 10)]
        (swap! a + i))
      ;; First iteration of the body is delayed 10 ms, so still 0
      (is (zero? @a))
      (pause!! 150)
      ;; Give the body time to finish executing (plus a generous
      ;; amount, else this gets flakey).
      (is (= 45 @a)))))

;;; WTF??!?!?
#_(deftest test-fork
  (let [c (arange 10)
        [c1 c2 c3] (map chan->seq (enos/fork c 3))]
    (async/close! c)
    (is (= (range 10) c1 c2 c3))))

(deftest test-pmap
  (let [c-in (arange 20 100)
        c-out (pmap< (slow identity 200) c-in)]
    (let [result (time (<!! (async/into [] c-out)))]
      (is (= (range 20) result)))))

(deftest test-pmap-retains-order
  (let [in-ch (arange 20 100)
        f     (fn [i]
                ;; Blocking function that is usually a bit slow, but
                ;; sometimes very slow
                (pause!! (if (#{1 4 5 11 15} i) 400 50))
                i)]
    (is (= (range 20) (chan->seq (pmap< f in-ch))))))

(deftest test-lazy-seq
  (is (= (range 10) (chan->seq (arange 10))))
  (let [c (arange 10 100)]
    (pause!! 310)
    ;; c should contain [0 1 2] by now...
    (is (= (range 3) (chan->seq c 50)))
    ))

;; Local Variables:
;; mode: clojure
;; eval: (define-clojure-indent (dochan! (quote defun))
;;                              (dochan!! (quote defun)))
;; End:
