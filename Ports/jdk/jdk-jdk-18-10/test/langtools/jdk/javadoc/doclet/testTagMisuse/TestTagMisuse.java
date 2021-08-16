/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @summary Determine if proper warning messages are printed.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @build TestTagMisuse
 * @run main TestTagMisuse
 */
import javadoc.tester.JavadocTester;

public class TestTagMisuse extends JavadocTester {

    /**
     * The entry point of the test.
     * @param args the array of command line arguments.
     * @throws Exception if the test fails
     */
    public static void main(String... args) throws Exception {
        TestTagMisuse tester = new TestTagMisuse();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-Xdoclint:none",
                "-d", "out",
                testSrc("TestTagMisuse.java"));
        checkExit(Exit.OK);

        checkOutput(Output.OUT, true,
                "warning: Tag @param cannot be used in field documentation.",
                "warning: Tag @throws cannot be used in field documentation.",
                "warning: Tag @return cannot be used in constructor documentation."
                /* DCerroneous, "warning: Tag @throws cannot be used in inline documentation."*/);
        checkOutput(Output.OUT, false, "DocletAbortException");
    }

    /**
     * {@throws blah}
     * Here is a bad field tag:
     * @throws foo
     * @param foo.
     */
    public int field;

    /**
     * Here is a bad constructor tag:
     * @return blah
     */
    public TestTagMisuse(){}

    /**
     * for @see and {@link}), and ThrowsTag (for @throws).
     */
    public void thisShouldNotCrash() {}
}
