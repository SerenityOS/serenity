/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8197532
 * @modules jdk.compiler
 *          jdk.jlink
 *          jdk.zipfs
 * @library src /test/lib
 * @build java.json/*
 * @run main DefaultModules
 * @summary Test that all modules that export an API are in the set of modules
 *          resolved when compiling or running code on the class path
 */

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.spi.ToolProvider;

import jdk.test.lib.process.ProcessTools;

/**
 * This test compiles and runs the following tests on the class path:
 *
 *   TestRootModules.java.java - tests that every module that exports an API
 *       is resolved. Also tests that java.se is not resolved.
 *
 *   TestJson.java - exercises APIs exported by the java.json module. The
 *       java.json module is not a Java SE module.
 */

public class DefaultModules {
    private static final PrintStream out = System.out;

    public static void main(String[] args) throws Exception {
        String javaHome = System.getProperty("java.home");
        String testSrc = System.getProperty("test.src");

        // $JDK_HOME/bin/java TestModules.java
        String source = Path.of(testSrc, "TestRootModules.java").toString();
        ProcessTools.executeTestJava("--add-exports", "java.base/jdk.internal.module=ALL-UNNAMED", source)
                .outputTo(System.out)
                .errorTo(System.err)
                .shouldHaveExitValue(0);

        /**
         * Create a run-time image containing java.se, java.json and the javac
         * compiler. Use the run-time image to compile and run both
         * TestModules.java and JsonTest.java
         */
        if (Files.exists(Path.of(javaHome, "jmods", "java.se.jmod"))) {
            // jlink --add-modules java.se,java.json,jdk.compiler,jdk.zipfs
            Path here = Path.of(".");
            Path image = Files.createTempDirectory(here, "images").resolve("myimage");
            ToolProvider jlink = ToolProvider.findFirst("jlink")
                    .orElseThrow(() -> new RuntimeException("jlink not found"));
            int exitCode = jlink.run(System.out, System.err,
                    "--module-path", System.getProperty("test.module.path"),
                    "--add-modules", "java.se,java.json,jdk.compiler,jdk.zipfs",
                    "--output", image.toString());
            if (exitCode != 0)
                throw new RuntimeException("jlink failed");

            // path to java launcher in run-time image
            String javaLauncher = image.resolve("bin").resolve("java").toString();
            if (System.getProperty("os.name").startsWith("Windows"))
                javaLauncher += ".exe";

            // $CUSTOM_JDK/bin/java TestRootModules.java
            source = Path.of(testSrc, "TestRootModules.java").toString();
            ProcessBuilder pb = new ProcessBuilder(javaLauncher,
                    "--add-exports", "java.base/jdk.internal.module=ALL-UNNAMED",
                    source);
            out.format("Command line: [%s]%n", pb.command());
            ProcessTools.executeProcess(pb)
                    .outputTo(System.out)
                    .errorTo(System.err)
                    .shouldHaveExitValue(0);

            // $CUSTOM_JDK/bin/java TestJson.java
            source = Path.of(testSrc, "TestJson.java").toString();
            out.format("Command line: [%s %s]%n", javaLauncher, source);
            ProcessTools.executeProcess(new ProcessBuilder(javaLauncher, source))
                    .outputTo(System.out)
                    .errorTo(System.err)
                    .shouldHaveExitValue(0);
        }
    }
}
