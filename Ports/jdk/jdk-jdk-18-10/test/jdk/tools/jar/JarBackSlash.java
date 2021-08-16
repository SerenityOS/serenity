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

/*
 * Portions Copyright (c) 2012 IBM Corporation
 */

/*
 * @test
 * @bug 7201156
 * @modules jdk.jartool
 * @summary jar tool fails to convert file separation characters for list and extract
 * @author Sean Chou
 */

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.spi.ToolProvider;

public class JarBackSlash {
    private static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
        .orElseThrow(() ->
            new RuntimeException("jar tool not found")
        );

    // used construct an entry JarBackSlash/dir/file.txt
    private static String JARBACKSLASH = "JarBackSlash";
    private static String DIR = "dir";
    private static String FILENAME = "file.txt";

    private static File createJarFile() throws IOException {
        File jarFile = File.createTempFile("JarBackSlashTest", ".jar");
        jarFile.deleteOnExit();

        try (JarOutputStream output = new JarOutputStream(new FileOutputStream(jarFile))) {
            JarEntry entry = new JarEntry(JARBACKSLASH + "/" + DIR + "/" + FILENAME);
            output.putNextEntry(entry);
        }

        return jarFile;
    }

    private static void testJarList(String jarFile) throws IOException {
        List<String> argList = new ArrayList<String>();
        argList.add("-tvf");
        argList.add(jarFile);
        argList.add(JARBACKSLASH + File.separatorChar + DIR + File.separatorChar + FILENAME);

        String jarArgs[] = new String[argList.size()];
        jarArgs = argList.toArray(jarArgs);

        PipedOutputStream pipedOutput = new PipedOutputStream();
        PipedInputStream pipedInput = new PipedInputStream(pipedOutput);
        PrintStream out = new PrintStream(pipedOutput);

        int rc = JAR_TOOL.run(out, System.err, jarArgs);
        if (rc != 0) {
            fail("Could not list jar file.");
        }

        out.flush();
        check(pipedInput.available() > 0);
    }


    private static void testJarExtract(String jarFile) throws IOException {
        List<String> argList = new ArrayList<String>();
        argList.add("-xvf");
        argList.add(jarFile);
        argList.add(JARBACKSLASH + File.separatorChar + DIR + File.separatorChar + FILENAME);

        String jarArgs[] = new String[argList.size()];
        jarArgs = argList.toArray(jarArgs);

        PipedOutputStream pipedOutput = new PipedOutputStream();
        PipedInputStream pipedInput = new PipedInputStream(pipedOutput);
        PrintStream out = new PrintStream(pipedOutput);

        int rc = JAR_TOOL.run(out, System.err, jarArgs);
        if (rc != 0) {
            fail("Could not list jar file.");
        }

        out.flush();
        check(pipedInput.available() > 0);
    }

    public static void realMain(String[] args) throws Throwable {
        File tmpJarFile = createJarFile();
        String tmpJarFilePath = tmpJarFile.getAbsolutePath();

        testJarList(tmpJarFilePath);
        testJarExtract(tmpJarFilePath);
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
