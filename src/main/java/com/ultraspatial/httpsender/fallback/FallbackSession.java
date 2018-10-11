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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

class FallbackSession {
   
   private Deque<Integer> stack = new ArrayDeque<>();
   private int attemptNo;

   public void beginTryRequest() {
      stack.push(0);
      attemptNo = 0;
   }
   
   public void endTryRequest() {
      stack.pop();
   }
   
   public void incrementTryRequest() {
      stack.push(stack.pop() + 1);
      attemptNo = 0;
   }
   
   public void incrementAttemptNo() {
      attemptNo++;
   }
   
   public String getNestedAttemptDescription() {
      Iterator<Integer> it = stack.descendingIterator();
      StringBuilder builder = new StringBuilder();
      boolean afterFirst = false;
      while (it.hasNext()) {
         if (afterFirst) {
            builder.append('.');
         }
         builder.append(it.next());
         afterFirst = true;
      }
      builder.append(" (");
      builder.append(attemptNo);
      builder.append(')');
      return builder.toString();
   }
   
   public int getAttemptNo() {
      return attemptNo;
   }
}
