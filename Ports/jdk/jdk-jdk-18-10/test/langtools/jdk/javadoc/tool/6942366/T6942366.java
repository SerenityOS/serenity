/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6942366
 * @summary javadoc no longer inherits doc from sourcepath
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build p.Base Test
 * @run main T6942366
 */

import java.io.*;
import java.util.*;

public class T6942366 {
    public static void main(String... args) throws Exception {
        new T6942366().run();
    }

    File testSrc;
    File testClasses;
    int count;
    int errors;

    void run() throws Exception {
        testSrc = new File(System.getProperty("test.src"));
        testClasses = new File(System.getProperty("test.classes"));

        test(true,  false);
        test(false, true);
        test(true,  true);

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void test(boolean useSourcePath, boolean useClassPath) throws Exception {
        System.out.println("test " + (++count) + " sp:" + useSourcePath + " cp:" + useClassPath);
        File testDir = new File("test" + count);
        testDir.mkdirs();

        List<String> args = new ArrayList<String>();
        //args.add("-verbose");
        args.add("-d");
        args.add(testDir.getPath());
        if (useSourcePath) {
            args.add("-sourcepath");
            args.add(testSrc.getPath());
        }
        if (useClassPath) {
            args.add("-classpath");
            args.add(testClasses.getPath());
        } else {
            // override classpath to avoid stuff jtreg might have put on papth
            args.add("-classpath");
            args.add(".");
        }

        args.add(new File(testSrc, "Test.java").getPath());
        System.out.println("javadoc: " + args);

        int rc = jdk.javadoc.internal.tool.Main.execute(args.toArray(new String[args.size()]));
        if (rc != 0)
            throw new Exception("unexpected exit from javadoc, rc=" + rc);

        if (useSourcePath && useClassPath) {
            long srcLastMod = new File(testSrc, "Test.java").lastModified();
            long classLastMod = new File(testClasses, "Test.class").lastModified();
            System.out.println("Test.java last modified:  " + new Date(srcLastMod));
            System.out.println("Test.class last modified: " + new Date(classLastMod));
            System.out.println((srcLastMod > classLastMod ? "source" : "class") + " is newer");
        }

        String s = "javadoc-for-Base.m";
        boolean expect = useSourcePath;
        boolean found = contains(new File(testDir, "Test.html"), s);
        if (found) {
            if (expect)
                System.out.println("javadoc content \"" + s + "\" found, as expected");
            else
                error("javadoc content \"" + s + "\" found unexpectedly");
        } else {
            if (expect)
                error("javadoc content \"" + s + "\" not found");
            else
                System.out.println("javadoc content \"" + s + "\" not found, as expected");
        }

        System.out.println();
    }

    boolean contains(File f, String s) throws Exception {
        byte[] buf = new byte[(int) f.length()];
        try (DataInputStream in = new DataInputStream(new FileInputStream(f))) {
            in.readFully(buf);
        }
        return new String(buf).contains(s);
    }

    void error(String msg) {
        System.out.println("Error: " + msg);
        errors++;
    }

}

