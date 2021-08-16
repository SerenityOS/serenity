/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4651598 8026567 8239804
 * @summary Javadoc wrongly inserts </DD> tags when using multiple @author tags
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main AuthorDD
 */

/**
 * Runs javadoc and runs regression tests on the resulting HTML.
 */
import javadoc.tester.JavadocTester;

public class AuthorDD extends JavadocTester {

    public static void main(String... args) throws Exception {
        AuthorDD tester = new AuthorDD();
        tester.runTests();
    }

    @Test
    public void test() {
        // Test for all cases except the split index page
        javadoc("-d", "out",
                "-author",
                "-version",
                "-sourcepath", testSrc,
                "p1");
        checkExit(Exit.OK);

        checkOutput("p1/C1.html", true,
                "<dl class=\"notes\">",
                // Test single @since tag:
                "<dt>Since:</dt>\n"
                + "<dd>JDK 1.0</dd>",
                // Test multiple @author tags:
                """
                    <dt>Author:</dt>
                    <dd>Alice, Bob, Eve</dd>""");
    }
}
