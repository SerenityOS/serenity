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

/**
 * @test
 * @bug 8259359
 * @summary javac does not attribute unexpected super constructor invocation qualifier, and may crash
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.api
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main T8259359
 */

import java.util.List;
import java.util.Arrays;

import toolbox.ToolBox;
import toolbox.TestRunner;
import toolbox.JavacTask;
import toolbox.Task;

public class T8259359 extends TestRunner {
    ToolBox tb;

    public T8259359() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        new T8259359().runTests();
    }

    @Test
    public void testSuperConstructorCallInErrorClass() throws Exception {
        String code = """
                public class SuperConstructorCallInErrorClass extends Undefined1 {
                     public SuperConstructorCallInErrorClass(int i) {
                         new Undefined2() { public void test(int i) { Undefined3 u; } }.super();
                     }
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDshould-stop.at=FLOW", "-XDdev", "-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "SuperConstructorCallInErrorClass.java:1:55: compiler.err.cant.resolve: kindname.class, Undefined1, , ",
                "SuperConstructorCallInErrorClass.java:3:14: compiler.err.cant.resolve.location: kindname.class, " +
                        "Undefined2, , , (compiler.misc.location: kindname.class, SuperConstructorCallInErrorClass, null)",
                "SuperConstructorCallInErrorClass.java:3:55: compiler.err.cant.resolve: kindname.class, Undefined3, , ",
                "3 errors");
        tb.checkEqual(expected, output);
    }

    @Test
    public void testSuperConstructorCallInNormalClass() throws Exception {
        String code = """
                public class SuperConstructorCallInNormalClass {
                     public SuperConstructorCallInNormalClass(int i) {
                         new Undefined2() { public void test(int i) { Undefined3 u; } }.super();
                     }
                }""";
        List<String> output = new JavacTask(tb)
                .sources(code)
                .options("-XDshould-stop.at=FLOW", "-XDdev", "-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "SuperConstructorCallInNormalClass.java:3:72: compiler.err.illegal.qual.not.icls: java.lang.Object",
                "SuperConstructorCallInNormalClass.java:3:14: compiler.err.cant.resolve.location: kindname.class, " +
                    "Undefined2, , , (compiler.misc.location: kindname.class, SuperConstructorCallInNormalClass, null)",
                "SuperConstructorCallInNormalClass.java:3:55: compiler.err.cant.resolve: kindname.class, Undefined3, , ",
                "3 errors");
        tb.checkEqual(expected, output);
    }
}
