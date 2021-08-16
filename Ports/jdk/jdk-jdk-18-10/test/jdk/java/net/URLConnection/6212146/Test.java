/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;

public class Test {

    public static void main(String[] args)
         throws Exception {
        String baseDir = args[0];
        String archiveName = args[1];
        String lProperty = System.getProperty("do.iterations", "5000");
        int lRepetitions = Integer.valueOf(lProperty);
        System.out.println("Start creating copys of the archive, "
                + lRepetitions + " times");
        for (int i = 0; i < lRepetitions; i++) {
            // Copy the given jar file and add a prefix
            copyFile(baseDir, archiveName, i);
        }
        System.out.println("Start opening the archives archive, "
                + lRepetitions + " times");
        System.out.println("First URL is jar:" + Paths.get(baseDir,
                0 + archiveName).toUri() + "!/foo/Test.class");
        for (int i = 0; i < lRepetitions; i++) {
            // Create URL
            String lURLPath = "jar:" + Paths.get(baseDir, i
                    + archiveName).toUri() + "!/foo/Test.class";
            URL lURL = new URL(lURLPath);
            // Open URL Connection
            try {
                URLConnection lConnection = lURL.openConnection();
                lConnection.getInputStream();
            } catch (java.io.FileNotFoundException fnfe) {
                // Ignore this one because we expect this one
            } catch (java.util.zip.ZipException ze) {
                throw new RuntimeException("Test failed: " + ze.getMessage());
            }
      }
   }

   private static void copyFile( String pBaseDir, String pArchiveName, int pIndex) {
      try {
         java.io.File lSource = new java.io.File( pBaseDir, pArchiveName );
         java.io.File lDestination = new java.io.File( pBaseDir, pIndex + pArchiveName );
         if( !lDestination.exists() ) {
            lDestination.createNewFile();
            java.io.InputStream lInput = new java.io.FileInputStream( lSource );
            java.io.OutputStream lOutput = new java.io.FileOutputStream( lDestination );
            byte[] lBuffer = new byte[ 1024 ];
            int lLength = -1;
            while( ( lLength = lInput.read( lBuffer ) ) > 0 ) {
               lOutput.write( lBuffer, 0, lLength );
            }
            lInput.close();
            lOutput.close();
         }
      } catch( Exception e ) {
         e.printStackTrace();
      }
   }
}
