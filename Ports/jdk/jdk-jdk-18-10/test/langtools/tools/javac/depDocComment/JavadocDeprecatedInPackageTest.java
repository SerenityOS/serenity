/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8160601
 * @summary Verify deprecated javadoc tag in package-info does not crash javac.
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask
 * @run main JavadocDeprecatedInPackageTest
 */

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.TestRunner;
import toolbox.ToolBox;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

public class JavadocDeprecatedInPackageTest extends TestRunner {

    protected ToolBox tb;

    JavadocDeprecatedInPackageTest() {
        super(System.err);
        tb = new ToolBox();
    }

    public static void main(String... args) throws Exception {
        JavadocDeprecatedInPackageTest t = new JavadocDeprecatedInPackageTest();
        t.runTests(m -> new Object[] { Paths.get(m.getName()) });
    }

    @Test
    public void javadocDeprecatedTag(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "/** @deprecated message */ @Deprecated package p1;",
                          "/** @deprecated message */ package p2;");

        List<String> actual =
                new JavacTask(tb, Task.Mode.CMDLINE)
                    .options("-Xlint:deprecation",
                             "-XDrawDiagnostics")
                    .files(tb.findJavaFiles(src))
                    .run(Task.Expect.SUCCESS)
                    .writeAll()
                    .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = List.of("package-info.java:1:48: compiler.warn.deprecated.annotation.has.no.effect: kindname.package",
                                        "1 warning");

        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Unexpected log output: " + actual);
        }
    }
}
