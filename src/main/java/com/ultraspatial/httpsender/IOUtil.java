package com.ultraspatial.httpsender;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Utility class with IO helper methods
 */
public final class IOUtil {

   private IOUtil() {
      //Utility class
   }

   /**
    * Fully read an InputStream and return the data as a byte[] and close the stream.
    * @param stream an InputStream
    * @return the entire contents of the InputStream as a byte[]
    * @throws IOException
    */
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
