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
 * @bug 4770745 4828461
 * @summary open zip files with more than 64k entries
 * @run main/timeout=600 ManyEntries
 * @author Martin Buchholz
 */

import java.util.*;
import java.util.zip.*;
import java.io.*;

public class ManyEntries {
    public static void main(String[] args) throws Exception {
        boolean runMoreTests = args.length > 0;
        int[] sizes = (runMoreTests ?
                       new int[]{1, 2, 42, 65534, 65535, 65536, 65537, 68000} :
                       new int[]{1, 2, 42, 68000});
        int[] methods = {ZipEntry.STORED, ZipEntry.DEFLATED};

        for (int size : sizes)
            for (int method : methods)
                test(size, method);
    }

    // JDK doesn't like having zip file contents changed at runtime
    static int uniquifier = 42;

    static void test(int N, int method) throws Exception {
        System.out.println("N="+N+", method="+method);
        long time = System.currentTimeMillis() / 2000 * 2000;
        CRC32 crc32 = new CRC32();
        File zipFile = new File(++uniquifier + ".zip");
        try {
            zipFile.delete();
            try (FileOutputStream fos = new FileOutputStream(zipFile);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 ZipOutputStream zos = new ZipOutputStream(bos))
            {
                for (int i = 0; i < N; i++) {
                    ZipEntry e = new ZipEntry("DIR/"+i);
                    e.setMethod(method);
                    e.setTime(time);
                    if (method == ZipEntry.STORED) {
                        e.setSize(1);
                        crc32.reset();
                        crc32.update((byte)i);
                        e.setCrc(crc32.getValue());
                    } else {
                        e.setSize(0);
                        e.setCrc(0);
                    }
                    zos.putNextEntry(e);
                    zos.write(i);
                }
            }

            try (ZipFile zip = new ZipFile(zipFile)) {
                if (! (zip.size() == N))
                    throw new Exception("Bad ZipFile size: " + zip.size());
                Enumeration entries = zip.entries();

                for (int i = 0; i < N; i++) {
                    if (i % 1000 == 0) {System.gc(); System.runFinalization();}
                    if (! (entries.hasMoreElements()))
                        throw new Exception("only " + i + " elements");
                    ZipEntry e = (ZipEntry)entries.nextElement();
                    if (! (e.getSize() == 1))
                        throw new Exception("bad size: " + e.getSize());
                    if (! (e.getName().equals("DIR/" + i)))
                        throw new Exception("bad name: " + i);
                    if (! (e.getMethod() == method))
                        throw new Exception("getMethod="+e.getMethod()+", method=" + method);
                    if (! (e.getTime() == time))
                        throw new Exception("getTime="+e.getTime()+", time=" + time);
                    if (! (zip.getInputStream(e).read() == (i & 0xff)))
                        throw new Exception("Bad file contents: " + i);
                }
                if (entries.hasMoreElements())
                    throw new Exception("too many elements");
            }
        } finally {
            zipFile.delete();
        }
    }
}
