package com.ultraspatial.httpsender;

import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.URL;
import java.util.function.Consumer;

public class TestingHttpUrlConnectionFactory implements HttpURLConnectionFactory {

   private HttpURLConnection connection;
   private Consumer<URL> urlChecker;
   
   public TestingHttpUrlConnectionFactory(HttpURLConnection connection) {
      this(connection, x -> {});
   }
   
   public TestingHttpUrlConnectionFactory(HttpURLConnection connection, Consumer<URL> urlChecker) {
      this.connection = connection;
      this.urlChecker = urlChecker;
   }
   
   @Override
   public HttpURLConnection build(URL url, Proxy proxy) throws Exception {
      urlChecker.accept(url);
      return connection;
   }

   
}
