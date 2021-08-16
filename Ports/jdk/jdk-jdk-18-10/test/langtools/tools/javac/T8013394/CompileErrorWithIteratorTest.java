/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8013394
 * @summary compile of iterator use fails with error "defined in an inaccessible class or interface"
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main CompileErrorWithIteratorTest
 */

import toolbox.JavacTask;
import toolbox.ToolBox;

public class CompileErrorWithIteratorTest {

    private static final String TestCollectionSrc =
        "package pkg;\n" +

        "import java.util.Iterator;\n" +
        "import java.util.NoSuchElementException;\n" +

        "public class TestCollection<E> implements Iterable<E> {\n" +
        "    public testCollectionIterator iterator() {\n" +
        "        return  new testCollectionIterator();\n" +
        "    }\n" +
        "    class testCollectionIterator implements Iterator<E> {\n" +
        "        public boolean hasNext() { return true; }\n" +
        "        public E next() throws NoSuchElementException\n" +
        "        {\n" +
        "            return null;\n" +
        "        }\n" +
        "        public void remove() {}\n" +
        "    }\n" +
        "}";

    private static final String TestSrc =
        "import pkg.TestCollection;\n" +
        "\n" +
        "public class Test {\n" +
        "\n" +
        "    public static void main(String[] args) {\n" +
        "        TestCollection<String>  tc1 = new TestCollection<String>();\n" +
        "        for (String s : tc1) {\n" +
        "            System.out.println(s);\n" +
        "        }\n" +
        "      }\n" +
        "}";

    public static void main(String args[]) throws Exception {
        new CompileErrorWithIteratorTest().run();
    }

    ToolBox tb = new ToolBox();

    void run() throws Exception {
        compile();
    }

    void compile() throws Exception {
        new JavacTask(tb)
                .sources(TestCollectionSrc, TestSrc)
                .run();
    }

}
