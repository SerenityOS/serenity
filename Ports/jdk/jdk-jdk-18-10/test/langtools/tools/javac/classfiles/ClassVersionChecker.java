/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7157626 8001112 8188870 8173382 8193290 8205619 8245586 8257453
 * @summary Test major version for all legal combinations for -source and -target
 * @author sgoel
 *
 * @modules jdk.compiler
 */

import java.io.*;
import java.nio.*;
import java.util.*;
import java.util.regex.*;

/*
 * If not explicitly specified the latest source and latest target
 * values are the defaults. If explicitly specified, the target value
 * has to be greater than or equal to the source value.
 */
public class ClassVersionChecker {
    private static enum Version {
        SEVEN("7", 51),
        EIGHT("8", 52),
        NINE("9", 53),
        TEN("10", 54),
        ELEVEN("11", 55),
        TWELVE("12", 56),
        THIRTEEN("13", 57),
        FOURTEEN("14", 58),
        FIFTEEN("15", 59),
        SIXTEEN("16", 60),
        SEVENTEEN("17", 61),
        EIGHTEEN("18", 62);

        private Version(String release, int classFileVer) {
            this.release = release;
            this.classFileVer = classFileVer;
        }
        private final String release;
        private final int classFileVer;

        String release() {return release;}
        int classFileVer() {return classFileVer;}
    }

    static final Version CURRENT;
    static {
        Version[] versions = Version.values();
        int index = versions.length;
        CURRENT = versions[index - 1];
    }

    int errors;

    File javaFile = null;

    public static void main(String[] args) throws Throwable {
        new ClassVersionChecker().run();
    }

    void run() throws Exception {
        writeTestFile();
        /*
         * Rules applicable for -source and -target combinations:
         * 1. If both empty, version num is for the current release
         * 2. If source is not empty and target is empty, version is
         * based on the current release
         * 3. If both non-empty, version is based on target
         */
        test("", "", CURRENT.classFileVer());
        for (Version source : Version.values()) {
            test(source.release(), "", CURRENT.classFileVer()); // no target
            for (Version target : Version.values()) {
                if (target.compareTo(source) < 0)
                    continue; // Target < source not a valid set of arguments
                else {
                    logMsg("Running for src = " + source + " target = "+ target +
                           " expected = " + target.classFileVer());
                    test(source.release(), target.release(), target.classFileVer());
                }
            }
        }

        if (errors > 0)
            throw new Exception(errors + " errors found");
    }

    void test(String i, String j, int expected) {
        File classFile = compileTestFile(i, j, javaFile);
        short majorVer = getMajorVersion(classFile);
        checkVersion(majorVer, expected);
    }

    void writeTestFile() throws IOException {
        javaFile = new File("Test.java");
        try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(javaFile)));) {
            out.println("class Test { ");
            out.println("  public void foo() { }");
            out.println("}");
        } catch (IOException ioe) {
            error("IOException while creating Test.java" + ioe);
        }
    }

    File compileTestFile(String i, String j, File f) {
        int rc = -1;
        // Src and target are empty
        if (i.isEmpty() && j.isEmpty() ) {
            rc = compile("-g", f.getPath());
        } else if( j.isEmpty()) {  // target is empty
            rc = compile("-source", i, "-g", f.getPath());
        } else {
            rc = compile("-source", i, "-target", j, "-g", f.getPath());
        }
        if (rc != 0)
            throw new Error("compilation failed. rc=" + rc);
        String path = f.getPath();
        return new File(path.substring(0, path.length() - 5) + ".class");
    }

    int compile(String... args) {
        return com.sun.tools.javac.Main.compile(args);
    }

    void logMsg (String str) {
        System.out.println(str);
    }

    short getMajorVersion(File f) {
        List<String> args = new ArrayList<String>();
        short majorVer = 0;
        try(DataInputStream in = new DataInputStream(new BufferedInputStream(new FileInputStream(f)));) {
            in.readInt();
            in.readShort();
            majorVer = in.readShort();
            System.out.println("major version:" +  majorVer);
        } catch (IOException e) {
            error("IOException while reading Test.class" + e);
        }
        return majorVer;
    }

    void checkVersion(short majorVer, int expected) {
        if (majorVer != expected ) {
            error("versions did not match, Expected: " + expected + "Got: " + majorVer);
        }
    }

    void error(String msg) {
       System.out.println("error: " + msg);
       errors++;
    }
}
