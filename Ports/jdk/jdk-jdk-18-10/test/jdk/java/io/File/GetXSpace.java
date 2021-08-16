/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4057701 6286712 6364377 8181919
 * @requires (os.family == "linux" | os.family == "mac" |
 *            os.family == "windows")
 * @summary Basic functionality of File.get-X-Space methods.
 * @run main/othervm -Djava.security.manager=allow GetXSpace
 */

import java.io.BufferedReader;
import java.io.File;
import java.io.FilePermission;
import java.io.InputStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.FileStore;
import java.nio.file.Path;
import java.security.Permission;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.System.err;
import static java.lang.System.out;

public class GetXSpace {

    private static SecurityManager [] sma = { null, new Allow(), new DenyFSA(),
                                              new DenyRead() };

    private static final String OS_NAME = System.getProperty("os.name");
    private static final boolean IS_MAC = OS_NAME.startsWith("Mac");
    private static final boolean IS_WIN = OS_NAME.startsWith("Windows");

    // FileSystem Total Used Available Use% MountedOn
    private static final Pattern DF_PATTERN = Pattern.compile("([^\\s]+)\\s+(\\d+)\\s+\\d+\\s+(\\d+)\\s+\\d+%\\s+([^\\s].*)\n");

    private static int fail = 0;
    private static int pass = 0;
    private static Throwable first;

    static void reset() {
        fail = 0;
        pass = 0;
        first = null;
    }

    static void pass() {
        pass++;
    }

    static void fail(String p) {
        setFirst(p);
        System.err.format("FAILED: %s%n", p);
        fail++;
    }

    static void fail(String p, long exp, String cmp, long got) {
        String s = String.format("'%s': %d %s %d", p, exp, cmp, got);
        setFirst(s);
        System.err.format("FAILED: %s%n", s);
        fail++;
    }

    private static void fail(String p, Class ex) {
        String s = String.format("'%s': expected %s - FAILED%n", p, ex.getName());
        setFirst(s);
        System.err.format("FAILED: %s%n", s);
        fail++;
    }

    private static void setFirst(String s) {
        if (first == null) {
            first = new RuntimeException(s);
        }
    }

    private static class Space {
        private static final long KSIZE = 1024;
        private final String name;
        private final long total;
        private final long free;

        Space(String total, String free, String name) {
            try {
                this.total = Long.valueOf(total) * KSIZE;
                this.free = Long.valueOf(free) * KSIZE;
            } catch (NumberFormatException x) {
                throw new RuntimeException("the regex should have caught this", x);
            }
            this.name = name;
        }

        String name() { return name; }
        long total() { return total; }
        long free() { return free; }
        boolean woomFree(long freeSpace) {
            return ((freeSpace >= (free / 10)) && (freeSpace <= (free * 10)));
        }
        public String toString() {
            return String.format("%s (%d/%d)", name, free, total);
        }
    }

    private static ArrayList<Space> space(String f) throws IOException {
        ArrayList<Space> al = new ArrayList<>();

        String cmd = "df -k -P" + (f == null ? "" : " " + f);
        StringBuilder sb = new StringBuilder();
        Process p = Runtime.getRuntime().exec(cmd);
        try (BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            String s;
            int i = 0;
            while ((s = in.readLine()) != null) {
                // skip header
                if (i++ == 0) continue;
                sb.append(s).append("\n");
            }
        }
        out.println(sb);

        Matcher m = DF_PATTERN.matcher(sb);
        int j = 0;
        while (j < sb.length()) {
            if (m.find(j)) {
                // swap can change while this test is running
                if (!m.group(1).equals("swap")) {
                    String name = f;
                    if (name == null) {
                        // cygwin's df lists windows path as FileSystem (1st group)
                        name = IS_WIN ? m.group(1) : m.group(4);
                    }
                    al.add(new Space(m.group(2), m.group(3), name));;
                }
                j = m.end() + 1;
            } else {
                throw new RuntimeException("unrecognized df output format: "
                                           + "charAt(" + j + ") = '"
                                           + sb.charAt(j) + "'");
            }
        }

        if (al.size() == 0) {
            // df did not produce output
            String name = (f == null ? "" : f);
            al.add(new Space("0", "0", name));
        }
        return al;
    }

