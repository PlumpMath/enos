(ns enos.core
  (:require [clojure.core.async :as async
             :refer [go go-loop <! >! <!! >!! thread]]))


(def ^:private
  processors
  (delay (-> (Runtime/getRuntime)
             (.availableProcessors))))

(defmacro dochan* [loop-sym take-sym [binding ch] & body]
  `(let [ch# ~ch]
     (~loop-sym []
                (let [v# (~take-sym ch#)]
                  (when-not (nil? v#)
                    (let [~binding v#]
                      ~@body
                      (recur)))))))

(defmacro dochan! [[binding ch] & body]
  `(dochan* go-loop <! [~binding  ~ch] ~@body))

(defmacro dochan!! [[binding ch] & body]
  `(dochan* loop <!! [~binding ~ch] ~@body))

(defn fork
  ([ch]
     (fork ch 2))
  ([ch n]
     (fork ch 2 nil))
  ([ch n buf-or-n]
     (let [m   (async/mult ch)
           chs (repeatedly n #(async/chan buf-or-n))]
       (doseq [ch chs]
         (async/tap m ch))
       chs)))

(defn pmap<
  ([f ch]
     (pmap< f ch (* 2 @processors)))
  ([f ch n]
     (let [[c1 c2] (fork (async/map< #(vector % (promise)) ch) 2 n)]
       (dotimes [_ n]
         (thread
          (dochan! [[v p] c1]
            (deliver p (f v)))))
       (async/map< (comp deref second) c2))))

(defn drain [ch]
  (go-loop [] (<! ch)))

(defmacro pdochan! [n [binding ch] & body]
  `(drain (pmap< (fn [~binding] ~@body :nil) ~ch ~n)))


