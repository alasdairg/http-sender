/*
 *    888    888 888    888              .d8888b.                         888
 *    888    888 888    888             d88P  Y88b                        888
 *    888    888 888    888             Y88b.                             888
 *    8888888888 888888 888888 88888b.   "Y888b.    .d88b.  88888b.   .d88888  .d88b.  888d888
 *    888    888 888    888    888 "88b     "Y88b. d8P  Y8b 888 "88b d88" 888 d8P  Y8b 888P"
 *    888    888 888    888    888  888       "888 88888888 888  888 888  888 88888888 888
 *    888    888 Y88b.  Y88b.  888 d88P Y88b  d88P Y8b.     888  888 Y88b 888 Y8b.     888
 *    888    888  "Y888  "Y888 88888P"   "Y8888P"   "Y8888  888  888  "Y88888  "Y8888  888
 *                             888
 *                             888
 *                             888
 *
 * Copyright 2017 Alasdair Gilmour
 * -------------------------------
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Apache License v2.0 which accompanies this distribution.
 *
 *     The Eclipse Public License is available at
 *     http://www.eclipse.org/legal/epl-v10.html
 *
 *     The Apache License v2.0 is available at
 *     http://www.opensource.org/licenses/apache2.0.php
 *
 * You may elect to redistribute this code under either of these licenses.
 */

package com.ultraspatial.httpsender;

import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.PasswordAuthentication;
import java.net.ProtocolException;
import java.net.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Logger;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;

/**
 * A single configurable HTTP Request of any type. Follows the Builder pattern to allow chaining
 * of calls to configuration methods.
 * 
 * @author Alasdair Gilmour
 */
public abstract class IndividualRequest<T extends IndividualRequest<?>> implements Request {

   private static Logger log = Logger.getLogger(IndividualRequest.class.getName());
   private static final HttpURLConnectionFactory INTERNAL_FACTORY = new InternalConnectionFactory();
   protected URL url;
   boolean trustAll = false;
   Proxy proxy;
   boolean followRedirects = true;
   int timeout = 30000;
   ClientCerts clientCerts;
   Map<String, HeaderValues> headers = new HashMap<>();
   Map<String, List<String>> queryParams = new HashMap<>();
   Map<String, String> placeholders = new HashMap<>();
   private HttpURLConnectionFactory connectionFactory = INTERNAL_FACTORY;

   /**
    * Create a request
    * @param url the request url
    */
   IndividualRequest(String url) {
      try {
         this.url = new URL(url);
      } catch (MalformedURLException e) {
         throw new RuntimeException(e);
      }
   }

   protected abstract T me();
   
   /**
    * Copy constructor. Construct a new instance identical to the one supplied
    * @param source the request to copy
    */
   IndividualRequest(T source) {
      this.url = source.url;
      this.trustAll = source.trustAll;
      this.proxy = source.proxy;
      this.followRedirects = source.followRedirects;
      this.headers = new HashMap<>(source.headers);
      this.queryParams = new HashMap<>(source.queryParams);
      this.placeholders = new HashMap<>(source.placeholders);
      this.timeout = source.timeout;
      this.clientCerts = source.clientCerts;
   }

   /**
    * @return the url of this request
    */
   public URL url() {
      return url;
   }
   
   /**
    * Set one or more values for the named HTTP header
    * @param name the header name
    * @param values one or more values for the header
    * @return this (Builder pattern)
    */
   public T header(String name, String... values) {
      String lookup = name.toLowerCase();
      HeaderValues hv = headers.get(lookup);
      if (hv == null) {
         hv = new HeaderValues(name);
         headers.put(lookup, hv);
      }
      for (String str : values) {
         hv.add(str);
      }
      return me();
   }

   /**
    * @return the headers in this request
    */
   public Map<String, HeaderValues> getHeaders() {
      return new HashMap<>(headers);
   }
   
   /**
    * Set a value that will be substituted for the named placeholder, wherever it appears in the
    * the URL (including query parameters) or request headers. A Placeholder is denoted by a name inside 
    * curly brackets {}.
    * @param name the name of the placeholder
    * @param value the value to be substituted
    * @return this (Builder pattern)
    */
   public T placeholder(String name, String value) {
      if (name.contains("{") || name.contains("}") || value.contains("{") || value.contains("}")) {
         throw new IllegalArgumentException("Placholders may not contain curly brace {} characters");
      }
      placeholders.put(name, value);
      return me();
   }

   /**
    * @param name the placeholder name
    * @return the value associated with this placeholder
    */
   public String getPlaceholder(String name) {
      return placeholders.get(name);
   }

