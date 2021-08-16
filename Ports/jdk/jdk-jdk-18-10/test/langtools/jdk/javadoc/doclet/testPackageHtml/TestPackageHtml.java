/*
 * Copyright (c) 2016, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug      8150096 8179704
 * @summary  test package.html handling
 * @library  ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build    javadoc.tester.*
 * @run main TestPackageHtml
 */

import javadoc.tester.JavadocTester;

public class TestPackageHtml extends JavadocTester {
    public static void main(String... args) throws Exception  {
        TestPackageHtml tester = new TestPackageHtml();
        tester.runTests();
    }

    // Make sure package.html is recognized by doclint
    @Test
    public void testPackageHtml() {
        javadoc("-d", "out-pkg-html-1",
                "-sourcepath", testSrc,
                "pkg1");
        checkExit(Exit.ERROR);
        checkOutput(Output.OUT, true, "package.html:4: error: malformed HTML");
    }

    // Doclet must handle empty body in package.html, must
    // ignore html comment in the first sentence and must
    // ignore trailing whitespace in a first sentence.
    @Test
    public void testPackageHtmlWithEmptyBody() {
        javadoc("-d", "out-pkg-html-2",
                "-sourcepath", testSrc,
                "pkg2", "pkg3", "pkg4");
        checkExit(Exit.OK);
        checkOutput("index-all.html", true,
              """
                  <dl class="index">
                  <dt><a href="pkg2/package-summary.html">pkg2</a> - package pkg2</dt>
                  <dt><a href="pkg3/package-summary.html">pkg3</a> - package pkg3</dt>
                  <dd>
                  <div class="block">This is a documentation for <a href="pkg3/package-summary.html"><code>pkg3</code></a></div>
                  </dd>
                  <dt><a href="pkg4/package-summary.html">pkg4</a> - package pkg4</dt>
                  <dd>
                  <div class="block">This is a documentation for <a href="pkg4/package-summary.html"><code>pkg4</code></a></div>
                  </dd>
                  </dl>
                  """);
    }
}
