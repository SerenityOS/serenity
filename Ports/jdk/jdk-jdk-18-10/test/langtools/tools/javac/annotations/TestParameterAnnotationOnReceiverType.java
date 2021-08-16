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
 * @bug 8239596
 * @summary PARAMETER annotation on receiver type should cause error
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main TestParameterAnnotationOnReceiverType
 */

import java.util.List;
import java.util.Arrays;

import toolbox.ToolBox;
import toolbox.TestRunner;
import toolbox.JavacTask;
import toolbox.Task;

public class TestParameterAnnotationOnReceiverType extends TestRunner {
    ToolBox tb;

    public TestParameterAnnotationOnReceiverType() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String[] args) throws Exception {
        TestParameterAnnotationOnReceiverType t = new TestParameterAnnotationOnReceiverType();
        t.runTests();
    }

    @Test
    public void testReceiverTypeDoesNotCauseError() throws Exception {
        String code = """
                import java.lang.annotation.ElementType;
                import java.lang.annotation.Retention;
                import java.lang.annotation.RetentionPolicy;
                import java.lang.annotation.Target;
                class Test8239596 {
                    @Retention(RetentionPolicy.RUNTIME)
                    @Target({ElementType.TYPE_USE})
                    @interface TypeUse { }

                    @Retention(RetentionPolicy.RUNTIME)
                    @Target({ElementType.PARAMETER})
                    @interface Param { }

                    public void test(@TypeUse @Param Test8239596 this) { }
                }""";

        List<String> output = new JavacTask(tb)
                .sources(code)
                .classpath(".")
                .options("-XDrawDiagnostics")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);
        List<String> expected = Arrays.asList(
                "Test8239596.java:14:31: compiler.err.annotation.type.not.applicable.to.type: Test8239596.Param",
                "1 error");
        tb.checkEqual(expected, output);
    }
}
