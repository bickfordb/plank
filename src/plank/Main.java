package plank;

import clojure.lang.RT;
import clojure.lang.Var;

public class Main extends Object {
  public static void main(String[] args) throws java.io.IOException {
    System.out.println("plank!");
    RT.var("clojure.core", "*use-context-classloader*").bindRoot(new Boolean(false));
    RT.var("clojure.core", "*command-line-args*").bindRoot(java.util.Arrays.asList(args));
    RT.loadResourceScript("plank/main.clj");
  }
}

