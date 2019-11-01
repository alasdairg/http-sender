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

import java.io.File;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

/**
 * Instances of this class represent a set of one or more client-side certificates that the
 * system can use to satisfy a client certificate request by the remote server during a TLS
 * handshake. Methods are provided to create these from an existing KeyStore, or alternatively 
 * from one or more PEM files containing certificate and private key sections. 
 *   
 * @author Alasdair Gilmour
 *
 */
public final class ClientCerts {

   private KeyStore keyStore;
   private char[] entryPassword;
   
   private ClientCerts(KeyStore keyStore, String entryPassword) {
      this.keyStore = keyStore;
      this.entryPassword = entryPassword.toCharArray();      
   }
   
   SSLSocketFactory getSocketFactory() {
      try {
         KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()); 
         kmf.init(keyStore, entryPassword); 
         SSLContext context = SSLContext.getInstance("TLS");  
         context.init(kmf.getKeyManagers(), null, null); 
         return context.getSocketFactory();
      } catch (Exception e) {
         return null;
      }
   }

   /**
    * @param keyStore a KeyStore
    * @return a ClientCerts instance backed by the specified KeyStore
    */
   public static ClientCerts fromKeystore(KeyStore keyStore) {
      return new ClientCerts(keyStore, null);
   }

   /**
    * @param keyStore a KeyStore
    * @param entryPass a password for entries in the KeyStore
    * @return a ClientCerts instance backed by the specified KeyStore
    */
   public static ClientCerts fromKeystore(KeyStore keyStore, String entryPass) {
      return new ClientCerts(keyStore, entryPass);
   }

   /**
    * @param keyStoreStream an InputStream to a KeyStore
    * @return a ClientCerts instance backed by the KeyStore loaded from the stream
    */
   public static ClientCerts fromKeystore(InputStream keyStoreStream) {
      return fromKeyStore(keyStoreStream, null, null);
   }

   /**
    * @param keyStoreStream an InputStream to a KeyStore
    * @param ksPass password for the KeyStore
    * @return a ClientCerts instance backed by the KeyStore loaded from the stream
    */
   public static ClientCerts fromKeystore(InputStream keyStoreStream, String ksPass) {
      return fromKeyStore(keyStoreStream, ksPass, null);
   }

   /**
    * @param keyStoreStream an InputStream to a KeyStore
    * @param ksPass password for the KeyStore
    * @param entryPass a password for entries in the KeyStore
    * @return a ClientCerts instance backed by the KeyStore loaded from the stream
    */
   public static ClientCerts fromKeyStore(InputStream keyStoreStream, String ksPass, String entryPass) {      
      try {
         KeyStore keyStore = KeyStore.getInstance("PKCS12");
         keyStore.load(keyStoreStream, ksPass.toCharArray());
         return new ClientCerts(keyStore, entryPass);
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

   /**
    * @param filePaths paths to one or more PEM files
    * @return a ClientCerts instance based on the combined contents of the specified PEM files
    */
   public static ClientCerts fromPEM(String ... filePaths) {
      File[] files = new File[filePaths.length];
      for (int i = 0; i < filePaths.length; i++) {
         files[i] = new File(filePaths[i]); 
      }
      return fromPEM(files);
   }

   /**
    * @param arrays byte arrays, each containing the contents of a PEM file
    * @return a ClientCerts instance based on the combined contents of the specified PEM files
    */
   public static ClientCerts fromPEM(byte[] ... arrays) {
      PEMFile[] pemFiles = new PEMFile[arrays.length];
      for (int i = 0; i < arrays.length; i++) {
         pemFiles[i] = PEMFile.from(arrays[i]); 
      }
      return fromPEM(pemFiles);
   }

   /**
    * @param streams InputStreams to one or more PEM files
    * @return a ClientCerts instance based on the combined contents of the specified PEM files
    */
   public static ClientCerts fromPEM(InputStream ... streams) {
      PEMFile[] pemFiles = new PEMFile[streams.length];
      for (int i = 0; i < streams.length; i++) {
         pemFiles[i] = PEMFile.from(streams[i]); 
      }
      return fromPEM(pemFiles);
   }

   /**
    * @param files one or more file objects that represent PEM files
    * @return a ClientCerts instance based on the combined contents of the specified PEM files
    */
   public static ClientCerts fromPEM(File ... files) {
      PEMFile[] pemFiles = new PEMFile[files.length];
      for (int i = 0; i < files.length; i++) {
         pemFiles[i] = PEMFile.from(files[i]); 
      }
      return fromPEM(pemFiles);
   }

   /**
    * @param files one or more PEM files
    * @return a ClientCerts instance based on the combined contents of the specified PEM files
    */
   public static ClientCerts fromPEM(PEMFile ... files) {
      Set<RSAPrivateKey> privateKeys = new HashSet<>();
      Set<X509Certificate> certificates = new HashSet<>();
      for (PEMFile pem: files) {
         privateKeys.addAll(pem.getPrivateKeys());
         certificates.addAll(pem.getCertificates());
      }
      return createFromKeysAndCerts(mapKeysToCerts(privateKeys, certificates));
   }
   
   private static Map<RSAPrivateKey, X509Certificate> mapKeysToCerts(Set<RSAPrivateKey> keys, Set<X509Certificate> certs) {
      Map<RSAPrivateKey, X509Certificate> map = new HashMap<>();
      for (RSAPrivateKey key: keys) {
         for (X509Certificate cert: certs) {
            if (cert.getPublicKey() instanceof RSAPublicKey) {
               RSAPublicKey pubKey = (RSAPublicKey) cert.getPublicKey();
               if (pubKey.getModulus().equals(key.getModulus())) {
                  map.put(key, cert);
               }
            }
         }
      }
      
      return map;
   }
   
   private static ClientCerts createFromKeysAndCerts(Map<RSAPrivateKey, X509Certificate> keysAndCerts) {
      try {
         KeyStore keystore = KeyStore.getInstance("JKS");
         keystore.load(null);
         int entryNum = 1;
         for (Map.Entry<RSAPrivateKey, X509Certificate> entry: keysAndCerts.entrySet()) {
            RSAPrivateKey key = entry.getKey();
            X509Certificate cert = entry.getValue();
            keystore.setCertificateEntry("certificate-" + entryNum, cert);
            keystore.setKeyEntry("privatekey-" + entryNum++, key, "changeit".toCharArray(), new Certificate[] { cert });
         }

         return new ClientCerts(keystore, "changeit");
      } catch (Exception e) {
         throw new RuntimeException(e);
      }
   }

}
