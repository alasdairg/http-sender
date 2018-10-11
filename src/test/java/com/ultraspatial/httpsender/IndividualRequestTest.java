package com.ultraspatial.httpsender;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.times;
//import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.util.collections.Sets;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class IndividualRequestTest {

   @Mock private HttpURLConnection conn;
   @Mock private HttpsURLConnection httpsConn;
   
   @Before
   public void setUp() throws Exception {
       MockitoAnnotations.initMocks(this);
   }
   
   @Test
   public void testContructor() {
      try {
         new Get("http://www.google.com");
      } catch (RuntimeException e) {
         fail(e.toString());
      }
      try {
         new Get("http//www.google.com");
         fail("Accepted malformed url");
      } catch (RuntimeException e) {
         //Success
      }
   }
   
   @Test
   public void testCopy() {
      Get get = new Get("http://test.only.com");
      get.followRedirects(true)
         .header("x", "x1", "x2", "x3")
         .placeholder("p", "PLACEHOLDER")
         .proxy(HttpProxy.at("10.10.10.10", 1234), "user", "password")
         .queryParam("q", "q1", "q2", "q3")
         .timeout(9999)
         .trustAll(true);
      Get get2 = get.copy();
      assertNotSame(get, get2);
      assertNotSame(get.getHeaders(), get2.getHeaders());
      assertEquals(get.getHeaders(), get2.getHeaders());
      assertNotSame(get.getPlaceholders(), get2.getPlaceholders());
      assertEquals(get.getPlaceholders(), get2.getPlaceholders());
      assertEquals(get.proxy(), get2.proxy());
      assertNotSame(get.getQueryParams(), get2.getQueryParams());
      assertEquals(get.getQueryParams(), get2.getQueryParams());
      assertEquals(get.getTimeout(), get2.getTimeout());
      assertEquals(get.getTrustAll(), get2.getTrustAll());
      assertEquals(get.url(), get2.url());
   }
   
   @Test
   public void testFollowRedirects() {
      Get get = new Get("http://test.only.com");
      get.followRedirects(false);
      final boolean[] called = {false};
      Mockito.doAnswer(new Answer<Void>() {
         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            boolean value = invocation.getArgument(0);
            assertFalse(value);
            called[0] = true;
            return null;
         }
     }).when(conn).setInstanceFollowRedirects(anyBoolean());
     get.setConnectionFactory(new TestingHttpUrlConnectionFactory(conn));
     get.execute();
     assertTrue(called[0]);
   }
  
   @Test
   public void testTimeout() {
      Get get = new Get("http://test.only.com");
      get.timeout(1234);
      final boolean[] called = {false};
      Mockito.doAnswer(new Answer<Void>() {
         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            int value = invocation.getArgument(0);
            assertEquals(1234, value);
            called[0] = true;
            return null;
         }
     }).when(conn).setConnectTimeout(anyInt());
     get.setConnectionFactory(new TestingHttpUrlConnectionFactory(conn));
     get.execute();
     assertTrue(called[0]);
   }
   
   @Test
   public void testTrustAll() {
      Get get = new Get("http://test.only.com");
      get.trustAll(true);
      final boolean[] called = {false};
      Mockito.doAnswer(new Answer<Void>() {
         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            HostnameVerifier verifier = invocation.getArgument(0);
            assertTrue(verifier.verify("anything at all", null));
            called[0] = true;
            return null;
         }
     }).when(httpsConn).setHostnameVerifier(any());
     get.setConnectionFactory(new TestingHttpUrlConnectionFactory(httpsConn));
     get.execute();
     assertTrue(called[0]);
   }
   
   @Test
   public void testHeaders() throws Exception {
      when(conn.getResponseCode()).thenReturn(200);
      Map<String, String> passedHeaders = new HashMap<>();
      Mockito.doAnswer(new Answer<Void>() {
         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            passedHeaders.put(key,value);
            return null;
         }
     }).when(conn).setRequestProperty(anyString(), anyString());
      Get get = new Get("http://test.only.com");
      get.setConnectionFactory(new TestingHttpUrlConnectionFactory(conn));
      
      get.header("A", "a1", "a2", "a3").header("a", "x1", "x2");
      Map<String, HeaderValues> headers = get.getHeaders();
      Assert.assertTrue(headers.size() == 1);
      Assert.assertEquals(headers.keySet().iterator().next(), "a");
      HeaderValues hv = headers.values().iterator().next();
      assertEquals(5, hv.getValues().size());
      Assert.assertTrue(hv.getValues().containsAll(Sets.newSet("a1", "a2", "a3", "x1", "x2")));
      
      get.execute();
      Assert.assertTrue(passedHeaders.size() == 1);
      Assert.assertEquals(passedHeaders.keySet().iterator().next(), "A");
      String values = passedHeaders.values().iterator().next();
      String[] valuesArray = values.split(",");
      List<String> reassembled = new ArrayList<>();
      for (String str: valuesArray) {
         reassembled.add(str.trim());
      }
      Assert.assertEquals(hv.getValues(), reassembled);
   }
   
   @Test
   public void testSetAndClearPlaceholders() throws Exception {
      Get get = new Get("http://test.only.com/blah");
      get.placeholder("one", "ONE").placeholder("two", "TWO").placeholder("three", "THREE");
      Map<String, String> placeholders = get.getPlaceholders();
      Assert.assertTrue(placeholders.size() == 3);
      Assert.assertEquals("ONE", placeholders.get("one"));
      Assert.assertEquals("TWO", placeholders.get("two"));
      Assert.assertEquals("THREE", placeholders.get("three"));
      get.clearPlaceholders();
      placeholders = get.getPlaceholders();
      assertTrue(placeholders.isEmpty());
   }
   
   @Test
   public void testPlaceholdersOverwrite() {
      Get get = new Get("http://test.only.com");
      get.placeholder("x", "1").placeholder("x", "2").placeholder("x", "3");
      assertEquals("3", get.getPlaceholder("x"));
   }
   
   
   @Test
   public void testHeadersAccumulate() {
      Get get = new Get("http://test.only.com");
      get.header("x", "1", "2", "3").header("x", "4", "5").header("x", "6").header("x", "7");
      HeaderValues hv = get.getHeaders().get("x");
      Set<String> set = new HashSet<>();
      set.add("1");
      set.add("2");
      set.add("3");
      set.add("4");
      set.add("5");
      set.add("6");
      set.add("7");
      assertTrue(hv.getValues().containsAll(set));
   }
   
   @Test
   public void testHeadersCaseInsensitive() {
      Get get = new Get("http://test.only.com");
      get.header("xxx", "1", "2", "3").header("xXX", "4", "5").header("XxX", "6").header("XXX", "7");
      HeaderValues hv = get.getHeaders().get("xxx");
      Set<String> set = new HashSet<>();
      set.add("1");
      set.add("2");
      set.add("3");
      set.add("4");
      set.add("5");
      set.add("6");
      set.add("7");
      assertTrue(hv.getValues().containsAll(set));
   }
   
   
   @Test
   public void testPlaceholdersInUrl() throws Exception {
      when(conn.getResponseCode()).thenReturn(200);
      Get get = new Get("http://test.{one}.com/{one}/{two}?abc={three}&{three}=yes");
      get.header("h1", "{one}").header("h2", "xx{two}xx").header("h3-{three}", "{three}")
         .placeholder("one", "ONE").placeholder("two", "TWO").placeholder("three", "THREE");
      final boolean urlPassed[] = {false};
      get.setConnectionFactory(new TestingHttpUrlConnectionFactory(conn, url -> {
         Assert.assertEquals("http://test.ONE.com/ONE/TWO?abc=THREE&THREE=yes", url.toString());
         urlPassed[0] = true;
      }));
      get.execute();
      assertTrue(urlPassed[0]);
   }
   
   @Test
   public void testPlaceholdersInHeaders() throws Exception {
      when(conn.getResponseCode()).thenReturn(200);
      Get get = new Get("http://test.only.com/blah");
      get.header("h1", "{one}").header("h2", "xx{two}xx").header("h3-{three}", "{three}")
         .placeholder("one", "ONE").placeholder("two", "TWO").placeholder("three", "THREE");
      
      get.setConnectionFactory(new TestingHttpUrlConnectionFactory(conn));
      
      Map<String, String> passedHeaders = new HashMap<>();
      Mockito.doAnswer(new Answer<Void>() {
         @Override
         public Void answer(InvocationOnMock invocation) throws Throwable {
            String key = invocation.getArgument(0);
            String value = invocation.getArgument(1);
            passedHeaders.put(key,value);
            return null;
         }
     }).when(conn).setRequestProperty(anyString(), anyString());
      
     get.execute();
     assertEquals("ONE", passedHeaders.get("h1"));
     assertEquals("xxTWOxx", passedHeaders.get("h2"));
     assertEquals("THREE", passedHeaders.get("h3-THREE"));
   }
   
   @Test
   public void testPlaceholdersInQueryParams() throws Exception {
      when(conn.getResponseCode()).thenReturn(200);
      Get get = new Get("http://test.only.com/blah");
      get.queryParam("q1", "{one}").queryParam("q2", "xx{two}xx").queryParam("q3-{three}", "{three}")
         .placeholder("one", "ONE").placeholder("two", "TWO").placeholder("three", "THREE");
      
      final boolean urlPassed[] = {false};
      get.setConnectionFactory(new TestingHttpUrlConnectionFactory(conn, url -> {
         String queryStr = url.getQuery();
         String[] kvPair = queryStr.split("&");
         Map<String, String> queryParams = new HashMap<>();
         for (String kv: kvPair) {
            String[] pair = kv.split("=");
            queryParams.put(pair[0], pair[1]);
         }
         assertEquals("ONE", queryParams.get("q1"));
         assertEquals("xxTWOxx", queryParams.get("q2"));
         assertEquals("THREE", queryParams.get("q3-THREE"));
         urlPassed[0] = true;
      }));
      get.execute();
      assertTrue(urlPassed[0]);
   }
   
   @Test
   public void testGetMethod() {
      Get get = new Get("http://test.only.com");
      testHttpMethod(get, "GET");
   }
   
   @Test
   public void testPutMethod() {
      Put put = new Put("http://test.only.com");
      testHttpMethod(put, "PUT");
   }
   
   @Test
   public void testDeleteMethod() {
      Delete delete = new Delete("http://test.only.com");
      testHttpMethod(delete, "DELETE");
   }
   
   @Test
   public void testPostMethod() {
      Post post = new Post("http://test.only.com");
      testHttpMethod(post, "POST");
   }
   
   @Test
   public void testOptionsMethod() {
      Options options = new Options("http://test.only.com");
      testHttpMethod(options, "OPTIONS");
   }
   
   public void testHttpMethod(IndividualRequest<? extends IndividualRequest<?>> req, String methodName) {
      assertEquals(methodName, req.getMethodName());
      final boolean[] called = {false};
      try {
         Mockito.doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
               String value = invocation.getArgument(0);
               assertEquals(methodName, value);
               called[0] = true;
               return null;
            }
    }).when(conn).setRequestMethod(anyString());
      } catch (ProtocolException e) {
         e.printStackTrace();
      }
     req.setConnectionFactory(new TestingHttpUrlConnectionFactory(conn));
     req.execute();
     assertTrue(called[0]);
   }
}
