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
 * @bug 4506980
 * @summary Test to make sure that there is no difference in the output
 * when specifying packages on the command line and specifying individual
 * classes.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestCmndLineClass
 */

import javadoc.tester.JavadocTester;

public class TestCmndLineClass extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestCmndLineClass tester = new TestCmndLineClass();
        tester.runTests();
    }

    @Test
    public void test() {
        String outdir1 = "out-1";
        String outdir2 = "out-2";

        javadoc("-d", outdir1,
                "-sourcepath", testSrc,
                "-notimestamp",
                testSrc("C5.java"), "pkg1", "pkg2");
        checkExit(Exit.OK);

        javadoc("-d", outdir2,
                "-sourcepath", testSrc,
                "-notimestamp",
                testSrc("C5.java"),
                testSrc("pkg1/C1.java"),
                testSrc("pkg1/C2.java"),
                testSrc("pkg2/C3.java"),
                testSrc("pkg2/C4.java"));
        checkExit(Exit.OK);

        diff(outdir1, outdir2,
                "C5.html",
                "pkg1/C1.html",
                "pkg1/C2.html",
                "pkg2/C3.html",
                "pkg2/C4.html");
    }
}
