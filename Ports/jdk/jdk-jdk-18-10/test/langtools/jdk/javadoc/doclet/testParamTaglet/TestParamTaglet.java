/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4802275 4967243 8026567 8239804
 * @summary  Make sure param tags are still printed even though they do not
 *           match up with a real parameters.
 *           Make sure inheritDoc cannot be used in an invalid param tag.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestParamTaglet
 */

import javadoc.tester.JavadocTester;

public class TestParamTaglet extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestParamTaglet tester = new TestParamTaglet();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "pkg");
        checkExit(Exit.ERROR);

        checkOutput("pkg/C.html", true,
                //Regular param tags.
                """
                    <dt>Parameters:</dt>
                    <dd><code>param1</code> - testing 1 2 3.</dd>
                    <dd><code>param2</code> - testing 1 2 3.""",
                //Param tags that don't match with any real parameters.
                """
                    <dt>Parameters:</dt>
                    <dd><code>p1</code> - testing 1 2 3.</dd>
                    <dd><code>p2</code> - testing 1 2 3.""",
                //{@inherit} doc misuse does not cause doclet to throw exception.
                // Param is printed with nothing inherited.
                //XXX: in the future when Configuration is available during doc inheritence,
                //print a warning for this mistake.
                "<code>inheritBug</code> -");
    }
}
