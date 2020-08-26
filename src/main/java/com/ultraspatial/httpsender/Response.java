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

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A Response to an  Http Request
 */
public class Response implements Closeable {

   private IndividualRequest<?> request;
   private HttpURLConnection conn;
   private Map<String, HeaderValues> headers = new HashMap<>();
   private InputStream responseBodyStream = new ByteArrayInputStream(new byte[] {});
   private int responseCode;
   private String responseStatus;
   private long start = -1L;
   private long finish = -1L;
   
   Response(IndividualRequest<?> request, HttpURLConnection conn, long start) {
      try {
         this.request = request.copy();
         this.conn = conn;
         this.start = start;
         responseCode = conn.getResponseCode();
         InputStream errorStream = conn.getErrorStream();
         if (errorStream != null) {
            responseBodyStream = new CloseTimeAwareInputStream(errorStream, end -> {finish = end;});
         } else {
            InputStream is = conn.getInputStream();
            if (is != null) {
               responseBodyStream = new CloseTimeAwareInputStream(is, end -> {finish = end;});
            } else {
               finish = System.currentTimeMillis();
            }
         }
         populateHeaders(conn.getHeaderFields());
         responseStatus = conn.getResponseMessage();
      } catch (IOException ioe) {
         throw new RuntimeException(ioe);
      }
   }

   private void populateHeaders(Map<String, List<String>> headerFields) {
      for (Map.Entry<String, List<String>> entry: headerFields.entrySet()) {
         HeaderValues hv = new HeaderValues(entry.getKey());
         for (String str: entry.getValue()) {
            hv.add(str);
         }
         String lookup = entry != null ? null : entry.getKey().toLowerCase();
         headers.put(lookup, hv);
      }
   }

   /**
    * @return a copy of the request that elicited this Response.
    */
   public IndividualRequest<?> getRequest() {
      return this.request;
   }
   
   /**
    * @return the Response headers
    */
   public HeaderValues getHeaders(String name) {
      return (headers.get(name.toLowerCase()));
   }
   
   /**
    * Read the entire Response as a String. If the response specified a charset then this will be used. 
    * Otherwise a default of UTF8 will be assumed. The response InputStream will be closed by this call.
    * @return the response body as a String
    */
   public String bodyAsString() {
      return bodyAsString(detectCharset());
   }
   
   /**
    * Read the entire Response as a String, using the specified charset. The response InputStream will 
    * be closed by this call.
    * @param charset
    * the charset to use
    * @return the response body as a String
    */
   public String bodyAsString(Charset charset) {
      try {
         return new String(readBodyFully(), charset);
      } catch (IOException e) {
         throw new RuntimeException(e);
      }
   }
   
   /**
    * @return the InputStream for the Response.
    */
   public InputStream bodyAsStream() {
      return responseBodyStream;
   }
   
   /**
    * @return the InputStream for the response wrapped with a Reader
    */
   public Reader bodyAsReader() {
      return new InputStreamReader(responseBodyStream);
   }
   
   /**
    * @return the HTTP status code of the response.
    */
   public int getResponseCode() {
      return responseCode;
   }
   
   /**
    * @return the response's status message
    */
   public String getResonseStatus() {
      return responseStatus;
   }
   
   /**
    * @return how long the request took, from initial connection to the closing of the response InputStream.
    */
   public long getElapsed() {
      if (finish < 0) {
         return System.currentTimeMillis() - start;
      }
      return finish - start;
   }
   
   /**
    * @return whether or not the Response is complete (i.e. its InputStream has been closed)
    */
   public boolean isComplete() {
      return finish >= 0;
   }
   
   private byte[] readBodyFully() throws IOException {
      return IOUtil.readStreamFully(responseBodyStream);
   }
   
   private Charset detectCharset() {
      HeaderValues hv = getHeaders("Content-Type");
      String contentType = null;
      if (hv != null && hv.getValues().size() > 0) {
         contentType = hv.getValues().get(0);
      }
      if (contentType != null && !contentType.isEmpty()) {
         String[] components = contentType.split(";");
         for (String item: components) {
            item = item.trim();
            if (item.toLowerCase().startsWith("charset")) {
               String[] keyAndValue = item.split("=");
               if (keyAndValue.length == 2) {
                  String value = keyAndValue[1].trim();
                  if (value.length() > 0) {
                     if (value.endsWith(";")) {
                        value = value.substring(0, value.length() - 1).trim();
                     }
                     try {
                        return Charset.forName(value);
                     } catch(UnsupportedCharsetException e) {
                     }
                  }
               }
            }
         }
      }
      return StandardCharsets.UTF_8;
   }

   /**
    * Close the underlying InputStream and clean up any resources.
    */
   @Override
   public void close() throws IOException {
      if (responseBodyStream != null) {
         try {   
            responseBodyStream.close();
         } catch(IOException ioe) {
         }
      }
      if (conn != null) {
         conn.disconnect();
      }
   }
}
