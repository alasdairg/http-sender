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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Interface exposing various methods for execution of a Request
 * 
 * @author Alasdair Gilmour
 */
public interface Request {

   /**
    * Execute the request on the current thread and block until the Response is available.
    * @throws RuntimeException if the request failed.
    * @return the server response
    */
	Response execute();

	/**
	 * Execute the request asynchronously, returning immediately. One of the supplied Consumers will
    * be called when the request actually completes: the Response Consumer if the Request completed
    * successfully or the Throwable consumer if it failed. 
	 * @param consumer Consumer for a successfully received Response
	 * @param error Consumer for a thrown error
	 */
	void executeAsync(Consumer<Response> consumer, Consumer<Throwable> error);

	/**
    * Execute the request asynchronously, returning immediately. One of the supplied Consumers will
    * be called when the request actually completes: the Response Consumer if the Request completed
    * successfully or the Throwable consumer if it failed. The specified Executor is used to actually 
    * execute the Request
    * @param consumer Consumer for a successfully received Response
    * @param error Consumer for a thrown error
    * @param executor the Executor to use to make the Request
    */
	void executeAsync(Consumer<Response> consumer, Consumer<Throwable> error, Executor executor);

	/**
	 * Execute the request asynchronously, immediately returning a Future that can be used to obtain
	 * the result of the operation. If the request failed, then calling get() or related methods
	 * on the Future will cause the Throwable denoting the failure to be thrown. Otherwise, get() 
	 * or related methods can be used to obtain the Response
	 * @return a CompletableFuture representing the completion of the request.
	 */
	CompletableFuture<Response> executeAsync();

	/**
    * Execute the request asynchronously, immediately returning a Future that can be used to obtain
    * the result of the operation. If the request failed, then calling get() or related methods
    * on the Future will cause the Throwable denoting the failure to be thrown. Otherwise, get() 
    * or related methods can be used to obtain the Response. The specified Executor is used to actually 
    * execute the Request
    * @param executor the Executor to use to make the Request
    * @return a CompletableFuture representing the completion of the request.
    */
	CompletableFuture<Response> executeAsync(Executor executor);
}