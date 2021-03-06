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

package com.ultraspatial.httpsender.fallback;

/**
 * When using a FallbackRequest, the RetryStrategy determines whether or not a 
 * request should be retried
 * 
 * @author Alasdair Gilmour
 */
public interface RetryStrategy {

   /**
    * Decide if a request should be retried after the specified number of unsuccessful
    * attempts
    * @param attemptNo the attempt number
    * @return true to retry the request, false to stop
    */
   boolean shouldRetry(int attemptNo);

   /**
    * Keep retrying the request until it succeeds. 
    * @return a RetryStrategy that retries until successful
    */
   public static RetryStrategy forever() {
      return x -> true;
   }

   /**
    * Retry the request until there have been 'max' failed attempts
    * @param max the maximum number of unsuccessful attempts
    * @return a RetryStrategy that retries until there have been 'max' failed attempts
    */
   public static RetryStrategy maxTotalTries(final int max) {
      return attemptNo -> attemptNo < max;
   }
}
