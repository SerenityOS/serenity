/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8142508 8146431
 * @modules java.base/java.util.zip:open
 * @summary Tests various ZipFile apis
 * @run main/manual TestZipFile
 */

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

public class TestZipFile {

    private static final Random r = new Random();
    private static final int    N = 50;
    private static final int    NN = 10;
    private static final int    ENUM = 10000;
    private static final int    ESZ = 10000;
    private static ExecutorService executor = Executors.newFixedThreadPool(20);
    private static final Set<Path> paths = new HashSet<>();
    private static final boolean isWindows = System.getProperty("os.name")
            .startsWith("Windows");

    static void realMain (String[] args) throws Throwable {
        try {
            for (int i = 0; i < N; i++) {
                test(r.nextInt(ENUM), r.nextInt(ESZ), false, true);
                test(r.nextInt(ENUM), r.nextInt(ESZ), true, true);
            }
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);
            executor = Executors.newFixedThreadPool(20);
            for (int i = 0; i < NN; i++) {
                test(r.nextInt(ENUM), 100000 + r.nextInt(ESZ), false, true);
                test(r.nextInt(ENUM), 100000 + r.nextInt(ESZ), true, true);
                if(!isWindows) {
                    testCachedDelete();
                    testCachedOverwrite();
                }
            }
            test(70000, 1000, false, true);   // > 65536 entry number;
            testDelete();                     // OPEN_DELETE

            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.MINUTES);
        } finally {
            for (Path path : paths) {
                Files.deleteIfExists(path);
            }
        }
    }

    static void test(int numEntry, int szMax, boolean addPrefix, boolean cleanOld) {
        String name = "test-" + r.nextInt() + ".zip";
        Zip zip = new Zip(name, numEntry, szMax, addPrefix, cleanOld);
        for (int i = 0; i < NN; i++) {
            executor.submit(() -> doTest(zip));
        }
     }

    // test scenario:
    // (1) open the ZipFile(zip) with OPEN_READ | OPEN_DELETE
    // (2) test the ZipFile works correctly
    // (3) check the zip is deleted after ZipFile gets closed
    static void testDelete() throws Throwable {
        String name = "testDelete-" + r.nextInt() + ".zip";
        Zip zip = new Zip(name, r.nextInt(ENUM), r.nextInt(ESZ), false, true);
        try (ZipFile zf = new ZipFile(new File(zip.name),
                                      ZipFile.OPEN_READ | ZipFile.OPEN_DELETE ))
        {
            doTest0(zip, zf);
        }
        Path p = Paths.get(name);
        if (Files.exists(p)) {
            fail("Failed to delete " + name + " with OPEN_DELETE");
        }
    }

    // test scenario:
    // (1) keep a ZipFile(zip1) alive (in ZipFile's cache), dont close it
    // (2) delete zip1 and create zip2 with the same name the zip1 with zip2
    // (3) zip1 tests should fail, but no crash
    // (4) zip2 tasks should all get zip2, then pass normal testing.
    static void testCachedDelete() throws Throwable {
        String name = "testCachedDelete-" + r.nextInt() + ".zip";
        Zip zip1 = new Zip(name, r.nextInt(ENUM), r.nextInt(ESZ), false, true);

        try (ZipFile zf = new ZipFile(zip1.name)) {
            for (int i = 0; i < NN; i++) {
                executor.submit(() -> verifyNoCrash(zip1));
            }
            // delete the "zip1"  and create a new one to test
            Zip zip2 = new Zip(name, r.nextInt(ENUM), r.nextInt(ESZ), false, true);
            /*
                System.out.println("========================================");
                System.out.printf("    zip1=%s, mt=%d, enum=%d%n    ->attrs=[key=%s, sz=%d, mt=%d]%n",
                    zip1.name, zip1.lastModified, zip1.entries.size(),
                    zip1.attrs.fileKey(), zip1.attrs.size(), zip1.attrs.lastModifiedTime().toMillis());
                System.out.printf("    zip2=%s, mt=%d, enum=%d%n    ->attrs=[key=%s, sz=%d, mt=%d]%n",
                    zip2.name, zip2.lastModified, zip2.entries.size(),
                    zip2.attrs.fileKey(), zip2.attrs.size(), zip2.attrs.lastModifiedTime().toMillis());
            */
            for (int i = 0; i < NN; i++) {
                executor.submit(() -> doTest(zip2));
            }
        }
    }

   // overwrite the "zip1"  and create a new one to test. So the two zip files
   // have the same fileKey, but probably different lastModified()
    static void testCachedOverwrite() throws Throwable {
        String name = "testCachedOverWrite-" + r.nextInt() + ".zip";
        Zip zip1 = new Zip(name, r.nextInt(ENUM), r.nextInt(ESZ), false, true);
        try (ZipFile zf = new ZipFile(zip1.name)) {
            for (int i = 0; i < NN; i++) {
                executor.submit(() -> verifyNoCrash(zip1));
            }
            // overwrite the "zip1"  with new contents
            Zip zip2 = new Zip(name, r.nextInt(ENUM), r.nextInt(ESZ), false, false);
            for (int i = 0; i < NN; i++) {
                executor.submit(() -> doTest(zip2));
            }
        }
    }

    // just check the entries and contents. since the file has been either overwritten
    // or deleted/rewritten, we only care if it crahes or not.
    static void verifyNoCrash(Zip zip) throws RuntimeException {
        try (ZipFile zf = new ZipFile(zip.name)) {
            List<ZipEntry> zlist = new ArrayList<>(zip.entries.keySet());
            String[] elist = zf.stream().map(e -> e.getName()).toArray(String[]::new);
            if (!Arrays.equals(elist,
                               zlist.stream().map( e -> e.getName()).toArray(String[]::new)))
            {
                //System.out.printf("++++++ LIST NG [%s] entries.len=%d, expected=%d+++++++%n",
                //                  zf.getName(), elist.length, zlist.size());
                return;
            }
            for (ZipEntry ze : zlist) {
                byte[] zdata = zip.entries.get(ze);
                ZipEntry e = zf.getEntry(ze.getName());
                if (e != null) {
                    checkEqual(e, ze);
                    if (!e.isDirectory()) {
                        // check with readAllBytes
                        try (InputStream is = zf.getInputStream(e)) {
                            if (!Arrays.equals(zdata, is.readAllBytes())) {
                                //System.out.printf("++++++ BYTES NG  [%s]/[%s] ++++++++%n",
                                //                  zf.getName(), ze.getName());
                            }
                        }
                    }
                }
            }
        } catch (Throwable t) {
            // t.printStackTrace();
            // fail(t.toString());
        }
    }

    static void checkEqual(ZipEntry x, ZipEntry y) {
        if (x.getName().equals(y.getName()) &&
            x.isDirectory() == y.isDirectory() &&
            x.getMethod() == y.getMethod() &&
            (x.getTime() / 2000) == y.getTime() / 2000 &&
            x.getSize() == y.getSize() &&
            x.getCompressedSize() == y.getCompressedSize() &&
            x.getCrc() == y.getCrc() &&
            x.getComment().equals(y.getComment())
        ) {
            pass();
        } else {
            fail(x + " not equal to " + y);
            System.out.printf("      %s       %s%n", x.getName(), y.getName());
            System.out.printf("      %d       %d%n", x.getMethod(), y.getMethod());
            System.out.printf("      %d       %d%n", x.getTime(), y.getTime());
            System.out.printf("      %d       %d%n", x.getSize(), y.getSize());
            System.out.printf("      %d       %d%n", x.getCompressedSize(), y.getCompressedSize());
            System.out.printf("      %d       %d%n", x.getCrc(), y.getCrc());
            System.out.println("-----------------");
        }
    }

    static void doTest(Zip zip) throws RuntimeException {
        //Thread me = Thread.currentThread();
        try (ZipFile zf = new ZipFile(zip.name)) {
            doTest0(zip, zf);
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    static void doTest0(Zip zip, ZipFile zf) throws Throwable {
        // (0) check zero-length entry name, no AIOOBE
        try {
            check(zf.getEntry("") == null);
        } catch (Throwable t) {
            unexpected(t);
        }

        List<ZipEntry> list = new ArrayList<>(zip.entries.keySet());
        // (1) check entry list, in expected order
        if (!check(Arrays.equals(
                list.stream().map( e -> e.getName()).toArray(String[]::new),
                zf.stream().map( e -> e.getName()).toArray(String[]::new)))) {
            return;
        }
        // (2) shuffle, and check each entry and its bytes
        Collections.shuffle(list);
        for (ZipEntry ze : list) {
            byte[] data = zip.entries.get(ze);
            ZipEntry e = zf.getEntry(ze.getName());
            checkEqual(e, ze);
            if (!e.isDirectory()) {
                // check with readAllBytes
                try (InputStream is = zf.getInputStream(e)) {
                    check(Arrays.equals(data, is.readAllBytes()));
                }
                // check with smaller sized buf
                try (InputStream is = zf.getInputStream(e)) {
                    byte[] buf = new byte[(int)e.getSize()];
                    int sz = r.nextInt((int)e.getSize()/4 + 1) + 1;
                    int off = 0;
                    int n;
                    while ((n = is.read(buf, off, buf.length - off)) > 0) {
                        off += n;
                    }
                    check(is.read() == -1);
                    check(Arrays.equals(data, buf));
                }
            }
        }
    }

    private static class Zip {
        final String name;
        final Map<ZipEntry, byte[]> entries;
        BasicFileAttributes attrs;
        long lastModified;

        Zip(String name, int num, int szMax, boolean prefix, boolean clean) {
            this.name = name;
            entries = new LinkedHashMap<>(num);
            try {
                Path p = Paths.get(name);
                if (clean) {
                    Files.deleteIfExists(p);
                }
                paths.add(p);
            } catch (Exception x) {
                throw new RuntimeException(x);
            }

            try (FileOutputStream fos = new FileOutputStream(name);
                 BufferedOutputStream bos = new BufferedOutputStream(fos);
                 ZipOutputStream zos = new ZipOutputStream(bos))
            {
                if (prefix) {
                    byte[] bytes = new byte[r.nextInt(1000)];
                    r.nextBytes(bytes);
                    bos.write(bytes);
                }
                CRC32 crc = new CRC32();
                for (int i = 0; i < num; i++) {
                    String ename = "entry-" + i + "-name-" + r.nextLong();
                    ZipEntry ze = new ZipEntry(ename);
                    int method = r.nextBoolean() ? ZipEntry.STORED : ZipEntry.DEFLATED;
                    writeEntry(zos, crc, ze, method, szMax);
                }
                // add some manifest entries
                for (int i = 0; i < r.nextInt(20); i++) {
                    String meta = "META-INF/" + "entry-" + i + "-metainf-" + r.nextLong();
                    ZipEntry ze = new ZipEntry(meta);
                    writeEntry(zos, crc, ze, ZipEntry.STORED, szMax);
                }
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
            try {
                this.attrs = Files.readAttributes(Paths.get(name), BasicFileAttributes.class);
                this.lastModified = new File(name).lastModified();
            } catch (Exception x) {
                throw new RuntimeException(x);
            }
        }

        private void writeEntry(ZipOutputStream zos, CRC32 crc,
                                ZipEntry ze, int method, int szMax)
            throws IOException
        {
            ze.setMethod(method);
            byte[] data = new byte[r.nextInt(szMax + 1)];
            r.nextBytes(data);
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

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void pass(String msg) {System.out.println(msg); passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}

    static boolean check(boolean cond) {if (cond) pass(); else fail(); return cond;}

    public static void main(String[] args) {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
