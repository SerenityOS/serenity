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

/**
 * @test
 * @bug 8241950 8247932
 * @summary Check the UI behavior of indentation
 * @library /tools/lib
 * @modules
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 *     jdk.jshell/jdk.internal.jshell.tool:open
 *     jdk.jshell/jdk.internal.jshell.tool.resources:open
 *     jdk.jshell/jdk.jshell:open
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build Compiler UITesting
 * @compile IndentUITest.java
 * @run testng IndentUITest
 */

import org.testng.annotations.Test;

@Test
public class IndentUITest extends UITesting {

    public IndentUITest() {
        super(true);
    }

    public void testIdent() throws Exception {
        doRunTest((inputSink, out) -> {
            inputSink.write("void test1() {\nSystem.err.println(1);\n}\n");
            waitOutput(out, "void test1\\(\\)\u001B\\[2D\u001B\\[2C \\{\n" +
                            CONTINUATION_PROMPT + "    System.err.println\\(1\\)\u001B\\[3D\u001B\\[3C;\n" +
                            CONTINUATION_PROMPT + "    \\}\u001B\\[2A\u001B\\[8C\n\n\u001B\\[K\\}\n" +
                            "\u001B\\[\\?2004l\\|  created method test1\\(\\)\n" +
                            "\u001B\\[\\?2004h" + PROMPT);
            inputSink.write(UP);
            waitOutput(out, "^void test1\\(\\) \\{\n" +
                            CONTINUATION_PROMPT + "    System.err.println\\(1\\);\n" +
                            CONTINUATION_PROMPT + "\\}");
            inputSink.write(DOWN);
            inputSink.write("/set indent 2\n");
            inputSink.write("void test2() {\nSystem.err.println(1);\n}\n");
            waitOutput(out, "void test2\\(\\)\u001B\\[2D\u001B\\[2C \\{\n" +
                            CONTINUATION_PROMPT + "  System.err.println\\(1\\)\u001B\\[3D\u001B\\[3C;\n" +
                            CONTINUATION_PROMPT + "  \\}\u001B\\[2A\u001B\\[10C\n\n\u001B\\[K\\}\n" +
                            "\u001B\\[\\?2004l\\|  created method test2\\(\\)\n" +
                            "\u001B\\[\\?2004h" + PROMPT);
            inputSink.write(UP);
            waitOutput(out, "^void test2\\(\\) \\{\n" +
                            CONTINUATION_PROMPT + "  System.err.println\\(1\\);\n" +
                            CONTINUATION_PROMPT + "\\}");
            inputSink.write(INTERRUPT);
            waitOutput(out, "\u001B\\[\\?2004h" + PROMPT);
            inputSink.write("\"\"\"\n");
            waitOutput(out, "^\"\"\"\n" +
                            CONTINUATION_PROMPT);
        });
    }

}
