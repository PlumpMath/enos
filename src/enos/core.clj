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

(defmacro dochan!
  "Asynchronously execute the body for each value in the channel,
  extracting with <!."
  [[binding ch] & body]
  `(dochan* go-loop <! [~binding  ~ch] ~@body))

(defmacro dochan!!
  "Synchronously execute the body for each value in the channel,
  extracting with <!!. Will block until the channel is closed."
  [[binding ch] & body]
  `(dochan* loop <!! [~binding ~ch] ~@body))

(defn fork
  "Return two or more new channels that tap the given channel."
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
  "Parallel map over the input channel. Executes `n` threads that
  apply the given function to values from the channel. Returns a new
  channel containing the return valus of `f`, in the same order as the
  input channel."
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
  "Consume and discard all values in the channel."
  (go-loop [] (<! ch)))

(defmacro pdochan! [n [binding ch] & body]
  "WIP - Execute the body in `n` threads."
  `(drain (pmap< (fn [~binding] ~@body :nil) ~ch ~n)))


