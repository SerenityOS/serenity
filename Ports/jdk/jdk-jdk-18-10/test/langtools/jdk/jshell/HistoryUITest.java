/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8178077 8232856
 * @summary Check the UI behavior of editing history.
 * @modules
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 *     jdk.jshell/jdk.internal.jshell.tool:open
 *     jdk.jshell/jdk.internal.jshell.tool.resources:open
 *     jdk.jshell/jdk.jshell:open
 * @library /tools/lib
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build Compiler UITesting
 * @compile HistoryUITest.java
 * @run testng HistoryUITest
 */

import org.testng.annotations.Test;

@Test
public class HistoryUITest extends UITesting {

    public HistoryUITest() {
        super(true);
    }

    public void testPrevNextSnippet() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("void test1() {\nSystem.err.println(1);\n}\n");
            waitOutput(out, PROMPT);
            inputSink.write("void test2() {\nSystem.err.println(2);\n}\n");
            waitOutput(out, PROMPT);
            inputSink.write(UP);
            waitOutput(out, "^void test2\\(\\) \\{\n" +
                            CONTINUATION_PROMPT + "    System.err.println\\(2\\);\n" +
                            CONTINUATION_PROMPT + "\\}");
            inputSink.write(UP);
            waitOutput(out, "^\u001b\\[A");
            inputSink.write(UP);
            waitOutput(out, "^\u001b\\[A");
            inputSink.write(UP);
            waitOutput(out, "^\u001b\\[8C1\n" +
                            "\u001b\\[23C1\n\u001b\\[C");
            inputSink.write(DOWN);
            waitOutput(out, "^\u001B\\[2A\u001b\\[8C2\n" +
                            "\u001b\\[23C2\n\u001b\\[C");
            inputSink.write(UP);
            waitOutput(out, "^\u001b\\[A");
            for (int i = 0; i < 23; i++) inputSink.write("\033[C");
            waitOutput(out, "C");
            inputSink.write("\u0008\"Modified!\"\n");
            waitOutput(out, PROMPT);
            inputSink.write("test2()\n");
            waitOutput(out, "\\u001B\\[\\?2004lModified!\n\\u001B\\[\\?2004h" + PROMPT);
        });
    }

    public void testReRun() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("System.err.println(\"RAN\");\n");
            waitOutput(out, "RAN.*" + PROMPT);
            inputSink.write("/!\n");
            waitOutput(out, "RAN.*" + PROMPT);
            inputSink.write(UP);
            inputSink.write("\n");
            waitOutput(out, "RAN.*" + PROMPT);
        });
    }

}
