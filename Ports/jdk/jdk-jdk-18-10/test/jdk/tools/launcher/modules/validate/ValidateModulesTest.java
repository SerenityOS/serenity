/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178380 8194937
 * @modules java.xml
 * @library src /test/lib
 * @build ValidateModulesTest hello/* jdk.test.lib.util.JarUtils
 * @run testng ValidateModulesTest
 * @summary Basic test for java --validate-modules
 */

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.util.JarUtils;

import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class ValidateModulesTest {

    /**
     * Basic test --validate-modules when there are no errors.
     */
    public void testNoErrors() throws Exception {
        String modulePath = System.getProperty("test.module.path");

        test("--validate-modules");

        test("--validate-modules", "-version")
                .shouldContain("Runtime Environment");

        test("--validate-modules", "--list-modules")
                .shouldContain("java.base");

        test("--validate-modules", "-d", "java.base")
                .shouldContain("exports java.lang");

        test("-p", modulePath, "-m", "hello/p.Main")
                .shouldContain("Hello world");

        test("-p", modulePath, "--validate-modules", "-m", "hello/p.Main")
                .shouldNotContain("Hello world");

        test("-p", modulePath, "--validate-modules", "--list-modules")
                .shouldContain("hello");

        test("-p", modulePath, "--validate-modules", "-d", "hello")
                .shouldContain("hello")
                .shouldContain("contains p");

        testExpectingError("--validate-modules", "--add-modules", "BAD")
                .shouldContain("Module BAD not found");

        testExpectingError("--validate-modules", "-m", "BAD")
                .shouldContain("Module BAD not found");
    }

    /**
     * Test an automatic module on the module path with classes in the same
     * package as a system module.
     */
    public void testPackageConflict() throws Exception {
        Path tmpdir = Files.createTempDirectory("tmp");

        Path classes = Files.createDirectory(tmpdir.resolve("classes"));
        touch(classes, "javax/xml/XMLConstants.class");
        touch(classes, "javax/xml/parsers/SAXParser.class");

        Path lib = Files.createDirectory(tmpdir.resolve("lib"));
        JarUtils.createJarFile(lib.resolve("xml.jar"), classes);

        testExpectingError("-p", lib.toString(), "--validate-modules")
                .shouldContain("xml automatic")
                .shouldContain("conflicts with module java.xml");
    }

    /**
     * Test two modules with the same name in a directory.
     */
    public void testDuplicateModule() throws Exception {
        Path tmpdir = Files.createTempDirectory("tmp");

        Path classes = Files.createDirectory(tmpdir.resolve("classes"));
        touch(classes, "org/foo/Bar.class");

        Path lib = Files.createDirectory(tmpdir.resolve("lib"));
        JarUtils.createJarFile(lib.resolve("foo-1.0.jar"), classes);
        JarUtils.createJarFile(lib.resolve("foo-2.0.jar"), classes);

        testExpectingError("-p", lib.toString(), "--validate-modules")
                .shouldContain("contains same module");
    }

    /**
     * Test two modules with the same name in different directories.
     */
    public void testShadowed() throws Exception {
        Path tmpdir = Files.createTempDirectory("tmp");

        Path classes = Files.createDirectory(tmpdir.resolve("classes"));
        touch(classes, "org/foo/Bar.class");

        Path lib1 = Files.createDirectory(tmpdir.resolve("lib1"));
        JarUtils.createJarFile(lib1.resolve("foo-1.0.jar"), classes);

        Path lib2 = Files.createDirectory(tmpdir.resolve("lib2"));
        JarUtils.createJarFile(lib2.resolve("foo-2.0.jar"), classes);

        test("-p", lib1 + File.pathSeparator + lib2, "--validate-modules")
                .shouldContain("shadowed by");
    }

    /**
     * Runs the java launcher with the given arguments, expecting a 0 exit code
     */
    private OutputAnalyzer test(String... args) throws Exception {
        OutputAnalyzer analyzer = ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out);
        assertTrue(analyzer.getExitValue() == 0);
        return analyzer;
    }

    /**
     * Runs the java launcher with the given arguments, expecting a non-0 exit code
     */
    private OutputAnalyzer testExpectingError(String... args) throws Exception {
        OutputAnalyzer analyzer = ProcessTools.executeTestJava(args)
                .outputTo(System.out)
                .errorTo(System.out);
        assertTrue(analyzer.getExitValue() != 0);
        return analyzer;
    }

    /**
     * Creates a file relative the given directory.
     */
    private void touch(Path dir, String relPath) throws IOException {
        Path file = dir.resolve(relPath.replace('/', File.separatorChar));
        Files.createDirectories(file.getParent());
        Files.createFile(file);
    }
}
