/*
 * Copyright (c) 2004, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      4732864 6280605 7064544 8014636 8016328 8025633 8071982 8182765
 * @summary  Make sure that you can link from one member to another using
 *           non-qualified name, furthermore, ensure the right one is linked.
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestLinkTaglet
 */

import javadoc.tester.JavadocTester;

public class TestLinkTaglet extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestLinkTaglet tester = new TestLinkTaglet();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-Xdoclint:none",
                "-d", "out",
                "-sourcepath", testSrc,
                "pkg", testSrc("checkPkg/B.java"));
        checkExit(Exit.OK);

        checkOutput("pkg/C.html", true,
                """
                    Qualified Link: <a href="C.InnerC.html" title="class in pkg"><code>C.InnerC</code></a>.<br/>
                     Unqualified Link1: <a href="C.InnerC.html" title="class in pkg"><code>C.InnerC</code></a>.<br/>
                     Unqualified Link2: <a href="C.InnerC.html" title="class in pkg"><code>C.InnerC</code></a>.<br/>
                     Qualified Link: <a href="#method(pkg.C.InnerC,pkg.C.InnerC2)"><code>method(pkg.\
                    C.InnerC, pkg.C.InnerC2)</code></a>.<br/>
                     Unqualified Link: <a href="#method(pkg.C.InnerC,pkg.C.InnerC2)"><code>method(C.InnerC, C.InnerC2)</code></a>.<br/>
                     Unqualified Link: <a href="#method(pkg.C.InnerC,pkg.C.InnerC2)"><code>method(InnerC, InnerC2)</code></a>.<br/>
                     Package Link: <a href="package-summary.html"><code>pkg</code></a>.<br/>""");

        checkOutput("pkg/C.InnerC.html", true,
                """
                    Link to member in outer class: <a href="C.html#MEMBER"><code>C.MEMBER</code></a> <br/>
                     Link to member in inner class: <a href="C.InnerC2.html#MEMBER2"><code>C.InnerC2.MEMBER2</code></a> <br/>
                     Link to another inner class: <a href="C.InnerC2.html" title="class in pkg"><code>C.InnerC2</code></a>""");

        checkOutput("pkg/C.InnerC2.html", true,
                """
                    <dl class="notes">
                    <dt>Enclosing class:</dt>
                    <dd><a href="C.html" title="class in pkg">C</a></dd>
                    </dl>""");

        checkOutput(Output.OUT, false,
                "Tag @see: reference not found: A");

        checkFiles(false, "checkPkg/A.html");
    }
}
