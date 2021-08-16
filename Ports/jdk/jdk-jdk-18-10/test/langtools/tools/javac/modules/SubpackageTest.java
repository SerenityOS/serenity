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

/*
 * @test
 * @summary tests for subpackage issues
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main SubpackageTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class SubpackageTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        SubpackageTest t = new SubpackageTest();
        t.runTests();
    }

    @Test // based on JDK-8075435
    public void testUnnamedModule(Path base) throws Exception {
        Path libsrc = base.resolve("lib/src");
        tb.writeJavaFiles(libsrc,
            "package p; public class E extends Error { }");
        Path libclasses = base.resolve("lib/classes");
        Files.createDirectories(libclasses);
        new JavacTask(tb)
                .outdir(libclasses)
                .files(findJavaFiles(libsrc))
                .run()
                .writeAll();

        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
            """
                package p.q;
                import p.E;
                class Test {
                  void m() { throw new E(); }
                }""");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
                .classpath(libclasses)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testSimpleMulti(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src.resolve("mp"),
                "module mp { exports p; }",
                "package p; public class C1 { }");
        tb.writeJavaFiles(src.resolve("mpq"),
                "module mpq { exports p.q; }",
                "package p.q; public class C2 { }");
        tb.writeJavaFiles(src.resolve("mpqr"),
                "module mpqr { exports p.q.r; }",
                "package p.q.r; public class C3 { }");
        tb.writeJavaFiles(src.resolve("m"),
                """
                    module m {  requires mp;
                      requires mpq;
                      requires mpqr;
                    }""",
                """
                    package x;
                    class C {
                      p.C1 c1;
                      p.q.C2 c2;
                      p.q.r.C3 c3;
                    }""");
        Path modules = base.resolve("modules");
        Files.createDirectories(modules);

        new JavacTask(tb)
                .options("--module-source-path", src.toString())
                .outdir(modules)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

}
