/*
 * Copyright (c) 2005, 2011, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6237956
 * @summary We must be able to read zip files created by Unix Info-Zip
 * @author Martin Buchholz
 */

import java.util.*;
import java.util.zip.*;
import java.io.*;

public class InfoZip {
    static int passed = 0, failed = 0;

    static void fail(String msg) {
        failed++;
        new Exception(msg).printStackTrace();
    }

    static void unexpected(Throwable t) {
        failed++;
        t.printStackTrace();
    }

    static void check(boolean condition, String msg) {
        if (! condition)
            fail(msg);
    }

    static void check(boolean condition) {
        check(condition, "Something's wrong");
    }

    private static String contents(ZipFile zf, ZipEntry ze) throws Exception {
        InputStream is = zf.getInputStream(ze);
        String result = contents(is);
        is.close();
        return result;
    }

    private static String contents(InputStream is) throws Exception {
        StringBuilder sb = new StringBuilder();
        int c;
        while ((c = is.read()) != -1)
            sb.append((char)c);
        return sb.toString();
    }

    private static void checkZipEntry(ZipEntry ze, String contents) {
        check(ze.getName().equals("someFile"), "filename");
        check(ze.getExtra() != null, "extra");
        check(contents.equals("Message in a Bottle\n"), "contents");
        check(ze.getSize() == "Message in a Bottle\n".length());
    }


    public static void main(String[] args) throws Exception {
        //----------------------------------------------------------------
        // The file InfoZip.zip was created using Unix Info-Zip's zip, thus:
        // echo Message in a Bottle > someFile; zip InfoZip.zip someFile
        // Such a file has a LOC extra different from the CEN extra,
        // which cannot happen using JDK APIs.
        //----------------------------------------------------------------
        File f = new File("InfoZip.zip");

        try (OutputStream os = new FileOutputStream(f)) {
            os.write(new byte[]
                {'P', 'K', 3, 4, 10, 0, 0, 0, 0, 0, -68, 8, 'k',
                 '2', 'V', -7, 'm', 9, 20, 0, 0, 0, 20, 0, 0, 0,
                 8, 0, 21, 0, 's', 'o', 'm', 'e', 'F', 'i', 'l', 'e', 'U',
                 'T', 9, 0, 3, 't', '_', '1', 'B', 't', '_', '1', 'B', 'U',
                 'x', 4, 0, -14, 'v', 26, 4, 'M', 'e', 's', 's', 'a', 'g',
                 'e', ' ', 'i', 'n', ' ', 'a', ' ', 'B', 'o', 't', 't', 'l', 'e',
                 10, 'P', 'K', 1, 2, 23, 3, 10, 0, 0, 0, 0, 0,
                 -68, 8, 'k', '2', 'V', -7, 'm', 9, 20, 0, 0, 0, 20,
                 0, 0, 0, 8, 0, 13, 0, 0, 0, 0, 0, 1, 0,
                 0, 0, -92, -127, 0, 0, 0, 0, 's', 'o', 'm', 'e', 'F',
                 'i', 'l', 'e', 'U', 'T', 5, 0, 3, 't', '_', '1', 'B', 'U',
                 'x', 0, 0, 'P', 'K', 5, 6, 0, 0, 0, 0, 1, 0,
                 1, 0, 'C', 0, 0, 0, 'O', 0, 0, 0, 0, 0, });
        }

        ZipEntry ze = null;
        try (ZipFile zf = new ZipFile(f)) {
            Enumeration<? extends ZipEntry> entries = zf.entries();
            ze = entries.nextElement();
            check(! entries.hasMoreElements());
            checkZipEntry(ze, contents(zf, ze));
        }

        try (FileInputStream fis = new FileInputStream(f);
             ZipInputStream is = new ZipInputStream(fis))
        {
            ze = is.getNextEntry();
            checkZipEntry(ze, contents(is));
            check(is.getNextEntry() == null);
        }
        f.delete();
        System.out.printf("passed = %d, failed = %d%n", passed, failed);
        if (failed > 0) throw new Exception("Some tests failed");
    }
}
