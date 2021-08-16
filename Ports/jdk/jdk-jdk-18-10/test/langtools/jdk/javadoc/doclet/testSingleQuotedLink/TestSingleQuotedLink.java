/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6457406
 * @summary Verify that a link in single quotes copied to the class-use page as is.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestSingleQuotedLink
 */
import javadoc.tester.JavadocTester;

public class TestSingleQuotedLink extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestSingleQuotedLink tester = new TestSingleQuotedLink();
        tester.runTests();
    }

    @Test
    public void run() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "-use",
                "pkg1");
        checkExit(Exit.OK);

        // We are testing the redirection algorithm with a known scenario when a
        // writer is not forced to ignore it: "-use".

        checkOutput("pkg1/class-use/C1.html", true,
            "<a href=\'http://download.oracle.com/javase/8/docs/technotes/guides/indexC2.html\'>");

        checkOutput("pkg1/class-use/C1.html", false,
            "pkg1/\'http://download.oracle.com/javase/8/docs/technotes/guides/indexC2.html\'>");
    }
}
