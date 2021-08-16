/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8027634 8231863
 * @summary Argument parsing from file
 * @modules jdk.compiler
 *          jdk.zipfs
 * @build TestHelper
 * @run main ArgsFileTest
 */
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ArgsFileTest extends TestHelper {
    private static File testJar = null;
    private static Map<String, String> env = new HashMap<>();

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

    private File createArgFile(String fname, List<String> lines, boolean endWithNewline) throws IOException {
        File argFile = new File(fname);
        argFile.delete();
        createAFile(argFile, lines, endWithNewline);
        return argFile;
    }

    private File createArgFile(String fname, List<String> lines) throws IOException {
        return createArgFile(fname, lines, true);
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
    public void expandAll() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("-Xmx32m");
        lines.add("-Xint");
        File argFile1 = createArgFile("argFile1", lines);
        lines = new ArrayList<>();
        lines.add("-jar");
        lines.add("test.jar");
        lines.add("uarg1 @uarg2 @@uarg3 -uarg4 uarg5");
        File argFile2 = createArgFile("argFile2", lines);

        TestResult tr = doExec(env, javaCmd, "@argFile1", "@argFile2");

        List<String> appArgs = new ArrayList<>();
        appArgs.add("uarg1");
        appArgs.add("@uarg2");
        appArgs.add("@@uarg3");
        appArgs.add("-uarg4");
        appArgs.add("uarg5");

        List<String> options = new ArrayList<>();
        options.add("-Xmx32m");
        options.add("-Xint");
        options.add("-jar");
        options.add("test.jar");
        options.addAll(appArgs);

        verifyOptions(options, tr);
        verifyUserArgs(appArgs, tr, 5);
        argFile1.delete();
        argFile2.delete();

        File cpFile = createArgFile("cpFile", Arrays.asList("-cp", "test.jar"));
        List<String> appCmd = new ArrayList<>();
        appCmd.add("Foo");
        appCmd.addAll(appArgs);
        File appFile = createArgFile("appFile", appCmd);

        tr = doExec(env, javaCmd, "@cpFile", "@appFile");
        verifyOptions(Arrays.asList("-cp", "test.jar", "Foo",
                "uarg1", "@uarg2", "@@uarg3", "-uarg4", "uarg5"), tr);
        verifyUserArgs(appArgs, tr, 4);
        cpFile.delete();
        appFile.delete();
    }

    @Test
    public void escapeArg() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("-Xmx32m");
        lines.add("-Xint");
        File argFile1 = createArgFile("argFile1", lines);

        TestResult tr = doExec(env, javaCmd, "-cp", "@@arg", "-cp", "@",
                "-cp", "@@@cp", "@argFile1", "@@@@Main@@@@", "-version");
        List<String> options = new ArrayList<>();
        options.add("-cp");
        options.add("@arg");
        options.add("-cp");
        options.add("@");
        options.add("-cp");
        options.add("@@cp");
        options.add("-Xmx32m");
        options.add("-Xint");
        options.add("@@@Main@@@@");
        options.add("-version");
        verifyOptions(options, tr);
        verifyUserArgs(Collections.emptyList(), tr, options.size());
        argFile1.delete();
    }

    @Test
    public void killSwitch() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("-Xmx32m");
        lines.add("-Xint");
        File argFile1 = createArgFile("argFile1", lines);
        lines = new ArrayList<>();
        lines.add("-jar");
        lines.add("test.jar");
        lines.add("uarg1 @uarg2 @@uarg3 -uarg4 uarg5");
        File argFile2 = createArgFile("argFile2", lines);
        File argKill = createArgFile("argKill",
            Collections.singletonList("--disable-@files"));

        TestResult tr = doExec(env, javaCmd, "@argFile1", "--disable-@files", "@argFile2");
        List<String> options = new ArrayList<>();
        options.add("-Xmx32m");
        options.add("-Xint");
        options.add("--disable-@files");
        options.add("@argFile2");
        verifyOptions(options, tr);
        // Main class is @argFile2
        verifyUserArgs(Collections.emptyList(), tr, 5);

        // Specify in file is same as specify inline
        tr = doExec(env, javaCmd, "@argFile1", "@argKill", "@argFile2");
        verifyOptions(options, tr);
        // Main class is @argFile2
        verifyUserArgs(Collections.emptyList(), tr, 5);

        // multiple is fine, once on is on.
        tr = doExec(env, javaCmd, "@argKill", "@argFile1", "--disable-@files", "@argFile2");
        options = Arrays.asList("--disable-@files", "@argFile1",
                "--disable-@files", "@argFile2");
        verifyOptions(options, tr);
        verifyUserArgs(Collections.emptyList(), tr, 3);

        // after main class, becoming an user application argument
        tr = doExec(env, javaCmd, "@argFile2", "@argKill");
        options = Arrays.asList("-jar", "test.jar", "uarg1", "@uarg2", "@@uarg3",
                "-uarg4", "uarg5", "@argKill");
        verifyOptions(options, tr);
        verifyUserArgs(Arrays.asList("uarg1", "@uarg2", "@@uarg3",
                "-uarg4", "uarg5", "@argKill"), tr, 3);

        argFile1.delete();
        argFile2.delete();
        argKill.delete();
    }

    @Test
    public void userApplication() throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("-Xmx32m");
        lines.add("-Xint");
        File vmArgs = createArgFile("vmArgs", lines);
        File jarOpt = createArgFile("jarOpt", Arrays.asList("-jar"));
        File cpOpt = createArgFile("cpOpt", Arrays.asList("-cp"));
        File jarArg = createArgFile("jarArg", Arrays.asList("test.jar"));
        File userArgs = createArgFile("userArgs", Arrays.asList("-opt", "arg", "--longopt"));

        TestResult tr = doExec(env, javaCmd,
                "@vmArgs", "@jarOpt", "test.jar", "-opt", "arg", "--longopt");
        verifyOptions(Arrays.asList(
                "-Xmx32m", "-Xint", "-jar", "test.jar", "-opt", "arg", "--longopt"), tr);
        verifyUserArgs(Arrays.asList("-opt", "arg", "--longopt"), tr, 5);

        tr = doExec(env, javaCmd, "@jarOpt", "@jarArg", "@vmArgs");
        verifyOptions(Arrays.asList("-jar", "test.jar", "@vmArgs"), tr);
        verifyUserArgs(Arrays.asList("@vmArgs"), tr, 3);

        tr = doExec(env, javaCmd, "-cp", "@jarArg", "@vmArgs", "Foo", "@userArgs");
        verifyOptions(Arrays.asList("-cp", "test.jar", "-Xmx32m", "-Xint",
                "Foo", "@userArgs"), tr);
        verifyUserArgs(Arrays.asList("@userArgs"), tr, 6);

        tr = doExec(env, javaCmd, "@cpOpt", "@jarArg", "@vmArgs", "Foo", "@userArgs");
        verifyOptions(Arrays.asList("-cp", "test.jar", "-Xmx32m", "-Xint",
                "Foo", "@userArgs"), tr);
        verifyUserArgs(Arrays.asList("@userArgs"), tr, 6);

        tr = doExec(env, javaCmd, "@cpOpt", "test.jar", "@vmArgs", "Foo", "@userArgs");
        verifyOptions(Arrays.asList("-cp", "test.jar", "-Xmx32m", "-Xint",
                "Foo", "@userArgs"), tr);
        verifyUserArgs(Arrays.asList("@userArgs"), tr, 6);

        vmArgs.delete();
        jarOpt.delete();
        cpOpt.delete();
        jarArg.delete();
        userArgs.delete();
    }

    @Test
    public void userApplicationWithoutEmptyLastLine() throws IOException {
        File cpOpt = createArgFile("cpOpt", Arrays.asList("-classpath ."), false);
        File vmArgs = createArgFile("vmArgs", Arrays.asList("-Xint"), false);

        TestResult tr = doExec(env, javaCmd, "-cp", "test.jar", "@cpOpt", "Foo", "-test");
        verifyOptions(Arrays.asList("-cp", "test.jar", "-classpath", ".", "Foo", "-test"), tr);
        verifyUserArgs(Arrays.asList("-test"), tr, 6);

        tr = doExec(env, javaCmd,  "-cp", "test.jar", "@vmArgs", "Foo", "-test");
        verifyOptions(Arrays.asList("-cp", "test.jar", "-Xint", "Foo", "-test"), tr);
        verifyUserArgs(Arrays.asList("-test"), tr, 5);

        cpOpt.delete();
        vmArgs.delete();
    }

    // test with missing file
    @Test
    public void missingFileNegativeTest() throws IOException {
        TestResult tr = doExec(javaCmd, "@" + "missing.cmd");
        tr.checkNegative();
        tr.contains("Error: could not open `missing.cmd'");
        if (!tr.testStatus) {
            System.out.println(tr);
            throw new RuntimeException("test fails");
        }
    }

    public static void main(String... args) throws Exception {
        init();
        ArgsFileTest a = new ArgsFileTest();
        a.run(args);
        if (testExitValue > 0) {
            System.out.println("Total of " + testExitValue + " failed");
            System.exit(1);
        } else {
            System.out.println("All tests pass");
        }
    }
}