   /**
    * @return then placeholder values
    */
   public Map<String, String> getPlaceholders() {
      return new HashMap<>(placeholders);
   }

   /**
    * Clear all placeholder values.
    * @return this (Builder pattern)
    */
   public T clearPlaceholders() {
      placeholders.clear();
      return me();
   }
   
   /**
    * Set a proxy to be used for the connection, using the provided user name
    * and password to authenticate to it.
    * @param proxy the proxy to use
    * @param user the user name 
    * @param password the password
    * @return this (Builder pattern)
    */
   public T proxy(Proxy proxy, String user, String password) {
      this.proxy = proxy;
      Authenticator authenticator = new Authenticator() {
         public PasswordAuthentication getPasswordAuthentication() {
            return (new PasswordAuthentication(user, password.toCharArray()));
         }
      };
      Authenticator.setDefault(authenticator);
      return me();
   }

   /**
    * Set a proxy to be used for the connection
    * @param proxy the proxy to use
    * @return this (Builder pattern)
    */
   public T proxy(Proxy proxy) {
      this.proxy = proxy;
      return me();
   }

   /**
    * @return the proxy set for this request
    */
   public Proxy proxy() {
      return proxy;
   }
   
   /**
    * If the connection is over HTTPS, do not attempt to validate the server's certificate. 
    * This can be useful in development environments when using self-signed certificates
    * @return this (Builder pattern)
    */
   public T trustAll(boolean trust) {
      trustAll = trust;
      return me();
   }

   public boolean getTrustAll() {
      return trustAll;
   }
   
   /**
    * If the server responds with a redirect (30X) response, this setting
    * determines whether to follow the redirect by making another
    * request or just return the redirect response to the caller
    * The default behaviour is to follow redirects (true)
    * @param follow true to follow, false otherwise
    * @return this (Builder pattern)
    */
   public T followRedirects(boolean follow) {
      this.followRedirects = follow;
      return me();
   }

   /**
    * @return whether this request is set to follow redirects or not.
    */
   public boolean getFollowRedirects() {
      return followRedirects;
   }
   
   /**
    * Set one or more values for the named query parameter. Query parameters specified this way will
    * be merged with any already present in the base provided URL
    * @param name the query parameter name
    * @param values one or more values for the query parameter
    * @return this (Builder pattern)
    */
   public T queryParam(String name, String ... values) {
      List<String> list = queryParams.get(name);
      if (list == null) {
         list = new ArrayList<>();
         queryParams.put(name.trim(), list);
      }
      for (String str : values) {
         list.add(str.trim());
      }
      return me();
   }

   /**
    * @return the query parameters set for this request
    */
   public Map<String, List<String>> getQueryParams() {
      return new HashMap<>(queryParams);
   }

   /**
    * Clear all query parameters.
    * @return this (Builder pattern)
    */
   public T clearQueryParams() {
      queryParams.clear();
      return me();
   }
   
   /**
    * Set the timeout in milliseconds to use when attempting to connect to a server
    * The default value is 30000. 
    * @param timeout the timeout in milliseconds
    * @return this (Builder pattern)
    */
   public T timeout(int timeout) {
      this.timeout = timeout;
      return me();
   }

   /**
    * @return the timeout set for this request.
    */
   public long getTimeout() {
      return timeout;
   }

   /**
    * For an Https request, specifies a set of one or more client-side certificates that the
    *  system can use to satisfy a client certificate request by the remote server during a TLS
    *  handshake.
    * @param cert client certificates
    * @return this (Builder pattern)
    */
   public T useClientCerts(ClientCerts cert) {
      this.clientCerts = cert;
      return me();
   }   
   
