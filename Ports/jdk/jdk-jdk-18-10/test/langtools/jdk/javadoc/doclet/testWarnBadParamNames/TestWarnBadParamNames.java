/*
 * Copyright (c) 2002, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4693440
 * @summary Test to make sure that warning is printed when bad parameter
 * name is used with param.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestWarnBadParamNames
 */

import javadoc.tester.JavadocTester;

public class TestWarnBadParamNames extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestWarnBadParamNames tester = new TestWarnBadParamNames();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-Xdoclint:none",
                "-d", "out",
                testSrc("C.java"));
        checkExit(Exit.OK);

        checkOutput(Output.OUT, true,
                """
                    warning: @param argument "int" is not a parameter name.""",
                """
                    warning: @param argument "IDontExist" is not a parameter name.""",
                """
                    warning: Parameter "arg" is documented more than once.""");
    }
}
