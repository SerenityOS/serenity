/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8156998
 * @summary Test --inherit-runtime-environment
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask toolbox.JavaTask ModuleTestBase
 * @run main InheritRuntimeEnvironmentTest
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import toolbox.ModuleBuilder;
import toolbox.JavaTask;
import toolbox.JavacTask;
import toolbox.Task;

/**
 * Tests that javac picks up runtime options with --inherit-runtime-environment.
 * For each option, javac is first run using the option directly, as a control.
 * javac is then run again, with the same option(s) being passed to the runtime,
 * and --inherit-runtime-environment being used by javac.
 * @author jjg
 */
public class InheritRuntimeEnvironmentTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        InheritRuntimeEnvironmentTest t = new InheritRuntimeEnvironmentTest();
        t.runTests();
    }

    /**
     * Tests that code being compiled can access JDK-internal API using -add-exports.
     * @param base
     * @throws Exception
     */
    @Test
    public void testAddExports(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "class C { com.sun.tools.javac.main.Main main; }");

        new TestCase(base)
                .testOpts("--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED")
                .files(findJavaFiles(src))
                .run();
    }

    /**
     * Tests that code in the unnamed module can access a module on the module path using --add-modules.
     */
    @Test
    public void testAddModules(Path base) throws Exception {
        Path modules = base.resolve("modules");
        new ModuleBuilder(tb, "m1")
                .exports("pkg1")
                .classes("package pkg1; public class C1 { }")
                .build(modules);

        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "class C { pkg1.C1 c1; }");

        new TestCase(base)
                .testOpts("--module-path", modules.toString(), "--add-modules", "m1")
                .files(findJavaFiles(src))
                .run();
    }

    /**
     * Tests that a module on the module path is not visible when --limit-modules is used to
     * restrict the set of observable modules.
     */
    @Test
    public void testLimitModules(Path base) throws Exception {
        Path modules = base.resolve("modules");
        new ModuleBuilder(tb, "m1")
                .exports("pkg1")
                .classes("package pkg1; public class C1 { }")
                .build(modules);

        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m2")
                .requires("m1")
                .classes("package pkg2; public class C2 { pkg1.C1 c1; }")
                .write(src);

        // This is the control, to verify that by default, the module being compiled will
        // be able to read modules on the module path
        new TestCase(base)
                .testOpts("--module-path", modules.toString())
                .otherOpts("--module-source-path", src.toString())
                .files(findJavaFiles(src))
                .run();

        Path emptyClassPath = base.resolve("emptyClassPath");

        Files.createDirectories(emptyClassPath);

        // This is the test, to verify that the module being compiled will not be able to read
        // modules on the module path when a --limit-modules is used
        new TestCase(base)
                .testOpts("--module-path", modules.toString(), "--limit-modules", "jdk.compiler")
                .otherOpts("-XDrawDiagnostics",
                        "--module-source-path", src.toString(),
                        "-classpath", emptyClassPath.toString())
                .files(findJavaFiles(src))
                .expect(Task.Expect.FAIL, "compiler.err.module.not.found")
                .run();
    }

    /**
     * Tests that a module being compiled can see another module on the module path
     * using --module-path.
     */
    @Test
    public void testModulePath(Path base) throws Exception {
        Path modules = base.resolve("modules");
        new ModuleBuilder(tb, "m1")
                .exports("pkg1")
                .classes("package pkg1; public class C1 { }")
                .build(modules);

        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m2")
                .requires("m1")
                .classes("package pkg2; public class C2 { pkg1.C1 c1; }")
                .write(src);

        new TestCase(base)
                .testOpts("--module-path", modules.toString())
                .otherOpts("--module-source-path", src.toString())
                .files(findJavaFiles(src))
                .run();
    }

    /**
     * Tests that a module being compiled can see classes patches into an existing module
     * with --patch-module
     */
    @Test
    public void testPatchModule(Path base) throws Exception {
        Path patchSrc = base.resolve("patchSrc");
        tb.writeJavaFiles(patchSrc,
                "package java.util; public class Xyzzy { }");
        Path patch = base.resolve("patch");
        Files.createDirectories(patch);

        new JavacTask(tb)
                .options("--patch-module", "java.base=" + patchSrc.toString())
                .outdir(patch)
                .sourcepath(patchSrc)
                .files(findJavaFiles(patchSrc))
                .run()
                .writeAll();

        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "public class C { java.util.Xyzzy x; }");

        new TestCase(base)
                .testOpts("--patch-module", "java.base=" + patch)
                .files(findJavaFiles(src))
                .run();
    }

    /**
     * Tests that options in @files are also effective.
     * The test is similar to testModulePath, except that the test options are provided in an @-file.
     */
    @Test
    public void testAtFile(Path base) throws Exception {
        Path modules = base.resolve("modules");
        new ModuleBuilder(tb, "m1")
                .exports("pkg1")
                .classes("package pkg1; public class C1 { }")
                .build(modules);

        Path src = base.resolve("src");
        new ModuleBuilder(tb, "m2")
                .requires("m1")
                .classes("package pkg2; public class C2 { pkg1.C1 c1; }")
                .write(src);

        Path atFile = base.resolve("atFile");
        tb.writeFile(atFile, "--module-path " + modules);

        new TestCase(base)
                .testOpts("@" + atFile)
                .otherOpts("--module-source-path", src.toString())
                .files(findJavaFiles(src))
                .run();
    }

    /**
     * Tests that --inherit-runtime-environment works in conjunction with
     * environment variables.
     * This is a variant of testAddExports.
     * The use of environment variables is sufficiently custom that it is
     * not easy to do this directly with a simple TestCase.
     */
    @Test
    public void testEnvVars(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "class C { com.sun.tools.javac.main.Main main; }");
        List<String> testOpts =
                Arrays.asList("--add-exports", "jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED");
        List<Path> files = Arrays.asList(findJavaFiles(src));

        String envName = "JDK_JAVAC_OPTIONS";
        String envValue = String.join(" ", testOpts);

        out.println("  javac:");
        Path javacOutDir = base.resolve("out-javac");
        Files.createDirectories(javacOutDir);

        out.println("    env: " + envName + "=" + envValue);
        out.println("    outdir: " + javacOutDir);
        out.println("    files: " + files);

        new JavacTask(tb, Task.Mode.EXEC)
                .envVar(envName, envValue)
                .outdir(javacOutDir)
                .files(files)
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        out.println("  java:");
        Path javaOutDir = base.resolve("out-java");
        Files.createDirectories(javaOutDir);

        Path atFile = base.resolve("atFile");
        tb.writeFile(atFile, String.join(" ", testOpts));

        List<String> vmOpts = Arrays.asList(
                "@" + atFile,
                "--module", "jdk.compiler/com.sun.tools.javac.Main"
        );

        List<String> classArgs = join(
                Arrays.asList("-d", javaOutDir.toString()),
                files.stream()
                        .map(p -> p.toString())
                        .collect(Collectors.toList())
        );

        envValue = "--inherit-runtime-environment";

        out.println("    env: " + envName + "=" + envValue);
        out.println("    vmOpts: " + vmOpts);
        out.println("    classArgs: " + classArgs);

        new JavaTask(tb)
                .envVar(envName, envValue)
                .vmOptions(vmOpts)
                .classArgs(classArgs)
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.STDERR);
    }

    /**
     * Runs javac with given test options,  first directly, and then again, specifying the
     * options to the runtime, and using --inherit-runtime-environment.
     */
    class TestCase {
        final Path base;
        List<String> testOpts = Collections.emptyList();
        List<String> otherOpts = Collections.emptyList();
        List<Path> files = Collections.emptyList();
        Task.Expect expect = Task.Expect.SUCCESS;
        String expectedText;

        /**
         * Creates a test case, specifying a base directory for work files.
         */
        TestCase(Path base) {
            this.base = base;
        }

        /**
         * Set the "test options" to be passed to javac or to the runtime.
         */
        TestCase testOpts(String... testOpts) {
            this.testOpts = Arrays.asList(testOpts);
            return this;
        }

        /**
         * Sets additional options required for the compilation.
         */
        TestCase otherOpts(String... otherOpts) {
            this.otherOpts = Arrays.asList(otherOpts);
            return this;
        }

        /**
         * Sets the files to be compiled.
         */
        TestCase files(Path... files) {
            this.files = Arrays.asList(files);
            return this;
        }

        /**
         * Sets the expected output, and any expected output from javac.
         * The default is {@code Expect.SUCCESS} and no specific output expected.
         */
        TestCase expect(Task.Expect expect, String expectedText) {
            this.expect = expect;
            this.expectedText = expectedText;
            return this;
        }

        /**
         * Runs the test case.
         * First, javac is run passing the test options directly to javac.
         * Then, javac is run again, passing the test options to the runtime,
         * and using --inherit-runtime-environment.
         */
        void run() throws IOException {
            runJavac();
            runJava();
        }

        private void runJavac() throws IOException {
            out.println("  javac:");
            Path javacOutDir = base.resolve("out-javac");
            Files.createDirectories(javacOutDir);

            List<String> options = join(testOpts, otherOpts);

            out.println("    options: " + options);
            out.println("    outdir: " + javacOutDir);
            out.println("    files: " + files);

            String log = new JavacTask(tb, Task.Mode.CMDLINE)
                    .options(options)
                    .outdir(javacOutDir)
                    .files(files)
                    .run(expect)
                    .writeAll()
                    .getOutput(Task.OutputKind.DIRECT);

            if (expectedText != null && !log.contains(expectedText))
                error("expected text not found");
        }

        private void runJava() throws IOException {
            out.println("  java:");
            Path javaOutDir = base.resolve("out-java");
            Files.createDirectories(javaOutDir);

            List<String> vmOpts = join(
                    testOpts,
                    Arrays.asList("--module", "jdk.compiler/com.sun.tools.javac.Main")
            );

            List<String> classArgs = join(
                    Arrays.asList("--inherit-runtime-environment",
                            "-d", javaOutDir.toString()),
                    otherOpts,
                    files.stream()
                            .map(p -> p.toString())
                            .collect(Collectors.toList())
            );

            out.println("    vmOpts: " + vmOpts);
            out.println("    classArgs: " + classArgs);

            String log = new JavaTask(tb)
                    .vmOptions(vmOpts)
                    .classArgs(classArgs)
                    .run(expect)
                    .writeAll()
                    .getOutput(Task.OutputKind.STDERR);

            if (expectedText != null && !log.contains(expectedText))
                error("expected text not found");
        }
    }

    /**
     * Join a series of lists.
     */
    @SafeVarargs
    private <T> List<T> join(List<T>... lists) {
        return Arrays.stream(lists)
            .flatMap(list -> list.stream())
            .collect(Collectors.toList());
    }

}

