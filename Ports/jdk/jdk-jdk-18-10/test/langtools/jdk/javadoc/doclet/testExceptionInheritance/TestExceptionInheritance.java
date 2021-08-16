/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8157682
 * @summary at-inheritDoc doesn't work with at-exception
 * @library /tools/lib ../../lib
 * @modules jdk.javadoc/jdk.javadoc.internal.tool
 * @build toolbox.ToolBox javadoc.tester.*
 * @run main TestExceptionInheritance
 */

import java.nio.file.Path;

import javadoc.tester.JavadocTester;

import toolbox.ToolBox;

public class TestExceptionInheritance extends JavadocTester {

    public static void main(String... args) throws Exception {
        TestExceptionInheritance tester = new TestExceptionInheritance();
        tester.runTests(m -> new Object[] { Path.of(m.getName()) });
    }

    ToolBox tb = new ToolBox();

    @Test
    public void testExceptionException(Path base) throws Exception {
        test(base, "exception", "exception");
    }

    @Test
    public void testExceptionThrows(Path base) throws Exception {
        test(base, "exception", "throws");
    }

    @Test
    public void testThrowsException(Path base) throws Exception {
        test(base, "throws", "exception");
    }

    @Test
    public void testThrowsThrows(Path base) throws Exception {
        test(base, "throws", "throws");
    }

    void test(Path base, String a, String b) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                """
                    package p;
                    public class A {
                        /**
                         * @param x a number
                         * @##A## NullPointerException if x is null
                         * @##A## IllegalArgumentException if {@code x < 0}
                         */
                        public void m(Integer x) { }
                    }
                    """.replace("##A##", a),
                """
                    package p;
                    public class A_Sub extends A {
                        /**
                         * @param x {@inheritDoc}
                         * @##B## NullPointerException {@inheritDoc}
                         * @##B## IllegalArgumentException {@inheritDoc}
                         */
                        @Override
                        public void m(Integer x) { }
                    }
                    """.replace("##B##", b)
                );

        javadoc("-d", base.resolve("out").toString(),
                "-sourcepath", src.toString(),
                "--no-platform-links",
                "p");
        checkExit(Exit.OK);

        checkOutput("p/A_Sub.html", true,
                "<code>java.lang.NullPointerException</code> - if x is null");

        checkOutput("p/A_Sub.html", true,
                "<code>java.lang.IllegalArgumentException</code> - if <code>x &lt; 0</code>");
    }
}
