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
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.Path;
import java.util.function.Predicate;
import java.util.jar.JarEntry;
import jdk.jpackage.test.JavaAppDesc;
import jdk.jpackage.test.JPackageCommand;
import jdk.jpackage.test.TKit;
import jdk.jpackage.test.Executor;
import jdk.jpackage.test.HelloApp;
import jdk.jpackage.test.JavaTool;
import jdk.jpackage.test.Annotations.Parameters;
import jdk.jpackage.test.Annotations.Test;
import jdk.jpackage.test.Functional.ThrowingConsumer;
import static jdk.jpackage.tests.MainClassTest.Script.MainClassType.*;


/*
 * @test
 * @summary test different settings of main class name for jpackage
 * @library ../../../../helpers
 * @build jdk.jpackage.test.*
 * @modules jdk.jpackage/jdk.jpackage.internal
 * @compile MainClassTest.java
 * @run main/othervm/timeout=360 -Xmx512m jdk.jpackage.test.Main
 *  --jpt-run=jdk.jpackage.tests.MainClassTest
 */

public final class MainClassTest {

    static final class Script {
        Script() {
            appDesc = JavaAppDesc.parse("test.Hello");
        }

        Script modular(boolean v) {
            appDesc.setModuleName(v ? "com.other" : null);
            return this;
        }

        Script withJLink(boolean v) {
            withJLink = v;
            return this;
        }

        Script withMainClass(MainClassType v) {
            mainClass = v;
            return this;
        }

        Script withJarMainClass(MainClassType v) {
            appDesc.setWithMainClass(v != NotSet);
            jarMainClass = v;
            return this;
        }

        Script expectedErrorMessage(String v) {
            expectedErrorMessage = v;
            return this;
        }

        @Override
        public String toString() {
            return Stream.of(
                    format("modular", appDesc.moduleName() != null ? 'y' : 'n'),
                    format("main-class", mainClass),
                    format("jar-main-class", jarMainClass),
                    format("jlink", withJLink ? 'y' : 'n'),
                    format("error", expectedErrorMessage)
            ).filter(Objects::nonNull).collect(Collectors.joining("; "));
        }

        private static String format(String key, Object value) {
            if (value == null) {
                return null;
            }
            return String.join("=", key, value.toString());
        }

        enum MainClassType {
            NotSet("n"),
            SetWrong("b"),
            SetRight("y");

            MainClassType(String label) {
                this.label = label;
            }

            @Override
            public String toString() {
                return label;
            }

            private final String label;
        };

        private JavaAppDesc appDesc;
        private boolean withJLink;
        private MainClassType mainClass;
        private MainClassType jarMainClass;
        private String expectedErrorMessage;
    }

    public MainClassTest(Script script) {
        this.script = script;

        nonExistingMainClass = Stream.of(
                script.appDesc.packageName(), "ThereIsNoSuchClass").filter(
                Objects::nonNull).collect(Collectors.joining("."));

        cmd = JPackageCommand
                .helloAppImage(script.appDesc)
                .ignoreDefaultRuntime(true);
        if (!script.withJLink) {
            cmd.addArguments("--runtime-image", Path.of(System.getProperty(
                    "java.home")));
        }

        final String moduleName = script.appDesc.moduleName();
        switch (script.mainClass) {
            case NotSet:
                if (moduleName != null) {
                    // Don't specify class name, only module name.
                    cmd.setArgumentValue("--module", moduleName);
                } else {
                    cmd.removeArgumentWithValue("--main-class");
                }
                break;

            case SetWrong:
                if (moduleName != null) {
                    cmd.setArgumentValue("--module",
                            String.join("/", moduleName, nonExistingMainClass));
                } else {
                    cmd.setArgumentValue("--main-class", nonExistingMainClass);
                }
        }
    }

    @Parameters
    public static Collection scripts() {
        final var withMainClass = Set.of(SetWrong, SetRight);

        List<Script[]> scripts = new ArrayList<>();
        for (var withJLink : List.of(true, false)) {
            for (var modular : List.of(true, false)) {
                for (var mainClass : Script.MainClassType.values()) {
                    for (var jarMainClass : Script.MainClassType.values()) {
                        Script script = new Script()
                            .modular(modular)
                            .withJLink(withJLink)
                            .withMainClass(mainClass)
                            .withJarMainClass(jarMainClass);

                        if (withMainClass.contains(jarMainClass)
                                || withMainClass.contains(mainClass)) {
                        } else if (modular) {
                            script.expectedErrorMessage(
                                    "Error: Main application class is missing");
                        } else {
                            script.expectedErrorMessage(
                                    "A main class was not specified nor was one found in the jar");
                        }

                        scripts.add(new Script[]{script});
                    }
                }
            }
        }
        return scripts;
    }

