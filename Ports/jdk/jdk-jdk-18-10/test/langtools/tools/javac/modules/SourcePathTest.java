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

/**
 * @test
 * @bug 8176327
 * @summary javac produces wrong module-info
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main SourcePathTest
 */

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import toolbox.JavacTask;
import toolbox.Task;

public class SourcePathTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        SourcePathTest t = new SourcePathTest();
        t.runTests();
    }

    @Test
    public void test_unnamedModuleOnSourcePath_fileNotOnPath(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package p; public class A { }");
        Path otherSrc = base.resolve("otherSrc");
        tb.writeJavaFiles(otherSrc, "package p2; public class B { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
            .options("-sourcepath", src.toString())
            .outdir(classes)
            .files(findJavaFiles(otherSrc))
            .run()
            .writeAll();

        showFiles(classes);
    }

    @Test
    public void test_unnamedModuleOnSourcePath_fileOnPath(Path base) throws IOException {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package p; public class A { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
            .options("-sourcepath", src.toString())
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll();

        showFiles(classes);
    }

    @Test
    public void test_namedModuleOnSourcePath_fileNotOnPath_1(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeFile(src.resolve("module-info.java"), "module m { exports p; }");
        tb.writeJavaFiles(src, "package p; public class A { }");
        Path otherSrc = base.resolve("otherSrc");
        tb.writeJavaFiles(otherSrc, "package p2; public class B { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
            .options("-XDrawDiagnostics=true", "-sourcepath", src.toString())
            .outdir(classes)
            .files(findJavaFiles(otherSrc))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        showFiles(classes);
        checkOutputContains(log,
                "B.java:1:1: compiler.err.file.sb.on.source.or.patch.path.for.module");
    }

    @Test
    public void test_namedModuleOnSourcePath_fileNotOnPath_2(Path base) throws Exception {
        // This is the original test case:
        // the source path contains one module, but the file to be compiled appears to be
        // in another module.
        Path src = base.resolve("src");
        Path src_mA = src.resolve("mA");
        tb.writeJavaFiles(src_mA,
                "module mA { exports p; }",
                "package p; public class A { }");
        Path src_mB = src.resolve("mB");
        tb.writeJavaFiles(src_mB,
                "module mA { exports p2; }",
                "package p2; public class B { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
            .options("-XDrawDiagnostics=true", "-sourcepath", src_mA.toString())
            .outdir(classes)
            .files(findJavaFiles(src_mB.resolve("p2")))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        showFiles(classes);
        checkOutputContains(log,
                "B.java:1:1: compiler.err.file.sb.on.source.or.patch.path.for.module");
    }

    @Test
    public void test_namedModuleOnSourcePath_fileOnPath(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeFile(src.resolve("module-info.java"), "module m { exports p; }");
        tb.writeJavaFiles(src, "package p; public class A { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
            .options("-XDrawDiagnostics=true", "-sourcepath", src.toString())
            .outdir(classes)
            .files(findJavaFiles(src.resolve("p")))
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        showFiles(classes);
    }

    @Test
    public void test_namedModuleOnSourcePath_fileOnPatchPath(Path base) throws Exception {
        // similar to test_namedModuleOnSourcePath_fileNotOnPath_1
        // except that other src directory is not put on the patch path
        Path src = base.resolve("src");
        tb.writeFile(src.resolve("module-info.java"), "module m { exports p; }");
        tb.writeJavaFiles(src, "package p; public class A { }");
        Path otherSrc = base.resolve("otherSrc");
        tb.writeJavaFiles(otherSrc, "package p2; public class B { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
            .options("-XDrawDiagnostics=true",
                    "-sourcepath", src.toString(),
                    "--patch-module", "m=" + otherSrc)
            .outdir(classes)
            .files(findJavaFiles(otherSrc))
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        showFiles(classes);
    }

    /*
     * The following tests are not for the source path, but they exercise similar test
     * cases for the module source path.
     */

    @Test
    public void test_moduleSourcePath_fileNotOnPath(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_mA = src.resolve("mA");
        tb.writeJavaFiles(src_mA,
                "module mA { exports p; }",
                "package p; public class A { }");
        Path src_mB = src.resolve("mB");
        tb.writeJavaFiles(src_mB,
                "module mB { exports p2; }",
                "package p2; public class B { }");
        Path otherSrc = base.resolve("otherSrc");
        tb.writeJavaFiles(otherSrc, "package p3; public class C { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
            .options("-XDrawDiagnostics=true",
                    "--module-source-path", src.toString())
            .outdir(classes)
            .files(findJavaFiles(otherSrc))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        showFiles(classes);
        checkOutputContains(log,
                "C.java:1:1: compiler.err.not.in.module.on.module.source.path");
    }

    @Test
    public void test_moduleSourcePath_fileOnPath(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_mA = src.resolve("mA");
        tb.writeJavaFiles(src_mA,
                "module mA { exports p; }",
                "package p; public class A { }");
        Path src_mB = src.resolve("mB");
        tb.writeJavaFiles(src_mB,
                "module mB { exports p2; }",
                "package p2; public class B { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
            .options("-XDrawDiagnostics=true",
                    "--module-source-path", src.toString())
            .outdir(classes)
            .files(findJavaFiles(src_mB.resolve("p2")))
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        showFiles(classes);

    }

    @Test
    public void test_moduleSourcePath_fileOnPatchPath(Path base) throws Exception {
        // similar to test_moduleSourcePath_fileNotOnPath
        // except that other src directory is not put on the patch path
        Path src = base.resolve("src");
        Path src_mA = src.resolve("mA");
        tb.writeJavaFiles(src_mA,
                "module mA { exports p; }",
                "package p; public class A { }");
        Path src_mB = src.resolve("mB");
        tb.writeJavaFiles(src_mB,
                "module mB { exports p2; }",
                "package p2; public class B { }");
        Path otherSrc = base.resolve("otherSrc");
        tb.writeJavaFiles(otherSrc, "package p3; public class C { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        String log = new JavacTask(tb)
            .options("-XDrawDiagnostics=true",
                    "--module-source-path", src.toString(),
                    "--patch-module", "mA=" + otherSrc)
            .outdir(classes)
            .files(findJavaFiles(otherSrc))
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        showFiles(classes);
    }

    /**
     * This method is primarily used to give visual confirmation that a test case
     * generated files when the compilation succeeds and so generates no other output,
     * such as error messages.
     */
    List<Path> showFiles(Path dir) throws IOException {
        List<Path> files = new ArrayList<>();
        Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {
                if (Files.isRegularFile(file)) {
                    out.println("Found " + file);
                    files.add(file);
                }
                return FileVisitResult.CONTINUE;
            }
        });
        return files;
    }
}

