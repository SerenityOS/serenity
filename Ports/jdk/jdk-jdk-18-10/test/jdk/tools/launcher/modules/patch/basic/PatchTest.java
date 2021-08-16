/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @library /test/lib
 * @modules jdk.compiler
 *          jdk.naming.dns
 * @build PatchTest
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.JarUtils
 * @run testng PatchTest
 * @summary Basic test for --patch-module
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.JarUtils;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

/**
 * Compiles and launches a test that uses --patch-module with two directories
 * of classes to override existing classes and add new classes to modules in
 * the boot layer.
 *
 * The classes overridden or added via --patch-module all define a public
 * no-arg constructor and override toString to return "hi". This allows the
 * launched test to check that the overridden classes are loaded.
 */

@Test
public class PatchTest {

    // top-level source directory
    private static final String TEST_SRC = System.getProperty("test.src");

    // source/destination tree for the test module
    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get("mods");

    // source/destination tree for patch tree 1
    private static final Path SRC1_DIR = Paths.get(TEST_SRC, "src1");
    private static final Path PATCHES1_DIR = Paths.get("patches1");

    // source/destination tree for patch tree 2
    private static final Path SRC2_DIR = Paths.get(TEST_SRC, "src2");
    private static final Path PATCHES2_DIR = Paths.get("patches2");

    // destination directory for patches packaged as JAR files
    private static final Path PATCHES_DIR = Paths.get("patches");


    // the classes overridden or added with --patch-module
    private static final String[] CLASSES = {

        // java.base = boot loader
        "java.base/java.text.Annotation",           // override class
        "java.base/java.text.AnnotationBuddy",      // add class to package
        "java.base/java.lang2.Object",              // new package

        // jdk.naming.dns = platform class loader
        "jdk.naming.dns/com.sun.jndi.dns.DnsClient",
        "jdk.naming.dns/com.sun.jndi.dns.DnsClientBuddy",
        "jdk.naming.dns/com.sun.jndi.dns2.Zone",

        // jdk.compiler = application class loaded
        "jdk.compiler/com.sun.tools.javac.Main",
        "jdk.compiler/com.sun.tools.javac.MainBuddy",
        "jdk.compiler/com.sun.tools.javac2.Main",

    };


    @BeforeTest
    public void setup() throws Exception {

        // javac -d mods/test src/test/**
        boolean compiled= CompilerUtils.compile(SRC_DIR.resolve("test"),
                                                MODS_DIR.resolve("test"));
        assertTrue(compiled, "classes did not compile");

        // javac --patch-module $MODULE=patches1/$MODULE -d patches1/$MODULE patches1/$MODULE/**
        // jar cf patches/$MODULE-1.jar -C patches1/$MODULE .
        for (Path src : Files.newDirectoryStream(SRC1_DIR)) {
            Path output = PATCHES1_DIR.resolve(src.getFileName());
            String mn = src.getFileName().toString();
            compiled  = CompilerUtils.compile(src, output,
                                              "--patch-module", mn + "=" + src.toString());
            assertTrue(compiled, "classes did not compile");
            JarUtils.createJarFile(PATCHES_DIR.resolve(mn + "-1.jar"), output);
        }

        // javac --patch-module $MODULE=patches2/$MODULE -d patches2/$MODULE patches2/$MODULE/**
        // jar cf patches/$MODULE-2.jar -C patches2/$MODULE .
        for (Path src : Files.newDirectoryStream(SRC2_DIR)) {
            Path output = PATCHES2_DIR.resolve(src.getFileName());
            String mn = src.getFileName().toString();
            compiled  = CompilerUtils.compile(src, output,
                                              "--patch-module", mn + "=" + src.toString());
            assertTrue(compiled, "classes did not compile");
            JarUtils.createJarFile(PATCHES_DIR.resolve(mn + "-2.jar"), output);
        }

    }

    /**
     * Run test with patches to java.base, jdk.naming.dns and jdk.compiler
     */
    void runTest(String basePatches, String dnsPatches, String compilerPatches)
        throws Exception
    {
        // the argument to the test is the list of classes overridden or added
        String arg = Stream.of(CLASSES).collect(Collectors.joining(","));

        int exitValue
            =  executeTestJava("--patch-module", "java.base=" + basePatches,
                               "--patch-module", "jdk.naming.dns=" + dnsPatches,
                               "--patch-module", "jdk.compiler=" + compilerPatches,
                               "--add-exports", "java.base/java.lang2=test",
                               "--add-exports", "jdk.naming.dns/com.sun.jndi.dns=test",
                               "--add-exports", "jdk.naming.dns/com.sun.jndi.dns2=test",
                               "--add-exports", "jdk.compiler/com.sun.tools.javac2=test",
                               "--add-modules", "jdk.naming.dns,jdk.compiler",
                               "--module-path", MODS_DIR.toString(),
                               "-m", "test/jdk.test.Main", arg)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    /**
     * Run test with ---patch-module and exploded patches
     */
    public void testWithExplodedPatches() throws Exception {

        // patches1/java.base:patches2/java.base
        String basePatches = PATCHES1_DIR.resolve("java.base")
                + File.pathSeparator + PATCHES2_DIR.resolve("java.base");

        String dnsPatches = PATCHES1_DIR.resolve("jdk.naming.dns")
                + File.pathSeparator + PATCHES2_DIR.resolve("jdk.naming.dns");

        String compilerPatches = PATCHES1_DIR.resolve("jdk.compiler")
                + File.pathSeparator + PATCHES2_DIR.resolve("jdk.compiler");

        runTest(basePatches, dnsPatches, compilerPatches);
    }


    /**
     * Run test with ---patch-module and patches in JAR files
     */
    public void testWithJarPatches() throws Exception {

        // patches/java.base-1.jar:patches/java-base-2.jar
        String basePatches = PATCHES_DIR.resolve("java.base-1.jar")
                + File.pathSeparator + PATCHES_DIR.resolve("java.base-2.jar");

        String dnsPatches = PATCHES_DIR.resolve("jdk.naming.dns-1.jar")
                +  File.pathSeparator + PATCHES_DIR.resolve("jdk.naming.dns-2.jar");

        String compilerPatches = PATCHES_DIR.resolve("jdk.compiler-1.jar")
                +  File.pathSeparator + PATCHES_DIR.resolve("jdk.compiler-2.jar");

        runTest(basePatches, dnsPatches, compilerPatches);

    }


    /**
     * Run test with ---patch-module and patches in JAR files and exploded patches
     */
    public void testWithJarAndExplodedPatches() throws Exception {

        // patches/java.base-1.jar:patches2/java.base
        String basePatches = PATCHES_DIR.resolve("java.base-1.jar")
                + File.pathSeparator + PATCHES2_DIR.resolve("java.base");

        // patches1/jdk.naming.dns:patches/jdk.naming.dns-2.jar
        String dnsPatches = PATCHES1_DIR.resolve("jdk.naming.dns")
                +  File.pathSeparator + PATCHES_DIR.resolve("jdk.naming.dns-2.jar");

        String compilerPatches = PATCHES1_DIR.resolve("jdk.compiler")
                +  File.pathSeparator + PATCHES_DIR.resolve("jdk.compiler-2.jar");

        runTest(basePatches, dnsPatches, compilerPatches);

    }
}
