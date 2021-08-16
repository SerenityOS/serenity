/*
 * Copyright (c) 2003, 2011, Oracle and/or its affiliates. All rights reserved.
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

/*
 *
 * Driven by: 3GBZipFiles.sh
 */

import java.io.RandomAccessFile;
import java.io.IOException;
import java.util.Random;

public class FileBuilder {
    private static void usageError() {
        System.err.println("Usage: FileBuilder filetype filename filesize");
        System.err.println("");
        System.err.println("Makes a file named FILENAME of size FILESIZE.");
        System.err.println("If FILETYPE is \"MostlyEmpty\", the file contents is mostly null bytes");
        System.err.println("(which might occupy no disk space if the right OS support exists).");
        System.err.println("If FILETYPE is \"SlightlyCompressible\", the file contents are");
        System.err.println("approximately 90% random data.");
        System.exit(1);
    }

    public static void main (String[] args) throws IOException {
        if (args.length != 3)
            usageError();
        String filetype = args[0];
        String filename = args[1];
        long   filesize = Long.parseLong(args[2]);

        if (! (filetype.equals("MostlyEmpty") ||
               filetype.equals("SlightlyCompressible")))
            usageError();

        try (RandomAccessFile raf = new RandomAccessFile(filename, "rw")) {
            if (filetype.equals("SlightlyCompressible")) {
                byte[] randomBytes = new byte[16384];
                byte[] nullBytes   = new byte[randomBytes.length/10];
                Random rand = new Random();
                for (int i = 0; raf.length() < filesize; ++i) {
                    rand.nextBytes(randomBytes);
                    raf.write(nullBytes);
                    raf.write(randomBytes);
                }
            }

            // Make sure file is exactly the requested size, and that
            // a unique identifying trailer is written.
            byte[] filenameBytes = filename.getBytes("UTF8");
            raf.seek(filesize-filenameBytes.length);
            raf.write(filenameBytes);
            raf.setLength(filesize);
        }
    }
}
