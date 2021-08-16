/*
 * Copyright (c) 2006, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6434207 6442687 6984046
 * @modules jdk.jartool
 * @summary Ensure that jar ufm actually updates the
 * existing jar file's manifest with contents of the
 * manifest file.
 */

import java.io.*;
import java.util.logging.*;
import java.util.spi.ToolProvider;
import java.util.zip.*;

public class UpdateManifest {
    static PrintStream out = System.out;
    static PrintStream err = System.err;
    static boolean debug = true;

    static final ToolProvider JAR_TOOL = ToolProvider.findFirst("jar")
        .orElseThrow(() ->
            new RuntimeException("jar tool not found")
        );

    static final Logger JAR_LOGGER = Logger.getLogger("java.util.jar");

    public static void realMain(String[] args) throws Throwable {
        if (args.length == 0) {
            debug = false;
            File tmp = File.createTempFile("system-out-err", ".txt");
            tmp.deleteOnExit();
            out = new PrintStream(new FileOutputStream(tmp));
            err = out;
            // Attributes.read() can log a message we don't care to see.
            JAR_LOGGER.setLevel(Level.OFF);
        }

        try { testManifestExistence(); } catch (Throwable t) { unexpected(t); }
        try { testManifestContents(); } catch (Throwable t) { unexpected(t); }
    }

    static void testManifestExistence() throws Throwable {
        // Create a file to put in a jar file
        File existence = createTextFile("existence");

        // Create a jar file, specifying a Main-Class
        final String jarFileName = "um-existence.jar";
        new File(jarFileName).delete(); // remove pre-existing first!
        int status = JAR_TOOL.run(out, err, "cfe", jarFileName,
                                  "Hello", existence.getPath());
        check(status == 0);
        checkManifest(jarFileName, "Hello");

        // Update that jar file by changing the Main-Class
        status = JAR_TOOL.run(out, err, "ufe", jarFileName, "Bye");
        check(status == 0);
        checkManifest(jarFileName, "Bye");
    }

    static void testManifestContents() throws Throwable {
        // Create some strings we expect to find in the updated manifest
        final String animal =
            "Name: animal/marsupial";
        final String specTitle =
            "Specification-Title: Wombat";

        // Create a text file with manifest entries
        File manifestOrig = File.createTempFile("manifestOrig", ".txt");
        if (!debug) manifestOrig.deleteOnExit();
        PrintWriter pw = new PrintWriter(manifestOrig);
        pw.println("Manifest-Version: 1.0");
        pw.println("Created-By: 1.7.0-internal (Oracle Corporation)");
        pw.println("");
        pw.println(animal);
        pw.println(specTitle);
        pw.close();

        File hello = createTextFile("hello");

        // Create a jar file
        final String jarFileName = "um-test.jar";
        new File(jarFileName).delete(); // remove pre-existing first!
        int status = JAR_TOOL.run(out, err, "cfm", jarFileName,
                                  manifestOrig.getPath(), hello.getPath());
        check(status == 0);

        // Create a new manifest, to use in updating the jar file.
        File manifestUpdate = File.createTempFile("manifestUpdate", ".txt");
        if (!debug) manifestUpdate.deleteOnExit();
        pw = new PrintWriter(manifestUpdate);
        final String createdBy =
            "Created-By: 1.7.0-special (Oracle Corporation)";
        final String specVersion =
            "Specification-Version: 1.0.0.0";
        pw.println(createdBy); // replaces line in the original
        pw.println("");
        pw.println(animal);
        pw.println(specVersion); // addition to animal/marsupial section
        pw.close();

        // Update jar file with manifest
        status = JAR_TOOL.run(out, err, "ufm",
                              jarFileName, manifestUpdate.getPath());
        check(status == 0);

        // Extract jar, and verify contents of manifest file
        File f = new File(jarFileName);
        if (!debug) f.deleteOnExit();
        ZipFile zf = new ZipFile(f);
        ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
        BufferedReader r = new BufferedReader(
            new InputStreamReader(zf.getInputStream(ze)));
        r.readLine(); // skip Manifest-Version
        check(r.readLine().equals(createdBy));
        r.readLine(); // skip blank line
        check(r.readLine().equals(animal));
        String s = r.readLine();
        if (s.equals(specVersion)) {
            check(r.readLine().equals(specTitle));
        } else if (s.equals(specTitle)) {
            check(r.readLine().equals(specVersion));
        } else {
            fail("did not match specVersion nor specTitle");
        }
        zf.close();
    }

    // --------------------- Convenience ---------------------------

    static File createTextFile(String name) throws Throwable {
        // Create a text file to put in a jar file
        File rc = File.createTempFile(name, ".txt");
        if (!debug) rc.deleteOnExit();
        PrintWriter pw = new PrintWriter(rc);
        pw.println("hello, world");
        pw.close();
        return rc;
    }

    static void checkManifest(String jarFileName, String mainClass)
                throws Throwable {
        File f = new File(jarFileName);
        if (!debug) f.deleteOnExit();
        ZipFile zf = new ZipFile(f);
        ZipEntry ze = zf.getEntry("META-INF/MANIFEST.MF");
        BufferedReader r = new BufferedReader(
            new InputStreamReader(zf.getInputStream(ze)));
        String line = r.readLine();
        while (line != null && !(line.startsWith("Main-Class:"))) {
            line = r.readLine();
        }
        if (line == null) {
            fail("Didn't find Main-Class in manifest");
        } else {
            check(line.equals("Main-Class: " + mainClass));
        }
        zf.close();
    }

    // --------------------- Infrastructure ---------------------------

    static volatile int passed = 0, failed = 0;

    static void pass() {
        passed++;
    }

    static void fail() {
        failed++;
        Thread.dumpStack();
    }

    static void fail(String msg) {
        System.out.println(msg);
        fail();
    }

    static void unexpected(Throwable t) {
        failed++;
        t.printStackTrace();
    }

    static void check(boolean cond) {
        if (cond)
            pass();
        else
            fail();
    }

    static void equal(Object x, Object y) {
        if ((x == null) ? (y == null) : x.equals(y))
            pass();
        else
            fail(x + " not equal to " + y);
    }

    public static void main(String[] args) throws Throwable {
        try {
            realMain(args);
        } catch (Throwable t) {
            unexpected(t);
        }
        System.out.println("\nPassed = " + passed + " failed = " + failed);
        if (failed > 0)
            throw new AssertionError("Some tests failed");
    }
}