    private static void tryCatch(Space s) {
        out.format("%s:%n", s.name());
        File f = new File(s.name());
        SecurityManager sm = System.getSecurityManager();
        if (sm instanceof Deny) {
            String fmt = "  %14s: \"%s\" thrown as expected%n";
            try {
                f.getTotalSpace();
                fail(s.name(), SecurityException.class);
            } catch (SecurityException x) {
                out.format(fmt, "getTotalSpace", x);
                pass();
            }
            try {
                f.getFreeSpace();
                fail(s.name(), SecurityException.class);
            } catch (SecurityException x) {
                out.format(fmt, "getFreeSpace", x);
                pass();
            }
            try {
                f.getUsableSpace();
                fail(s.name(), SecurityException.class);
            } catch (SecurityException x) {
                out.format(fmt, "getUsableSpace", x);
                pass();
            }
        }
    }

    private static void compare(Space s) {
        File f = new File(s.name());
        long ts = f.getTotalSpace();
        long fs = f.getFreeSpace();
        long us = f.getUsableSpace();

        out.format("%s:%n", s.name());
        String fmt = "  %-4s total= %12d free = %12d usable = %12d%n";
        out.format(fmt, "df", s.total(), 0, s.free());
        out.format(fmt, "getX", ts, fs, us);

        // if the file system can dynamically change size, this check will fail
        if (ts != s.total()) {
            long blockSize = 1;
            long numBlocks = 0;
            try {
                FileStore fileStore = Files.getFileStore(f.toPath());
                blockSize = fileStore.getBlockSize();
                numBlocks = fileStore.getTotalSpace()/blockSize;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }


            // On macOS, the number of 1024 byte blocks might be incorrectly
            // calculated by 'df' using integer division by 2 of the number of
            // 512 byte blocks, resulting in a size smaller than the actual
            // value when the number of blocks is odd.
            if (!IS_MAC || blockSize != 512 || numBlocks % 2 == 0
                || ts - s.total() != 512) {
                fail(s.name(), s.total(), "!=", ts);
            }
        } else {
            pass();
        }

        // unix df returns statvfs.f_bavail
        long tsp = (!IS_WIN ? us : fs);
        if (!s.woomFree(tsp)) {
            fail(s.name(), s.free(), "??", tsp);
        } else {
            pass();
        }

        if (fs > s.total()) {
            fail(s.name(), s.total(), ">", fs);
        } else {
            pass();
        }

        if (us > s.total()) {
            fail(s.name(), s.total(), ">", us);
        } else {
            pass();
        }
    }

    private static String FILE_PREFIX = "/getSpace.";
    private static void compareZeroNonExist() {
        File f;
        while (true) {
            f = new File(FILE_PREFIX + Math.random());
            if (f.exists()) {
                continue;
            }
            break;
        }

        long [] s = { f.getTotalSpace(), f.getFreeSpace(), f.getUsableSpace() };

        for (int i = 0; i < s.length; i++) {
            if (s[i] != 0L) {
                fail(f.getName(), s[i], "!=", 0L);
            } else {
                pass();
            }
        }
    }

    private static void compareZeroExist() {
        try {
            File f = File.createTempFile("tmp", null, new File("."));

            long [] s = { f.getTotalSpace(), f.getFreeSpace(), f.getUsableSpace() };

            for (int i = 0; i < s.length; i++) {
                if (s[i] == 0L) {
                    fail(f.getName(), s[i], "==", 0L);
                } else {
                    pass();
                }
            }
        } catch (IOException x) {
            x.printStackTrace();
            fail("Couldn't create temp file for test");
        }
    }

    private static class Allow extends SecurityManager {
        public void checkRead(String file) {}
        public void checkPermission(Permission p) {}
        public void checkPermission(Permission p, Object context) {}
    }