    @Test
    public void test() throws IOException {
        if (script.jarMainClass == SetWrong) {
            initJarWithWrongMainClass();
        }

        if (script.expectedErrorMessage != null) {
            // This is the case when main class is not found nor in jar
            // file nor on command line.
            List<String> output = cmd
                    .saveConsoleOutput(true)
                    .execute(1)
                    .getOutput();
            TKit.assertTextStream(script.expectedErrorMessage).apply(output.stream());
            return;
        }

        // Get here only if main class is specified.
        boolean appShouldSucceed = false;

        // Should succeed if valid main class is set on the command line.
        appShouldSucceed |= (script.mainClass == SetRight);

        // Should succeed if main class is not set on the command line but set
        // to valid value in the jar.
        appShouldSucceed |= (script.mainClass == NotSet && script.jarMainClass == SetRight);

        if (appShouldSucceed) {
            cmd.executeAndAssertHelloAppImageCreated();
        } else {
            cmd.executeAndAssertImageCreated();
            if (!cmd.isFakeRuntime(String.format("Not running [%s]",
                    cmd.appLauncherPath()))) {
                List<String> output = new Executor()
                    .setDirectory(cmd.outputDir())
                    .setExecutable(cmd.appLauncherPath())
                    .dumpOutput().saveOutput()
                    .execute(1).getOutput();
                TKit.assertTextStream(String.format(
                        "Error: Could not find or load main class %s",
                        nonExistingMainClass)).apply(output.stream());
            }
        }
    }

    private void initJarWithWrongMainClass() throws IOException {
        // Call JPackageCommand.executePrerequisiteActions() to build app's jar.
        // executePrerequisiteActions() is called by JPackageCommand instance
        // only once.
        cmd.executePrerequisiteActions();

        final Path jarFile;
        if (script.appDesc.moduleName() != null) {
            jarFile = Path.of(cmd.getArgumentValue("--module-path"),
                    script.appDesc.jarFileName());
        } else {
            jarFile = cmd.inputDir().resolve(cmd.getArgumentValue("--main-jar"));
        }

        // Create new jar file filtering out main class from the old jar file.
        TKit.withTempDirectory("repack-jar", workDir -> {
            // Extract app's class from the old jar.
            explodeJar(jarFile, workDir,
                    jarEntry -> Path.of(jarEntry.getName()).equals(
                            script.appDesc.classFilePath()));

            // Create app's jar file with different main class.
            var badAppDesc = JavaAppDesc
                    .parse(script.appDesc.toString())
                    .setClassName(nonExistingMainClass);
            HelloApp.createBundle(badAppDesc, jarFile.getParent());

            // Extract new jar but skip app's class.
            explodeJar(jarFile, workDir,
                    jarEntry -> !Path.of(jarEntry.getName()).equals(
                            badAppDesc.classFilePath()));

            // At this point we should have:
            // 1. Manifest from the new jar referencing non-existing class
            //  as the main class.
            // 2. Module descriptor referencing non-existing class as the main
            //  class in case of modular app.
            // 3. App's class from the old jar. We need it to let jlink find some
            //  classes in the package declared in module descriptor
            //  in case of modular app.

            Files.delete(jarFile);
            new Executor().setToolProvider(JavaTool.JAR)
            .addArguments("-v", "-c", "-M", "-f", jarFile.toString())
            .addArguments("-C", workDir.toString(), ".")
            .dumpOutput()
            .execute();
        });
    }

    private static void explodeJar(Path jarFile, Path workDir,
            Predicate<JarEntry> filter) throws IOException {
        try (var jar = new JarFile(jarFile.toFile())) {
            jar.stream()
            .filter(Predicate.not(JarEntry::isDirectory))
            .filter(filter)
            .sequential().forEachOrdered(ThrowingConsumer.toConsumer(
                jarEntry -> {
                    try (var in = jar.getInputStream(jarEntry)) {
                        Path fileName = workDir.resolve(jarEntry.getName());
                        Files.createDirectories(fileName.getParent());
                        Files.copy(in, fileName);
                    }
                }));
        }
    }

    private final JPackageCommand cmd;
    private final Script script;
    private final String nonExistingMainClass;
}
