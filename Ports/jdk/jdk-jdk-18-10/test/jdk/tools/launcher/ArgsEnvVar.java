/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8170832 8180447
 * @summary Arguments passed in environment variable
 * @modules jdk.compiler
 *          jdk.zipfs
 * @build TestHelper
 * @run main ArgsEnvVar
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.nio.file.Paths;
import java.nio.file.Path;

public class ArgsEnvVar extends TestHelper {
    private static File testJar = null;
    private static Map<String, String> env = new HashMap<>();

    private static String JDK_JAVA_OPTIONS = "JDK_JAVA_OPTIONS";

    static void init() throws IOException {
        if  (testJar != null) {
            return;
        }
        testJar = new File("test.jar");
        StringBuilder tsrc = new StringBuilder();
        tsrc.append("public static void main(String... args) {\n");
        tsrc.append("   for (String x : args) {\n");
        tsrc.append("        System.out.println(x);\n");
        tsrc.append("   }\n");
        tsrc.append("}\n");
        createJar(testJar, new File("Foo"), tsrc.toString());

        env.put(JLDEBUG_KEY, "true");
    }

    private File createArgFile(String fname, List<String> lines) throws IOException {
        File argFile = new File(fname);
        argFile.delete();
        createAFile(argFile, lines);
        return argFile;
    }

    private void verifyOptions(List<String> args, TestResult tr) {
        if (args.isEmpty()) {
            return;
        }

        int i = 1;
        for (String x : args) {
            tr.matches(".*argv\\[" + i + "\\] = " + Pattern.quote(x) + ".*");
            i++;
        }
        if (! tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("test fails");
        }
    }

    private void verifyUserArgs(List<String> args, TestResult tr, int index) {
        if (javaCmd != TestHelper.javaCmd) {
            tr.contains("\tFirst application arg index: 1");
        } else {
            tr.contains("\tFirst application arg index: " + index);

            for (String arg: args) {
                tr.matches("^" + Pattern.quote(arg) + "$");
            }
        }

        if (! tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("test fails");
        }
    }

    @Test
    // Verify prepend and @argfile expansion
    public void basic() throws IOException {
        File argFile1 = createArgFile("argFile1", List.of("-Xmx32m"));
        File argFile2 = createArgFile("argFile2", List.of("-Darg.file2=TWO"));
        File argFile3 = createArgFile("argFile3", List.of("-Darg.file3=THREE"));

        env.put(JDK_JAVA_OPTIONS, "@argFile1\n-Xint\r-cp @@escaped\t@argFile2");

        TestResult tr = doExec(env, javaCmd, "@argFile3", "-cp", "test.jar", "Foo", "uarg1", "@uarg2");

        List<String> appArgs = new ArrayList<>();
        appArgs.add("uarg1");
        appArgs.add("@uarg2");

        List<String> options = new ArrayList<>();
        options.add("-Xmx32m");
        options.add("-Xint");
        options.add("-cp");
        options.add("@escaped");
        options.add("-Darg.file2=TWO");
        options.add("-Darg.file3=THREE");
        options.add("-cp");
        options.add("test.jar");
        options.add("Foo");
        options.addAll(appArgs);

        verifyOptions(options, tr);
        verifyUserArgs(appArgs, tr, 10);
        argFile1.delete();
        argFile2.delete();
        argFile3.delete();
    }

    private TestResult testInEnv(List<String> options) {
        env.put(JDK_JAVA_OPTIONS, String.join(" ", options));
        return doExec(env, javaCmd, "-jar", "test.jar");
    }

    private TestResult testInEnvAsArgFile(List<String> options) throws IOException {
        File argFile = createArgFile("argFile", options);
        env.put(JDK_JAVA_OPTIONS, "@argFile");
        TestResult tr = doExec(env, javaCmd, "-jar", "test.jar");
        argFile.delete();
        return tr;
    }

    @Test
    public void noTerminalOpt() throws IOException {
        List<List<String>> terminal_opts = List.of(
                List.of("-jar", "test.jar"),
                List.of("-m", "test/Foo"),
                List.of("--module", "test/Foo"),
                List.of("--module=test/Foo"),
                List.of("--dry-run"),
                List.of("-h"),
                List.of("-?"),
                List.of("-help"),
                List.of("--help"),
                List.of("-X"),
                List.of("--help-extra"),
                List.of("-version"),
                List.of("--version"),
                List.of("-fullversion"),
                List.of("--full-version"));

        for (List<String> options: terminal_opts) {
            // terminal opt in environment variable
            TestResult tr = testInEnv(options);
            tr.checkNegative();
            if (!tr.testStatus) {
                System.out.println(tr);
                throw new RuntimeException("test fails");
            }

            // terminal opt in environment variable through @file
            tr = testInEnvAsArgFile(options);
            tr.checkNegative();
            if (!tr.testStatus) {
                System.out.println(tr);
                throw new RuntimeException("test fails");
            }
        }
    }

