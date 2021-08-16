/*
 * Copyright (c) 2002, 2013, Oracle and/or its affiliates. All rights reserved.
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

/* @test
   @bug 4759207 4403166 4165006 4403166 6182812 6274272 7160013
   @summary Test to see if win32 path length can be greater than 260
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.DirectoryNotEmptyException;

public class MaxPathLength {
    private static String sep = File.separator;
    private static String pathComponent = sep +
        "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
    private static String fileName =
                 "areallylongfilenamethatsforsur";
    private static boolean isWindows = false;

    private static final int MAX_LENGTH = 256;

    private static int counter = 0;

    public static void main(String[] args) throws Exception {
        String osName = System.getProperty("os.name");
        if (osName.startsWith("Windows")) {
            isWindows = true;
        }

        for (int i = 4; i < 7; i++) {
            String name = fileName;
            while (name.length() < MAX_LENGTH) {
                testLongPath (i, name, false);
                testLongPath (i, name, true);
                name = getNextName(name);
            }
        }

        // test long paths on windows
        // And these long pathes cannot be handled on Solaris and Mac platforms
        if (isWindows) {
            String name = fileName;
            while (name.length() < MAX_LENGTH) {
                testLongPath (20, name, false);
                testLongPath (20, name, true);
                name = getNextName(name);
            }
        }
    }

    private static String getNextName(String fName) {
        return (fName.length() < MAX_LENGTH/2) ? fName + fName
                                               : fName + "A";
    }

    static void testLongPath(int max, String fn,
                             boolean tryAbsolute) throws Exception {
        String[] created = new String[max];
        String pathString = ".";
        for (int i = 0; i < max -1; i++) {
            pathString = pathString + pathComponent + (counter++);
            created[max - 1 -i] = pathString;
        }

        File dirFile = new File(pathString);
        File f = new File(pathString + sep + fn);

        String tPath = f.getPath();
        if (tryAbsolute) {
            tPath = f.getCanonicalPath();
        }
        created[0] = tPath;

        //for getCanonicalPath testing on win32
        File fu = new File(pathString + sep + fn.toUpperCase());

        if (dirFile.exists()) {
            System.err.println("Warning: Test directory structure exists already!");
            return;
        }

        try {
            Files.createDirectories(dirFile.toPath());

            if (tryAbsolute)
                dirFile = new File(dirFile.getCanonicalPath());
            if (!dirFile.isDirectory())
                throw new RuntimeException ("File.isDirectory() failed");
            f = new File(tPath);
            if (!f.createNewFile()) {
                throw new RuntimeException ("File.createNewFile() failed");
            }

            if (!f.exists())
                throw new RuntimeException ("File.exists() failed");
            if (!f.isFile())
                throw new RuntimeException ("File.isFile() failed");
            if (!f.canRead())
                throw new RuntimeException ("File.canRead() failed");
            if (!f.canWrite())
                throw new RuntimeException ("File.canWrite() failed");

            if (!f.delete())
                throw new RuntimeException ("File.delete() failed");

            FileOutputStream fos = new FileOutputStream(f);
            fos.write(1);
            fos.close();

            if (f.length() != 1)
                throw new RuntimeException ("File.length() failed");
            long time = System.currentTimeMillis();
            if (!f.setLastModified(time))
                throw new RuntimeException ("File.setLastModified() failed");
            if (f.lastModified() == 0) {
                throw new RuntimeException ("File.lastModified() failed");
            }
            String[] list = dirFile.list();
            if (list == null || !fn.equals(list[0])) {
                throw new RuntimeException ("File.list() failed");
            }

            File[] flist = dirFile.listFiles();
            if (flist == null || !fn.equals(flist[0].getName()))
                throw new RuntimeException ("File.listFiles() failed");

            if (isWindows &&
                !fu.getCanonicalPath().equals(f.getCanonicalPath()))
                throw new RuntimeException ("getCanonicalPath() failed");

            char[] cc = tPath.toCharArray();
            cc[cc.length-1] = 'B';
            File nf = new File(new String(cc));
            if (!f.renameTo(nf)) {
                /*there is a known issue that renameTo fails if
                  (1)the path is a UNC path and
                  (2)the path length is bigger than 1092
                  so don't stop if above are true
                */
                String abPath = f.getAbsolutePath();
                if (!abPath.startsWith("\\\\") ||
                    abPath.length() < 1093) {
                    throw new RuntimeException ("File.renameTo() failed for lenth="
                                                + abPath.length());
                }
            } else {
                if (!nf.canRead())
                    throw new RuntimeException ("Renamed file is not readable");
                if (!nf.canWrite())
                    throw new RuntimeException ("Renamed file is not writable");
                if (nf.length() != 1)
                    throw new RuntimeException ("Renamed file's size is not correct");
                if (!nf.renameTo(f)) {
                    created[0] = nf.getPath();
                }
                /* add a script to test these two if we got a regression later
                if (!f.setReadOnly())
                    throw new RuntimeException ("File.setReadOnly() failed");
                f.deleteOnExit();
                */
            }
        } finally {
            // Clean up
            for (int i = 0; i < max; i++) {
                Path p = (new File(created[i])).toPath();
                try {
                    Files.deleteIfExists(p);
                    // Test if the file is really deleted and wait for 1 second at most
                    for (int j = 0; j < 10 && Files.exists(p); j++) {
                        Thread.sleep(100);
                    }
                } catch (DirectoryNotEmptyException ex) {
                    // Give up the clean-up, let jtreg handle it.
                    System.err.println("Dir, " + p + ", is not empty");
                    break;
                }
            }
        }
    }
}
