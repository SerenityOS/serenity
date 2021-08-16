/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.nio.file.*;
import java.util.*;

import static jdk.test.lib.Asserts.*;
import jdk.test.lib.JDKToolFinder;
import jdk.test.lib.process.ProcessTools;

/*
 * @test
 * @bug 8214230
 * @summary Test that jlinks generates reproducible modules files
 * @library /test/lib
 * @run driver JLinkReproducibleTest
 */
public class JLinkReproducibleTest {
    private static void run(List<String> cmd) throws Exception {
        var pb = new ProcessBuilder(cmd.toArray(new String[0]));
        var res = ProcessTools.executeProcess(pb);
        res.shouldHaveExitValue(0);
    }

    private static void jlink(Path image, boolean with_default_trace_file) throws Exception {
        var cmd = new ArrayList<String>();
        cmd.add(JDKToolFinder.getJDKTool("jlink"));
        cmd.addAll(List.of(
            "--module-path", JMODS_DIR.toString() + File.pathSeparator + CLASS_DIR.toString(),
            "--add-modules", "main",
            "--compress=2",
            "--output", image.toString()
        ));
        if (!with_default_trace_file) {
            cmd.add("--generate-jli-classes=@file-not-exists");
        }
        run(cmd);
    }

    private static void javac(String... args) throws Exception {
        var cmd = new ArrayList<String>();
        cmd.add(JDKToolFinder.getJDKTool("javac"));
        cmd.addAll(Arrays.asList(args));
        run(cmd);
    }

    private static final List<String> MODULE_INFO = List.of(
        "module main {",
        "    exports org.test.main;",
        "}"
    );

    private static final List<String> MAIN_CLASS = List.of(
        "package org.test.main;",
        "public class Main {",
        "    public static void main(String[] args) {",
        "        System.out.println(\"Hello, world\");",
        "    }",
        "}"
    );

    private static final Path CLASS_DIR = Path.of("classes");
    private static final Path JMODS_DIR = Path.of(System.getProperty("java.home"), "jmods");

    public static void main(String[] args) throws Exception {
        // Write the source code
        var srcDir = Path.of("main", "org", "test", "main");
        Files.createDirectories(srcDir.toAbsolutePath());

        var srcFile = srcDir.resolve("Main.java");
        Files.write(srcFile, MAIN_CLASS);

        var moduleFile = Path.of("main").resolve("module-info.java");
        Files.write(moduleFile, MODULE_INFO);

        // Compile the source code to class files
        javac("--module-source-path", ".",
              "--module", "main",
              "-d", CLASS_DIR.toString());

        // Link the first image
        var firstImage = Path.of("image-first");
        jlink(firstImage, true);
        var firstModulesFile = firstImage.resolve("lib")
                                         .resolve("modules");

        // Link the second image
        var secondImage = Path.of("image-second");
        jlink(secondImage, true);
        var secondModulesFile = secondImage.resolve("lib")
                                           .resolve("modules");

        // Ensure module files are identical
        assertEquals(-1L, Files.mismatch(firstModulesFile, secondModulesFile));

        // Link the third image
        var thirdImage = Path.of("image-third");
        jlink(thirdImage, false);
        var thirdModulesFile = thirdImage.resolve("lib")
                                         .resolve("modules");
        // Link the fourth image
        var fourthImage = Path.of("image-fourth");
        jlink(fourthImage, false);
        var fourthModulesFile = fourthImage.resolve("lib")
                                           .resolve("modules");

        // Ensure module files are identical
        assertEquals(-1L, Files.mismatch(thirdModulesFile, fourthModulesFile));
    }
}
