/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8164519
 * @summary Verify that javac emits proper super type index (65535) for an annotated extends
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavapTask
 * @run compile -g AnnotatedExtendsTest.java
 * @run main AnnotatedExtendsTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

import toolbox.JavapTask;
import toolbox.Task;
import toolbox.ToolBox;

public class AnnotatedExtendsTest {

    @Target(ElementType.TYPE_USE)
    public @interface TA {
    };

    public class Inner extends @TA Object {}

    public static strictfp void main(String args[]) throws Exception {
        ToolBox tb = new ToolBox();
        Path classPath = Paths.get(ToolBox.testClasses, "AnnotatedExtendsTest$Inner.class");
        String javapOut = new JavapTask(tb)
                .options("-v", "-p")
                .classes(classPath.toString())
                .run()
                .getOutput(Task.OutputKind.DIRECT);
        if (!javapOut.contains("0: #22(): CLASS_EXTENDS, type_index=65535"))
            throw new AssertionError("Expected output missing: " + javapOut);
    }
}