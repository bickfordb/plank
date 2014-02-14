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
  (clojure.main/load-script PROJECT-CLJ)
  (println "project:" @project)
  )

(defn class-loader-urls
  []
  (map #(java.net.URL. (str "file:" %)) (concat
                                          ;["build/"]
                                          ["/Users/bran/projects/plank/foo/clojure-1.5.1/clojure-1.5.1.jar"]
                                          (:source-paths (:options @project))
                                          (:test-paths (:options @project))
                                          [(System/getenv "CLOJURE_JAR")])))

(defn load-string-in-project
  [s]
  (let [p (plank.Loader. (class-loader-urls) true "project-build/")]
    (.invoke p "clojure.core" "load-string" [s])))

(defn test-eval-project
  [args]
  (load-project)
  (println "result:"
  (load-string-in-project (str '(+ 1 2)))))

(defn with-project-class-loader
  [f]
  (let [class-loader (java.net.URLClassLoader. (into-array java.net.URL (class-loader-urls)))]
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
       (reset! plank/project (quote ~proj)))))

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
    test-paths))

(defn test-project
  [args]
  (load-project)
  (let [test-paths (find-test-paths)
        test-paths (apply vector test-paths)]
    (load-string-in-project
      (str
        `(do
           (doseq [test-path# ~test-paths]
             (require (symbol test-path#)))
           (require 'clojure.test)
           (clojure.test/run-all-tests))))))


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
    (spit dst payload)))

(defn run-main
  [args]
  (println "args" args)
  (let [[command & args] args]
    (condp = command
          "run" (run-project args)
          "test" (test-project args)
          "init" (init-project args)
          "eval" (test-eval-project args)
          "help" (display-help args)
          "jar" (create-jar args)
          (display-help args))))

(run-main *command-line-args*)