    @Test
    public void quote() throws IOException {
        File argFile1 = createArgFile("arg File 1", List.of("-Xint"));
        File argFile2 = createArgFile("arg File 2", List.of("-Dprop='value with spaces'"));
        File argFile3 = createArgFile("arg File 3", List.of("-Xmx32m"));
        env.put(JDK_JAVA_OPTIONS, "'@arg File 1' @\"arg File 2\" @'arg File'\" 3\"");

        TestResult tr = doExec(env, javaCmd, "-jar", "test.jar");
        List<String> options = new ArrayList<>();
        options.add("-Xint");
        options.add("-Dprop=value with spaces");
        options.add("-Xmx32m");
        options.add("-jar");
        options.add("test.jar");
        verifyOptions(options, tr);
        argFile1.delete();
        argFile2.delete();
        argFile3.delete();
    }

    @Test
    public void openQuoteShouldFail() {
        env.put(JDK_JAVA_OPTIONS, "-Dprop='value missing close quote");
        TestResult tr = doExec(env, javaCmd, "-version");
        tr.checkNegative();
        if (!tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("test fails");
        }
    }

    @Test
    public void noWildcard() {
        env.put(JDK_JAVA_OPTIONS, "-cp *");
        TestResult tr = doExec(env, javaCmd, "-jar", "test.jar");
        verifyOptions(List.of("-cp", "*", "-jar", "test.jar"), tr);

        env.put(JDK_JAVA_OPTIONS, "-p ?");
        tr = doExec(env, javaCmd, "-jar", "test.jar", "one", "two");
        verifyOptions(List.of("-p", "?", "-jar", "test.jar", "one", "two"), tr);
    }

    @Test
    public void testTrailingSpaces() {
        env.put(JDK_JAVA_OPTIONS, "--add-exports java.base/jdk.internal.misc=ALL-UNNAMED ");
        TestResult tr = doExec(env, javaCmd, "-jar", "test.jar");
        verifyOptions(List.of("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED", "-jar", "test.jar"), tr);

        env.put(JDK_JAVA_OPTIONS, "--class-path ' '");
        tr = doExec(env, javaCmd, "-jar", "test.jar");
        verifyOptions(List.of("--class-path", " ", "-jar", "test.jar"), tr);

        env.put(JDK_JAVA_OPTIONS, "  --add-exports java.base/jdk.internal.misc=ALL-UNNAMED ");
        tr = doExec(env, javaCmd, "-jar", "test.jar");
        verifyOptions(List.of("--add-exports", "java.base/jdk.internal.misc=ALL-UNNAMED", "-jar", "test.jar"), tr);
    }


    @Test
    // That that we can correctly handle the module longform argument option
    // when supplied in an argument file
    public void modulesInArgsFile() throws IOException {
        File cwd = new File(".");
        File testModuleDir = new File(cwd, "modules_test");

        createEchoArgumentsModule(testModuleDir);

        Path SRC_DIR = Paths.get(testModuleDir.getAbsolutePath(), "src");
        Path MODS_DIR = Paths.get(testModuleDir.getAbsolutePath(), "mods");

        // test module / main class
        String MODULE_OPTION = "--module=test/launcher.Main";
        String TEST_MODULE = "test";

        // javac -d mods/test src/test/**
        TestResult tr = doExec(
            javacCmd,
            "-d", MODS_DIR.toString(),
            "--module-source-path", SRC_DIR.toString(),
            "--module", TEST_MODULE);

        if (!tr.isOK()) {
            System.out.println("test did not compile");
            throw new RuntimeException("Error: modules test did not compile");
        }

        // verify the terminating ability of --module= through environment variables
        File argFile = createArgFile("cmdargs", List.of("--module-path", MODS_DIR.toString(), MODULE_OPTION, "--hello"));
        env.put(JDK_JAVA_OPTIONS, "@cmdargs");
        tr = doExec(env, javaCmd);
        tr.checkNegative();
        tr.contains("Error: Option " + MODULE_OPTION + " in @cmdargs is not allowed in environment variable JDK_JAVA_OPTIONS");
        if (!tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("test fails");
        }

        // check that specifying --module and --module-path with file works
        tr = doExec(javaCmd, "-Dfile.encoding=UTF-8", "@cmdargs");
        tr.contains("[--hello]");
        if (!tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("test fails");
        }

        // check with reversed --module-path and --module in the arguments file, this will fail, --module= is terminating
        File argFile1 = createArgFile("cmdargs1", List.of(MODULE_OPTION, "--module-path", MODS_DIR.toString(), "--hello"));
        tr = doExec(javaCmd, "-Dfile.encoding=UTF-8", "@cmdargs1");
        tr.checkNegative();
        if (!tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("test fails");
        }

        // clean-up
        argFile.delete();
        argFile1.delete();
        recursiveDelete(testModuleDir);
    }

    public static void main(String... args) throws Exception {
        init();
        ArgsEnvVar a = new ArgsEnvVar();
        a.run(args);
        if (testExitValue > 0) {
            System.out.println("Total of " + testExitValue + " failed");
            System.exit(1);
        } else {
            System.out.println("All tests pass");
        }
    }
}
