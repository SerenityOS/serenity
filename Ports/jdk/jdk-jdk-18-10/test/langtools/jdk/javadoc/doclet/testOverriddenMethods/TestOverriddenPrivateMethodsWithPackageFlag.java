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
 * @bug 4634891 8025633 8026567 8182765
 * @summary Determine if overridden methods are properly documented when
 * -protected (default) visibility flag is used.
 * @library ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build javadoc.tester.*
 * @run main TestOverriddenPrivateMethodsWithPackageFlag
 */

import javadoc.tester.JavadocTester;

public class TestOverriddenPrivateMethodsWithPackageFlag extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestOverriddenPrivateMethodsWithPackageFlag tester = new TestOverriddenPrivateMethodsWithPackageFlag();
        tester.runTests();
    }

    @Test
    public void test() {
        javadoc("-d", "out",
                "-sourcepath", testSrc,
                "-package",
                "pkg1", "pkg2");
        checkExit(Exit.OK);

        // The public method should be overridden
        checkOutput("pkg1/SubClass.html", true,
                """
                    <dt>Overrides:</dt>
                    <dd><code><a href="BaseClass.html#publicMethod()">publicMethod</a></code>&nbsp;i\
                    n class&nbsp;<code><a href="BaseClass.html" title="class in pkg1">BaseClass</a><\
                    /code></dd>""");

        // The public method in different package should be overridden
        checkOutput("pkg2/SubClass.html", true,
                """
                    <dt>Overrides:</dt>
                    <dd><code><a href="../pkg1/BaseClass.html#publicMethod()">publicMethod</a></code\
                    >&nbsp;in class&nbsp;<code><a href="../pkg1/BaseClass.html" title="class in pkg1\
                    ">BaseClass</a></code></dd>""");

        // The package private method should be overridden since the base and sub class are in the same
        // package.
        checkOutput("pkg1/SubClass.html", true,
                """
                    <dt>Overrides:</dt>
                    <dd><code><a href="BaseClass.html#packagePrivateMethod()">packagePrivateMethod</\
                    a></code>&nbsp;in class&nbsp;<code><a href="BaseClass.html" title="class in pkg1\
                    ">BaseClass</a></code></dd>""");

        // The private method in should not be overridden
        checkOutput("pkg1/SubClass.html", false,
                """
                    <dt>Overrides:</dt>
                    <dd><code><a href="BaseClass.html#privateMethod--">""");

        // The private method in different package should not be overridden
        checkOutput("pkg2/SubClass.html", false,
                """
                    <dt><span class="overrideSpecifyLabel">Overrides:</span></dt>
                    <dd><code><a href="../pkg1/BaseClass.html#privateMethod--">""");

        // The package private method should not be overridden since the base and sub class are in
        // different packages.
        checkOutput("pkg2/SubClass.html", false,
                """
                    <dt><span class="overrideSpecifyLabel">Overrides:</span></dt>
                    <dd><code><a href="../pkg1/BaseClass.html#packagePrivateMethod--">""");
    }
}
