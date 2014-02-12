(ns plank
  (:require clojure.main
            clojure.core
            clojure.string
            clojure.java.io
            clojure.test))


(def PROJECT-CLJ "project.clj")

(defn display-help-task
  [cmd help]
  (println (format "%-20s %s" cmd help)))

(def default-project-parameters {:dependencies []
                                 :test-paths ["test/"]
                                 :resource-paths ["resources/"]
                                 :source-paths ["src/"]})

(def project (atom {}))

(defn display-help
  [args]
  (println "Plank is a tool for building Clojure projects")
  (println)
  (println "Tasks")
  (display-help-task "help" "Display a help message")
  (display-help-task "test" "Run the tests")
  (display-help-task "jar" "Produce a jar")
  (display-help-task "run" "Run the project"))

(defn load-project
  []
  (clojure.main/load-script PROJECT-CLJ))

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

(defn check-project
  [project]
  true)

(defmacro defproject
  [package version & project-params]
  (let
    [opts (apply hash-map project-params)
     proj {:package package
           :version version
           :options (conj default-project-parameters opts)}]
    `(let []
       (reset! plank/project (quote ~proj))
       ;//(println "project" @plank/project)
       )))

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
      (require (symbol test-path))))
    (clojure.test/run-all-tests))

(defn test-project
  [args]
  (load-project)
  (with-project-class-loader (fn [class-loader]
                               (find-test-paths))))

(defn create-jar
  [args]
  )

(defn init-project
  [args]
  (let [[package-name & _] args
        version "1.0"
        dst (clojure.java.io/file PROJECT-CLJ)
        payload (list
                  'defproject
                  (symbol package-name)
                  version)
        payload (apply list (concat payload (apply concat (seq default-project-parameters))))]
    (spit dst payload)
  ))

(defn run-main
  [args]
  (let [[command & args] args]
    (condp = command
          "run" (run-project args)
          "test" (test-project args)
          "init" (init-project args)
          "help" (display-help args)
          "jar" (create-jar args)
          (display-help args))))

(run-main *command-line-args*)

