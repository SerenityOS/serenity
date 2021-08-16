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

/*
 * @test
 * @summary Basic test of jlink to create jmods and images
 * @author Andrei Eremeev
 * @library /test/lib
 * @modules java.base/jdk.internal.module
 *          jdk.jlink
 *          jdk.compiler
 * @build jdk.test.lib.process.ProcessTools
 *        jdk.test.lib.process.OutputAnalyzer
 *        jdk.test.lib.compiler.CompilerUtils
 *        jdk.test.lib.util.JarUtils
 * @run main BasicTest
 */

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.spi.ToolProvider;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.util.JarUtils;

public class BasicTest {
    static final ToolProvider JMOD_TOOL = ToolProvider.findFirst("jmod")
        .orElseThrow(() ->
            new RuntimeException("jmod tool not found")
        );

    static final ToolProvider JLINK_TOOL = ToolProvider.findFirst("jlink")
        .orElseThrow(() ->
            new RuntimeException("jlink tool not found")
        );

    private final String TEST_MODULE = "test";
    private final Path jdkHome = Paths.get(System.getProperty("test.jdk"));
    private final Path jdkMods = jdkHome.resolve("jmods");
    private final Path testSrc = Paths.get(System.getProperty("test.src"));
    private final Path src = testSrc.resolve("src").resolve(TEST_MODULE);
    private final Path classes = Paths.get("classes");
    private final Path jmods = Paths.get("jmods");
    private final Path jars = Paths.get("jars");

    public static void main(String[] args) throws Throwable {
        new BasicTest().run();
    }

    public void run() throws Throwable {
        if (Files.notExists(jdkMods)) {
            return;
        }

        if (!CompilerUtils.compile(src, classes)) {
            throw new AssertionError("Compilation failure. See log.");
        }

        Files.createDirectories(jmods);
        Files.createDirectories(jars);
        Path jarfile = jars.resolve("test.jar");
        JarUtils.createJarFile(jarfile, classes);

        Path image = Paths.get("mysmallimage");
        runJmod(jarfile.toString(), TEST_MODULE, true);
        runJlink(image, TEST_MODULE, "--compress", "2", "--launcher", "foo=" + TEST_MODULE);
        execute(image, "foo");

        Files.delete(jmods.resolve(TEST_MODULE + ".jmod"));

        image = Paths.get("myimage");
        runJmod(classes.toString(), TEST_MODULE, true);
        runJlink(image, TEST_MODULE, "--launcher", "bar=" + TEST_MODULE);
        execute(image, "bar");
        Files.delete(jmods.resolve(TEST_MODULE + ".jmod"));

        image = Paths.get("myimage2");
        runJmod(classes.toString(), TEST_MODULE, false /* no ModuleMainClass! */);
        // specify main class in --launcher command line
        runJlink(image, TEST_MODULE, "--launcher", "bar2=" + TEST_MODULE + "/jdk.test.Test");
        execute(image, "bar2");
        Files.delete(jmods.resolve(TEST_MODULE + ".jmod"));

        image = Paths.get("myadder");
        runJmod(classes.toString(), TEST_MODULE, false /* no ModuleMainClass! */);
        // specify main class in --launcher command line
        runJlink(image, TEST_MODULE, "--launcher", "adder=" + TEST_MODULE + "/jdk.test.Adder");
        addAndCheck(image, "adder");
    }

    private void addAndCheck(Path image, String scriptName) throws Throwable {
        String cmd = image.resolve("bin").resolve(scriptName).toString();
        OutputAnalyzer analyzer;
        if (System.getProperty("os.name").startsWith("Windows")) {
            analyzer = ProcessTools.executeProcess("sh.exe", cmd, "12", "8", "7", "--", "foo bar");
        } else {
            analyzer = ProcessTools.executeProcess(cmd, "12", "8", "7", "--", "foo bar");
        }
        if (analyzer.getExitValue() != 27) {
            throw new AssertionError("Image invocation failed: expected 27, rc=" + analyzer.getExitValue());
        }
        // last argument contains space and should be properly quoted.
        analyzer.stdoutShouldContain("Num args: 5");
    }

    private void execute(Path image, String scriptName) throws Throwable {
        String cmd = image.resolve("bin").resolve(scriptName).toString();
        OutputAnalyzer analyzer;
        if (System.getProperty("os.name").startsWith("Windows")) {
            analyzer = ProcessTools.executeProcess("sh.exe", cmd, "1", "2", "3");
        } else {
            analyzer = ProcessTools.executeProcess(cmd, "1", "2", "3");
        }
        if (analyzer.getExitValue() != 0) {
            throw new AssertionError("Image invocation failed: rc=" + analyzer.getExitValue());
        }
    }

    private void runJlink(Path image, String modName, String... options) {
        List<String> args = new ArrayList<>();
        Collections.addAll(args,
                "--module-path", jdkMods + File.pathSeparator + jmods,
                "--add-modules", modName,
                "--output", image.toString());
        Collections.addAll(args, options);

        PrintWriter pw = new PrintWriter(System.out);
        int rc = JLINK_TOOL.run(pw, pw, args.toArray(new String[args.size()]));
        if (rc != 0) {
            throw new AssertionError("Jlink failed: rc = " + rc);
        }
    }

    private void runJmod(String cp, String modName, boolean main) {
        int rc;
        if (main) {
            rc = JMOD_TOOL.run(System.out, System.out, new String[] {
                "create",
                "--class-path", cp,
                "--module-version", "1.0",
                "--main-class", "jdk.test.Test",
                jmods.resolve(modName + ".jmod").toString()
            });
        } else {
            rc = JMOD_TOOL.run(System.out, System.out, new String[] {
                "create",
                "--class-path", cp,
                "--module-version", "1.0",
                jmods.resolve(modName + ".jmod").toString(),
            });
        }

        if (rc != 0) {
            throw new AssertionError("Jmod failed: rc = " + rc);
        }
    }
}