    private static class Deny extends SecurityManager {
        public void checkPermission(Permission p) {
            if (p.implies(new RuntimePermission("setSecurityManager"))
                || p.implies(new RuntimePermission("getProtectionDomain")))
              return;
            super.checkPermission(p);
        }

        public void checkPermission(Permission p, Object context) {
            if (p.implies(new RuntimePermission("setSecurityManager"))
                || p.implies(new RuntimePermission("getProtectionDomain")))
              return;
            super.checkPermission(p, context);
        }
    }

    private static class DenyFSA extends Deny {
        private String err = "sorry - getFileSystemAttributes";

        public void checkPermission(Permission p) {
            if (p.implies(new RuntimePermission("getFileSystemAttributes")))
                throw new SecurityException(err);
            super.checkPermission(p);
        }

        public void checkPermission(Permission p, Object context) {
            if (p.implies(new RuntimePermission("getFileSystemAttributes")))
                throw new SecurityException(err);
            super.checkPermission(p, context);
        }
    }

    private static class DenyRead extends Deny {
        private String err = "sorry - checkRead()";

        public void checkRead(String file) {
            throw new SecurityException(err);
        }
    }

    private static int testFile(Path dir) {
        String dirName = dir.toString();
        out.format("--- Testing %s%n", dirName);
        ArrayList<Space> l;
        try {
            l = space(dirName);
        } catch (IOException x) {
            throw new RuntimeException(dirName + " can't get file system information", x);
        }
        compare(l.get(0));

        if (fail != 0) {
            err.format("%d tests: %d failure(s); first: %s%n",
                fail + pass, fail, first);
        } else {
            out.format("all %d tests passed%n", fail + pass);
        }

        return fail != 0 ? 1 : 0;
    }

    private static int testDF() {
        out.println("--- Testing df");
        // Find all of the partitions on the machine and verify that the size
        // returned by "df" is equivalent to File.getXSpace() values.
        ArrayList<Space> l;
        try {
            l = space(null);
        } catch (IOException x) {
            throw new RuntimeException("can't get file system information", x);
        }
        if (l.size() == 0)
            throw new RuntimeException("no partitions?");

        for (int i = 0; i < sma.length; i++) {
            System.setSecurityManager(sma[i]);
            SecurityManager sm = System.getSecurityManager();
            if (sma[i] != null && sm == null)
                throw new RuntimeException("Test configuration error "
                                           + " - can't set security manager");

            out.format("%nSecurityManager = %s%n" ,
                       (sm == null ? "null" : sm.getClass().getName()));
            for (var s : l) {
                if (sm instanceof Deny) {
                    tryCatch(s);
                } else {
                    compare(s);
                    compareZeroNonExist();
                    compareZeroExist();
                }
            }
        }

        System.setSecurityManager(null);

        if (fail != 0) {
            err.format("%d tests: %d failure(s); first: %s%n",
                fail + pass, fail, first);
        } else {
            out.format("all %d tests passed%n", fail + pass);
        }

        return fail != 0 ? 1 : 0;
    }

    private static void perms(File file, boolean allow) throws IOException {
        file.setExecutable(allow, false);
        file.setReadable(allow, false);
        file.setWritable(allow, false);
    }

    private static void deny(Path path) throws IOException {
        perms(path.toFile(), false);
    }

    private static void allow(Path path) throws IOException {
        perms(path.toFile(), true);
    }

    public static void main(String[] args) throws Exception {
        int failedTests = testDF();
        reset();

        Path tmpDir = Files.createTempDirectory(null);
        Path tmpSubdir = Files.createTempDirectory(tmpDir, null);
        Path tmpFile = Files.createTempFile(tmpSubdir, "foo", null);

        deny(tmpSubdir);
        failedTests += testFile(tmpFile);

        allow(tmpSubdir);
        Files.delete(tmpFile);
        Files.delete(tmpSubdir);
        Files.delete(tmpDir);

        if (failedTests > 0) {
            throw new RuntimeException(failedTests + " test(s) failed");
        }
    }
}
