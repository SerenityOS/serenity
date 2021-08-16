/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6271292
 * @summary Verify that javap prints StackMapTable attribute contents
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.JavapTask
 * @run main StackmapTest
 */

import java.util.ArrayList;
import java.util.List;

import toolbox.JavacTask;
import toolbox.JavapTask;
import toolbox.Task;
import toolbox.ToolBox;

// Original test: test/tools/javap/stackmap/T6271292.sh
public class StackmapTest {

    private static final String TestSrc =
        "public class Test extends SuperClass {\n" +
        "    public static void main(String[] args) {\n" +
        "        new SuperClass((args[0].equals(\"0\")) ? 0 : 1)\n" +
        "            .test();\n" +
        "    }\n" +
        "    Test(boolean b) {\n" +
        "        super(b ? 1 : 2);\n" +
        "    }\n" +
        "}\n" +
        "class SuperClass {\n" +
        "    double d;\n" +
        "    SuperClass(double dd) { d = dd; }\n" +
        "    double test() {\n" +
        "        if (d == 0)\n" +
        "            return d;\n" +
        "        else\n" +
        "            return d > 0 ? d++ : d--;\n" +
        "    }\n" +
        "}\n";

    private static final String goldenOut =
        "        frame_type = 255 /* full_frame */\n" +
        "        frame_type = 255 /* full_frame */\n" +
        "        frame_type = 73 /* same_locals_1_stack_item */\n" +
        "        frame_type = 255 /* full_frame */\n" +
        "          offset_delta = 19\n" +
        "          offset_delta = 0\n" +
        "          offset_delta = 2\n" +
        "          stack = [ uninitialized 0, uninitialized 0 ]\n" +
        "          stack = [ uninitialized 0, uninitialized 0, double ]\n" +
        "          stack = [ this ]\n" +
        "          stack = [ this, double ]\n" +
        "          locals = [ class \"[Ljava/lang/String;\" ]\n" +
        "          locals = [ class \"[Ljava/lang/String;\" ]\n" +
        "          locals = [ this, int ]\n";

    public static void main(String[] args) throws Exception {
        ToolBox tb = new ToolBox();

        new JavacTask(tb)
                .sources(TestSrc)
                .run();

        List<String> out = new JavapTask(tb)
                .options("-v")
                .classes("Test.class")
                .run()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> grepResult = new ArrayList<>();
        grepResult.addAll(tb.grep("frame_type",   out));
        grepResult.addAll(tb.grep("offset_delta", out));
        grepResult.addAll(tb.grep("stack = ",     out));
        grepResult.addAll(tb.grep("locals = ",    out));

        List<String> goldenList = tb.split(goldenOut, "\n");
        tb.checkEqual(goldenList, grepResult);
    }

}
