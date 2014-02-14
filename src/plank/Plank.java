package plank;

import clojure.lang.RT;
import clojure.lang.Var;

public class Plank extends Object {
  public static void main(String[] args) throws java.io.IOException {
    System.out.println("plank!");
    RT.loadResourceScript("plank.clj");
  }
}

