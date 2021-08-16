/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug 8243254
 * @summary Tests a simple set of operations on Zip files in various encodings
 *          focusing on ensuring metadata is properly encoded and read.
 * @run testng TestZipFileEncodings
 */
import org.testng.annotations.AfterClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import static org.testng.Assert.*;

public class TestZipFileEncodings {

    private static int NUM_ENTRIES = 100;
    private static int METAINF_ENTRIES = 5;
    private static int ENTRY_SIZE  = 100;

    private static final AtomicInteger SEQUENCE = new AtomicInteger(0);

    private static Set<Path> paths = new HashSet<>();

    private static Random random() {
        return ThreadLocalRandom.current();
    }

    @DataProvider(name = "non-unicode-charsets")
    public Object[][] nonUnicodeCharsets() {
        return new Object[][] {
                { "ISO-8859-1" },
                { "IBM01149" },
                { "IBM037" },
                { "IBM-Thai" }
        };
    }

    @DataProvider(name = "unicode-charsets")
    public Object[][] unicodeCharsets() {
        return new Object[][] {
                { "UTF-8" },
                { "UTF-16" },
                { "UTF-16LE" },
                { "UTF-16BE" },
                { "UTF-32" }
        };
    }

    @Test(dataProvider = "non-unicode-charsets")
    public void testNonUnicode(String charsetName) throws Throwable {
        test(NUM_ENTRIES, 100 + random().nextInt(ENTRY_SIZE), false, Charset.forName(charsetName));
    }

    @Test(dataProvider = "unicode-charsets")
    public void testUnicode(String charsetName) throws Throwable {
        test(NUM_ENTRIES, 100 + random().nextInt(ENTRY_SIZE), true, Charset.forName(charsetName));
    }

    @Test(dataProvider = "non-unicode-charsets")
    public void testNonUnicodeManyEntries(String charsetName) throws Throwable {
        test(70000, 10, false, Charset.forName(charsetName));
    }

    @Test(dataProvider = "unicode-charsets")
    public void testUnicodeManyEntries(String charsetName) throws Throwable {
        test(70000, 10, true, Charset.forName(charsetName));
    }

    @AfterClass
    public void tearDown() {
        for (Path path : paths) {
            path.toFile().deleteOnExit();
        }
    }

    static void test(int numEntry, int szMax, boolean unicode, Charset cs) throws Throwable {
        String name = "zfenc-" + SEQUENCE.incrementAndGet() + ".zip";
        Zip zip = new Zip(name, numEntry, szMax, unicode, cs);
        doTest(zip);
    }

    static void checkEqual(ZipEntry x, ZipEntry y) {
        assertEquals(x.getName(), y.getName());
        assertEquals(x.isDirectory(), y.isDirectory());
        assertEquals(x.getMethod(), y.getMethod());
        assertEquals((x.getTime() / 2000), y.getTime() / 2000);
        assertEquals(x.getSize(), y.getSize());
        assertEquals(x.getCompressedSize(), y.getCompressedSize());
        assertEquals(x.getCrc(), y.getCrc());
        assertEquals(x.getComment(), y.getComment());
    }

    static void doTest(Zip zip) throws Throwable {
        try (ZipFile zf = new ZipFile(zip.name, zip.cs)) {
            doTest0(zip, zf);
        }
    }

    static void doTest0(Zip zip, ZipFile zf) throws Throwable {
        // (0) check zero-length entry name, no AIOOBE
        assertEquals(zf.getEntry(""), null);

        List<ZipEntry> list = new ArrayList(zip.entries.keySet());
        // check each entry and its bytes
        for (ZipEntry ze : list) {
            byte[] data = zip.entries.get(ze);
            String name = ze.getName();
            ZipEntry e = zf.getEntry(name);
            checkEqual(e, ze);
            if (!e.isDirectory()) {
                // check with readAllBytes
                try (InputStream is = zf.getInputStream(e)) {
                    assertEquals(data, is.readAllBytes());
                }
                int slash = name.indexOf('/');
                if (slash > 0) {
                    ZipEntry dir1 = zf.getEntry(name.substring(0, slash));
                    ZipEntry dir2 = zf.getEntry(name.substring(0, slash + 1));
                    assertNotNull(dir1);
                    assertNotNull(dir2);
                    assertTrue(dir1.isDirectory());
                    assertTrue(dir2.isDirectory());
                    checkEqual(dir1, dir2);
                }
            } else {
                ZipEntry unslashLookup = zf.getEntry(name.substring(0, name.length() - 1));
                checkEqual(e, unslashLookup);
            }
        }
    }

    private static class Zip {
        String name;
        Charset cs;
        Map<ZipEntry, byte[]> entries;
        BasicFileAttributes attrs;
        long lastModified;

        Zip(String name, int num, int szMax, boolean unicode, Charset cs) {
            this.cs = cs;
            this.name = name;
            entries = new LinkedHashMap<>(num);
            try {
                Path p = Paths.get(name);
                Files.deleteIfExists(p);
                paths.add(p);
            } catch (Exception x) {
                throw (RuntimeException)x;
            }

            try (FileOutputStream fos = new FileOutputStream(name);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 ZipOutputStream zos = new ZipOutputStream(bos, cs))
            {
                CRC32 crc = new CRC32();
                for (int i = 0; i < num; i++) {
                    String ename = "entry-" + i + "-name-" + random().nextLong();
                    if (unicode) {
                        // Provokes compatibility issue with slash handling for
                        // non-ASCII compatible Unicode encodings
                        ename = ename + '\u2F2F';
                        zos.putNextEntry(new ZipEntry(ename + '/'));
                        ename = ename + '/' + ename;
                    }
                    ZipEntry ze = new ZipEntry(ename);
                    assertTrue(!ze.isDirectory());
                    writeEntry(zos, crc, ze, ZipEntry.STORED, szMax);
                }
                // add some manifest entries
                zos.putNextEntry(new ZipEntry("META-INF/"));
                for (int i = 0; i < METAINF_ENTRIES; i++) {
                    String meta = "META-INF/" + "entry-" + i + "-metainf-" + random().nextLong();
                    ZipEntry ze = new ZipEntry(meta);
                    writeEntry(zos, crc, ze, ZipEntry.STORED, szMax);
                }
            } catch (Exception x) {
                throw (RuntimeException)x;
            }
            try {
                this.attrs = Files.readAttributes(Paths.get(name), BasicFileAttributes.class);
                this.lastModified = new File(name).lastModified();
            } catch (Exception x) {
                throw (RuntimeException)x;
            }
        }

        private void writeEntry(ZipOutputStream zos, CRC32 crc,
                                ZipEntry ze, int method, int szMax)
            throws IOException
        {
            ze.setMethod(method);
            byte[] data = new byte[random().nextInt(szMax + 1)];
            random().nextBytes(data);
            if (method == ZipEntry.STORED) {  // must set size/csize/crc
                ze.setSize(data.length);
                ze.setCompressedSize(data.length);
                crc.reset();
                crc.update(data);
                ze.setCrc(crc.getValue());
            }
            ze.setTime(System.currentTimeMillis());
            ze.setComment(ze.getName());
            zos.putNextEntry(ze);
            zos.write(data);
            zos.closeEntry();
            entries.put(ze, data);
        }
    }
}
