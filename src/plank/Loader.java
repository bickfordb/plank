package plank;

import java.net.URLClassLoader;
import java.net.URL;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import java.util.List;


public class Loader extends Object {
  public class LoaderThread extends Thread {
    public Object result;
    public Exception exception;

  }
  private URLClassLoader classLoader;

  public Class rt() throws Exception {
    return classLoader.loadClass("clojure.lang.RT");
  }

  public Loader(List<URL> urls, boolean compile, String compilePath) throws Exception {
    classLoader = new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
    ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
    Thread.currentThread().setContextClassLoader(classLoader);
    try {
      rt();
      setVar("clojure.core", "*use-context-classloader*", new Boolean(false));

      if (compile) {
        setVar("clojure.core", "*compile-files*", new Boolean(true));
        setVar("clojure.core", "*compile-path*", compilePath);
      } else {
        setVar("clojure.core", "*compile-files*", new Boolean(false));
      }
      Object loaderVar = classLoader.loadClass("clojure.lang.Compiler").getField("LOADER").get(null);
      loaderVar.getClass().getMethod("bindRoot", Object.class).invoke(loaderVar, classLoader);
    } finally {
      Thread.currentThread().setContextClassLoader(oldClassLoader);
    }
  }

  public void setVar(String namespace, String symbol, Object value) throws Exception {
    Object var = getVar(namespace, symbol);
    var.getClass().getMethod("bindRoot", Object.class).invoke(var, value);
  }

  public Object getVar(String namespace, String symbol) throws Exception {
    Method varMethod = rt().getMethod("var", String.class, String.class);
    return varMethod.invoke(null, namespace, symbol);
  }

  public Object invoke(String namespace, String symbol, List<Object> args) throws Exception
  {
    final List<Object> args0 = args;
    final String namespace0 = namespace;
    final String symbol0 = symbol;
    LoaderThread th = new LoaderThread() {
      public void run() {
        try {
          Thread.currentThread().setContextClassLoader(classLoader);
          Method varMethod = rt().getMethod("var", String.class, String.class);
          Class[] argTypes = new Class[args0.size()];
          for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = Object.class;
          }
          Object aVar = getVar(namespace0, symbol0);
          result = aVar.getClass().getMethod("invoke", argTypes).invoke(aVar, args0.toArray(new Object[args0.size()]));
        } catch (Exception e) {
          exception = e;
        }
      }
    };
    th.start();
    th.join();
    if (th.exception != null) {
      throw th.exception;
    }
    return th.result;
  }
}
