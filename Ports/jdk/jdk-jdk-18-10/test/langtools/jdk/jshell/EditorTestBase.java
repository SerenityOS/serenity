/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

public abstract class EditorTestBase extends ReplToolTesting {

    private static ExecutorService executor;

    public abstract void writeSource(String s);
    public abstract String getSource();
    public abstract void accept();
    public abstract void exit();
    public abstract void cancel();
    public abstract void shutdownEditor();

    public void testEditor(ReplTest... tests) {
        testEditor(false, new String[]{"--no-startup"}, tests);
    }

    public void testEditor(boolean defaultStartup, String[] args, ReplTest... tests) {
        test(defaultStartup, args, tests);
    }

    abstract void assertEdit(boolean after, String cmd,
                                    Consumer<String> checkInput, Consumer<String> checkOutput, Action action);

    void assertEditInput(boolean after, String cmd, Consumer<String> checkInput, Action action) {
        assertEdit(after, cmd, checkInput, s -> {}, action);
    }

    void assertEditOutput(boolean after, String cmd, Consumer<String> checkOutput, Action action) {
        assertEdit(after, cmd, s -> {}, checkOutput, action);
    }

    void assertEditInput(boolean after, String cmd, String input, Action action) {
        assertEditInput(after, cmd, s -> assertEquals(s, input, "Input"), action);
    }

    void assertEditOutput(boolean after, String cmd, String output, Action action) {
        assertEditOutput(after, cmd, s -> assertEquals(s.trim(), output.trim(), "command"), action);
    }

    @Test
    public void testEditNegative() {
        for (String edit : new String[] {"/ed", "/edit"}) {
            test(new String[]{"--no-startup"},
                    a -> assertCommandOutputStartsWith(a, edit + " 1",
                            "|  No snippet with ID: 1"),
                    a -> assertCommandOutputStartsWith(a, edit + " unknown",
                            "|  No such snippet: unknown")
            );
        }
    }

    @Test
    public void testDoNothing() {
        testEditor(
                a -> assertVariable(a, "int", "a", "0", "0"),
                a -> assertEditOutput(a, "/ed 1", "", this::exit),
                a -> assertCommandCheckOutput(a, "/v", assertVariables())
        );
    }

    @Test
    public void testEditVariable1() {
        testEditor(
                a -> assertVariable(a, "int", "a", "0", "0"),
                a -> assertEditOutput(a, "/ed 1", "a ==> 10", () -> {
                    writeSource("\n\n\nint a = 10;\n\n\n");
                    exit();
                    loadVariable(true, "int", "a", "10", "10");
                }),
                a -> assertEditOutput(a, "/ed 1", "a ==> 15", () -> {
                    writeSource("int a = 15;");
                    exit();
                    loadVariable(true, "int", "a", "15", "15");
                }),
                a -> assertCommandCheckOutput(a, "/v", assertVariables())
        );
    }

    @Test
    public void testEditVariable2() {
        testEditor(
                a -> assertVariable(a, "int", "a", "0", "0"),
                a -> assertEditOutput(a, "/ed 1", "b ==> 10", () -> {
                    writeSource("int b = 10;");
                    exit();
                    loadVariable(true, "int", "b", "10", "10");
                }),
                a -> assertEditOutput(a, "/ed 1", "a ==> 15", () -> {
                    writeSource("int a = 15;");
                    exit();
                    loadVariable(true, "int", "a", "15", "15");
                }),
                a -> assertCommandCheckOutput(a, "/v", assertVariables())
        );
    }

    @Test
    public void testWriteVariables() {
        testEditor(
                a -> assertEditOutput(a, "/edit",
                                "x ==> 1\n" +
                                "y ==> 2\n" +
                                "z ==> 3",
                        () -> {
                            writeSource(
                                            "var x = 1;\n" +
                                            "var y = 2;\n" +
                                            "var z = 3;\n");
                            exit();
                        }),
                a -> assertCommand(a, "z", "z ==> 3")
        );
    }

    public void testEditClass1() {
        testEditor(
                a -> assertClass(a, "class A {}", "class", "A"),
                a -> assertEditOutput(a, "/ed 1", "", () -> {
                    writeSource("\n\n\nclass A {}\n\n\n");
                    exit();
                    loadClass(true, "class A {}", "class", "A");
                }),
                a -> assertEditOutput(a, "/ed 1",
                        "|  replaced enum A", () -> {
                    writeSource("enum A {}");
                    exit();
                    loadClass(true, "enum A {}", "enum", "A");
                }),
                a -> assertCommandCheckOutput(a, "/types", assertClasses())
        );
    }

