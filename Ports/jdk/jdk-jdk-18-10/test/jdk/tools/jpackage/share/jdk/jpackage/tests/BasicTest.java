/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.tests;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.JavaAppDesc;
import jdk.jpackage.test.PackageTest;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.Executor;
import jdk.jpackage.test.JavaTool;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Annotations.Parameter;

/*
 * @test
 * @summary jpackage basic testing
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile BasicTest.java
 * @run main/othervm/timeout=720 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.BasicTest
 */

public final class BasicTest {
    @Test
    public void testNoArgs() {
        List<String> output =
                getJPackageToolProvider().executeAndGetOutput();
        TKit.assertStringListEquals(List.of("Usage: jpackage <options>",
                "Use jpackage --help (or -h) for a list of possible options"),
                output, "Check jpackage output");
    }

    @Test
    public void testJpackageProps() {
        String appVersion = "3.0";
        JPackageCommand cmd = JPackageCommand.helloAppImage(
                JavaAppDesc.parse("Hello"))
                // Disable default logic adding `--verbose` option
                // to jpackage command line.
                .ignoreDefaultVerbose(true)
                .saveConsoleOutput(true)
                .addArguments("--app-version", appVersion, "--arguments",
                    "jpackage.app-version jpackage.app-path")
                .ignoreDefaultRuntime(true);

        cmd.executeAndAssertImageCreated();
        Path launcherPath = cmd.appLauncherPath();

        List<String> output = HelloApp.executeLauncher(cmd).getOutput();

        TKit.assertTextStream("jpackage.app-version=" + appVersion).apply(output.stream());
        TKit.assertTextStream("jpackage.app-path=").apply(output.stream());
    }

    @Test
    public void testVersion() {
        List<String> output =
                getJPackageToolProvider()
                        .addArgument("--version")
                        .executeAndGetOutput();
        TKit.assertStringListEquals(List.of(System.getProperty("java.version")),
                output, "Check jpackage output");
    }

