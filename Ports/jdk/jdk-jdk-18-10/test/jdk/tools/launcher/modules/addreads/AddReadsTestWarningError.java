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
 * @summary  Basic argument validation for --add-reads
 * @library /test/lib
 * @modules jdk.compiler
 * @build jdk.test.lib.compiler.ModuleInfoMaker
 * @build jdk.test.lib.compiler.CompilerUtils
 * @build AddReadsTestWarningError
 * @run testng AddReadsTestWarningError
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
public class AddReadsTestWarningError {

    private static final Path MODS_DIR = Paths.get("mods");
    private static final Path SRC_DIR = Paths.get("src");
    private static final String M1_MAIN = "m1/p1.C1";
    private static final String M4_MAIN = "m4/p4.C4";

    @BeforeTest
    public void setup() throws Exception {
        ModuleInfoMaker builder = new ModuleInfoMaker(SRC_DIR);
        builder.writeJavaFiles("m1",
            "module m1 { requires m4; }",
            "package p1; public class C1 { " +
            "    public static void main(String... args) {" +
            "        p2.C2 c2 = new p2.C2();" +
            "        p3.C3 c3 = new p3.C3();" +
            "    }" +
            "}"
        );

        builder.writeJavaFiles("m2",
            "module m2 { exports p2; }",
            "package p2; public class C2 { }"
        );

        builder.writeJavaFiles("m3",
            "module m3 { exports p3; }",
            "package p3; public class C3 { }"
        );

        builder.writeJavaFiles("m4",
            "module m4 { requires m2; requires m3; }",
            "package p4; public class C4 { " +
            "    public static void main(String... args) {}" +
            "}"
        );

        builder.compile("m2", MODS_DIR);
        builder.compile("m3", MODS_DIR);
        builder.compile("m4", MODS_DIR);
        builder.compile("m1", MODS_DIR, "--add-reads", "m1=m2,m3");
    }


    @DataProvider(name = "goodcases")
    public Object[][] goodCases() {
        return new Object[][]{
            // empty items
            { "m1=,m2,m3",       null },
            { "m1=m2,,m3",       null },
            { "m1=m2,m3,",       null },

            // duplicates
            { "m1=m2,m2,m3,,",  null },

        };
    }


    @Test(dataProvider = "goodcases")
    public void test(String value, String ignore) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(new BufferedOutputStream(baos));
        OutputAnalyzer outputAnalyzer =
            executeTestJava("--add-reads", value,
                            "--module-path", MODS_DIR.toString(),
                            "-m", M1_MAIN)
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


    @DataProvider(name = "illFormedAddReads")
    public Object[][] illFormedAddReads() {
        return new Object[][]{
            { "m1",         "Unable to parse --add-reads <module>=<value>: m1" },

            // missing source part
            { "=m2",        "Unable to parse --add-reads <module>=<value>: =m2" },

            // empty list, missing target
            { "m1=",        "Unable to parse --add-reads <module>=<value>: m1=" },

            // empty list
            { "m1=,,",      "Target must be specified: --add-reads m1=,," },
        };
    }


    @Test(dataProvider = "illFormedAddReads")
    public void testIllFormedAddReads(String value, String msg) throws Exception {
        int exitValue =
            executeTestJava("--add-reads", value,
                            "--module-path", MODS_DIR.toString(),
                            "-m", M4_MAIN)
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldContain(msg)
                .getExitValue();

        assertTrue(exitValue != 0);
    }


    @DataProvider(name = "unknownNames")
    public Object[][] unknownNames() {
        return new Object[][]{

            // source not found
            {"DoesNotExist=m2",    "WARNING: Unknown module: DoesNotExist specified to --add-reads"},

            // target not found
            {"m2=DoesNotExist",    "WARNING: Unknown module: DoesNotExist specified to --add-reads"},

            // bad names
            {"m*=m2",              "WARNING: Unknown module: m* specified to --add-reads"},
            {"m2=m!",              "WARNING: Unknown module: m! specified to --add-reads"},

        };
    }

    @Test(dataProvider = "unknownNames")
    public void testUnknownNames(String value, String msg) throws Exception {
        int exitValue =
            executeTestJava("--add-reads", value,
                            "--module-path", MODS_DIR.toString(),
                            "-m", M4_MAIN)
                .outputTo(System.out)
                .errorTo(System.out)
                .shouldContain(msg)
                .getExitValue();

        assertTrue(exitValue == 0);
    }


    @DataProvider(name = "missingArguments")
    public Object[][] missingArguments() {
        return new Object[][]{
            { new String[] {"--add-reads" },
                "Error: --add-reads requires modules to be specified"},

            { new String[] { "--add-reads=" },
                "Error: --add-reads= requires modules to be specified"},

            { new String[] { "--add-reads", "" },
                "Error: --add-reads requires modules to be specified"},
        };
    }

    @Test(dataProvider = "missingArguments")
    public void testEmptyArgument(String[] options, String msg) throws Exception {
        String[] args = Stream.concat(Arrays.stream(options), Stream.of("-version"))
                              .toArray(String[]::new);
        int exitValue = executeTestJava(args)
            .outputTo(System.out)
            .errorTo(System.out)
            .shouldContain(msg)
            .getExitValue();

        assertTrue(exitValue != 0);
    }
}
