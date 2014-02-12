(ns plank
  (:require clojure.main
            clojure.core
            clojure.string
            clojure.java.io
            clojure.test))

(def ^:dynamic *class-loader* nil)

(defn display-help-task
  [cmd help]
  (println (format "%-20s %s" cmd help)))

(def project (atom {:title ""
                    :description ""
                    :version ""
                    :dependencies []
                    :test-paths ["test/"]
                    :source-paths ["src/"]}))

(defn display-help
  []
  (println "Plank is a tool for building Clojure projects")
  (println)
  (println "Tasks")
  (display-help-task "help" "Display a help message")
  (display-help-task "run" "Run the project"))

(defn load-project
  []
  (clojure.main/load-script "project.clj"))

(defn with-project-class-loader
  [f]
  (let [classloader-paths (concat ["build/"]
                                  (:source-paths (:options @project))
                                  (:test-paths (:options @project))
                                  [(System/getenv "CLOJURE_JAR")])
         classloader-urls (map #(java.net.URL. (str "file:" %)) classloader-paths)
         class-loader (java.net.URLClassLoader. (into-array java.net.URL classloader-urls))]
      (with-bindings {#'*compile-path* "./build"
                      #'*compile-files* true
                      #'*use-context-classloader* false
                      clojure.lang.Compiler/LOADER class-loader}
        (f class-loader))))

;(defn build-project
;  [path]
;  (with-project-class-loader
;    (fn [class-loader] (let [s (namespace (symbol path))
;                 params (into-array java.lang.Class [(.loadClass class-loader "java.lang.String")])
;                 load (. (. class-loader loadClass "clojure.lang.RT") getMethod "load" params)]
;             (.invoke load nil (into-array java.lang.Object [s]))))))


(defn parse-project-dependencies
  [dependencies]
  (apply vector (for [[k v] dependencies] {:path [(namespace k) (name k)] :version v})))

(defn parse-project-params
  [params]
  (apply conj
         @project
         (for [[k v] (apply hash-map params)]
           (condp = k
             :main {:namespace (namespace v) :name (name v)}
             :dependencies {:dependencies (parse-project-dependencies v)}
             nil))))

(defmacro defproject
  [project-name project-description & project-params]
  (let
    [project-name (name project-name)
     project-opts (plank/parse-project-params project-params)]
    `(let []
       (reset! plank/project {:title ~project-name
                              :description ~project-description
                              :options ~project-opts})
       (println "defproject:" @plank/project))))

(defn run-project
  [[path & args]]
  (load-project)
  (with-project-class-loader
    (fn [classloader]
      (-> path symbol namespace symbol require)
      ((-> path symbol eval)))))


(defn path->module
  [subpath]
  (clojure.string/replace (clojure.string/replace subpath #"[/]" ".") #".clj$" ""))

(defn find-test-paths-in-test-root
  [test-root]
  (let [test-root-file  (clojure.java.io/file test-root)
        test-root-path (.getName test-root-file)]
    (for [test-file (file-seq test-root-file)
          :when (and
                  (.isFile test-file)
                  (.endsWith (.getName test-file) ".clj"))]
      (let [path (.getPath test-file)
          subpath (clojure.string/replace path #"[^/]+[/](.+)$" "$1")]
        (path->module subpath)))))

(defn find-test-paths
  []
  (let [test-roots (:test-paths (:options @project))
        test-paths (reduce concat (map find-test-paths-in-test-root test-roots))]
    (doseq [test-path test-paths]
      (println "found test path" (symbol test-path))
      (require (symbol test-path))
      ))
    (clojure.test/run-all-tests))

(defn test-project
  []
  (load-project)
  (with-project-class-loader (fn [class-loader]
                               (find-test-paths)

                               )))

(defn run-main
  [args]
  (let [[command & args] args]
    (condp = command
          "run" (run-project args)
          "test" (test-project)
          "help" (display-help)
          (display-help))))

(run-main *command-line-args*)

