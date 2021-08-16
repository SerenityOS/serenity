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
 * @bug      4663254 8016328 8025633 8026567 8081854 8182765 8205593
 * @summary  Verify that spaces do not appear in hrefs and anchors.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestHref
 */

import javadoc.tester.JavadocTester;

public class TestHref extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestHref tester = new TestHref();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-Xdoclint:none",
                "-d", "out",
                "-source", "8",
                "-sourcepath", testSrc,
                "-linkoffline", "http://java.sun.com/j2se/1.4/docs/api/", testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/C1.html", true,
                //External link.
                """
                    href="http://java.sun.com/j2se/1.4/docs/api/java/lang/Object.html#wait(long,int)\"""",
                //Member summary table link.
                """
                    href="#method(int,int,java.util.ArrayList)\"""",
                //Anchor test.
                """
                    <section class="detail" id="method(int,int,java.util.ArrayList)">""");

        checkOutput("pkg/C2.html", true,
                //{@link} test.
                """
                    Link: <a href="C1.html#method(int,int,java.util.ArrayList)">""",
                //@see test.
                """
                    See Also:</dt>
                    <dd>
                    <ul class="see-list-long">
                    <li><a href="C1.html#method(int,int,java.util.ArrayList)">"""
        );

        checkOutput("pkg/C4.html", true,
                //Header does not link to the page itself.
                "Class C4&lt;E extends C4&lt;E&gt;&gt;</h1>",
                //Signature does not link to the page itself.
                """
                    <span class="modifiers">public abstract class </span><span class="element-name type-name\
                    -label">C4&lt;E extends C4&lt;E&gt;&gt;</span>"""
        );

        checkOutput(Output.OUT, false,
                "<a> tag is malformed");
    }

}