    @Test
    public void testEditClass2() {
        testEditor(
                a -> assertClass(a, "class A {}", "class", "A"),
                a -> assertEditOutput(a, "/ed 1", "|  created class B", () -> {
                    writeSource("class B { }");
                    exit();
                    loadClass(true, "class B {}", "class", "B");
                }),
                a -> assertEditOutput(a, "/ed 1",
                        "|  replaced enum A", () -> {
                    writeSource("enum A {}");
                    exit();
                    loadClass(true, "enum A {}", "enum", "A");
                }),
                a -> assertCommandCheckOutput(a, "/types", assertClasses())
        );
    }

    public void testEditMethod1() {
        testEditor(
                a -> assertMethod(a, "void f() {}", "()void", "f"),
                a -> assertEditOutput(a, "/ed 1", "", () -> {
                    writeSource("\n\n\nvoid f() {}\n\n\n");
                    exit();
                    loadMethod(true, "void f() {}", "()void", "f");
                }),
                a -> assertEditOutput(a, "/ed 1",
                        "|  replaced method f()", () -> {
                    writeSource("double f() { return 0; }");
                    exit();
                    loadMethod(true, "double f() { return 0; }", "()double", "f");
                }),
                a -> assertCommandCheckOutput(a, "/m", assertMethods())
        );
    }

    @Test
    public void testEditMethod2() {
        testEditor(
                a -> assertMethod(a, "void f() {}", "()void", "f"),
                a -> assertEditOutput(a, "/ed 1", "|  created method g()", () -> {
                    writeSource("void g() {}");
                    exit();
                    loadMethod(true, "void g() {}", "()void", "g");
                }),
                a -> assertEditOutput(a, "/ed 1",
                        "|  replaced method f()", () -> {
                    writeSource("double f() { return 0; }");
                    exit();
                    loadMethod(true, "double f() { return 0; }", "()double", "f");
                }),
                a -> assertCommandCheckOutput(a, "/m", assertMethods())
        );
    }

    @Test
    public void testNoArguments() {
        testEditor(
                a -> assertVariable(a, "int", "a"),
                a -> assertMethod(a, "void f() {}", "()void", "f"),
                a -> assertClass(a, "class A {}", "class", "A"),
                a -> assertEditInput(a, "/ed", s -> {
                    String[] ss = s.split("\n");
                    assertEquals(ss.length, 3, "Expected 3 lines: " + s);
                    assertEquals(ss[0], "int a;");
                    assertEquals(ss[1], "void f() {}");
                    assertEquals(ss[2], "class A {}");
                }, this::exit)
        );
    }

    @Test
    public void testStartup() {
        testEditor(true, new String[0],
                a -> assertEditInput(a, "/ed", s -> assertTrue(s.isEmpty(), "Checking of startup: " + s), this::cancel),
                a -> assertEditInput(a, "/ed s1", assertStartsWith("import"), this::cancel));
    }

    @Test
    public void testCancel() {
        testEditor(
                a -> assertVariable(a, "int", "a"),
                a -> assertEditOutput(a, "/ed a", "", () -> {
                    writeSource("int b = 10");
                    cancel();
                })
        );
    }

    @Test
    public void testAccept() {
        testEditor(
                a -> assertVariable(a, "int", "a"),
                a -> assertEditOutput(a, "/ed a", "b ==> 10", () -> {
                    writeSource("int b = 10");
                    accept();
                    exit();
                })
        );
    }

    @Test(enabled = false) // TODO JDK-8191875
    public void testStatementMush() {
        testEditor(
                a -> assertCommand(a, "System.out.println(\"Hello\")",
                        "", "", null, "Hello\n", ""),
                a -> assertEditOutput(a, "/ed", "b ==> 10", () -> {
                    writeSource(getSource() + "\nint b = 10");
                    exit();
                }),

                //TODO: this is a work-around to JDK-8170369
                a -> assertCommand(a, "1234",
                        null, "", null, null, "")
        );
    }

    public static ExecutorService getExecutor() {
        if (executor == null) {
            executor = Executors.newSingleThreadExecutor();
        }
        return executor;
    }

    public static void executorShutdown() {
        if (executor != null) {
            executor.shutdown();
            executor = null;
        }
    }

    interface Action {
        void accept();
    }
}
