/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8168836
 * @summary Basic argument validation for --add-exports
 * @library /test/lib
 * @modules jdk.compiler
 * @build jdk.test.lib.compiler.ModuleInfoMaker
 *        jdk.test.lib.compiler.CompilerUtils
 * @run testng AddExportsTestWarningError
 */

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

import jdk.test.lib.compiler.ModuleInfoMaker;
import jdk.test.lib.process.OutputAnalyzer;
import static jdk.test.lib.process.ProcessTools.*;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.*;

@Test
public class AddExportsTestWarningError {

    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path SRC_DIR = Paths.get("src");
    private static final String M1_MAIN = "m1/p1.C1";
    private static final String M3_MAIN = "m3/p3.C3";

    @BeforeTest
    public void setup() throws Exception {
        ModuleInfoMaker builder = new ModuleInfoMaker(SRC_DIR);
        builder.writeJavaFiles("m1",
            "module m1 { }",
            "package p1; public class C1 { " +
                "    public static void main(String... args) {}" +
                "}");

        builder.writeJavaFiles("m2",
            "module m2 { requires m1; exports p2; }",
            "package p2; public class C2 {  private p1.C1 c1; }");

        builder.writeJavaFiles("m3",
            "module m3 { requires m2; }",
            "package p3; class C3 { " +
                "    p1.C1 c; " +
                "    public static void main(String... args) { new p2.C2(); }" +
                "}");

        builder.compile("m1", MODS_DIR);
        builder.compile("m2", MODS_DIR, "--add-exports", "m1/p1=m2");
        builder.compile("m3", MODS_DIR, "--add-exports", "m1/p1=m3", "--add-reads", "m3=m1");
    }


    @DataProvider(name = "goodcases")
    public Object[][] goodCases() {
        return new Object[][]{

            // empty items
            { "m1/p1=,m2,m3",       null },
            { "m1/p1=m2,,m3",       null },
            { "m1/p1=m2,m3,",       null },

            // duplicates
            { "m1/p1=m2,m2,m3,,",   null },

        };
    }


    @Test(dataProvider = "goodcases")
    public void test(String value, String ignore) throws Exception {
        testNoWarning(value);
    }


    @DataProvider(name = "illFormedAddExports")
    public Object[][] illFormedAddExports() {
        return new Object[][]{
            { "m1",         "Unable to parse --add-exports <module>=<value>: m1"},

            // missing source part
            { "=m2",        "Unable to parse --add-exports <module>=<value>: =m2"},
            { "/=m2",       "Unable to parse --add-exports <module>/<package>: /" },
            { "m1=m2",      "Unable to parse --add-exports <module>/<package>: m1" },
            { "/p1=m2",     "Unable to parse --add-exports <module>/<package>: /p1" },
            { "m1p1=m2",    "Unable to parse --add-exports <module>/<package>: m1p1" },

            // empty list, missing target
            { "m1/p1=",     "Unable to parse --add-exports <module>=<value>: m1/p1=" },
            { "m1/p1=,,",   "Target must be specified: --add-exports m1/p1=,," },
        };
    }

    @Test(dataProvider = "illFormedAddExports")
    public void testIllFormedAddExports(String value, String msg) throws Exception {
        testError(value, msg);
    }


    @DataProvider(name = "unknownNames")
    public Object[][] unknownNames() {
        return new Object[][]{

            // source not found
            {"DoesNotExist/p=m1",  "WARNING: Unknown module: DoesNotExist specified to --add-exports"},
            {"m1/DoesNotExist=m2", "WARNING: package DoesNotExist not in m1"},

            // target not found
            {"m1/p1=DoesNotExist", "WARNING: Unknown module: DoesNotExist specified to --add-exports"},

            // bad names
            {"m*/p1=m2",           "WARNING: Unknown module: m* specified to --add-exports"},
            {"m1/p!=m2",           "WARNING: package p! not in m1"},
            {"m1/p1=m!",           "WARNING: Unknown module: m! specified to --add-exports"},

        };
    }


    @Test(dataProvider = "unknownNames")
    public void testUnknownNames(String value, String msg) throws Exception {
        testWarning(value, msg);
    }


    @DataProvider(name = "missingArguments")
    public Object[][] missingArguments() {
        return new Object[][]{
            { new String[] { "--add-exports" },
                "Error: --add-exports requires modules to be specified"},

            { new String[] { "--add-exports=" },
                "Error: --add-exports= requires modules to be specified" },

            { new String[] { "--add-exports", "" },
                "Error: --add-exports requires modules to be specified"}

        };
    }


    @Test(dataProvider = "missingArguments")
    public void testMissingArguments(String[] options, String msg) throws Exception {
        String[] args = Stream.concat(Arrays.stream(options),
                                      Stream.of("-version"))
                              .toArray(String[]::new);
        int exitValue = executeTestJava(args)
            .outputTo(System.out)
            .errorTo(System.out)
            .shouldContain(msg)
            .getExitValue();

        assertTrue(exitValue != 0);
    }

     private void testWarning(String value, String msg) throws Exception {
        int exitValue =
            executeTestJava("--add-exports", value,
                            "--module-path", MODS_DIR.toString(),
                            "-m", M1_MAIN)
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldContain(msg)
                .getExitValue();

        assertTrue(exitValue == 0);
    }

    private void testError(String value, String msg) throws Exception {
        int exitValue =
            executeTestJava("--add-exports", value,
                            "--module-path", MODS_DIR.toString(),
                            "-m", M1_MAIN)
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldContain(msg)
                .getExitValue();

        assertTrue(exitValue != 0);
    }

    private void testNoWarning(String value) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(new BufferedOutputStream(baos));
        OutputAnalyzer outputAnalyzer =
            executeTestJava("--add-exports", value,
                            "--module-path", MODS_DIR.toString(),
                            "-m", M3_MAIN)
                .outputTo(ps)
                .errorTo(ps);

        assertTrue(outputAnalyzer.getExitValue() == 0);

        System.out.println(baos.toString());
        String[] output = baos.toString().split("\\R");
        assertFalse(Arrays.stream(output)
                          .filter(s -> !s.matches("WARNING: Module name .* may soon be illegal"))
                          .filter(s -> s.startsWith("WARNING:"))
                          .findAny().isPresent());

    }
}
