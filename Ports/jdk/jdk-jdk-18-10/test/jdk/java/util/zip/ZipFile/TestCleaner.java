/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8185582 8197989
 * @modules java.base/java.util.zip:open java.base/jdk.internal.vm.annotation
 * @summary Check the resources of Inflater, Deflater and ZipFile are always
 *          cleaned/released when the instance is not unreachable
 */

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.zip.*;
import jdk.internal.vm.annotation.DontInline;
import static java.nio.charset.StandardCharsets.US_ASCII;

public class TestCleaner {

    public static void main(String[] args) throws Throwable {
        testDeInflater();
        testZipFile();
    }

    private static long addrOf(Object obj) {
        try {
            Field addr = obj.getClass().getDeclaredField("address");
            if (!addr.trySetAccessible()) {
                return -1;
            }
            return addr.getLong(obj);
        } catch (Exception x) {
            return -1;
        }
    }

    // verify the "native resource" of In/Deflater has been cleaned
    private static void testDeInflater() throws Throwable {
        Field zsRefDef = Deflater.class.getDeclaredField("zsRef");
        Field zsRefInf = Inflater.class.getDeclaredField("zsRef");
        if (!zsRefDef.trySetAccessible() || !zsRefInf.trySetAccessible()) {
            throw new RuntimeException("'zsRef' is not accesible");
        }
        if (addrOf(zsRefDef.get(new Deflater())) == -1 ||
            addrOf(zsRefInf.get(new Inflater())) == -1) {
            throw new RuntimeException("'addr' is not accesible");
        }
        List<Object> list = new ArrayList<>();
        byte[] buf1 = new byte[1024];
        byte[] buf2 = new byte[1024];
        for (int i = 0; i < 10; i++) {
            var def = new Deflater();
            list.add(zsRefDef.get(def));
            def.setInput("hello".getBytes());
            def.finish();
            int n = def.deflate(buf1);

            var inf = new Inflater();
            list.add(zsRefInf.get(inf));
            inf.setInput(buf1, 0, n);
            n = inf.inflate(buf2);
            if (!"hello".equals(new String(buf2, 0, n))) {
                throw new RuntimeException("compression/decompression failed");
            }
        }

        int n = 10;
        long cnt = list.size();
        while (n-- > 0 && cnt != 0) {
            Thread.sleep(100);
            System.gc();
            cnt = list.stream().filter(o -> addrOf(o) != 0).count();
        }
        if (cnt != 0)
            throw new RuntimeException("cleaner failed to clean : " + cnt);

    }

    @DontInline
    private static Object openAndCloseZipFile(File zip) throws Throwable {
        try {
            try (var fos = new FileOutputStream(zip);
                 var zos = new ZipOutputStream(fos)) {
                zos.putNextEntry(new ZipEntry("hello"));
                zos.write("hello".getBytes(US_ASCII));
                zos.closeEntry();
            }

            var zf = new ZipFile(zip);
            var es = zf.entries();
            while (es.hasMoreElements()) {
                zf.getInputStream(es.nextElement()).read();
            }

            Field fieldRes = ZipFile.class.getDeclaredField("res");
            if (!fieldRes.trySetAccessible()) {
                throw new RuntimeException("'ZipFile.res' is not accesible");
            }
            Object zfRes = fieldRes.get(zf);
            if (zfRes == null) {
                throw new RuntimeException("'ZipFile.res' is null");
            }
            Field fieldZsrc = zfRes.getClass().getDeclaredField("zsrc");
            if (!fieldZsrc.trySetAccessible()) {
                throw new RuntimeException("'ZipFile.zsrc' is not accesible");
            }
            return fieldZsrc.get(zfRes);
        } finally {
            zip.delete();
        }
    }


    private static void testZipFile() throws Throwable {
        File dir = new File(System.getProperty("test.dir", "."));
        File zip = File.createTempFile("testzf", "zip", dir);

        Object zsrc = openAndCloseZipFile(zip);
        if (zsrc != null) {
            Field zfileField = zsrc.getClass().getDeclaredField("zfile");
            if (!zfileField.trySetAccessible()) {
                throw new RuntimeException("'ZipFile.Source.zfile' is not accesible");
            }
            //System.out.println("zffile: " +  zfileField.get(zsrc));
            int n = 10;
            while (n-- > 0 && zfileField.get(zsrc) != null) {
                System.out.println("waiting gc ... " + n);
                System.gc();
                Thread.sleep(100);
            }
            if (zfileField.get(zsrc) != null) {
                throw new RuntimeException("cleaner failed to clean zipfile.");
            }
        }
    }
}
