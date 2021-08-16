/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8144342 8149658 8162713
 * @summary javac doesn't report errors if module exports non-existent package
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main ReportNonExistentPackageTest
 */

import java.nio.file.Files;
import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.ToolBox;

public class ReportNonExistentPackageTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        ReportNonExistentPackageTest t = new ReportNonExistentPackageTest();
        t.runTests();
    }

    @Test
    public void testExportUnknownPackage(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module m { exports p1; }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.contains("module-info.java:1:20: compiler.err.package.empty.or.not.found: p1"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testExportEmptyPackage(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { exports p1; }",
                "package p1;");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.contains("module-info.java:1:20: compiler.err.package.empty.or.not.found: p1"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testPackageWithMemberWOPackageDeclaration(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "module m { exports p1; }");
        Path p1 = src.resolve("p1");
        Path C = p1.resolve("C.java");
        tb.writeFile(C, "// comment");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.contains("module-info.java:1:20: compiler.err.package.empty.or.not.found: p1"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testOpensEmptyPackage(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { opens p; }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.contains("module-info.java:1:18: compiler.warn.package.empty.or.not.found: p"))
            throw new Exception("expected output not found, actual output: " + log);
    }

    @Test
    public void testOpensEmptyPackageSuppressed(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "@SuppressWarnings(\"opens\") module m { opens p; }");
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.SUCCESS)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.equals(""))
            throw new Exception("expected output not found, actual output: " + log);
    }

    @Test
    public void testOpenOnlyWithResources(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                "module m { opens p; }");
        Path resource = src.resolve("p").resolve("resource.properties");
        Files.createDirectories(resource.getParent());
        Files.newOutputStream(resource).close();
        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
                .sourcepath(src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);
        if (!log.equals(""))
            throw new Exception("expected output not found, actual output: " + log);
    }
}
