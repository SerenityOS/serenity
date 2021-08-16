/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164705
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          jdk.compiler
 * @build jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.Utils
 *        jdk.test.lib.Asserts
 *        jdk.test.lib.JDKToolFinder
 *        jdk.test.lib.JDKToolLauncher
 *        jdk.test.lib.Platform
 *        jdk.test.lib.process.*
 *        jdk.test.lib.util.JarUtils
 * @run main ReadFileOnPath
 * @summary Still able to read file on the same path
 */

import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.util.JarUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ReadFileOnPath {

    private static final Path SRC_DIR    = Paths.get(System.getProperty("test.src"));
    private static final Path HERE_DIR   = Paths.get(".");
    private static final Path MODS_DIR   = Paths.get("modules");

    public static void main(String args[]) throws Exception {
        CompilerUtils.compile(SRC_DIR.resolve("m"), MODS_DIR.resolve("m"));
        Files.write(MODS_DIR.resolve("m/base"), "base".getBytes());
        Files.write(MODS_DIR.resolve("m/p/child"), "child".getBytes());
        JarUtils.createJarFile(HERE_DIR.resolve("old.jar"),
                MODS_DIR.resolve("m"),
                "base", "p/App.class", "p/child");
        JarUtils.createJarFile(HERE_DIR.resolve("new.jar"),
                MODS_DIR.resolve("m"),
                "module-info.class", "base", "p/App.class", "p/child");

        // exploded module
        test("--module-path", "modules", "-m", "m/p.App", "SS++++0");

        // module in jar
        test("--module-path", "new.jar", "-m", "m/p.App", "SSSS++0");

        // exploded classpath
        test("-cp", "modules/m", "p.App", "SS+++++");

        // classpath in jar
        test("-cp", "old.jar", "p.App", "SSSS++0");
    }

    static void test(String... args) throws Exception {
        List<String> cmds = new ArrayList<>();
        cmds.add("-Djava.security.manager");
        cmds.addAll(Arrays.asList(args));
        cmds.addAll(List.of(
                "x", "modules/m", "modules/m/base", "modules/m/p/child",
                "-", "child", "/base", "../base"));
        ProcessTools.executeTestJvm(cmds.toArray(new String[cmds.size()]))
                .shouldHaveExitValue(0);
    }
}
