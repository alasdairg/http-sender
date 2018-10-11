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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.nio.charset.Charset;

@SuppressWarnings("unchecked")
public abstract class WriteRequest<T extends WriteRequest<?>> extends IndividualRequest<T> {

   private InputStreamProvider inputStreamProvider;
   
   public WriteRequest(String url) {
      super(url);
      inputStreamProvider = new RepeatableInputStreamProvider(new byte[] {});
   }
   
   protected WriteRequest(T source) {
      super(source);
   }
   
   @Override
   protected void preObtainResponse(HttpURLConnection conn) throws Exception {
      conn.setDoOutput(true);
      writeRequestBody(conn);
   }
   
   public T requestBody(String body) {
      inputStreamProvider = new RepeatableInputStreamProvider(body);
      return (T) this;
   }
   
   public T requestBody(byte[] bytes) {
      inputStreamProvider = new RepeatableInputStreamProvider(bytes);
      return (T) this;
   }
   
   public T requestBody(InputStream body) {
      inputStreamProvider = new RepeatableInputStreamProvider(body);
      return (T) this;
   }
   
   public T requestBody(InputStreamProvider provider) {
      inputStreamProvider = provider;
      return (T) this;
   }
   
   public T contentType(String type) {
      header("Content-Type", type);
      return (T) this;
   }
   
   public T contentType(String type, Charset charset) {
      header("Content-Type", type + "; charset=" + charset.name());
      return (T) this;
   }
   
   private void writeRequestBody(HttpURLConnection conn) throws IOException {
      
      try(OutputStream os = conn.getOutputStream();
          InputStream is = inputStreamProvider.getInputStream();) {
         byte[] buffer = new byte[10000];
         int len;
         while ((len = is.read(buffer)) != -1) {
            os.write(buffer, 0, len);
         }
      }
   }
}
