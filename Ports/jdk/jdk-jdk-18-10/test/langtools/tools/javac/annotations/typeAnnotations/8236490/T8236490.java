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

/**
 * @test
 * @bug 8236490
 * @summary If the exception class index in constant pool exceeds 256,
 *          the type annotations in conresponding catch expression should be compiled successfully.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.main
 *          jdk.compiler/com.sun.tools.javac.api
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main T8236490
 */

import toolbox.ToolBox;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;

public class T8236490 extends TestRunner {
    ToolBox tb;

    public T8236490() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        new T8236490().runTests();
    }

    @Test
    public void testTypeAnnotationInCatchExpression() throws Exception {
        // Generate a class which contains more than 256 constant pool entries
        // and the exception class index in constant pool exceeds 256.
        // The code is shown as below:
        // import java.lang.annotation.ElementType;
        // import java.lang.annotation.Target;
        // public class Test8236490 {
        //     private class Test0 {}
        //     ... many classes ...
        //     private class Test299 {}
        //     @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
        //     private @interface AnnotationTest {}
        //     public void test() {
        //         Test0 test0 = new Test0();
        //         ... many variables ...
        //         Test299 test299 = new Test299();
        //         try {
        //             System.out.println("Hello");
        //         } catch (@AnnotationTest Exception e) {}
        //     }
        // }
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("""
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Target;
                public class Test8236490 {
                """);
        for (int i = 0; i < 300; i++) {
            stringBuilder.append("    private class Test" + i + " {}\n");
        }
        stringBuilder.append("""
                    @Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})
                    private @interface AnnotationTest {}
                    public void test() {
                """);
        for (int i = 0; i < 300; i++) {
            stringBuilder.append("        Test" + i + " test" + i + " = new Test" + i + "();\n");
        }
        stringBuilder.append("""
                        try {
                            System.out.println("Hello");
                        } catch (@AnnotationTest Exception e) {}
                    }
                }
                """);

        new JavacTask(tb)
                .sources(stringBuilder.toString())
                .outdir(".")
                .run();
    }
}
