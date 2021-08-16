/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8231622
 * @summary SuppressWarning("serial") ignored on field serialVersionUID
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main T8231622
 */

import java.util.List;
import java.util.Objects;
import java.util.Arrays;

import toolbox.ToolBox;
import toolbox.TestRunner;
import toolbox.JavacTask;
import toolbox.Task;

public class T8231622 extends TestRunner {
    ToolBox tb;

    T8231622() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] main) throws Exception {
        T8231622 t = new T8231622();
        t.runTests();
    }

    @Test
    public void testSerialWarning() throws Exception {
        String code = """
                import java.io.Serializable;
                class T8231622_1 implements Serializable {
                    public static final int serialVersionUID = 1;
                }""";

        List<String> output = new JavacTask(tb)
                .sources(code)
                .classpath(".")
                .options("-XDrawDiagnostics", "-Xlint:serial")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "T8231622_1.java:3:29: compiler.warn.long.SVUID: T8231622_1",
                "1 warning");
        tb.checkEqual(expected, output);
    }

    @Test
    public void testSuppressSerialWarningInClass() throws Exception {
        String code = """
                import java.io.Serializable;
                @SuppressWarnings("serial")
                class T8231622_2 implements Serializable {
                    public static final int serialVersionUID = 1;
                }""";

        List<String> output = new JavacTask(tb)
                .sources(code)
                .classpath(".")
                .options("-XDrawDiagnostics", "-Xlint:serial")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList("");
        tb.checkEqual(expected, output);
    }

    @Test
    public void testSuppressSerialWarningInItsField() throws Exception {
        String code = """
                import java.io.Serializable;
                class T8231622_3 implements Serializable {
                    @SuppressWarnings("serial")
                    public static final int serialVersionUID = 1;
                }""";

        List<String> output = new JavacTask(tb)
                .sources(code)
                .classpath(".")
                .options("-XDrawDiagnostics", "-Xlint:serial")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList("");
        tb.checkEqual(expected, output);
    }
}
