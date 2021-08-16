/*
 * Copyright (c) 2009, 2016, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 4244499 4532049 4700978 4820807 4980042
 * @summary Test ZipInputStream, ZipOutputStream and ZipFile with non-UTF8 encoding
 * @modules jdk.charsets
 */

import java.io.*;
import java.nio.charset.*;
import java.util.*;
import java.util.zip.*;

public class ZipCoding {

    public static void main(String[] args) throws Exception {

        test("MS932",
             "\u4e00\u4e01", "\uff67\uff68\uff69\uff6a\uff6b\uff6c");

        test("ibm437",
             "\u00e4\u00fc", "German Umlaut \u00fc in comment");

        test("utf-8",
             "\u4e00\u4e01", "\uff67\uff68\uff69\uff6a\uff6b\uff6c");

        test("utf-8",
             "\u00e4\u00fc", "German Umlaut \u00fc in comment");

        test("utf-8",
             "Surrogate\ud801\udc01", "Surrogates \ud800\udc00 in comment");

    }

    static void testZipInputStream(InputStream is, Charset cs,
                                   String name, String comment, byte[] bb)
        throws Exception
    {
        try (ZipInputStream zis = new ZipInputStream(is, cs)) {
            ZipEntry e = zis.getNextEntry();
            if (e == null || ! name.equals(e.getName()))
                throw new RuntimeException("ZipIS name doesn't match!");
            byte[] bBuf = new byte[bb.length << 1];
            int n = zis.read(bBuf, 0, bBuf.length);
            if (n != bb.length ||
                !Arrays.equals(bb, Arrays.copyOf(bBuf, n))) {
                throw new RuntimeException("ZipIS content doesn't match!");
            }
        }
    }

    static void testZipFile(File f, Charset cs,
                            String name, String comment, byte[] bb)
        throws Exception
    {
        try (ZipFile zf = new ZipFile(f, cs)) {
            Enumeration<? extends ZipEntry> zes = zf.entries();
            ZipEntry e = (ZipEntry)zes.nextElement();
            if (! name.equals(e.getName()) ||
                ! comment.equals(e.getComment()))
                throw new RuntimeException("ZipFile: name/comment doesn't match!");
            InputStream is = zf.getInputStream(e);
            if (is == null)
                throw new RuntimeException("ZipFile: getIS failed!");
            byte[] bBuf = new byte[bb.length << 1];
            int n = 0;
            int nn =0;
            while ((nn = is.read(bBuf, n, bBuf.length-n)) != -1) {
                n += nn;
            }
            if (n != bb.length ||
                !Arrays.equals(bb, Arrays.copyOf(bBuf, n))) {
                throw new RuntimeException("ZipFile content doesn't match!");
            }
        }
    }

    static void test(String csn, String name, String comment)
        throws Exception
    {
        byte[] bb = "This is the content of the zipfile".getBytes("ISO-8859-1");
        Charset cs = Charset.forName(csn);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, cs)) {
            ZipEntry e = new ZipEntry(name);
            e.setComment(comment);
            zos.putNextEntry(e);
            zos.write(bb, 0, bb.length);
            zos.closeEntry();
        }
        ByteArrayInputStream bis = new ByteArrayInputStream(baos.toByteArray());
        testZipInputStream(bis, cs, name, comment, bb);

        if ("utf-8".equals(csn)) {
            // USE_UTF8 should be set
            bis.reset();
            testZipInputStream(bis, Charset.forName("MS932"), name, comment, bb);
        }

        File f = new File(new File(System.getProperty("test.dir", ".")),
                          "zfcoding.zip");
        try (FileOutputStream fos = new FileOutputStream(f)) {
            baos.writeTo(fos);
        }
        testZipFile(f, cs, name, comment, bb);
        if ("utf-8".equals(csn)) {
            testZipFile(f, Charset.forName("MS932"), name, comment, bb);
        }
        f.delete();
    }
}