    @Test
    public void testHelp() {
        List<String> hOutput = getJPackageToolProvider()
                .addArgument("-h").executeAndGetOutput();
        List<String> helpOutput = getJPackageToolProvider()
                .addArgument("--help").executeAndGetOutput();

        TKit.assertStringListEquals(hOutput, helpOutput,
                "Check -h and --help parameters produce the same output");

        final String windowsPrefix = "--win-";
        final String linuxPrefix = "--linux-";
        final String osxPrefix = "--mac-";

        final String expectedPrefix;
        final List<String> unexpectedPrefixes;

        if (TKit.isWindows()) {
            expectedPrefix = windowsPrefix;
            unexpectedPrefixes = List.of(osxPrefix, linuxPrefix);
        } else if (TKit.isLinux()) {
            expectedPrefix = linuxPrefix;
            unexpectedPrefixes = List.of(windowsPrefix, osxPrefix);
        } else if (TKit.isOSX()) {
            expectedPrefix = osxPrefix;
            unexpectedPrefixes = List.of(linuxPrefix,  windowsPrefix);
        } else {
            throw TKit.throwUnknownPlatformError();
        }

        Function<String, Predicate<String>> createPattern = (prefix) -> {
            return Pattern.compile("^  " + prefix).asPredicate();
        };

        Function<List<String>, Long> countStrings = (prefixes) -> {
            return hOutput.stream().filter(
                    prefixes.stream().map(createPattern).reduce(x -> false,
                            Predicate::or)).peek(TKit::trace).count();
        };

        TKit.trace("Check parameters in help text");
        TKit.assertNotEquals(0, countStrings.apply(List.of(expectedPrefix)),
                "Check help text contains platform specific parameters");
        TKit.assertEquals(0, countStrings.apply(unexpectedPrefixes),
                "Check help text doesn't contain unexpected parameters");
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testVerbose() {
        JPackageCommand cmd = JPackageCommand.helloAppImage()
                // Disable default logic adding `--verbose` option
                // to jpackage command line.
                .ignoreDefaultVerbose(true)
                .saveConsoleOutput(true)
                .setFakeRuntime().executePrerequisiteActions();

        List<String> expectedVerboseOutputStrings = new ArrayList<>();
        expectedVerboseOutputStrings.add("Creating app package:");
        if (TKit.isWindows()) {
            expectedVerboseOutputStrings.add(
                    "Succeeded in building Windows Application Image package");
        } else if (TKit.isLinux()) {
            expectedVerboseOutputStrings.add(
                    "Succeeded in building Linux Application Image package");
        } else if (TKit.isOSX()) {
            expectedVerboseOutputStrings.add("Preparing Info.plist:");
            expectedVerboseOutputStrings.add(
                    "Succeeded in building Mac Application Image package");
        } else {
            TKit.throwUnknownPlatformError();
        }

        TKit.deleteDirectoryContentsRecursive(cmd.outputDir());
        List<String> nonVerboseOutput = cmd.execute().getOutput();
        List<String>[] verboseOutput = (List<String>[])new List<?>[1];

        // Directory clean up is not 100% reliable on Windows because of
        // antivirus software that can lock .exe files. Setup
        // different output directory instead of cleaning the default one for
        // verbose jpackage run.
        TKit.withTempDirectory("verbose-output", tempDir -> {
            cmd.setArgumentValue("--dest", tempDir);
            cmd.addArgument("--verbose");
            verboseOutput[0] = cmd.execute().getOutput();
        });

        TKit.assertTrue(nonVerboseOutput.size() < verboseOutput[0].size(),
                "Check verbose output is longer than regular");

        expectedVerboseOutputStrings.forEach(str -> {
            TKit.assertTextStream(str).label("regular output")
                    .predicate(String::contains).negate()
                    .apply(nonVerboseOutput.stream());
        });

        expectedVerboseOutputStrings.forEach(str -> {
            TKit.assertTextStream(str).label("verbose output")
                    .apply(verboseOutput[0].stream());
        });
    }

    @Test
    public void testNoName() {
        final String mainClassName = "Greetings";

        JPackageCommand cmd = JPackageCommand.helloAppImage(mainClassName)
                .removeArgumentWithValue("--name");

        Path expectedImageDir = cmd.outputDir().resolve(mainClassName);
        if (TKit.isOSX()) {
            expectedImageDir = expectedImageDir.getParent().resolve(
                    expectedImageDir.getFileName().toString() + ".app");
        }

        cmd.executeAndAssertHelloAppImageCreated();
        TKit.assertEquals(expectedImageDir.toAbsolutePath().normalize().toString(),
                cmd.outputBundle().toAbsolutePath().normalize().toString(),
                String.format(
                        "Check [%s] directory is filled with application image data",
                        expectedImageDir));
    }

    @Test
    // Regular app
    @Parameter("Hello")
    // Modular app in .jar file
    @Parameter("com.other/com.other.Hello")
    // Modular app in .jmod file
    @Parameter("hello.jmod:com.other/com.other.Hello")
    // Modular app in exploded .jmod file
    @Parameter("hello.ejmod:com.other/com.other.Hello")
    public void testApp(String javaAppDesc) {
        JavaAppDesc appDesc = JavaAppDesc.parse(javaAppDesc);
        JPackageCommand cmd = JPackageCommand.helloAppImage(appDesc);
        if (appDesc.jmodFileName() != null) {
            // .jmod files are not supported at run-time. They should be
            // bundled in Java run-time with jlink command, so disable
            // use of external Java run-time if any configured.
            cmd.ignoreDefaultRuntime(true);
        }
        cmd.executeAndAssertHelloAppImageCreated();
    }

    @Test
    public void testWhitespaceInPaths() {
        JPackageCommand.helloAppImage("a/b c.jar:Hello")
        .setArgumentValue("--input", TKit.workDir().resolve("The quick brown fox"))
        .setArgumentValue("--dest", TKit.workDir().resolve("jumps over the lazy dog"))
        .executeAndAssertHelloAppImageCreated();
    }

    @Test
    @Parameter("ALL-MODULE-PATH")
    @Parameter("ALL-DEFAULT")
    @Parameter("java.desktop")
    @Parameter("java.desktop,jdk.jartool")
    @Parameter({ "java.desktop", "jdk.jartool" })
    public void testAddModules(String... addModulesArg) {
        JPackageCommand cmd = JPackageCommand
                .helloAppImage("goodbye.jar:com.other/com.other.Hello")
                .ignoreDefaultRuntime(true); // because of --add-modules
        Stream.of(addModulesArg).map(v -> Stream.of("--add-modules", v)).flatMap(
                s -> s).forEachOrdered(cmd::addArgument);
        cmd.executeAndAssertHelloAppImageCreated();
    }

    /**
     * Test --temp option. Doesn't make much sense for app image as temporary
     * directory is used only on Windows. Test it in packaging mode.
     * @throws IOException
     */
    @Test
    @Parameter("true")
    @Parameter("false")
    public void testTemp(boolean withExistingTempDir) throws IOException {
        final Path tempRoot = TKit.createTempDirectory("tmp");
        // This Test has problems on windows where path in the temp dir are too long
        // for the wix tools.  We can't use a tempDir outside the TKit's WorkDir, so
        // we minimize both the tempRoot directory name (above) and the tempDir name
        // (below) to the extension part (which is necessary to differenciate between
        // the multiple PackageTypes that will be run for one JPackageCommand).
        // It might be beter if the whole work dir name was shortened from:
        // jtreg_open_test_jdk_tools_jpackage_share_jdk_jpackage_tests_BasicTest_java.
        Function<JPackageCommand, Path> getTempDir = cmd -> {
            String ext = cmd.outputBundle().getFileName().toString();
            int i = ext.lastIndexOf(".");
            if (i > 0 && i < (ext.length() - 1)) {
                ext = ext.substring(i+1);
            }
            return tempRoot.resolve(ext);
        };

        Supplier<PackageTest> createTest = () -> {
            return new PackageTest()
            .configureHelloApp()
            // Force save of package bundle in test work directory.
            .addInitializer(JPackageCommand::setDefaultInputOutput)
            .addInitializer(cmd -> {
                Path tempDir = getTempDir.apply(cmd);
                if (withExistingTempDir) {
                    Files.createDirectories(tempDir);
                } else {
                    Files.createDirectories(tempDir.getParent());
                }
                cmd.addArguments("--temp", tempDir);
            });
        };

        createTest.get()
        .addBundleVerifier(cmd -> {
            // Check jpackage actually used the supplied directory.
            Path tempDir = getTempDir.apply(cmd);
            TKit.assertNotEquals(0, tempDir.toFile().list().length,
                    String.format(
                            "Check jpackage wrote some data in the supplied temporary directory [%s]",
                            tempDir));
        })
        .run(PackageTest.Action.CREATE);

        createTest.get()
        // Temporary directory should not be empty,
        // jpackage should exit with error.
        .setExpectedExitCode(1)
        .run(PackageTest.Action.CREATE);
    }

    @Test
    public void testAtFile() throws IOException {
        JPackageCommand cmd = JPackageCommand
                .helloAppImage()
                .setArgumentValue("--dest", TKit.createTempDirectory("output"));

        // Init options file with the list of options configured
        // for JPackageCommand instance.
        final Path optionsFile = TKit.createTempFile(Path.of("options"));
        Files.write(optionsFile,
                List.of(String.join(" ", cmd.getAllArguments())));

        // Build app jar file.
        cmd.executePrerequisiteActions();

        // Instead of running jpackage command through configured
        // JPackageCommand instance, run vanilla jpackage command with @ file.
        getJPackageToolProvider()
                .addArgument(String.format("@%s", optionsFile))
                .execute();

        // Verify output of jpackage command.
        cmd.assertImageCreated();
        HelloApp.executeLauncherAndVerifyOutput(cmd);
    }

    private static Executor getJPackageToolProvider() {
        return getToolProvider(JavaTool.JPACKAGE);
    }

    private static Executor getToolProvider(JavaTool tool) {
        return new Executor().dumpOutput().saveOutput().setToolProvider(tool);
    }
}
