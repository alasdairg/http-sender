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

import com.ultraspatial.httpsender.Request;

/**
 * Holder for a Request and its associated fallback options.
 */
class RequestHolder {
   private Request request;
   private boolean retryOnErrorResponse;
   private RetryStrategy retryStrategy;
   private BackoffStrategy backoffStrategy;

   public RequestHolder(Request request, boolean retryOnErrorResponse, RetryStrategy retryStrategy, BackoffStrategy backoffStrategy) {
      this.request = request;
      this.retryOnErrorResponse = retryOnErrorResponse;
      this.retryStrategy = retryStrategy;
      this.backoffStrategy = backoffStrategy;

   }

   public RequestHolder(Request request, boolean retryOnErrorResponse, RetryStrategy retryStrategy) {
      this(request, retryOnErrorResponse, retryStrategy, BackoffStrategy.none());
   }

   public RequestHolder(Request request, boolean retryOnErrorResponse) {
      this(request, retryOnErrorResponse, RetryStrategy.maxTotalTries(1));
   }
   
   public RequestHolder(Request request) {
      this(request, false);
   }
   
   public Request getRequest() {
      return request;
   }

   public boolean isRetryOnErrorResponse() {
      return retryOnErrorResponse;
   }

   public RetryStrategy getRetryStrategy() {
      return retryStrategy;
   }

   public BackoffStrategy getBackoffStrategy() {
      return backoffStrategy;
   }
}
