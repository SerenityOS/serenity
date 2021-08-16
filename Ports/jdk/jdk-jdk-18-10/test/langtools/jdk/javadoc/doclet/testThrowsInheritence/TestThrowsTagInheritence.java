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
 * @bug 4684827 4633969
 * @summary This test verifies that throws tags in implementing class
 * override the throws tags in interface. This test also verifies that throws tags are inherited properly
 * the case where the name of one exception is not fully qualified.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestThrowsTagInheritence
 */

// TODO: should be TestThrowsInheritance!
import javadoc.tester.JavadocTester;

public class TestThrowsTagInheritence extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestThrowsTagInheritence tester = new TestThrowsTagInheritence();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                testSrc("C.java"),
                testSrc("I.java"),
                testSrc("Foo.java"),
                testSrc("Iface.java"));
        checkExit(Exit.OK);

        // The class should not inherit the tag from the interface.
        checkOutput("Foo.html", true,
                "Test 1 passes.");

        //The class should not inherit the tag from the interface.
        checkOutput("C.html", false,
                "Test 1 fails.");
    }

    @Test
    public void test1() {
        javadoc("-d", "out-1",
                "-sourcepath", testSrc,
                "-package", "pkg");
        checkExit(Exit.OK);

        // The method should not inherit the IOOBE throws tag from the abstract class,
        // for now keep keep this bug compatible, should fix this correctly in
        // the future.
        checkOutput("pkg/Extender.html", false, "java.lang.IndexOutOfBoundsException");
    }
}
