/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8156871
 * @summary package in the boot layer is repeatedly exported to unique module created in layers on top of the boot layer
 * @modules java.base/jdk.internal.misc
 * @library /test/lib
 * @compile ../CompilerUtils.java
 * @run driver ExportModuleStressTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.process.OutputAnalyzer;

public class ExportModuleStressTest {

    private static final String TEST_SRC = System.getProperty("test.src");
    private static final String TEST_CLASSES = System.getProperty("test.classes");

    private static final Path SRC_DIR = Paths.get(TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get(TEST_CLASSES, "mods");

    /**
     * Compiles all module definitions used by the test
     */
    public static void main(String[] args) throws Exception {

        boolean compiled;
        // Compile module jdk.test declaration
        compiled = CompilerUtils.compile(
            SRC_DIR.resolve("jdk.test"),
            MODS_DIR.resolve("jdk.test"));
        if (!compiled) {
            throw new RuntimeException("Test failed to compile module jdk.test");
        }

        // Compile module jdk.translet declaration
        compiled = CompilerUtils.compile(
            SRC_DIR.resolve("jdk.translet"),
            MODS_DIR.resolve("jdk.translet"),
            "--add-exports=jdk.test/test=jdk.translet",
            "-p", MODS_DIR.toString());
        if (!compiled) {
            throw new RuntimeException("Test failed to compile module jdk.translet");
        }

        // Sanity check that the test, jdk.test/test/Main.java
        // runs without error.
        ProcessBuilder pb = ProcessTools.createJavaProcessBuilder(
            "-p", MODS_DIR.toString(),
            "-m", "jdk.test/test.Main");
        OutputAnalyzer output = new OutputAnalyzer(pb.start());
        output.shouldContain("failed: 0")
              .shouldHaveExitValue(0);
    }
}
