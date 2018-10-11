package com.ultraspatial.httpsender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public final class IOUtil {

   private IOUtil() {
      //Utility class
   }
   
   public static byte[] readStreamFully(InputStream stream) throws IOException {
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try(InputStream is = stream;) {
         byte[] buffer = new byte[10000];
         int len;
         while ((len = stream.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
         }
      }
      return baos.toByteArray();
   }
}
