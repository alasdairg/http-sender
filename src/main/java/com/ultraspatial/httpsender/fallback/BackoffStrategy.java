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
 * When using a FallbackRequest, the BackoffStrategy determines how long
 * to wait before retrying a request
 * 
 * @author Alasdair Gilmour
 */
public interface BackoffStrategy {

   /**
    * Return the number of milliseconds to wait before retrying a request
    * @param attemptNo the number of the attempt
    * @return the backoff time in milliseconds for the specified attempt
    */
   long getBackoff(int attemptNo);

   /**
    * Simple backoff strategy that backs off for the specified number of milliseconds for each attempt.
    * 
    * For example, specified(1000, 2000, 3000) would backoff for 1 second after the first attempt,
    * 2 seconds after the second attempt, and 3 seconds after the third and subsequent attempts.
    * @param ms the number of milliseconds to back off for each attempt
    * @return a BackoffStrategy that backs off for the specified time in milliseconds corresponding to
    * the attempt number
    */
   public static BackoffStrategy specified(long ... ms) {
      return attemptNo -> {
         if (attemptNo < 1 || ms == null || ms.length == 0) {
            return 0L;
         }
         int idx = attemptNo - 1;
         if (attemptNo > ms.length) {
            idx = ms.length - 1;
         }
         return ms[idx];
      };
   }

   /**
    * Retry immediately
    * @return a BackoffStrategy that backs off for 0 miliiseconds
    */
   public static BackoffStrategy none() {
      return attemptNo -> 0L;
   }
}
