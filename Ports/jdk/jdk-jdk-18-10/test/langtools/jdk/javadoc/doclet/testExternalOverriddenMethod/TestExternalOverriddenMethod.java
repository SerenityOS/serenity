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
 * @bug 4857717 8025633 8026567 8164407 8182765 8205593
 * @summary Test to make sure that externally overridden and implemented methods
 * are documented properly.  The method should still include "implements" or
 * "overrides" documentation even though the method is external.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.* TestExternalOverriddenMethod
 * @run main TestExternalOverriddenMethod
 */
import javadoc.tester.JavadocTester;

public class TestExternalOverriddenMethod extends JavadocTester {

    static final String uri = "http://java.sun.com/j2se/1.4.1/docs/api";

    public static void main(String... args) throws Exception {
        TestExternalOverriddenMethod tester = new TestExternalOverriddenMethod();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-source","8",
                "-sourcepath", testSrc,
                "-linkoffline", uri, testSrc,
                "pkg");
        checkExit(Exit.OK);

        checkOutput("pkg/XReader.html", true,
                """
                    <dt>Overrides:</dt>
                    <dd><code><a href=\"""" + uri + """
                    /java/io/FilterReader.html#read()" title="class or interface in java.io" class="\
                    external-link">read</a></code>&nbsp;in class&nbsp;<code><a href=\"""" + uri + """
                    /java/io/FilterReader.html" title="class or interface in java.io" class="external-link">FilterReader</a></code></dd>""",
                """
                    <dt>Specified by:</dt>
                    <dd><code><a href=\"""" + uri + """
                    /java/io/DataInput.html#readInt()" title="class or interface in java.io" class="\
                    external-link">readInt</a></code>&nbsp;in interface&nbsp;<code><a href=\"""" + uri + """
                    /java/io/DataInput.html" title="class or interface in java.io" class="external-link">DataInput</a></code></dd>"""
        );
    }
}
