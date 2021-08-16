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
 * @bug 8024437
 * @summary Inferring the exception thrown by a lambda: sometimes fails to compile
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main ExceptionInferenceFromClassFileTest
 */

import java.nio.file.Files;
import java.nio.file.Paths;

import toolbox.JavacTask;
import toolbox.ToolBox;

public class ExceptionInferenceFromClassFileTest {

    static final String ABSrc =
            "class B {\n" +
            "    public static <E extends Throwable> void t(A<E> a) throws E {\n" +
            "        a.run();\n" +
            "    }\n" +
            "}\n" +

            "interface A<E extends Throwable> {\n" +
            "    void run() throws E;\n" +
            "}";

    static final String CSrc =
            "class C {\n" +
            "    public void d() {\n" +
            "        B.t(null);\n" +
            "    }\n" +
            "}";

    public static void main(String[] args) throws Exception {
        ToolBox tb = new ToolBox();
        tb.createDirectories("out");

        new JavacTask(tb)
                .outdir("out")
                .sources(ABSrc)
                .run();

        new JavacTask(tb)
                .outdir("out")
                .classpath("out")
                .sources(CSrc)
                .run();
    }

}
