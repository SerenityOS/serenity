/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;

/**
 * @test
 * @key headful
 * @bug 6522586
 * @summary Enforce limits on font creation
 * @run main/othervm -Djava.security.manager=allow BigFont 1 A.ttf
 * @run main/othervm -Djava.security.manager=allow BigFont 2 A.ttf
 */
public class BigFont {

   static private class SizedInputStream extends InputStream {

       int size;
       int cnt = 0;

       SizedInputStream(int size) {
           this.size = size;
       }

       public int read() {
           if (cnt < size) {
              cnt++;
              return 0;
           } else {
              return -1;
           }
       }

       public int getCurrentSize() {
           return cnt;
       }
   }

    static String id;
    static String fileName;

    public static void main(final String[] args) {
        id = args[0];
        fileName = args[1];

        System.out.println("Applet " + id + " "+
                           Thread.currentThread().getThreadGroup());
        if (System.getSecurityManager() == null) {
            System.setSecurityManager(new SecurityManager());
        }
        // Larger than size for a single font.
        int fontSize = 64 * 1000 * 1000;
        SizedInputStream sis = new SizedInputStream(fontSize);
        try {
             Font font = Font.createFont(Font.TRUETYPE_FONT, sis);
        } catch (Throwable t) {
            t.printStackTrace();
            if (t instanceof FontFormatException ||
                fontSize <= sis.getCurrentSize())
            {
                System.out.println(sis.getCurrentSize());
                System.out.println(t);
                throw new RuntimeException("Allowed file to be too large.");
            }
        }
        // The following part of the test was verified manually but
        // is impractical to enable  because it requires a fairly large
        // valid font to be part of the test, and we can't easily include
        // that, nor dependably reference one from the applet environment.
        /*
        if (fileName == null) {
            return;
        }
        int size = getFileSize(fileName);
        if (size == 0) {
            return;
        }
        int fontCnt = 1000 * 1000 * 1000 / size;
        loadMany(size, fontCnt, fileName);
        System.gc(); System.gc();
        fontCnt = fontCnt / 2;
        System.out.println("Applet " + id + " load more.");
        loadMany(size, fontCnt, fileName);
        */
        System.out.println("Applet " + id + " finished.");
    }

    int getFileSize(String fileName) {
        try {
            String path = Paths.get(System.getProperty("test.src", "."),
                                    fileName).toAbsolutePath().normalize()
                                             .toString();
            URL url = new URL(path);
            InputStream inStream = url.openStream();
            BufferedInputStream fontStream = new BufferedInputStream(inStream);
            int size = 0;
            while (fontStream.read() != -1) {
                size++;
            }
            fontStream.close();
            return size;
        } catch (IOException e) {
            return 0;
        }

    }
    void loadMany(int oneFont, int fontCnt, String fileName) {
        System.out.println("fontcnt= " + fontCnt);
        Font[] fonts = new Font[fontCnt];
        int totalSize = 0;
        boolean gotException = false;
        for (int i=0; i<fontCnt; i++) {
            try {
                String path = Paths.get(System.getProperty("test.src", "."),
                                        fileName).toAbsolutePath().normalize()
                                                 .toString();
                URL url = new URL(path);
                InputStream inStream = url.openStream();
                BufferedInputStream fontStream =
                    new BufferedInputStream(inStream);
                fonts[i] = Font.createFont(Font.TRUETYPE_FONT, fontStream);
                totalSize += oneFont;
                fontStream.close();
            } catch (Throwable t) {
                gotException = true;
                System.out.println("Applet " + id + " " + t);
            }
        }
        if (!gotException) {
          throw new RuntimeException("No expected exception");
        }
    }
}

