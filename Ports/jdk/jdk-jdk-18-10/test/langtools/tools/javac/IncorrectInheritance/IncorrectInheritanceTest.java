/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8034924
 * @summary Incorrect inheritance of inaccessible static method
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main IncorrectInheritanceTest
 */

import toolbox.JavacTask;
import toolbox.ToolBox;

public class IncorrectInheritanceTest {
    private static final String ASrc =
            "package pkg;\n" +
            "\n" +
            "public class A {\n" +
            "    static void foo(Object o) {}\n" +
            "    private static void bar(Object o) {}\n" +
            "}";

    private static final String BSrc =
            "import pkg.A;\n" +
            "class B extends A {\n" +
            "    public void foo(Object o) {}\n" +
            "    public void bar(Object o) {}\n" +
            "}";

    private static final String CSrc =
            "class C extends B {\n" +
            "    public void m(Object o) {\n" +
            "        foo(o);\n" +
            "        bar(o);\n" +
            "    }\n" +
            "}";

    public static void main(String[] args) throws Exception {
        new IncorrectInheritanceTest().test();
    }

    public void test() throws Exception {
        ToolBox tb = new ToolBox();

        new JavacTask(tb)
                .sources(ASrc, BSrc, CSrc)
                .run();
    }

}