   /**
    * {@inheritDoc}
    */
   @Override
   public Response execute() {
      long start = System.currentTimeMillis();
      HttpURLConnection conn = null;
      try {
         URL assembledUrl = new URL(assembleUrl());
         log.fine("Requesting " + assembledUrl.toString());
         conn = connectionFactory.build(assembledUrl, proxy);
         preRequestConfig(conn);
         if (conn instanceof HttpsURLConnection) {
            preRequestConfigHttps((HttpsURLConnection) conn);
         }
         preObtainResponse(conn);
         return new Response(this, conn, start);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
   
   /**
    * {@inheritDoc}
    */
   @Override
   public void executeAsync(Consumer<Response> consumer, Consumer<Throwable> error) {
      executeAsync(consumer, error, Executors.defaultExecutor());
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void executeAsync(Consumer<Response> consumer, Consumer<Throwable> error, Executor executor) {
      executor.execute(() -> {
         try {
            Response response = this.execute();
            consumer.accept(response);
         } catch (Throwable thrown) {
            error.accept(thrown);
         }
      });
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public CompletableFuture<Response> executeAsync(Executor executor) {
      CompletableFuture<Response> cf = new CompletableFuture<>();
      executor.execute(() -> {
         try {
            Response response = this.execute();
            cf.complete(response);
         } catch (Throwable thrown) {
            cf.completeExceptionally(thrown);
         }
         
      });
      return cf;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public CompletableFuture<Response> executeAsync() {
      return executeAsync(Executors.defaultExecutor());
   }

   private void preRequestConfig(HttpURLConnection conn) throws ProtocolException {
      conn.setRequestMethod(getMethodName());
      setHeaderValues(conn);
      conn.setInstanceFollowRedirects(followRedirects);
      conn.setConnectTimeout(timeout);
   }

   private void setHeaderValues(HttpURLConnection conn) {
      for (HeaderValues hv : headers.values()) {
         StringBuilder sb = new StringBuilder();
         boolean comma = false;
         for (String item : hv.getValues()) {
            if (comma) {
               sb.append(", ");
            }
            sb.append(item);
            comma = true;
         }
         conn.setRequestProperty(processPlaceholders(hv.getName()), processPlaceholders(sb.toString()));
      }
   }
   
   private void preRequestConfigHttps(HttpsURLConnection conn) {
      if (trustAll) {
         conn.setHostnameVerifier(new HostnameVerifier() {
            public boolean verify(String name, SSLSession session) {
               return true;
            }
         });
      }
      if (clientCerts != null) {
         SSLSocketFactory factory = clientCerts.getSocketFactory();
         if (factory != null) {
            conn.setSSLSocketFactory(factory);
         }
      }
   }

   private String assembleUrl() {
      String protocol = url.getProtocol();
      int port = url.getPort();
      String host = processPlaceholders(url.getHost());
      String path = processPlaceholders(url.getPath());
      String query = prepareQuery(url.getQuery());
      query = processPlaceholders(query);

      StringBuilder urlBuilder = new StringBuilder();
      urlBuilder.append(protocol);
      urlBuilder.append("://");
      urlBuilder.append(host);
      if (port >= 0) {
         urlBuilder.append(':');
         urlBuilder.append(port);
      }
      urlBuilder.append(path);
      if (!query.isEmpty()) {
         urlBuilder.append('?');
         urlBuilder.append(query);
      }
      return urlBuilder.toString();
   }

   private String prepareQuery(String initial) {
      StringBuilder queryBuilder = new StringBuilder();
      boolean and = false;
      if (initial != null) {
         queryBuilder.append(initial);
         and = true;
      }
      for (Map.Entry<String, List<String>> entry : queryParams.entrySet()) {
         for (String str : entry.getValue()) {
            if (and) {
               queryBuilder.append('&');
            }
            queryBuilder.append(entry.getKey());
            queryBuilder.append('=');
            queryBuilder.append(str);
            and = true;
         }
      }
      return queryBuilder.toString();
   }

   protected String processPlaceholders(String str) {
      StringBuilder pathBuilder = new StringBuilder();
      if (str != null) {
         pathBuilder.append(str);
      }
      for (Map.Entry<String, String> entry : placeholders.entrySet()) {
         String key = "{" + entry.getKey() + "}";
         int idx = -1;
         do {
            if (idx < 0) {
               idx = pathBuilder.indexOf(key);
            } else {
               idx = pathBuilder.indexOf(key, idx);
            }
            if (idx >= 0) {
               pathBuilder.replace(idx, idx + key.length(), entry.getValue());
            }
         } while (idx >= 0);
      }
      return pathBuilder.toString();
   }

   protected void preObtainResponse(HttpURLConnection conn) throws Exception {
   }

   @Override
   public String toString() {
      return getMethodName() + " " + url;
   }

   /**
    * @return the HTTP verb for this request type.
    */
   public String getMethodName() {
      return this.getClass().getSimpleName().toUpperCase();
   }

   /**
    * @return an independent copy of this request
    */
   public abstract IndividualRequest<?> copy();
   
   //For testing purposes - package private
   void setConnectionFactory(HttpURLConnectionFactory factory) {
      if (factory == null) {
         throw new IllegalArgumentException("Factory may not be null");
      }
      connectionFactory = factory;
   }
   
   private static class InternalConnectionFactory implements HttpURLConnectionFactory {

      @Override
      public HttpURLConnection build(URL url, java.net.Proxy proxy) throws Exception {
         if (proxy != null) {
            return (HttpURLConnection) url.openConnection(proxy);
         } else {
            return (HttpURLConnection) url.openConnection();
         }
      }
   }
}
