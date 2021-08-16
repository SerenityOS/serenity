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
 * @bug 8226216
 * @summary parameter modifiers are not visible to javac plugins across compilation boundaries
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main ParameterModifiersAcrossCompilationBoundaries
 */

import java.util.Arrays;
import java.util.List;
import java.nio.file.Path;

import toolbox.TestRunner;
import toolbox.ToolBox;
import toolbox.JavacTask;
import toolbox.Task;

public class ParameterModifiersAcrossCompilationBoundaries extends TestRunner {
    ToolBox tb;

    String moduleCode = """
                module P {
                    requires transitive jdk.compiler;
                    provides com.sun.source.util.Plugin with p.P;
                }
                """;

    String pluginCode = """
                package p;

                import com.sun.source.util.JavacTask;
                import com.sun.source.util.Plugin;
                import com.sun.source.util.TaskEvent;
                import com.sun.source.util.TaskListener;

                import javax.lang.model.element.ExecutableElement;
                import javax.lang.model.element.TypeElement;
                import javax.lang.model.element.VariableElement;
                import javax.lang.model.util.ElementFilter;
                import javax.lang.model.util.Elements;

                public class P implements Plugin {

                    @Override
                    public String getName() {
                        return "P";
                    }

                    @Override
                    public void init(JavacTask javacTask, String... strings) {
                        javacTask.addTaskListener(
                                new TaskListener() {
                                    @Override
                                    public void finished(TaskEvent e) {
                                        if (e.getKind() != TaskEvent.Kind.ENTER) {
                                            return;
                                        }
                                        Elements elements = javacTask.getElements();
                                        TypeElement typeElement = elements.getTypeElement("B");
                                        for (ExecutableElement m : ElementFilter.methodsIn(typeElement.getEnclosedElements())) {
                                            for (VariableElement p : m.getParameters()) {
                                                System.err.println(p.getSimpleName() + " " + p.getModifiers());
                                            }
                                        }
                                    }
                                });
                    }
                }
                """;

    String aCode = """
            public class A { }
            """;

    String bCode = """
            public class B {
                void f(final int x) {
                }
            }
            """;

    public ParameterModifiersAcrossCompilationBoundaries() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        var t = new ParameterModifiersAcrossCompilationBoundaries();
        t.runTests();
    }

    @Test
    public void testParameterModifiersNotVisible() throws Exception {
        Path base = Path.of(".");
        Path pluginPath = base.resolve("plugin");
        Path testPath = base.resolve("test");
        tb.writeFile(pluginPath.resolve("p").resolve("P.java"), pluginCode);
        tb.writeFile(pluginPath.resolve("module-info.java"), moduleCode);
        tb.writeFile(testPath.resolve("A.java"), aCode);
        tb.writeFile(testPath.resolve("B.java"), bCode);

        new JavacTask(tb)
                .files("plugin/module-info.java", "plugin/p/P.java")
                .run()
                .writeAll();

        List<String> firstOutput = new JavacTask(tb)
                .options("--processor-module-path", "plugin", "-Xplugin:P",
                         "-parameters")
                .files("test/A.java", "test/B.java")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDERR);
        List<String> firstExpected = Arrays.asList("x [final]", "x [final]");
        tb.checkEqual(firstExpected, firstOutput);

        List<String> secondOutput = new JavacTask(tb)
                .options("--processor-module-path", "plugin", "-Xplugin:P",
                         "-parameters", "-classpath", "test")
                .files("test/A.java")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.STDERR);
        List<String> secondExpected = Arrays.asList("x [final]");
        tb.checkEqual(secondExpected, secondOutput);
    }

}
