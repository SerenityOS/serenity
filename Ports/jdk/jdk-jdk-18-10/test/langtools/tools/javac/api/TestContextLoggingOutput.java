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
 * @bug 8198317
 * @summary Enhance JavacTool.getTask for flexibility
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox
 * @run main TestContextLoggingOutput
 */

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.List;
import java.util.Arrays;
import javax.tools.ToolProvider;
import javax.tools.SimpleJavaFileObject;
import javax.tools.JavaFileObject;

import toolbox.ToolBox;
import toolbox.TestRunner;
import static toolbox.ToolBox.lineSeparator;

public class TestContextLoggingOutput extends TestRunner {
    ToolBox tb;

    public TestContextLoggingOutput() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        TestContextLoggingOutput t = new TestContextLoggingOutput();
        t.runTests();
    }

    @Test
    public void testLogSettingInJavacTool() throws Exception {
        String code = """
                import java.io.Serializable;
                class Test implements Serializable {
                    public static final int serialVersionUID = 1;
                }""";

        List<String> expected = Arrays.asList(
                "Test.java:3:29: compiler.warn.long.SVUID: Test",
                "1 warning");

        List<? extends JavaFileObject> files = Arrays.asList(new MemFile("Test.java", code));

        // Situation: out is null and the value is not set in the context.
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(baos);
        PrintStream prev = System.err;
        try {
            System.setErr(printStream);
            ToolProvider.getSystemJavaCompiler()
                    .getTask(null, null, null, Arrays.asList("-XDrawDiagnostics", "-Xlint:serial"), null, files)
                    .call();
            tb.checkEqual(expected, Arrays.asList(baos.toString().split(lineSeparator)));
        } finally {
            System.setErr(prev);
        }

        // Situation: out is not null and out is a PrintWriter.
        StringWriter stringWriter2 = new StringWriter();
        PrintWriter expectedPW2 = new PrintWriter(stringWriter2);
        ToolProvider.getSystemJavaCompiler()
                .getTask(expectedPW2, null, null, Arrays.asList("-XDrawDiagnostics", "-Xlint:serial"), null, files)
                .call();
        tb.checkEqual(expected, Arrays.asList(stringWriter2.toString().split(lineSeparator)));

        // Situation: out is not null and out is not a PrintWriter.
        StringWriter stringWriter3 = new StringWriter();
        ToolProvider.getSystemJavaCompiler()
                .getTask(stringWriter3, null, null, Arrays.asList("-XDrawDiagnostics", "-Xlint:serial"), null, files)
                .call();
        tb.checkEqual(expected, Arrays.asList(stringWriter3.toString().split(lineSeparator)));
    }

    class MemFile extends SimpleJavaFileObject {
        public final String text;

        MemFile(String name, String text) {
            super(URI.create(name), JavaFileObject.Kind.SOURCE);
            this.text = text;
        }

        @Override
        public String getName() {
            return uri.toString();
        }

        @Override
        public String getCharContent(boolean ignoreEncodingErrors) {
            return text;
        }
    }
}
