/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8144168 8148432
 * @summary No type annotations generated for nested lambdas
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavapTask
 * @run compile -source 16 -target 16 -g NestedLambdasCastedTest.java
 * @run main NestedLambdasCastedTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import toolbox.JavapTask;
import toolbox.Task;
import toolbox.ToolBox;

public class NestedLambdasCastedTest {

    // Expected output can't be directly encoded into NestedLambdasCastedTest !!!
    static class ExpectedOutputHolder {
        public String[] outputs = {
                      "public static strictfp void main(java.lang.String[])",
                      "private static strictfp void lambda$main$3();",
                      "private static strictfp void lambda$main$2();",
                      "private static strictfp void lambda$main$1();",
                      "private static strictfp void lambda$main$0();",
                      "0: #111(#112=s#113): CAST, offset=5, type_index=0",
                      "0: #111(#112=s#119): CAST, offset=5, type_index=0",
                      "0: #111(#112=s#122): CAST, offset=5, type_index=0",
                      "0: #111(#112=s#125): CAST, offset=5, type_index=0"
        };
    }

    @Target(ElementType.TYPE_USE)
    public @interface TA {
        String value() default "";
    };

    public static strictfp void main(String args[]) throws Exception {
        Runnable one = (@TA("1") Runnable) () -> {
            Runnable two = (@TA("2") Runnable) () -> {
                Runnable three = (@TA("3") Runnable) () -> {
                    Runnable four = (@TA("4") Runnable) () -> {
                    };
                };
            };
        };
        ToolBox tb = new ToolBox();
        Path classPath = Paths.get(ToolBox.testClasses, "NestedLambdasCastedTest.class");
        String javapOut = new JavapTask(tb)
                .options("-v", "-p")
                .classes(classPath.toString())
                .run()
                .getOutput(Task.OutputKind.DIRECT);
        ExpectedOutputHolder holder = new ExpectedOutputHolder();
        for (String s : holder.outputs) {
            if (!javapOut.contains(s))
                throw new AssertionError("Expected type annotation on LOCAL_VARIABLE missing");
        }
    }
}
