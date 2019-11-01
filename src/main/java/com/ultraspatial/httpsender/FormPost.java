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
 * Copyright 2018 Alasdair Gilmour
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

import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A convenience class for making Form POST requests. (i.e. like those made by
 * a web browser submitting an HTML Form). The Content-Type header is automatically
 * set to application/x-www-form-urlencoded and the keys and values are URL encoded. 
 * Methods are provided for setting the form fields. Form keys and values may use 
 * placeholders, similar to the placeholder support for query parameters and http 
 * headers.
 * 
 * @author Alasdair Gilmour
 *
 */
public class FormPost extends WriteRequest<FormPost> {

   private static final String CONTENT_TYPE = "Content-Type";
   private static final String MIME_TYPE = "application/x-www-form-urlencoded";
   private static final String UNSUPPORTED = "Use formField() to add items to the Form";
   private static final String UNSUPPORTED_CT = "Content-Type is fixed as " + MIME_TYPE;
   
   private Map<String, List<String>> formFields = new HashMap<>();

   /**
    * Construct a Form POST request
    * @param url the url to post to
    */
   public FormPost(String url) {
      super(url);
   }

   private FormPost(FormPost source) {
      super(source);
   }

   /**
    * Create an independent copy of this Form POST request
    * @return a copy
    */
   public FormPost copy() {
      return new FormPost(this);
   }
   
   @Override
   protected FormPost me() {
      return this;
   }

   /**
    * Not supported for FormPost - use formField() methods to set the content
    * @param body
    * @return
    */
   public FormPost requestBody(String body) {
      throw new UnsupportedOperationException(UNSUPPORTED);
   }

   /**
    * Not supported for FormPost - use formField() methods to set the content
    * @param bytes
    * @return
    */
   public FormPost requestBody(byte[] bytes) {
      throw new UnsupportedOperationException(UNSUPPORTED);
   }

   /**
    * Not supported for FormPost - use formField() methods to set the content
    * @param body
    * @return
    */
   public FormPost requestBody(InputStream body) {
      throw new UnsupportedOperationException(UNSUPPORTED);
   }

   /**
    * Not supported for FormPost - use formField() methods to set the content
    * @param provider
    * @return
    */
   public FormPost requestBody(InputStreamProvider provider) {
      throw new UnsupportedOperationException(UNSUPPORTED);
   }

   /**
    * Not supported for FormPost - content type defaults to application/x-www-form-urlencoded
    * @param type
    * @return
    */
   public FormPost contentType(String type) {
      throw new UnsupportedOperationException(UNSUPPORTED_CT);
   }

   /**
    * Not supported for FormPost - content type defaults to application/x-www-form-urlencoded
    * @param type
    * @return
    */
   public FormPost contentType(String type, Charset charset) {
      throw new UnsupportedOperationException(UNSUPPORTED_CT);
   }

   @Override
   public FormPost header(String name, String ... values) {
      if (name.equalsIgnoreCase(CONTENT_TYPE)) {
         throw new UnsupportedOperationException(UNSUPPORTED_CT);
      }
      super.header(name, values);
      return this;
   }

   /**
    * Set a form field to the specified value(s)
    * @param name the field name
    * @param values the value(s)
    * @return this (Builder pattern)
    */
   public FormPost formField(String name, String ... values) {
      List<String> list = formFields.get(name);
      if (list == null) {
         list = new ArrayList<>();
         formFields.put(name, list);
      }
      for (String val: values) {
         list.add(val);
      }
      return me();
   }

   /**
    * Clear all the form fields.
    */
   public void clearFormFields() {
      formFields.clear();
   }

   /**
    * Get the form's fields as a Map.
    * @return the form fields as a Map
    */
   public Map<String, List<String>> getFormFields() {
      return new HashMap<>(formFields);
   }
   
   @Override
   protected void preObtainResponse(HttpURLConnection conn) throws Exception {
      super.header(CONTENT_TYPE, MIME_TYPE);
      String encoded = encodeFormFields();
      super.requestBody(encoded);
      super.preObtainResponse(conn);
   }
   
   private String encodeFormFields() {
      StringBuilder builder = new StringBuilder();
      boolean notFirst = false;
      for (Map.Entry<String, List<String>> entry: formFields.entrySet()) {
         String name = entry.getKey();
         String processedName = urlEncode(processPlaceholders(name));
         List<String> values = entry.getValue();
         for (String str: values) {
            if (notFirst) {
               builder.append('&');
            }
            builder.append(processedName);
            builder.append('=');
            builder.append(urlEncode(processPlaceholders(str)));
            notFirst = true;
         }
      }
      return builder.toString();
   }
   
   private String urlEncode(String str) {
      try {
         return URLEncoder.encode(str, "UTF-8");
      } catch (UnsupportedEncodingException e) {
         throw new RuntimeException(e);
      } 
   }

   @Override
   public String getMethodName() {
      return "POST";
   }
}
