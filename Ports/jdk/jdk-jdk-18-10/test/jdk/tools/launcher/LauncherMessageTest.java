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
 * @bug 8167063
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.zipfs
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 * @run main LauncherMessageTest
 * @summary LauncherHelper should not throw JNI error for LinkageError
 */

import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import jdk.test.lib.util.FileUtils;

public class LauncherMessageTest {

    public static void main(String[] args) throws Exception {
        String userDir = System.getProperty("user.dir", ".");
        File testDir = new File(userDir, "test");
        List<String> srcContent = new ArrayList<>();

        // Try to create a test directory before proceeding further
        if (!testDir.mkdir()) {
            throw new Exception("Test failed: unable to create"
                    + " writable working directory "
                    + testDir.getAbsolutePath());
        }

        // Create test sub-directories for sources, classes and modules respectively
        File srcA = new File(testDir.getPath(), "srcA");
        srcA.mkdir();
        File srcB = new File(testDir.getPath(), "srcB");
        srcB.mkdir();
        File classesA = new File(testDir.getPath(), "classesA");
        classesA.mkdir();
        File classesB = new File(testDir.getPath(), "classesB");
        classesB.mkdir();
        File modules = new File(testDir.getPath(), "modules");
        modules.mkdir();

        // Define content and create module-info.java and corresponding source files
        File modAinfo = new File(srcA.getPath(), "module-info.java");
        srcContent.add("module mod.a { exports pkgA; }");
        TestHelper.createFile(modAinfo, srcContent);

        File classA = new File(srcA.getPath(), "ClassA.java");
        srcContent.clear();
        srcContent.add("package pkgA; public class ClassA { }");
        TestHelper.createFile(classA, srcContent);

        File modBinfo = new File(srcB.getPath(), "module-info.java");
        srcContent.clear();
        srcContent.add("module mod.b { requires mod.a; }");
        TestHelper.createFile(modBinfo, srcContent);

        File classB = new File(srcB.getPath(), "ClassB.java");
        srcContent.clear();
        srcContent.add("package pkgB;");
        srcContent.add("import pkgA.ClassA;");
        srcContent.add("public class ClassB extends ClassA {");
        srcContent.add("public static void main(String[] args) { } }");
        TestHelper.createFile(classB, srcContent);

        // Compile all source files and create Jars
        TestHelper.compile("-d", classesA.getPath(), classA.getPath(), modAinfo.getPath());
        TestHelper.createJar("cf", Paths.get(modules.getPath(), "mod.a.jar").toString(),
                "-C", classesA.getPath(), ".");
        TestHelper.compile("-d", classesB.getPath(), "--module-path", modules.getPath(),
                classB.getPath(), modBinfo.getPath());
        TestHelper.createJar("cf", Paths.get(modules.getPath(), "mod.b.jar").toString(),
                "-C", classesB.getPath(), ".");

        // Delete the module-info.java and Jar file corresponding to mod.a
        FileUtils.deleteFileWithRetry(Paths.get(modAinfo.getPath()));
        FileUtils.deleteFileWithRetry(Paths.get(modules.getPath(), "mod.a.jar"));

        // Re-create module-info.java (by removing "exports pkgA;")
        // and corresponding Jar file
        srcContent.clear();
        srcContent.add("module mod.a { }");
        TestHelper.createFile(modAinfo, srcContent);
        TestHelper.compile("-d", classesA.getPath(), classA.getPath(), modAinfo.getPath());
        TestHelper.createJar("cf", Paths.get(modules.getPath(), "mod.a.jar").toString(),
                "-C", classesA.getPath(), ".");

        // Execute the main class
        String[] commands = {TestHelper.javaCmd, "--module-path", modules.getPath(),
            "-m", "mod.b/pkgB.ClassB"};
        TestHelper.TestResult result = TestHelper.doExec(commands);

        // Clean the test directory and check test status
        FileUtils.deleteFileTreeWithRetry(Paths.get(testDir.getPath()));
        if (result.isOK()) {
            throw new Exception("Test Passed Unexpectedly!");
        } else {
            result.testOutput.forEach(System.err::println);
            if (result.contains("JNI error")) {
                throw new Exception("Test Failed with JNI error!");
            }
        }
        System.out.println("Test passes, failed with expected error message");
    }
}
