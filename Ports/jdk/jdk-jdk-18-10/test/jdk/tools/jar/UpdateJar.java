/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7175845
 * @modules jdk.jartool
 * @summary jar -uf should not change file permission
 */

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.Set;
import java.util.spi.ToolProvider;

public class UpdateJar {
    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
        .orElseThrow(() ->
            new RuntimeException("jar tool not found")
        );

    private static void cleanup(String... fnames) throws Throwable {
        for (String fname : fnames) {
            Files.deleteIfExists(Paths.get(fname));
        }
    }

    public static void realMain(String[] args) throws Throwable {
        if (!System.getProperty("os.name").startsWith("Windows")) {
            String jar = "testUpdateJar.jar";
            String e0  = "testUpdateJar_entry0.txt";
            String e1  = "testUpdateJar_entry1.txt";
            cleanup(jar, e0, e1);
            try {
                try (FileOutputStream fos0 = new FileOutputStream(e0);
                     FileOutputStream fos1 = new FileOutputStream(e1)) {
                    fos0.write(0);
                    fos1.write(0);
                }
                String[] jarArgs = new String[] {"cfM0", jar, e0};
                if (JAR_TOOL.run(System.out, System.err, jarArgs) != 0) {
                    fail("Could not create jar file.");
                }
                Set<PosixFilePermission> pm = Files.getPosixFilePermissions(Paths.get(jar));
                jarArgs = new String[] {"uf", jar, e1};
                if (JAR_TOOL.run(System.out, System.err, jarArgs) != 0) {
                    fail("Could not create jar file.");
                }
                equal(pm, Files.getPosixFilePermissions(Paths.get(jar)));
            } finally {
                cleanup(jar, e0, e1);
            }
        }
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
