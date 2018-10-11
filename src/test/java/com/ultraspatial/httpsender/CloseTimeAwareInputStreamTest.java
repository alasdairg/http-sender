package com.ultraspatial.httpsender;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class CloseTimeAwareInputStreamTest {

   @Test
   public void testConsumerCalledOnClose() {
      CompletableFuture<Long> receivedLong = new CompletableFuture<>();
      byte[] data = "test".getBytes(StandardCharsets.UTF_8);
      ByteArrayInputStream bais = new ByteArrayInputStream(data);
      long start = System.currentTimeMillis();
      try (CloseTimeAwareInputStream ctais = new CloseTimeAwareInputStream(bais, num -> {
         receivedLong.complete(num);
      });) {
         ctais.read(new byte[data.length]);   
      } catch (IOException ioe) {
         fail(ioe.toString());
      }
      try {
         Long val = receivedLong.get(1, TimeUnit.MILLISECONDS);
         assertNotNull(val);
         assertTrue(val >= start);
      } catch (Exception e) {
         fail(e.toString());
      }
   }
}
