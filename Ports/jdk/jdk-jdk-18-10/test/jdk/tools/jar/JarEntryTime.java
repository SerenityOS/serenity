/*
 * Copyright (c) 2006, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4225317 6969651
 * @modules jdk.jartool
 * @summary Check extracted files have date as per those in the .jar file
 */

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.attribute.FileTime;
import java.util.Date;
import java.util.TimeZone;
import java.util.spi.ToolProvider;

public class JarEntryTime {
    static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
        .orElseThrow(() ->
            new RuntimeException("jar tool not found")
        );


    // ZipEntry's mod date has 2 seconds precision: give extra time to
    // allow for e.g. rounding/truncation and networked/samba drives.
    static final long PRECISION = 10000L;

    static final TimeZone TZ = TimeZone.getDefault();
    static final boolean DST = TZ.inDaylightTime(new Date());

    static boolean cleanup(File dir) throws Throwable {
        boolean rc = true;
        File[] x = dir.listFiles();
        if (x != null) {
            for (int i = 0; i < x.length; i++) {
                rc &= x[i].delete();
            }
        }
        return rc & dir.delete();
    }

    static void extractJar(File jarFile, boolean useExtractionTime) throws Throwable {
        String javahome = System.getProperty("java.home");
        String jarcmd = javahome + File.separator + "bin" + File.separator + "jar";
        String[] args;
        if (useExtractionTime) {
            args = new String[] {
                jarcmd,
                "-J-Dsun.tools.jar.useExtractionTime=true",
                "xf",
                jarFile.getName() };
        } else {
            args = new String[] {
                jarcmd,
                "xf",
                jarFile.getName() };
        }
        Process p = Runtime.getRuntime().exec(args);
        check(p != null && (p.waitFor() == 0));
    }

    public static void realMain(String[] args) throws Throwable {

        File dirOuter = new File("outer");
        File dirInner = new File(dirOuter, "inner");
        File jarFile = new File("JarEntryTime.jar");
        File testFile = new File("JarEntryTimeTest.txt");

        // Remove any leftovers from prior run
        cleanup(dirInner);
        cleanup(dirOuter);
        jarFile.delete();
        testFile.delete();

        /* Create a directory structure
         * outer/
         *     inner/
         *         foo.txt
         * Set the lastModified dates so that outer is created now, inner
         * yesterday, and foo.txt created "earlier".
         */
        check(dirOuter.mkdir());
        check(dirInner.mkdir());
        File fileInner = new File(dirInner, "foo.txt");
        try (PrintWriter pw = new PrintWriter(fileInner)) {
            pw.println("hello, world");
        }

        // Get the "now" from the "last-modified-time" of the last file we
        // just created, instead of the "System.currentTimeMillis()", to
        // workaround the possible "time difference" due to nfs.
        final long now = fileInner.lastModified();
        final long earlier = now - (60L * 60L * 6L * 1000L);
        final long yesterday = now - (60L * 60L * 24L * 1000L);

        check(dirOuter.setLastModified(now));
        check(dirInner.setLastModified(yesterday));
        check(fileInner.setLastModified(earlier));

        // Make a jar file from that directory structure
        check(JAR_TOOL.run(System.out, System.err,
                           "cf", jarFile.getName(), dirOuter.getName()) == 0);
        check(jarFile.exists());

        check(cleanup(dirInner));
        check(cleanup(dirOuter));

        // Extract and check that the last modified values are those specified
        // in the archive
        extractJar(jarFile, false);
        check(dirOuter.exists());
        check(dirInner.exists());
        check(fileInner.exists());
        checkFileTime(dirOuter.lastModified(), now);
        checkFileTime(dirInner.lastModified(), yesterday);
        checkFileTime(fileInner.lastModified(), earlier);

        check(cleanup(dirInner));
        check(cleanup(dirOuter));

        try (PrintWriter pw = new PrintWriter(testFile)) {
            pw.println("hello, world");
        }
        final long start = testFile.lastModified();

        // Extract and check the last modified values are the current times.
        extractJar(jarFile, true);

        try (PrintWriter pw = new PrintWriter(testFile)) {
            pw.println("hello, world");
        }
        final long end = testFile.lastModified();

        check(dirOuter.exists());
        check(dirInner.exists());
        check(fileInner.exists());
        checkFileTime(start, dirOuter.lastModified(), end);
        checkFileTime(start, dirInner.lastModified(), end);
        checkFileTime(start, fileInner.lastModified(), end);

        check(cleanup(dirInner));
        check(cleanup(dirOuter));

        check(jarFile.delete());
        check(testFile.delete());
    }

    static void checkFileTime(long now, long original) {
        if (isTimeSettingChanged()) {
            return;
        }

        if (Math.abs(now - original) > PRECISION) {
            System.out.format("Extracted to %s, expected to be close to %s%n",
                FileTime.fromMillis(now), FileTime.fromMillis(original));
            fail();
        }
    }

    static void checkFileTime(long start, long now, long end) {
        if (isTimeSettingChanged()) {
            return;
        }

        if (now < start || now > end) {
            System.out.format("Extracted to %s, "
                              + "expected to be after %s and before %s%n",
                              FileTime.fromMillis(now),
                              FileTime.fromMillis(start),
                              FileTime.fromMillis(end));
            fail();
        }
    }

    private static boolean isTimeSettingChanged() {
        TimeZone currentTZ = TimeZone.getDefault();
        boolean currentDST = currentTZ.inDaylightTime(new Date());
        return (!currentTZ.equals(TZ) || currentDST != DST);
    }

    //--------------------- Infrastructure ---------------------------
    static volatile int passed = 0, failed = 0;
    static void pass() {passed++;}
    static void fail() {failed++; Thread.dumpStack();}
    static void fail(String msg) {System.out.println(msg); fail();}
    static void unexpected(Throwable t) {failed++; t.printStackTrace();}
    static void check(boolean cond) {if (cond) pass(); else fail();}
    static void equal(Object x, Object y) {
        if (x == null ? y == null : x.equals(y)) pass();
        else fail(x + " not equal to " + y);}
    public static void main(String[] args) throws Throwable {
        try {realMain(args);} catch (Throwable t) {unexpected(t);}
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0) throw new AssertionError("Some tests failed");}
}
