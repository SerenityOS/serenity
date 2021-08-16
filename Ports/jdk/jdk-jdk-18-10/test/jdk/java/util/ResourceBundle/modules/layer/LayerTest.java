/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8180375 8185251 8210408
 * @summary Tests resource bundles are correctly loaded from modules through
 *          "<packageName>.spi.<simpleName>Provider" types.
 * @library /test/lib
 *          ..
 * @build jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Utils
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.process.ProcessTools
 *        ModuleTestUtil
 * @run main LayerTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import jdk.test.lib.JDKToolLauncher;
import jdk.test.lib.Utils;
import jdk.test.lib.process.ProcessTools;

public class LayerTest {
    private static final Path SRC_DIR = Paths.get(Utils.TEST_SRC, "src");
    private static final Path MODS_DIR = Paths.get(Utils.TEST_CLASSES, "mods");
    private static final List<String> MODULE_LIST = List.of("m1", "m2");

    public static void main(String[] args) throws Throwable {
        MODULE_LIST.forEach(mn -> ModuleTestUtil.prepareModule(SRC_DIR,
                MODS_DIR, mn, ".properties"));
        compileCmd();
        runCmd();
    }

    private static void compileCmd() throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("javac");
        launcher.addToolArg("-d")
                .addToolArg(Utils.TEST_CLASSES)
                .addToolArg(SRC_DIR.resolve("Main.java").toString());

        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                                   .getExitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Compile of the test failed. "
                    + "Unexpected exit code: " + exitCode);
        }
    }

    private static void runCmd() throws Throwable {
        JDKToolLauncher launcher = JDKToolLauncher.createUsingTestJDK("java");
        launcher.addToolArg("-ea")
                .addToolArg("-esa")
                .addToolArg("-cp")
                .addToolArg(Utils.TEST_CLASSES)
                .addToolArg("Main")
                .addToolArg(Utils.TEST_CLASSES);

        int exitCode = ProcessTools.executeCommand(launcher.getCommand())
                                   .getExitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Execution of the test failed. "
                    + "Unexpected exit code: " + exitCode);
        }
    }
}