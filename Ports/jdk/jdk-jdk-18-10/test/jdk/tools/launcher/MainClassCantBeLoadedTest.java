/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8174694 8181033
 * @summary improve error message shown when main class can't be loaded
 * @compile MainClassCantBeLoadedTest.java
 * @run main MainClassCantBeLoadedTest
 */

import java.io.*;
import java.util.*;

public class MainClassCantBeLoadedTest extends TestHelper {
    private MainClassCantBeLoadedTest(){}

    @Test
    void testLoadingClassWithMissingSuper() throws Exception {
        if (!isEnglishLocale()) {
            return;
        }

        File cwd = new File(".");
        File srcDir = new File(cwd, "src");
        if (srcDir.exists()) {
            recursiveDelete(srcDir);
        }
        srcDir.mkdirs();

        /* we want to generate two classes A and B, where B is the superclass of A
         * class A has a main method
         */
        ArrayList<String> scratchpad = new ArrayList<>();
        scratchpad.add("public class A extends B {");
        scratchpad.add("    public static void main(String... args) {}");
        scratchpad.add("}");
        createFile(new File(srcDir, "A.java"), scratchpad);

        scratchpad.clear();
        scratchpad.add("class B {}");
        createFile(new File(srcDir, "B.java"), scratchpad);

        // let's compile both
        TestResult trCompilation = doExec(javacCmd,
                "-d", "out",
                new File(srcDir, "A.java").toString(),
                new File(srcDir, "B.java").toString());
        if (!trCompilation.isOK()) {
            System.err.println(trCompilation);
            throw new RuntimeException("Error: compiling");
        }

        // and now B is removed
        File outDir = new File(cwd, "out");
        File bClass = new File(outDir, "B.class");
        bClass.delete();

        // if A is executed
        TestResult trExecution = doExec(javaCmd, "-cp", "out", "A");
        // then this error message should be generated
        trExecution.contains("Error: Could not find or load main class A");
        trExecution.contains("Caused by: java.lang.NoClassDefFoundError: B");
        if (!trExecution.testStatus)
            System.err.println(trExecution);
    }

    @Test
    void testFailToInitializeMainClass() throws Exception {
        if (!isEnglishLocale()) {
            return;
        }

        File cwd = new File(".");
        File srcDir = new File(cwd, "src");
        if (srcDir.exists()) {
            recursiveDelete(srcDir);
        }
        srcDir.mkdirs();

        /* we want to generate class C that will resolve additional class
         */
        ArrayList<String> scratchpad = new ArrayList<>();
        scratchpad.add("public class C {");
        scratchpad.add("    public static void main(String... args) {");
        scratchpad.add("        try {");
        scratchpad.add("            System.out.println(\"loading of restricted class\");");
        scratchpad.add("        } catch (Exception e) {");
        scratchpad.add("            java.security.Provider p = new com.sun.crypto.provider.SunJCE();");
        scratchpad.add("            p.toString();");
        scratchpad.add("        }");
        scratchpad.add("    }");
        scratchpad.add("}");
        createFile(new File(srcDir, "C.java"), scratchpad);


        // Compile and execute C should succeed
        TestResult trCompilation = doExec(javacCmd,
            "--add-exports", "java.base/com.sun.crypto.provider=ALL-UNNAMED",
            "-d", "out",
            new File(srcDir, "C.java").toString());
        if (!trCompilation.isOK()) {
            System.err.println(trCompilation);
            throw new RuntimeException("Error: compiling");
        }

        TestResult trExecution = doExec(javaCmd,
            "--add-exports", "java.base/com.sun.crypto.provider=ALL-UNNAMED",
            "-cp", "out", "C");
        if (!trExecution.isOK()) {
            System.err.println(trExecution);
            throw new RuntimeException("Error: executing");
        }

        // Execute C with security manager will fail with AccessControlException
        trExecution = doExec(javaCmd,
            "-Djava.security.manager",
            "--add-exports", "java.base/com.sun.crypto.provider=ALL-UNNAMED",
            "-cp", "out", "C");

        // then this error message should be generated
        trExecution.contains("Error: Unable to initialize main class C");
        trExecution.contains("Caused by: java.security.AccessControlException: " +
            "access denied (\"java.lang.RuntimePermission\"" +
            " \"accessClassInPackage.com.sun.crypto.provider\")");
        if (!trExecution.testStatus)
            System.err.println(trExecution);
    }

    public static void main(String[] args) throws Exception {
        MainClassCantBeLoadedTest a = new MainClassCantBeLoadedTest();
        a.run(args);
        if (testExitValue > 0) {
            System.out.println("Total of " + testExitValue + " failed");
            throw new RuntimeException("Test failed");
        } else {
            System.out.println("Test passed");
        }
    }
}
