(ns codeitlater.core
  (:require [clojure.java.io :as io]
            [clojure.data.json :as json]
            )
  (:gen-class))

(defn read-json
  ([]
   (json/read-str (slurp "./src/codeitlater/comments.json")))
  ([path]
   (json/read-str (slurp (str path "/comments.json")))))


;; This regex expression get help from: https://stackoverflow.com/questions/45848999/clojure-regex-delete-whitespace-in-pattern
(defn make-pattern [commentMark]
  (re-pattern (str commentMark "+:=\\s*" "(.+)")))


(defn read-comments-inline [commentMark line]
  (let [result (re-find (make-pattern commentMark) line)]
    (second result))) 


(defn read-comments-in-file [filepath commentMark]
  (with-open [codefile (io/reader filepath)]
    (let [count (atom 0)
          pickcomment (partial read-comments-inline commentMark)]
      (for [thisline (doall (line-seq codefile))
            :let [comment (pickcomment thisline)
                  lineNum (swap! count inc)] ;;:= atom?
            :when comment]
        (list lineNum comment))
      )))


(defn get-all-files
  "return a lazy sequence including all files"
  ([]
   (map #(.getPath %) (file-seq (io/file "."))))
  ([root]
   (map #(.getPath %) (file-seq (io/file root))))
  )


;;(partial read-comments-inline commentMark)
(defn read-files
  "Read all files depend on root path and file types, find comments inside and return."
  ([commentDict]
   (doall (for [filepath (get-all-files)
                :when (not (.isDirectory (io/file filepath)))
                :let [mark (get commentDict (re-find #"(?<=\w)\.\w+$" filepath))]
                :when mark]
            (conj (read-comments-in-file filepath mark)
                  filepath))))
  ([commentDict root]
   (doall (for [filepath (get-all-files root)
                :when (not (.isDirectory (io/file filepath)))
                :let [mark (get commentDict (re-find #"(?<=\w)\.\w+$" filepath))]
                :when mark]
            (conj (read-comments-in-file filepath mark)
                  filepath))))
  ([commentDict root filetypes]
   (let [typepatterns (for [filetype filetypes
                            :when (not= "" filetype)]
                        (list (re-pattern (str ".+" filetype "$"))
                              (str "." filetype)))]
     (doall (for [filepath (get-all-files root)
                  :when (not (.isDirectory (io/file filepath)))
                  typepattern typepatterns
                  :when (re-matches (first typepattern) filepath)
                  :let [mark (get commentDict (re-find #"(?<=\w)\.\w+$" filepath))]
                  :when mark]
              (conj (read-comments-in-file filepath (get commentDict (last typepattern)))
                    filepath))))))

;;:= TODO: make diferent behavior of deffirent args.
;;:= TODO: make tags options, for example, -json file.json
(defn -main [& args]
  (let [commentDict (read-json)
        [root & filetypes] args]
    ;(println commentDict)
    ;(println args)
    ;(println root)
    ;(println filetypes)
    (cond filetypes (println (read-files commentDict root filetypes))
          root (println (read-files commentDict root))
          :else (println (read-files commentDict))))
)
