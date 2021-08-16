/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7068051
 * @summary SIGSEGV in PhaseIdealLoop::build_loop_late_post on T5440
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run main/othervm -showversion -Xbatch compiler.c2.Test7068051
 */

package compiler.c2;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.process.OutputAnalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Test7068051 {
    private static final String SELF_NAME = Test7068051.class.getSimpleName();
    private static final String SELF_FILE_NAME = SELF_NAME + ".java";
    private static final String JAR_NAME = "foo.jar";
    private static final String TEST_PATH = System.getProperty("test.src");
    private static final Path CURRENT_DIR = Paths.get(".");
    private static final Path TEST_SOURCE_PATH = Paths.get(TEST_PATH, SELF_FILE_NAME);

    public static void main (String[] args) throws IOException {
        createTestJarFile();
        System.out.println("Running test...");

        try (ZipFile zf = new ZipFile(JAR_NAME)) {

            Enumeration<? extends ZipEntry> entries = zf.entries();
            ArrayList<String> names = new ArrayList<String>();
            while (entries.hasMoreElements()) {
                names.add(entries.nextElement().getName());
            }

            byte[] bytes = new byte[16];
            for (String name : names) {
                ZipEntry e = zf.getEntry(name);

                if (e.isDirectory()) {
                    continue;
                }

                try (final InputStream is = zf.getInputStream(e)) {
                    try {
                        while (is.read(bytes) >= 0) {
                        }
                    } catch (IOException x) {
                        System.out.println("..................................");
                        System.out.println("          -->  is :" + is);
                        System.out.println("          is.hash :" + is.hashCode());
                        System.out.println();
                        System.out.println("           e.name :" + e.getName());
                        System.out.println("           e.hash :" + e.hashCode());
                        System.out.println("         e.method :" + e.getMethod());
                        System.out.println("           e.size :" + e.getSize());
                        System.out.println("          e.csize :" + e.getCompressedSize());
                        System.out.println("..................................");

                        throw new AssertionError("IOException was throwing while read the archive. Test failed.", x);
                    }
                }
            }
        }
        System.out.println("Test passed.");
    }

    private static void createTestJarFile() {
        ArrayList<String> jarOptions = new ArrayList<>();

        // jar cf foo.jar *
        System.out.println("Creating jar file..");
        jarOptions.add("cf");
        jarOptions.add(JAR_NAME);
        try {
            for (int i = 0; i < 100; ++i) {
                Path temp = Files.createTempFile(CURRENT_DIR, SELF_NAME, ".java");
                Files.copy(TEST_SOURCE_PATH, temp, StandardCopyOption.REPLACE_EXISTING);
                jarOptions.add(temp.toString());
            }
        } catch (IOException ex) {
            throw new AssertionError("TESTBUG: Creating temp files failed.", ex);
        }
        runJar(jarOptions);

        // jar -uf0 foo.jar Test7068051.java
        System.out.println("Adding unpacked file...");
        jarOptions.clear();
        jarOptions.add("-uf0");
        jarOptions.add(JAR_NAME);
        jarOptions.add(TEST_SOURCE_PATH.toString());
        runJar(jarOptions);
    }

    private static void runJar(List<String> params) {
        JDKToolLauncher jar = JDKToolLauncher.create("jar");
        for (String p : params) {
            jar.addToolArg(p);
        }
        ProcessBuilder pb = new ProcessBuilder(jar.getCommand());
        try {
            OutputAnalyzer output = new OutputAnalyzer(pb.start());
            output.shouldHaveExitValue(0);
        } catch (IOException ex) {
            throw new AssertionError("TESTBUG: jar failed.", ex);
        }
    }
}
