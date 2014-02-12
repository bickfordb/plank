(ns plank
  (:require clojure.main
            clojure.core
            ))

(defn display-help-task
  [cmd help]
  (println (format "%-20s %s" cmd help)))

(def default-project
  {:title ""
   :description ""
   :version ""
   :dependencies []
   :source-paths ["src"]})

(def classloader-urls
  (map #(.toURL (.toURI (java.io.File. %))) ["build/" "src/" (System/getenv "CLOJURE_JAR")]))

(println classloader-urls)

(def class-loader (java.net.URLClassLoader. (into-array java.net.URL classloader-urls)
                                            ))

(println "resource" (.getResource class-loader "foo.clj"))

(def clojure-rt (. class-loader loadClass "clojure.lang.RT"))

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

(defmacro with-project-compiler
  [& body]
  `(with-bindings {#'*compile-path* "./build"
                   #'*compile-files* true
                   #'*use-context-classloader* false
                   clojure.lang.Compiler/LOADER class-loader}
    ~@body
    ))

(defn build-project
  [path]
  (with-project-compiler
    (let [s (namespace (symbol path))
          params (into-array java.lang.Class [(.loadClass class-loader "java.lang.String")])
          load (. clojure-rt getMethod "load" params)]
      (.invoke load nil (into-array java.lang.Object [s])))))

(def project (atom {}))

(defn parse-project-dependencies
  [dependencies]
  (apply vector (for [[k v] dependencies] {:path [(namespace k) (name k)] :version v})))

(defn parse-project-params
  [params]
  (apply conj
         {}
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
  (with-project-compiler
    (-> path symbol namespace symbol require)
    ((-> path symbol eval))))

(defn test-project
  []
  (println "running tests"))

(defn run-main
  [args]
  (let [[command & args] args]
    (condp = command
          "run" (run-project args)
          "test" (test-project)
          "help" (display-help)
          (display-help))))

(run-main *command-line-args*)

