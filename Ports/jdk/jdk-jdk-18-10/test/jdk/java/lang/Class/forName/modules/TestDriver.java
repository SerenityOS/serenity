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

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import jdk.test.lib.util.FileUtils;
import jdk.test.lib.compiler.CompilerUtils;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;

/**
 * @test
 * @bug 8087335
 * @summary Tests for Class.forName(Module,String)
 * @library /test/lib
 * @modules jdk.compiler
 * @build jdk.test.lib.Platform
 *        jdk.test.lib.util.FileUtils
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.process.ProcessTools
 *        TestDriver TestMain TestLayer
 * @run testng TestDriver
 */

public class TestDriver {

    private static final String TEST_SRC =
            Paths.get(System.getProperty("test.src")).toString();
    private static final String TEST_CLASSES =
            Paths.get(System.getProperty("test.classes")).toString();

    private static final Path MOD_SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MOD_DEST_DIR = Paths.get("mods");

    private static final String[] modules = new String[] {"m1", "m2", "m3"};

    /**
     * Compiles all modules used by the test.
     */
    @BeforeClass
    public void setup() throws Exception {
        assertTrue(CompilerUtils.compile(
                        MOD_SRC_DIR, MOD_DEST_DIR,
                        "--module-source-path",
                        MOD_SRC_DIR.toString()));

        copyDirectories(MOD_DEST_DIR.resolve("m1"), Paths.get("mods1"));
        copyDirectories(MOD_DEST_DIR.resolve("m2"), Paths.get("mods2"));
    }

    @Test
    public void test() throws Exception {
        String[] options = new String[] {
                "-cp", TEST_CLASSES,
                "--module-path", MOD_DEST_DIR.toString(),
                "--add-modules", String.join(",", modules),
                "-m", "m2/p2.test.Main"
        };
        runTest(options);
    }

    @Test
    public void testUnnamedModule() throws Exception {
        String[] options = new String[] {
                "-cp", TEST_CLASSES,
                "--module-path", MOD_DEST_DIR.toString(),
                "--add-modules", String.join(",", modules),
                "TestMain"
        };
        runTest(options);
    }

    @Test
    public void testLayer() throws Exception {
        String[] options = new String[] {
                "-cp", TEST_CLASSES,
                "TestLayer"
        };

        runTest(options);
    }

    @Test
    public void testDeniedClassLoaderAccess() throws Exception {
        String[] options = new String[] {
                "--module-path", MOD_DEST_DIR.toString(),
                "--add-modules", String.join(",", modules),
                "-Djava.security.manager=allow",
                "-m", "m3/p3.NoGetClassLoaderAccess"
        };
        assertTrue(executeTestJava(options)
                        .outputTo(System.out)
                        .errorTo(System.err)
                        .getExitValue() == 0);
    }

    @Test
    public void testDeniedAccess() throws Exception {
        Path policyFile = Paths.get(TEST_SRC, "policy.denied");

        String[] options = new String[] {
                "-Djava.security.manager",
                "-Djava.security.policy=" + policyFile.toString(),
                "--module-path", MOD_DEST_DIR.toString(),
                "--add-modules", String.join(",", modules),
                "-m", "m3/p3.NoAccess"
        };
        assertTrue(executeTestJava(options)
                        .outputTo(System.out)
                        .errorTo(System.err)
                        .getExitValue() == 0);
    }

    private String[] runWithSecurityManager(String[] options) {
        Path policyFile = Paths.get(TEST_SRC, "policy");
        Stream<String> opts = Stream.concat(Stream.of("-Djava.security.manager",
                                                      "-Djava.security.policy=" + policyFile.toString()),
                                            Arrays.stream(options));
        return opts.toArray(String[]::new);
    }

    private void runTest(String[] options) throws Exception {
        assertTrue(executeTestJava(options)
                        .outputTo(System.out)
                        .errorTo(System.err)
                        .getExitValue() == 0);

        assertTrue(executeTestJava(runWithSecurityManager(options))
                        .outputTo(System.out)
                        .errorTo(System.err)
                        .getExitValue() == 0);
    }

    private void copyDirectories(Path source, Path dest) throws IOException {
        if (Files.exists(dest))
            FileUtils.deleteFileTreeWithRetry(dest);
        Files.walk(source, Integer.MAX_VALUE)
                .filter(Files::isRegularFile)
                .forEach(p -> {
                    try {
                        Path to = dest.resolve(source.relativize(p));
                        Files.createDirectories(to.getParent());
                        Files.copy(p, to);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
    }
}
