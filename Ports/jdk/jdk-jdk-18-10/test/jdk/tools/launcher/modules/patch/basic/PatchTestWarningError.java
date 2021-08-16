/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168836
 * @summary Basic argument validation for --patch-module
 * @library /test/lib
 * @modules jdk.compiler
 * @build PatchTestWarningError
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.JarUtils
 * @run testng PatchTestWarningError
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
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;


/**
 * This test
 * See PatchTestWarningError for test description.
 */

@Test
public class PatchTestWarningError {

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

    // patch path for java.base
    private static final String PATCHES_PATH =
        PATCHES1_DIR.resolve("java.base") + File.pathSeparator +
            PATCHES2_DIR.resolve("java.base");

    // the classes overridden or added with --patch-module
    private static final String[] CLASSES = {

        // java.base = boot loader
        "java.base/java.text.Annotation",           // override class
        "java.base/java.text.AnnotationBuddy",      // add class to package
        "java.base/java.lang2.Object",              // new package

    };


    @BeforeTest
    public void setup() throws Exception {

        // javac -d mods/test src/test/**
        boolean compiled= CompilerUtils.compile(SRC_DIR.resolve("test"),
                                                MODS_DIR.resolve("test"));
        assertTrue(compiled, "classes did not compile");

        // javac --patch-module $MODULE=patches1/$MODULE -d patches1/$MODULE patches1/$MODULE/**
        Path src = SRC1_DIR.resolve("java.base");
        Path output = PATCHES1_DIR.resolve(src.getFileName());
        Files.createDirectories(output);
        String mn = src.getFileName().toString();
        compiled  = CompilerUtils.compile(src, output,
                                          "--patch-module", mn + "=" + src.toString());
        assertTrue(compiled, "classes did not compile");

        // javac --patch-module $MODULE=patches2/$MODULE -d patches2/$MODULE patches2/$MODULE/**
        src = SRC2_DIR.resolve("java.base");
        output = PATCHES2_DIR.resolve(src.getFileName());
        Files.createDirectories(output);
        mn = src.getFileName().toString();
        compiled  = CompilerUtils.compile(src, output,
                                          "--patch-module", mn + "=" + src.toString());
        assertTrue(compiled, "classes did not compile");

    }

    /**
     * Test with --patch-module options patching the same module
     */
    public void testDuplicateModule() throws Exception {
        int exitValue =
            executeTestJava("--patch-module", "java.base=" + PATCHES1_DIR.resolve("java.base"),
                            "--patch-module", "java.base=" + PATCHES2_DIR.resolve("java.base"),
                            "--module-path", MODS_DIR.toString(),
                            "-m", "test/jdk.test.Main")
                .outputTo(System.out)
                .errorTo(System.out)
                // error output by VM
                .shouldContain("Cannot specify java.base more than once to --patch-module")
                .getExitValue();

        assertTrue(exitValue != 0);
    }

    @DataProvider(name = "emptyItem")
    public Object[][] emptyItems() {
        String patch1 = PATCHES1_DIR.resolve("java.base").toString();
        String patch2 = PATCHES2_DIR.resolve("java.base").toString();
        String pathSep = File.pathSeparator;
        return new Object[][]{

            { "java.base="+ pathSep + patch1 + pathSep + patch2,            null },
            { "java.base="+ patch1 + pathSep + pathSep + patch2,            null },
            { "java.base="+ patch1 + pathSep + patch2 + pathSep + pathSep,  null },
        };
    }

    /**
     * Empty item in a non-empty path list
     */
    @Test(dataProvider = "emptyItem")
    public void testEmptyItem(String value, String msg) throws Exception {
        // the argument to the test is the list of classes overridden or added
        String arg = Stream.of(CLASSES).collect(Collectors.joining(","));

        int exitValue =
            executeTestJava("--patch-module", value,
                            "--add-exports", "java.base/java.lang2=test",
                            "--module-path", MODS_DIR.toString(),
                            "-m", "test/jdk.test.Main", arg)
                .outputTo(System.out)
                .errorTo(System.out)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    /**
     * Test bad module name that should emit a warning
     */
    public void testBadName() throws Exception {
        // the argument to the test is the list of classes overridden or added
        String arg = Stream.of(CLASSES).collect(Collectors.joining(","));

        int exitValue =
            executeTestJava("--patch-module", "DoesNotExist=tmp",
                            "--patch-module", "java.base=" + PATCHES_PATH,
                            "--add-exports", "java.base/java.lang2=test",
                            "--module-path", MODS_DIR.toString(),
                            "-m", "test/jdk.test.Main", arg)
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldContain("WARNING: Unknown module: DoesNotExist specified to --patch-module")
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    @DataProvider(name = "badArguments")
    public Object[][] badArguments() {
        return new Object[][]{

            // source not found
            { "=tmp",            "Unable to parse --patch-module <module>=<value>: =tmp" },

            // target not found: check by VM
            { "java.base",       "Missing '=' in --patch-module specification" },
            { "foo",             "Missing '=' in --patch-module specification" },

            // target not found
            { "java.base=",      "Unable to parse --patch-module <module>=<value>: java.base="  },
            { "java.base=" + File.pathSeparator,
              "Target must be specified: --patch-module java.base=" + File.pathSeparator }
        };
    }

    /**
     * Test ill-formed argument to --patch-module
     */
    @Test(dataProvider = "badArguments")
    public void testBadArgument(String value, String msg) throws Exception {
        int exitValue =
            executeTestJava("--patch-module", value,
                            "--module-path", MODS_DIR.toString(),
                            "-m", "test/jdk.test.Main")
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldContain(msg)
                .getExitValue();

        assertTrue(exitValue != 0);
    }
}
