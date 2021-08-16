/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Test archived module graph with custom runtime image
 * @requires vm.cds.archived.java.heap
 * @library /test/jdk/lib/testlibrary /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build sun.hotspot.WhiteBox
 * @compile CheckArchivedModuleApp.java
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar app.jar CheckArchivedModuleApp
 * @run driver jdk.test.lib.helpers.ClassFileInstaller -jar WhiteBox.jar sun.hotspot.WhiteBox
 * @run driver ArchivedModuleWithCustomImageTest
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import jdk.test.lib.compiler.CompilerUtils;
import jdk.test.lib.process.OutputAnalyzer;
import jdk.test.lib.process.ProcessTools;
import jdk.test.lib.helpers.ClassFileInstaller;

public class ArchivedModuleWithCustomImageTest {
    private static final String JAVA_HOME = System.getProperty("java.home");
    private static final String TEST_MODULE = "test";
    private static final Path jdkHome = Paths.get(System.getProperty("test.jdk"));
    private static final Path jdkMods = jdkHome.resolve("jmods");
    private static final Path testSrc = Paths.get(System.getProperty("test.src"));
    private static final Path src = testSrc.resolve("src").resolve(TEST_MODULE);
    private static final Path classes = Paths.get("classes");
    private static final Path jmods = Paths.get("jmods");

    public static void main(String[] args) throws Throwable {
        if (Files.notExists(jdkMods)) {
            System.out.println("No jmods/ in test JDK, not supported.");
            return;
        }

        // compile test module class
        if (!CompilerUtils.compile(src, classes)) {
            throw new RuntimeException("Compilation failure.");
        }

        // create custom runtime image named 'myimage'
        Files.createDirectories(jmods);
        Path image = Paths.get("myimage");
        runJmod(classes.toString(), TEST_MODULE);
        runJlink(image, TEST_MODULE);

        // test using 'myimage'
        testArchivedModuleUsingImage(image);

        Files.delete(jmods.resolve(TEST_MODULE + ".jmod"));
    }

    private static void runJlink(Path image, String modName) throws Throwable {
        Path jlink = Paths.get(JAVA_HOME, "bin", "jlink");
        OutputAnalyzer output = ProcessTools.executeProcess(jlink.toString(),
                        "--output", image.toString(),
                        "--add-modules", modName,
                        "--module-path", jdkMods + File.pathSeparator + jmods);
        output.shouldHaveExitValue(0);
    }

    private static void runJmod(String cp, String modName) throws Throwable {
        Path jmod = Paths.get(JAVA_HOME, "bin", "jmod");
        OutputAnalyzer output = ProcessTools.executeProcess(jmod.toString(),
                       "create",
                       "--class-path", cp,
                       "--module-version", "1.0",
                       "--main-class", "jdk.test.Test",
                       jmods.resolve(modName + ".jmod").toString());
        output.shouldHaveExitValue(0);
    }

    private static void testArchivedModuleUsingImage(Path image)
                            throws Throwable {
        String wbJar = ClassFileInstaller.getJarPath("WhiteBox.jar");
        String use_whitebox_jar = "-Xbootclasspath/a:" + wbJar;
        String appJar = ClassFileInstaller.getJarPath("app.jar");
        Path customJava = Paths.get(image.toString(), "bin", "java");

        // -Xshare:dump with custom runtime image
        String[] dumpCmd = {
            customJava.toString(),
            "-XX:SharedArchiveFile=./ArchivedModuleWithCustomImageTest.jsa",
            "-Xshare:dump", "-Xlog:cds"};
        printCommand(dumpCmd);
        ProcessBuilder pbDump = new ProcessBuilder();
        pbDump.command(dumpCmd);
        OutputAnalyzer output = TestCommon.executeAndLog(
            pbDump, "custom.runtime.image.dump");
        TestCommon.checkDump(output);

        // Test case 1):
        // test archived module graph objects are used with custome runtime image
        System.out.println("------------------- Test case 1 -------------------");
        String[] runCmd = {customJava.toString(),
                           use_whitebox_jar,
                           "-XX:SharedArchiveFile=./ArchivedModuleWithCustomImageTest.jsa",
                           "-cp",
                           appJar,
                           "-Xshare:on",
                           "-XX:+UnlockDiagnosticVMOptions",
                           "-XX:+WhiteBoxAPI",
                           "CheckArchivedModuleApp",
                           "yes",
                           "yes"};
        printCommand(runCmd);
        ProcessBuilder pbRun = new ProcessBuilder();
        pbRun.command(runCmd);
        output = TestCommon.executeAndLog(pbRun, "custom.runtime.image.run");
        output.shouldHaveExitValue(0);


        // Test case 2):
        // verify --show-module-resolution output
        System.out.println("------------------- Test case 2 -------------------");

        // myimage/bin/java -Xshare:off --show-module-resolution -version
        String[] showModuleCmd1 = {customJava.toString(),
                                   "-Xshare:off",
                                   "--show-module-resolution",
                                   "-version"};
        printCommand(showModuleCmd1);
        pbRun = new ProcessBuilder();
        pbRun.command(showModuleCmd1);
        output = TestCommon.executeAndLog(
            pbRun, "custom.runtime.image.showModuleResolution.nocds");
        output.shouldHaveExitValue(0);
        String moduleResolutionOut1 = output.getStdout();

        // myimage/bin/java -Xshare:on --show-module-resolution -version
        //    -XX:SharedArchiveFile=./ArchivedModuleWithCustomImageTest.jsa
        String[] showModuleCmd2 = {
            customJava.toString(),
            "-XX:SharedArchiveFile=./ArchivedModuleWithCustomImageTest.jsa",
            "-Xshare:on",
            "--show-module-resolution",
            "-version"};
        printCommand(showModuleCmd2);
        pbRun = new ProcessBuilder();
        pbRun.command(showModuleCmd2);
        output = TestCommon.executeAndLog(
            pbRun, "custom.runtime.image.showModuleResolution.cds");
        if (output.getStderr().contains("sharing")) {
            String moduleResolutionOut2 = output.getStdout();
            TestCommon.checkOutputStrings(
                moduleResolutionOut1, moduleResolutionOut2, "\n");
        }
    }

    private static void printCommand(String opts[]) {
        StringBuilder cmdLine = new StringBuilder();
        for (String cmd : opts)
            cmdLine.append(cmd).append(' ');
        System.out.println("Command line: [" + cmdLine.toString() + "]");
    }
}
