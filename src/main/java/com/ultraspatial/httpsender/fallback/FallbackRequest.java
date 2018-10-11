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

import com.ultraspatial.httpsender.Executors;
import com.ultraspatial.httpsender.Request;
import com.ultraspatial.httpsender.Response;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A FallbackRequest is used to define a sequence of Requests to be attempted in the case of failure. For example,
 * alternative urls can be tried if a server is not available. Because a FallbackRequest itself implements the Request
 * interface, these can be arbitrarily nested, allowing for non-trivial patterns of fallback behaviour to be specified. 
 * For each request to be attempted, a BackoffStrategy and a RetryStrategy can be specified, determining how long to 
 * wait between tries, and how many times a request should be retried. Once a request succeeds, the Response to that 
 * Request is made available exactly as for a simple request. If the FallbackRequest finally fails, then the last 
 * exception or error response encountered is passed back to the caller.
 * 
 * @author Alasdair Gilmour
 */
public class FallbackRequest implements Request {

   private static Logger log = Logger.getLogger(FallbackRequest.class.getName());

   private List<RequestHolder> requests = new ArrayList<>();

   /**
    * Add a Request to be tried
    * @param request a Request to be tried
    * @param retryOnErrorResponse if set to true, this will treat an error Response (i.e. a response with status
    * code >= 400) as a failure for the purposes of retrying. If set to false, only a thrown exception will be treated
    * as a failure
    * @param retryStrategy determines whether to retry this request
    * @param backoffStrategy determines a length of time before retrying
    * @return this (Builder pattern)
    */
   public FallbackRequest tryRequest(Request request, boolean retryOnErrorResponse, 
         RetryStrategy retryStrategy, BackoffStrategy backoffStrategy) {
      RequestHolder holder = new RequestHolder(request, retryOnErrorResponse, retryStrategy, backoffStrategy);
      requests.add(holder);
      return this;
   }

   /**
    * Add a Request to be tried. BackOffStrategy is defaulted to none. (i.e. retry immediately)
    * @param request a Request to be tried
    * @param retryOnErrorResponse if set to true, this will treat an error Response (i.e. a response with status
    * code >= 400) as a failure for the purposes of retrying. If set to false, only a thrown exception will be treated
    * as a failure
    * @param retryStrategy determines whether to retry this request
    * @return this (Builder pattern)
    */
   public FallbackRequest tryRequest(Request request, boolean retryOnErrorResponse, RetryStrategy retryStrategy) {
      RequestHolder holder = new RequestHolder(request, retryOnErrorResponse, retryStrategy);
      requests.add(holder);
      return this;
   }

   /**
    * Add a Request to be tried. BackOffStrategy is defaulted to none. (i.e. retry immediately). RetryStrategy 
    * is defaulted to no retries.
    * @param request a Request to be tried
    * @param retryOnErrorResponse if set to true, this will treat an error Response (i.e. a response with status
    * code >= 400) as a failure for the purposes of retrying. If set to false, only a thrown exception will be treated
    * as a failure
    * @return this (Builder pattern)
    */
   public FallbackRequest tryRequest(Request request, boolean retryOnErrorResponse) {
      RequestHolder holder = new RequestHolder(request, retryOnErrorResponse);
      requests.add(holder);
      return this;
   }

   /**
    * Add a Request to be tried. BackOffStrategy is defaulted to none. (i.e. retry immediately). RetryStrategy 
    * is defaulted to no retries. retryOnErrorResponse is defaulted to false. (i.e. only treat thrown exceptions
    * as failures).
    * @param request a Request to be tried
    * @return this (Builder pattern)
    */
   public FallbackRequest tryRequest(Request request) {
      RequestHolder holder = new RequestHolder(request);
      requests.add(holder);
      return this;
   }


   /**
    * {@inheritDoc}
    */
   @Override
   public Response execute() {
      return execute(new FallbackSession());
   }

   private Response execute(FallbackSession session) {
      Response lastResponse = null;
      Exception lastException = null;
      boolean retry;
      session.beginTryRequest();
      for (RequestHolder holder: requests) {
         session.incrementTryRequest();
         do {
            session.incrementAttemptNo();
            Request request = null;
            try {
               request = holder.getRequest();
               lastResponse = null;
               if (request instanceof FallbackRequest) {
                  lastResponse = ((FallbackRequest) request).execute(session);
               } else {
                  log.fine("Attempt no. " + session.getNestedAttemptDescription() + " ...");
                  lastResponse = holder.getRequest().execute();
               }
               boolean retryOnErrorResponse = holder.isRetryOnErrorResponse();
               if (!retryOnErrorResponse || (retryOnErrorResponse && lastResponse.getResponseCode() < 400)) {
                  return lastResponse;   
               } else {
                  log.fine(request + ": Response was an error (status " + lastResponse.getResponseCode() + ") and retryOnErrorResponse is set"); 
                  lastResponse.bodyAsString(); //Force close of InputStream
                  retry = holder.getRetryStrategy().shouldRetry(session.getAttemptNo());
               }
            } catch(Exception e) {
               if (!(request instanceof FallbackRequest)) {
                  log.log(Level.WARNING, request + ": Attempt failed with Exception", e);
               }
               lastException = e;
               retry = holder.getRetryStrategy().shouldRetry(session.getAttemptNo());
            }
            log.fine("RetryStrategy decision: " + retry);
            if (retry) {
               long backoff = holder.getBackoffStrategy().getBackoff(session.getAttemptNo());
               if (backoff > 0) {
                  try {
                     log.fine("Backing off for " + backoff + " ms");
                     Thread.sleep(backoff);   
                  } catch (InterruptedException ie) {
                  }
               }
            }
         } while (retry);         
      }
      session.endTryRequest();
      if (lastResponse == null) {
         log.fine("Rethrowing exception from last try.");
         throw new RuntimeException(lastException);  
      }
      log.fine("Returning response from last try.");
      return lastResponse;
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
         Response response = this.execute();
         cf.complete(response);
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
}
