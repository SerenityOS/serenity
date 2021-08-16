/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4764045 8004825 8026567 8191030 8204330 8239804
 * @summary  This test ensures that the value tag works in all
 *           use cases, the tests are explained below.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestValueTag
 */

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import javadoc.tester.JavadocTester;

public class TestValueTag extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestValueTag tester = new TestValueTag();
        tester.runTests();
    }

    @Test
    public void test1() {
        javadoc("-d", "out1",
                "-sourcepath", testSrc,
                "-tag", "todo",
                "pkg1", "pkg2");
        checkExit(Exit.ERROR);

        checkOutput("pkg1/Class1.html", true,
                // Base case:  using @value on a constant.
                "Result:  \"Test 1 passes\"",
                // Retrieve value of constant in same class.
                """
                    Result:  <a href="#TEST_2_PASSES">"Test 2 passes"</a>""",
                """
                    Result:  <a href="#TEST_3_PASSES">"Test 3 passes"</a>""",
                """
                    Result:  <a href="#TEST_4_PASSES">"Test 4 passes"</a>""",
                """
                    Result:  <a href="#TEST_5_PASSES">"Test 5 passes"</a>""",
                """
                    Result:  <a href="#TEST_6_PASSES">"Test 6 passes"</a>""");

        checkOutput("pkg1/Class2.html", true,
                // Retrieve value of constant in different class.
                """
                    Result:  <a href="Class1.html#TEST_7_PASSES">"Test 7 passes"</a>""",
                """
                    Result:  <a href="Class1.html#TEST_8_PASSES">"Test 8 passes"</a>""",
                """
                    Result:  <a href="Class1.html#TEST_9_PASSES">"Test 9 passes"</a>""",
                """
                    Result:  <a href="Class1.html#TEST_10_PASSES">"Test 10 passes"</a>""",
                """
                    Result:  <a href="Class1.html#TEST_11_PASSES">"Test 11 passes"</a>""",
                // Retrieve value of constant in different package
                """
                    Result:  <a href="../pkg2/Class3.html#TEST_12_PASSES">"Test 12 passes"</a>""",
                """
                    Result:  <a href="../pkg2/Class3.html#TEST_13_PASSES">"Test 13 passes"</a>""",
                """
                    Result:  <a href="../pkg2/Class3.html#TEST_14_PASSES">"Test 14 passes"</a>""",
                """
                    Result:  <a href="../pkg2/Class3.html#TEST_15_PASSES">"Test 15 passes"</a>""",
                """
                    Result:  <a href="../pkg2/Class3.html#TEST_16_PASSES">"Test 16 passes"</a>""");

        checkOutput("pkg2/package-summary.html", true,
                // Retrieve value of constant from a package page
                """
                    Result: <a href="Class3.html#TEST_17_PASSES">"Test 17 passes"</a>""");

        checkOutput("pkg1/CustomTagUsage.html", true,
                // Test @value tag used with custom tag.
                """
                    <dt>Todo:</dt>
                    <dd>the value of this constant is 55.</dd>""");

        checkOutput(Output.OUT, true,
                // Test @value errors printed due to invalid use or when used with
                // non-constant or with bad references.
                """
                    error: value does not refer to a constant
                         * Result:  {@value TEST_12_ERROR}""",
                """
                    error: {@value} not allowed here
                         * Result:  {@value}""",
                """
                    error: value does not refer to a constant
                         * Result:  {@value NULL}""",
                """
                    error: {@value} not allowed here
                         * Invalid (null): {@value}""",
                """
                    error: {@value} not allowed here
                         * Invalid (non-constant field): {@value}""",
                """
                    error: value does not refer to a constant
                         * Here is a bad value reference: {@value UnknownClass#unknownConstant}""",
                """
                    error: reference not found
                         * Here is a bad value reference: {@value UnknownClass#unknownConstant}""",
                """
                    error: {@value} not allowed here
                         * @todo the value of this constant is {@value}"""
        );

        checkOutput("pkg1/Class1.html", false,
                // Base case:  using @value on a constant.
                """
                    Result:  <a href="#TEST_12_ERROR">"Test 12 generates an error message"</a>""");

        checkForException();
    }

    @Test
    public void test2() {
        javadoc("-Xdoclint:none",
                "-d", "out2",
                "-sourcepath", testSrc,
                "-tag", "todo",
                "pkg1", "pkg2");
        checkExit(Exit.OK);
        checkOutput(Output.OUT, true,
                // Test @value warning printed when used with non-constant.
                "warning: @value tag (which references nonConstant) "
                + "can only be used in constants.",
                "warning: @value tag (which references NULL) "
                + "can only be used in constants.",
                "warning: @value tag (which references TEST_12_ERROR) "
                + "can only be used in constants.",
                // Test warning printed for bad reference.
                "warning: {@value UnknownClass#unknownConstant}"
                + " (referenced by @value tag) is an unknown reference."
        );
        checkForException();
    }

    @Test
    public void test3() {
        javadoc("-d", "out3",
                "-sourcepath", testSrc,
                "pkg2", "pkg3");
        checkExit(Exit.OK);

        checkOrder("pkg3/RT.html",
                """
                    The value is <a href="#CONSTANT">"constant"</a>.""",
                """
                    The value1 is <a href="#CONSTANT">"constant"</a>.""",
                """
                    The value2 is <a href="#CONSTANT">"constant"</a>.""",
                """
                    The value3 is <a href="../pkg2/Class3.html#TEST_12_PASSES">"Test 12 passes"</a>.""");
        checkForException();
    }

    @Test
    public void test4() throws IOException {
        Path base = Paths.get("test4");
        Path src = base.resolve("src");
        Files.createDirectories(src.resolve("p"));
        Files.write(src.resolve("p").resolve("C.java"), List.of(
                "package p;",
                "/** This class defines specialChars: {@value C#specialChars}. */",
                "public class C {",
                "    /** The value is {@value}. */",
                """
                    \s   public static final String specialChars = "abc < def & ghi > jkl";""",
                "}"));

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "p");
        checkExit(Exit.OK);
        checkOutput("p/C.html", false,
                """
                    The value is "abc < def & ghi > jkl".""");
        checkOutput("p/C.html", true,
                """
                    The value is "abc &lt; def &amp; ghi &gt; jkl".""");

        checkForException();
    }


    void checkForException() {
        checkOutput(Output.STDERR, false, "DocletAbortException");
        checkOutput(Output.STDERR, false, "IllegalArgumentException");
    }
}
