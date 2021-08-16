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
 * @bug 4671694
 * @summary Test to make sure link to superclass is generated for
 * each class in serialized form page.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestSuperClassInSerialForm
 */

import javadoc.tester.JavadocTester;

public class TestSuperClassInSerialForm extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestSuperClassInSerialForm tester = new TestSuperClassInSerialForm();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "--no-platform-links",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutput("serialized-form.html", true,
                """
                    <h3>Class&nbsp;<a href="pkg/SubClass.html" title="class in pkg">pkg.SubClass</a></h3>
                    <div class="type-signature">class SubClass extends <a href="pkg/SuperClass.html" tit\
                    le="class in pkg">SuperClass</a> implements java.io.Serializable</div>""");
    }
}
