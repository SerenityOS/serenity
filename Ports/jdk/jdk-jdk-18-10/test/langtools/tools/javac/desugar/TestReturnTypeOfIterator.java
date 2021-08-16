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
 * @bug 8232765
 * @summary NullPointerException at Types.eraseNotNeeded()
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main TestReturnTypeOfIterator
 */

import java.util.Arrays;
import java.util.List;

import toolbox.JavacTask;
import toolbox.ToolBox;
import toolbox.TestRunner;
import toolbox.Task;

public class TestReturnTypeOfIterator extends TestRunner {
    ToolBox tb;

    TestReturnTypeOfIterator() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        var t = new TestReturnTypeOfIterator();
        t.runTests();
    }

    @Test
    public void testVoidReturnTypeAtTopClass() throws Exception {
        String code = """
                public class T8232765 {
                    public void test(String[] args) {
                        MyLinkedList<Integer> list = new MyLinkedList<>();
                        for (int x : list)
                            System.out.print(x);
                    }
                }
                class MyLinkedList<T> implements java.lang.Iterable<T> {
                    public void iterator() {}
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "T8232765.java:4:9: compiler.err.foreach.not.applicable.to.type: " +
                        "MyLinkedList<java.lang.Integer>, (compiler.misc.type.req.array.or.iterable)",
                "T8232765.java:8:1: compiler.err.does.not.override.abstract: MyLinkedList, iterator(), java.lang.Iterable",
                "T8232765.java:9:17: compiler.err.override.incompatible.ret: (compiler.misc.cant.implement: iterator()," +
                        " MyLinkedList, iterator(), java.lang.Iterable), void, java.util.Iterator<T>",
                "3 errors");
        tb.checkEqual(expected, output);
    }

    @Test
    public void testVoidReturnTypeAtMemberClass() throws Exception {
        String code = """
                public class T8232765 {
                    public void test(String[] args) {
                        MyLinkedList<Integer> list = new MyLinkedList<>();
                        for (int x : list)
                            System.out.print(x);
                    }
                    class MyLinkedList<T> implements java.lang.Iterable<T> {
                        public void iterator() {}
                    }
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "T8232765.java:4:9: compiler.err.foreach.not.applicable.to.type: " +
                        "T8232765.MyLinkedList<java.lang.Integer>, (compiler.misc.type.req.array.or.iterable)",
                "T8232765.java:7:5: compiler.err.does.not.override.abstract: T8232765.MyLinkedList, iterator(), java.lang.Iterable",
                "T8232765.java:8:21: compiler.err.override.incompatible.ret: (compiler.misc.cant.implement: iterator()," +
                        " T8232765.MyLinkedList, iterator(), java.lang.Iterable), void, java.util.Iterator<T>",
                "3 errors");
        tb.checkEqual(expected, output);
    }

    @Test
    public void testNonOverride() throws Exception {
        String code = """
                import java.util.Iterator;
                public class T8232765 {
                    public void test(String[] args) {
                        MyLinkedList<Integer> list = new MyLinkedList<>();
                        for (int x : list)
                            System.out.print(x);
                    }
                }
                class MyLinkedList<T> implements java.lang.Iterable<T> {
                    public Iterator<T> iterator(int a) {return null;}
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "T8232765.java:9:1: compiler.err.does.not.override.abstract: MyLinkedList, iterator(), java.lang.Iterable",
                "1 error");
        tb.checkEqual(expected, output);
    }

    @Test
    public void testNonOverrideAndVoidReturnType() throws Exception {
        String code = """
                public class T8232765 {
                    public void test(String[] args) {
                        MyLinkedList<Integer> list = new MyLinkedList<>();
                        for (int x : list)
                            System.out.print(x);
                    }
                }
                class MyLinkedList<T> implements java.lang.Iterable<T> {
                    public void iterator(int a) { }
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "T8232765.java:8:1: compiler.err.does.not.override.abstract: MyLinkedList, iterator(), java.lang.Iterable",
                "1 error");
        tb.checkEqual(expected, output);
    }

    @Test
    public void testOverloadAndVoidReturnType() throws Exception {
        String code = """
                public class T8232765 {
                    public void test(String[] args) {
                        MyLinkedList<Integer> list = new MyLinkedList<>();
                        for (int x : list)
                            System.out.print(x);
                    }
                }
                class MyLinkedList<T> implements java.lang.Iterable<T> {
                    public void iterator() { }
                    public void iterator(int a) { }
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "T8232765.java:4:9: compiler.err.foreach.not.applicable.to.type: MyLinkedList<java.lang.Integer>, " +
                        "(compiler.misc.type.req.array.or.iterable)",
                "T8232765.java:8:1: compiler.err.does.not.override.abstract: MyLinkedList, iterator(), java.lang.Iterable",
                "T8232765.java:9:17: compiler.err.override.incompatible.ret: (compiler.misc.cant.implement: iterator(), " +
                        "MyLinkedList, iterator(), java.lang.Iterable), void, java.util.Iterator<T>",
                "3 errors");
        tb.checkEqual(expected, output);
    }

    @Test
    public void testOverload() throws Exception {
        String code = """
                import java.util.Iterator;
                public class T8232765 {
                    public void test(String[] args) {
                        MyLinkedList<Integer> list = new MyLinkedList<>();
                        for (int x : list)
                            System.out.print(x);
                    }
                }
                class MyLinkedList<T> implements java.lang.Iterable<T> {
                    public Iterator<T> iterator() {return null;}
                    public Iterator<T> iterator(int a) {return null;}
                }""";
        new JavacTask(tb)
                .sources(code)
                .options("-XDrawDiagnostics")
                .run(Task.Expect.SUCCESS);
    }

    @Test
    public void testPrivate() throws Exception {
        String code = """
                import java.util.Iterator;
                public class T8232765 {
                    public void test(String[] args) {
                        MyLinkedList<Integer> list = new MyLinkedList<>();
                        for (int x : list)
                            System.out.print(x);
                    }
                }
                class MyLinkedList<T> implements java.lang.Iterable<T> {
                    private Iterator<T> iterator() {return null;}
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "T8232765.java:5:9: compiler.err.report.access: iterator(), private, MyLinkedList",
                "T8232765.java:10:25: compiler.err.override.weaker.access: (compiler.misc.cant.implement: " +
                        "iterator(), MyLinkedList, iterator(), java.lang.Iterable), public",
                "2 errors");
        tb.checkEqual(expected, output);
    }

    @Test
    public void testPrivateAndOverload() throws Exception {
        String code = """
                import java.util.Iterator;
                public class T8232765 {
                    public void test(String[] args) {
                        MyLinkedList<Integer> list = new MyLinkedList<>();
                        for (int x : list)
                            System.out.print(x);
                    }
                }
                class MyLinkedList<T> implements java.lang.Iterable<T> {
                    private Iterator<T> iterator() {return null;}
                    public Iterator<T> iterator(int a) {return null;}
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "T8232765.java:5:9: compiler.err.cant.apply.symbols: kindname.method, iterator, compiler.misc.no.args," +
                        "{(compiler.misc.inapplicable.method: kindname.method, MyLinkedList, iterator(), " +
                        "(compiler.misc.report.access: iterator(), private, MyLinkedList))," +
                        "(compiler.misc.inapplicable.method: kindname.method, MyLinkedList, iterator(int), " +
                        "(compiler.misc.arg.length.mismatch))}",
                "1 error",
                "Fatal Error: Unable to find method iterator");
        tb.checkEqual(expected, output);
    }

    @Test
    public void testPrivateAndOverloadAndVoidReturnType() throws Exception {
        String code = """
                public class T8232765 {
                    public void test(String[] args) {
                        MyLinkedList<Integer> list = new MyLinkedList<>();
                        for (int x : list)
                            System.out.print(x);
                    }
                }
                class MyLinkedList<T> implements java.lang.Iterable<T> {
                    private void iterator() { }
                    public void iterator(int a) { }
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "T8232765.java:8:1: compiler.err.does.not.override.abstract: MyLinkedList, iterator(), java.lang.Iterable",
                "T8232765.java:9:18: compiler.err.override.weaker.access: (compiler.misc.cant.implement: iterator(), " +
                        "MyLinkedList, iterator(), java.lang.Iterable), public",
                "2 errors");
        tb.checkEqual(expected, output);
    }
}
