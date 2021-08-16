/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @key intermittent
 * @bug 8081845 8147898 8143955  8165405 8178023
 * @summary Tests for /reload in JShell tool
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.internal.jshell.tool
 * @library /tools/lib
 * @build KullaTesting TestingInputStream toolbox.ToolBox Compiler
 * @run testng ToolReloadTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Function;

import org.testng.annotations.Test;
import static org.testng.Assert.assertTrue;


@Test
public class ToolReloadTest extends ReplToolTesting {

    public void testReloadSnippets() {
        test(
                (a) -> assertVariable(a, "int", "x", "5", "5"),
                (a) -> assertMethod(a, "int m(int z) { return z * z; }",
                        "(int)int", "m"),
                (a) -> evaluateExpression(a, "int", "m(x)", "25"),
                (a) -> assertCommand(a, "/reload",
                        "|  Restarting and restoring state.\n" +
                        "-: int x = 5;\n" +
                        "-: int m(int z) { return z * z; }\n" +
                        "-: m(x)\n"),
                (a) -> evaluateExpression(a, "int", "m(x)", "25"),
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods())
        );
    }

    public void testReloadClasspath() {
        Function<String,String> prog = (s) -> String.format(
                "package pkg; public class A { public String toString() { return \"%s\"; } }\n", s);
        Compiler compiler = new Compiler();
        Path outDir = Paths.get("testClasspathDirectory");
        compiler.compile(outDir, prog.apply("A"));
        Path classpath = compiler.getPath(outDir);
        test(
                (a) -> assertCommand(a, "/env --class-path " + classpath,
                        "|  Setting new options and restoring state."),
                (a) -> assertMethod(a, "String foo() { return (new pkg.A()).toString(); }",
                        "()String", "foo"),
                (a) -> assertVariable(a, "String", "v", "foo()", "\"A\""),
                (a) -> {
                       if (!a) compiler.compile(outDir, prog.apply("Aprime"));
                       assertCommand(a, "/reload",
                        "|  Restarting and restoring state.\n" +
                        "-: String foo() { return (new pkg.A()).toString(); }\n" +
                        "-: String v = foo();\n");
                       },
                (a) -> assertCommand(a, "v", "v ==> \"Aprime\""),
                (a) -> evaluateExpression(a, "String", "foo()", "\"Aprime\""),
                (a) -> evaluateExpression(a, "pkg.A", "new pkg.A();", "Aprime")
        );
    }

    public void testReloadDrop() {
        test(false, new String[]{"--no-startup"},
                a -> assertVariable(a, "int", "a"),
                a -> dropVariable(a, "/dr 1", "int a = 0", "|  dropped variable a"),
                a -> assertMethod(a, "int b() { return 0; }", "()int", "b"),
                a -> dropMethod(a, "/drop b", "int b()", "|  dropped method b()"),
                a -> assertClass(a, "class A {}", "class", "A"),
                a -> dropClass(a, "/dr A", "class A", "|  dropped class A"),
                a -> assertCommand(a, "/reload",
                        "|  Restarting and restoring state.\n" +
                        "-: int a;\n" +
                        "-: /drop 1\n" +
                        "-: int b() { return 0; }\n" +
                        "-: /drop b\n" +
                        "-: class A {}\n" +
                        "-: /drop A\n"),
                a -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                a -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                a -> assertCommandCheckOutput(a, "/types", assertClasses()),
                a -> assertCommandCheckOutput(a, "/imports", assertImports())
        );
    }

    public void testReloadQuiet() {
        test(false, new String[]{"--no-startup"},
                a -> assertVariable(a, "int", "a"),
                a -> dropVariable(a, "/dr 1", "int a = 0", "|  dropped variable a"),
                a -> assertMethod(a, "int b() { return 0; }", "()int", "b"),
                a -> dropMethod(a, "/drop b", "int b()", "|  dropped method b()"),
                a -> assertClass(a, "class A {}", "class", "A"),
                a -> dropClass(a, "/dr A", "class A", "|  dropped class A"),
                a -> assertCommand(a, "/reload -quiet",
                        "|  Restarting and restoring state."),
                a -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                a -> assertCommandCheckOutput(a, "/methods", assertMethods()),
                a -> assertCommandCheckOutput(a, "/types", assertClasses()),
                a -> assertCommandCheckOutput(a, "/imports", assertImports())
        );
    }

    public void testReloadRepeat() {
        test(false, new String[]{"--no-startup"},
                (a) -> assertVariable(a, "int", "c", "7", "7"),
                (a) -> assertCommand(a, "++c", null),
                (a) -> assertCommand(a, "/!", null),
                (a) -> assertCommand(a, "/2", null),
                (a) -> assertCommand(a, "/-1", null),
                (a) -> assertCommand(a, "/reload",
                        "|  Restarting and restoring state.\n" +
                        "-: int c = 7;\n" +
                        "-: ++c\n" +
                        "-: ++c\n" +
                        "-: ++c\n" +
                        "-: ++c\n"
                ),
                (a) -> assertCommand(a, "c", "c ==> 11"),
                (a) -> assertCommand(a, "$4", "$4 ==> 10")
        );
    }

    public void testReloadIgnore() {
        test(false, new String[]{"--no-startup"},
                (a) -> assertCommand(a, "(-)", null),
                (a) -> assertCommand(a, "/list", null),
                (a) -> assertCommand(a, "/history", null),
                (a) -> assertCommand(a, "/help", null),
                (a) -> assertCommand(a, "/vars", null),
                (a) -> assertCommand(a, "/save abcd", null),
                (a) -> assertCommand(a, "/reload",
                        "|  Restarting and restoring state.")
        );
    }

    public void testReloadResetRestore() {
        test(
                (a) -> assertVariable(a, "int", "x", "5", "5"),
                (a) -> assertMethod(a, "int m(int z) { return z * z; }",
                        "(int)int", "m"),
                (a) -> evaluateExpression(a, "int", "m(x)", "25"),
                (a) -> assertCommand(a, "/reset", "|  Resetting state."),
                (a) -> assertCommand(a, "/reload -restore",
                        "|  Restarting and restoring from previous state.\n" +
                        "-: int x = 5;\n" +
                        "-: int m(int z) { return z * z; }\n" +
                        "-: m(x)\n"),
                (a) -> evaluateExpression(a, "int", "m(x)", "25"),
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods())
        );
    }

    public void testReloadCrashRestore() {
        test(
                (a) -> assertVariable(a, "int", "x", "5", "5"),
                (a) -> assertMethod(a, "int m(int z) { return z * z; }",
                        "(int)int", "m"),
                (a) -> evaluateExpression(a, "int", "m(x)", "25"),
                (a) -> assertCommand(a, "System.exit(1);",
                        "|  State engine terminated.\n" +
                        "|  Restore definitions with: /reload -restore"),
                (a) -> assertCommand(a, "/reload -restore",
                        "|  Restarting and restoring from previous state.\n" +
                        "-: int x = 5;\n" +
                        "-: int m(int z) { return z * z; }\n" +
                        "-: m(x)\n"),
                (a) -> evaluateExpression(a, "int", "m(x)", "25"),
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods())
        );
    }

    public void testEnvBadModule() {
        test(
                (a) -> assertVariable(a, "int", "x", "5", "5"),
                (a) -> assertMethod(a, "int m(int z) { return z * z; }",
                        "(int)int", "m"),
                (a) -> assertCommandCheckOutput(a, "/env --add-module unKnown",
                        s -> {
                            assertTrue(s.startsWith(
                                "|  Setting new options and restoring state.\n" +
                                "|  Restart failed:"));
                            assertTrue(s.contains("unKnown"),
                                    "\"unKnown\" missing from: " + s);
                            assertTrue(s.contains("previous settings"),
                                    "\"previous settings\" missing from: " + s);
                                      }),
                (a) -> evaluateExpression(a, "int", "m(x)", "25"),
                (a) -> assertCommandCheckOutput(a, "/vars", assertVariables()),
                (a) -> assertCommandCheckOutput(a, "/methods", assertMethods())
        );
    }

    public void testReloadExitRestore() {
        test(false, new String[]{"--no-startup"},
                (a) -> assertVariable(a, "int", "x", "5", "5"),
                (a) -> assertMethod(a, "int m(int z) { return z * z; }",
                        "(int)int", "m"),
                (a) -> evaluateExpression(a, "int", "m(x)", "25")
        );
        test(false, new String[]{"--no-startup"},
                (a) -> assertCommand(a, "/reload -restore",
                        "|  Restarting and restoring from previous state.\n" +
                        "-: int x = 5;\n" +
                        "-: int m(int z) { return z * z; }\n" +
                        "-: m(x)\n"),
                (a) -> evaluateExpression(a, "int", "m(x)", "25")
        );
    }
}
