/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8051768
 * @summary Verify that javap prints "param" for RuntimeInvisibleParameterAnnotations
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.JavapTask toolbox.Assert
 * @run main InvisibleParameterAnnotationsTest
 */

import toolbox.Assert;
import toolbox.JavacTask;
import toolbox.JavapTask;
import toolbox.Task;
import toolbox.ToolBox;

import java.util.Collections;
import java.util.List;

public class InvisibleParameterAnnotationsTest {

    private static final String TestSrc =
            "import java.lang.annotation.Retention \n;" +
            "import java.lang.annotation.RetentionPolicy \n;" +

            "public class Sample { \n" +

                "@Retention(RetentionPolicy.CLASS) \n" +
                "public @interface InvisAnno{} \n" +
                "@Retention(RetentionPolicy.RUNTIME) \n" +
                "public @interface VisAnno{} \n" +

                "public void Method(@InvisAnno int arg1,@VisAnno int arg2){};" +
            "}";

    private static final String ExpectedSubstring =
            "    RuntimeVisibleParameterAnnotations:\n" +
            "      parameter 0:\n" +
            "      parameter 1:\n" +
            "        0: #14()\n" +
            "          Sample$VisAnno\n" +
            "    RuntimeInvisibleParameterAnnotations:\n" +
            "      parameter 0:\n" +
            "        0: #16()\n" +
            "          Sample$InvisAnno\n" +
            "      parameter 1:";

    public static void main(String[] args) throws Exception {
        ToolBox tb = new ToolBox();
        new JavacTask(tb).sources(TestSrc).run();

        List<String> res = new JavapTask(tb)
                .options("-v")
                .classes("Sample.class")
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expectedList = tb.split(ExpectedSubstring, "\n");
        Boolean found = Collections.indexOfSubList(res, expectedList) > -1;
        Assert.check(found, "expected output not found: " + ExpectedSubstring);
    }
}
