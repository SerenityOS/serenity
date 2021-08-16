/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8158224
 * @summary NullPointerException in com.sun.tools.javac.comp.Modules.checkCyclicDependencies when module missing
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @build Processor
 * @run main T8158224
 */

// previously:
// @compile/fail/ref=T8158224.out -XDrawDiagnostics -processor Processor mods/foo/module-info.java

import java.util.List;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class T8158224 {
    public static void main(String... args) throws Exception {
        ToolBox tb = new ToolBox();

        List<String> log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                        "-processor", "Processor",
                        "-sourcepath", tb.testSrc + "/mods/foo",
                        "-classpath", tb.testClasses)
                .files(tb.testSrc + "/mods/foo/module-info.java")
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!log.equals(List.of(
                "module-info.java:4:14: compiler.err.module.not.found: nonexistent",
                "1 error"))) {
            throw new Exception("Expected output not found");
        }
    }
}
