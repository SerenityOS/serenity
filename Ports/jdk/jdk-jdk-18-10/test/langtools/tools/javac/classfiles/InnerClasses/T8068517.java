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

/* @test
 * @bug 8034854
 * @summary Verify that nested enums have correct abstract flag in the InnerClasses attribute.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build T8068517
 * @run main T8068517
 */

import com.sun.tools.javac.util.Assert;
import java.util.Arrays;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class T8068517 {

    public static void main(String[] args) throws Exception {
        new T8068517().run();
    }

    void run() throws Exception {
        runTest("class A {\n" +
                "    enum AInner implements Runnable {\n" +
                "        A {\n" +
                "            public void run() {}\n" +
                "        };\n" +
                "    }\n" +
                "}\n",
                "class B {\n" +
                "    A.AInner a;\n" +
                "}");
        runTest("class A {\n" +
                "    enum AInner implements Runnable {\n" +
                "        A {\n" +
                "            public void run() {}\n" +
                "        };\n" +
                "    }\n" +
                "    AInner aInner;\n" +
                "}\n",
                "class B {\n" +
                "    void test(A a) {;\n" +
                "        switch (a.aInner) {\n" +
                "            case A: break;\n" +
                "        }\n" +
                "    };\n" +
                "}");
        runTest("class A {\n" +
                "    enum AInner implements Runnable {\n" +
                "        A {\n" +
                "            public void run() {}\n" +
                "        };\n" +
                "    }\n" +
                "    AInner aInner;\n" +
                "}\n",
                "class B {\n" +
                "    void test(A a) {;\n" +
                "        System.err.println(a.aInner.toString());\n" +
                "    };\n" +
                "}");
        runTest("class A {\n" +
                "    enum AInner implements Runnable {\n" +
                "        A {\n" +
                "            public void run() {}\n" +
                "        };\n" +
                "    }\n" +
                "    AInner aInner() {\n" +
                "        return null;\n" +
                "    }\n" +
                "}\n",
                "class B {\n" +
                "    void test(A a) {;\n" +
                "        System.err.println(a.aInner().toString());\n" +
                "    };\n" +
                "}");
    }

    void runTest(String aJava, String bJava) throws Exception {
        try (JavaFileManager fm = ToolProvider.getSystemJavaCompiler().getStandardFileManager(null, null, null)) {
            ToolBox tb = new ToolBox();
            ToolBox.MemoryFileManager memoryFM1 = new ToolBox.MemoryFileManager(fm);
            new JavacTask(tb).fileManager(memoryFM1)
                              .sources(aJava, bJava)
                              .run();
            ToolBox.MemoryFileManager memoryFM2 = new ToolBox.MemoryFileManager(fm);
            new JavacTask(tb).fileManager(memoryFM2)
                              .sources(bJava, aJava)
                              .run();

            Assert.check(Arrays.equals(memoryFM1.getFileBytes(StandardLocation.CLASS_OUTPUT, "B"),
                                       memoryFM2.getFileBytes(StandardLocation.CLASS_OUTPUT, "B")));
        }
    }
}
