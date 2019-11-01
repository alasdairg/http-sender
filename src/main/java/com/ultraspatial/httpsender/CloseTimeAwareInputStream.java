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
import java.util.function.Consumer;

/**
 * An InputStream that notifies a listener with the current system time when it is closed.
 */
public class CloseTimeAwareInputStream extends InputStream {

   private InputStream wrapped;
   private Consumer<Long> consumer;

   /**
    * Wrap an InputStream with a CloseTimeAwareInputStream
    * @param wrapped an InputStream
    * @param consumer someone to notify with the time when the wrapped stream is closed
    */
   public CloseTimeAwareInputStream(InputStream wrapped, Consumer<Long> consumer) {
      this.wrapped = wrapped;
      this.consumer = consumer;
   }
   
   @Override
   public void close() throws IOException {
      wrapped.close();
      long closeTime = System.currentTimeMillis();
      consumer.accept(closeTime);
   }

   @Override
   public int read() throws IOException {
      return wrapped.read();
   }

   @Override
   public int read(byte[] b) throws IOException {
      return wrapped.read(b);
   }

   @Override
   public int read(byte[] b, int off, int len) throws IOException {
      return wrapped.read(b, off, len);
   }

   @Override
   public long skip(long n) throws IOException {
      return wrapped.skip(n);
   }

   @Override
   public int available() throws IOException {
      return wrapped.available();
   }

   @Override
   public synchronized void mark(int readlimit) {
      wrapped.mark(readlimit);
   }

   @Override
   public synchronized void reset() throws IOException {
      wrapped.reset();
   }

   @Override
   public boolean markSupported() {
      return wrapped.markSupported();
   }
}
