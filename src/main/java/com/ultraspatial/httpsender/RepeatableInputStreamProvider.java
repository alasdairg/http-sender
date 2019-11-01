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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * A provider that returns an InputStream which returns the same data every time it is called.
 */
public class RepeatableInputStreamProvider implements InputStreamProvider {

   private byte[] data = {};
   private InputStream inputStream;

   /**
    * Construct a provider that will return an InputStream to the specified data every time
    * an InputStream is requested.
    * @param string the underlying data
    * @param charset the charset
    */
   public RepeatableInputStreamProvider(String string, Charset charset) {
      data = string.getBytes(charset);
   }

   /**
    * Construct a provider that will return an InputStream to the specified data every time
    * an InputStream is requested.
    * @param string the underlying data
    */
   public RepeatableInputStreamProvider(String string) {
      this(string, StandardCharsets.UTF_8);
   }

   /**
    * Construct a provider that will return an InputStream to the specified data every time
    * an InputStream is requested.
    * @param bytes the underlying data
    */
   public RepeatableInputStreamProvider(byte[] bytes) {
      data = bytes;
   }

   /**
    * Construct a provider that will return an InputStream to the specified data every time
    * an InputStream is requested.
    * @param is an InputStream that provides the underlying data
    */
   public RepeatableInputStreamProvider(InputStream is) {
      inputStream = is;
   }

   @Override
   public InputStream getInputStream() {
      if (inputStream != null) {
         readBytes();
         inputStream = null;
      } 
      return new ByteArrayInputStream(data);
   }
      
   private void readBytes() {
      try(ByteArrayOutputStream baos = new ByteArrayOutputStream();
          InputStream is = inputStream;) {
         byte[] buffer = new byte[10000];
         int len;
         while ((len = inputStream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
         }
         data = baos.toByteArray();
      } catch (IOException ioe) {
         throw new RuntimeException(ioe);
      }
   }
}
