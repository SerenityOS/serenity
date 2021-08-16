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
 * @bug 8182297 8242919 8267459
 * @summary Verify that pasting multi-line snippets works properly.
 * @library /tools/lib
 * @modules
 *     java.base/java.lang:open
 *     java.base/java.io:open
 *     jdk.compiler/com.sun.tools.javac.api
 *     jdk.compiler/com.sun.tools.javac.main
 *     jdk.internal.le/jdk.internal.org.jline.reader.impl
 *     jdk.jshell/jdk.internal.jshell.tool:open
 *     jdk.jshell/jdk.internal.jshell.tool.resources:open
 *     jdk.jshell/jdk.jshell:open
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build Compiler UITesting
 * @build PasteAndMeasurementsUITest
 * @run testng/othervm PasteAndMeasurementsUITest
 */

import java.io.Console;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import jdk.internal.org.jline.reader.impl.LineReaderImpl;

import org.testng.annotations.Test;

@Test
public class PasteAndMeasurementsUITest extends UITesting {

    public PasteAndMeasurementsUITest() {
        super(true);
    }

    public void testPrevNextSnippet() throws Exception {
        Field cons = System.class.getDeclaredField("cons");
        cons.setAccessible(true);
        Constructor console = Console.class.getDeclaredConstructor();
        console.setAccessible(true);
        cons.set(null, console.newInstance());
        doRunTest((inputSink, out) -> {
            inputSink.write("void test1() {\nSystem.err.println(1);\n}\n" + //LOC +
                            "void test2() {\nSystem.err.println(1);\n}\n"/* + LOC + LOC + LOC + LOC + LOC*/);
            waitOutput(out,       "void test1\\(\\)\u001B\\[2D\u001B\\[2C \\{\n" +
                            CONTINUATION_PROMPT + "    System.err.println\\(1\\)\u001B\\[3D\u001B\\[3C;\n" +
                            CONTINUATION_PROMPT + "    \\}\u001B\\[2A\u001B\\[8C\n\n\u001B\\[K\\}\n" +
                            "\u001B\\[\\?2004l\\|  created method test1\\(\\)\n" +
                            "\u001B\\[\\?2004h" + PROMPT + "void test2\\(\\)\u001B\\[2D\u001B\\[2C \\{\n" +
                            CONTINUATION_PROMPT + "    System.err.println\\(1\\)\u001B\\[3D\u001B\\[3C;\n" +
                            CONTINUATION_PROMPT + "    \\}\u001B\\[2A\u001B\\[8C\n\n\u001B\\[K\\}\n" +
                            "\u001B\\[\\?2004l\\|  created method test2\\(\\)\n" +
                            "\u001B\\[\\?2004h" + PROMPT);
        });
    }
        private static final String LOC = "\033[12;1R";

    public void testBracketedPaste() throws Exception {
        Field cons = System.class.getDeclaredField("cons");
        cons.setAccessible(true);
        Constructor console = Console.class.getDeclaredConstructor();
        console.setAccessible(true);
        cons.set(null, console.newInstance());
        doRunTest((inputSink, out) -> {
            inputSink.write(LineReaderImpl.BRACKETED_PASTE_BEGIN +
                            "int i;" +
                            LineReaderImpl.BRACKETED_PASTE_END);
            waitOutput(out,       "int i;");
        });
    }

    public void testBracketedPasteNonAscii() throws Exception {
        Field cons = System.class.getDeclaredField("cons");
        cons.setAccessible(true);
        Constructor console = Console.class.getDeclaredConstructor();
        console.setAccessible(true);
        cons.set(null, console.newInstance());
        doRunTest((inputSink, out) -> {
            inputSink.write(LineReaderImpl.BRACKETED_PASTE_BEGIN +
                            "int \u010d;" +
                            LineReaderImpl.BRACKETED_PASTE_END);
            waitOutput(out,       "int \uffc4\uff8d;"); //UTF-8 encoding of \u010d
        });
    }
}
