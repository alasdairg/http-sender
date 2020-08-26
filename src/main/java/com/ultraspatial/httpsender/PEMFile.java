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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * Instances of this class represent all unique X509 Certificates and all unique RSA Private Keys
 * conained in a .PEM file containing certificate and private key sections encoded in PKCS#8 format.
 * The keys must be unencrypted. Entries which are not X509 certificates or RSA Private
 * Keys are ignored.
 * 
 * @author Alasdair Gilmour
 *
 */
public class PEMFile {

   private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
   private static final String END_CERT = "-----END CERTIFICATE-----";
   private static final String BEGIN_KEY = "-----BEGIN PRIVATE KEY-----";
   private static final String END_KEY = "-----END PRIVATE KEY-----";
   
   private String contents;
   private Set<RSAPrivateKey> privateKeys = new HashSet<>();
   private Set<X509Certificate> certificates = new HashSet<>();
   
   private PEMFile(byte[] pem) {
      contents = new String(pem, StandardCharsets.UTF_8);
      process();
   }
   
   private void process() {
      int pos = 0;     
      while (pos < contents.length()) {
         int startCert = contents.indexOf(BEGIN_CERT, pos);
         int startKey = contents.indexOf(BEGIN_KEY, pos);
         if (startKey == startCert) {
            break;
         }
         boolean doKey = false;
         if (startCert == -1) {
            doKey = true;
         } else if (startKey == -1) {
            doKey = false;
         } else {
            doKey = startKey < startCert;
         }
         
         if (doKey) {
            int endKey = contents.indexOf(END_KEY, pos);
            if (endKey < 0) {
               throw new IllegalStateException("End of Private Key delimiter not found");
            }
            privateKeys.add(processKey(contents.substring(startKey + BEGIN_KEY.length(), endKey)));
            pos = endKey + END_KEY.length();            
         } else {
            int endCert = contents.indexOf(END_CERT, pos);
            if (endCert < 0) {
               throw new IllegalStateException("End of Certificate delimiter not found");
            }
            certificates.add(processCert(contents.substring(startCert + BEGIN_CERT.length(), endCert)));
            pos = endCert + END_CERT.length();
         }
      }
   }        
   
   private RSAPrivateKey processKey(String keySection) {
      byte[] keyBytes = Base64.getDecoder().decode(keySection);
      try { 
         PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
         KeyFactory factory = KeyFactory.getInstance("RSA");
         RSAPrivateKey key = (RSAPrivateKey) factory.generatePrivate(spec); 
         return key;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }
   
   private X509Certificate processCert(String certSection) {
      byte[] certBytes = Base64.getDecoder().decode(certSection);
      try {
         CertificateFactory factory = CertificateFactory.getInstance("X.509");
         X509Certificate cert = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(certBytes));
         return cert;
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * @param pem a file in PEM format
    * @return a PEMFile instance based on the PEM file
    */
   public static PEMFile from(File pem) {
      try {
         return from(new FileInputStream(pem));
      } catch (IOException ioe) {
         throw new RuntimeException(ioe);
      }
   }

   /**
    * @param pem InputStream to a file in PEM format
    * @return a PEMFile instance based on the PEM file
    */
   public static PEMFile from(InputStream pem) {
      try {
         return from(IOUtil.readStreamFully(pem));
      } catch (IOException ioe) {
         throw new RuntimeException(ioe);
      }
   }

   /**
    * @param pem byte[] contents of a file in PEM format
    * @return a PEMFile instance based on the PEM file
    */
   public static PEMFile from(byte[] pem) {
      return new PEMFile(pem);
   }
   
   /**
    * @return the unique RSA Private Keys found in the file
    */
   public Set<RSAPrivateKey> getPrivateKeys() {
      return privateKeys;  
   }
   
   /*
    * "return the unique X509 Certificates found in the file
    */
   public Set<X509Certificate> getCertificates() {
      return certificates;
   }
}
