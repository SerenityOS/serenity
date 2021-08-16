/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4330479
 * @summary ASCII SUB character is rejected in multi-line comments
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main SubChar
 */

import toolbox.JavacTask;
import toolbox.JavaTask;
import toolbox.Task;
import toolbox.ToolBox;


public class SubChar {
    private static final ToolBox TOOLBOX = new ToolBox();

    private static final String SOURCE = """
        /*
        Note: this source file has been crafted very carefully to end with the
        unicode escape sequence for the control-Z character without a
        following newline.  The scanner is specified to allow control-Z there.
        If you edit this source file, please make sure that your editor does
        not insert a newline after that trailing line.
        */

        /** \\u001A */
        class ControlZTest {
            public static void main(String args[]) {
                return;
            }
        }
        /* \\u001A */\
        """;

        public static void main(String... args) {
            String output = new JavacTask(TOOLBOX)
                    .sources(SOURCE)
                    .classpath(".")
                    .options("-encoding", "utf8")
                    .run()
                    .writeAll()
                    .getOutput(Task.OutputKind.DIRECT);
            System.out.println(output);
        }
}
