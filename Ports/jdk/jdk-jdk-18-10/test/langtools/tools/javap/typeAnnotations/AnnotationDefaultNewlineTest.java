/*
 * Copyright (c) 2013, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8041793
 * @summary Verify that javap prints newline character after AnnotationDefault value
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.JavapTask toolbox.Assert
 * @run main AnnotationDefaultNewlineTest
 */

import java.util.Collections;
import java.util.List;

import toolbox.Assert;
import toolbox.JavacTask;
import toolbox.JavapTask;
import toolbox.Task;
import toolbox.ToolBox;

public class AnnotationDefaultNewlineTest {

    private static final String TestSrc =
            "public @interface AnnotationDefault { \n" +
                "public abstract int value() default 1; \n" +
            "}";

    private static final String ExpectedSubstring =
            "    AnnotationDefault:\n" +
            "      default_value: I#9\n";

    public static void main(String[] args) throws Exception {
        ToolBox tb = new ToolBox();
        new JavacTask(tb).sources(TestSrc).run();

        List<String> res = new JavapTask(tb)
                .options("-v")
                .classes("AnnotationDefault.class")
                .run()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> goldenList = tb.split(ExpectedSubstring, "\n");
        Boolean found = Collections.indexOfSubList(res, goldenList) > -1;

        Assert.check(found, "expected output not found: " + ExpectedSubstring);
    }
}
