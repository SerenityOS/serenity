/*
 * Copyright (c) 2002, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4525364
 * @summary Determine if duplicate throws tags can be used.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestDupThrowsTags
 */
import javadoc.tester.JavadocTester;

public class TestDupThrowsTags extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestDupThrowsTags tester = new TestDupThrowsTags();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                testSrc("TestDupThrowsTags.java"));
        checkExit(Exit.ERROR);

        checkOutput("TestDupThrowsTags.html", true,
                "Test 1 passes",
                "Test 2 passes",
                "Test 3 passes",
                "Test 4 passes");
    }

    /**
     * @throws java.io.IOException Test 1 passes
     * @throws java.io.IOException Test 2 passes
     * @throws java.lang.NullPointerException Test 3 passes
     * @throws java.io.IOException Test 4 passes
     */
    public void method() {}

}
