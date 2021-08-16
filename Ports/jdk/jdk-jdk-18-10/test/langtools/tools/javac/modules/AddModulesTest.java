/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8167975 8173596
 * @summary Test the --add-modules option
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.Assert toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main AddModulesTest
 */


import java.nio.file.Path;
import java.util.Arrays;

import javax.tools.JavaCompiler;
import javax.tools.JavaCompiler.CompilationTask;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import toolbox.Assert;
import toolbox.JavacTask;
import toolbox.Task;

public class AddModulesTest extends ModuleTestBase {
    public static void main(String... args) throws Exception {
        new AddModulesTest().runTests();
    }

    @Test
    public void testEmpty(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        testEmpty(src, classes, "--add-modules", "");
        testEmpty(src, classes, "--add-modules=");
    }

    private void testEmpty(Path src, Path classes, String... options) throws Exception {
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options(options)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "error: no value for --add-modules option");
    }

    @Test
    public void testEmptyItem(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        testEmptyItem(src, classes, ",m1x");
        testEmptyItem(src, classes, "m1x,,m2x");
        testEmptyItem(src, classes, "m1x,");
    }

    private void testEmptyItem(Path src, Path classes, String option) throws Exception {
        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-modules", option)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testEmptyList(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("--module-source-path", src.toString(),
                         "--add-modules", ",")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "error: bad value for --add-modules option");
    }

    @Test
    public void testInvalidName(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-modules", "BadModule!")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "- compiler.err.bad.name.for.option: --add-modules, BadModule!");
    }

    @Test
    public void testUnknownName(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-modules", "DoesNotExist")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "- compiler.err.module.not.found: DoesNotExist");
    }

    @Test
    public void testDuplicate(Path base) throws Exception {
        Path src = base.resolve("src");

        // setup a utility module
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb)
                .options("--module-source-path", src.toString())
                .outdir(modules)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        // now test access to the module
        Path src2 = base.resolve("src2");
        tb.writeJavaFiles(src2,
                          "class Dummy { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-path", modules.toString(),
                         "--add-modules", "m1x,m1x")
                .outdir(classes)
                .files(findJavaFiles(src2))
                .run()
                .writeAll();
    }

    @Test
    public void testRepeatable(Path base) throws Exception {
        Path src = base.resolve("src");

        // setup some utility modules
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { exports p2; }",
                          "package p2; public class C2 { }");
        Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb)
                .options("--module-source-path", src.toString())
                .outdir(modules)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        // now test access to the modules
        Path src2 = base.resolve("src2");
        tb.writeJavaFiles(src2,
                          "class Dummy { p1.C1 c1; p2.C2 c2; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-path", modules.toString(),
                         "--add-modules", "m1x",
                         "--add-modules", "m2x")
                .outdir(classes)
                .files(findJavaFiles(src2))
                .run()
                .writeAll();
    }

    @Test
    public void testAddModulesAPI(Path base) throws Exception {
        Path src = base.resolve("src");

        // setup some utility modules
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { exports p2; }",
                          "package p2; public class C2 { }");
        Path modules = base.resolve("modules");
        tb.createDirectories(modules);

        new JavacTask(tb)
                .options("--module-source-path", src.toString())
                .outdir(modules)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        // now test access to the modules
        Path src2 = base.resolve("src2");
        tb.writeJavaFiles(src2,
                          "class Dummy { p1.C1 c1; p2.C2 c2; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            fm.setLocationFromPaths(StandardLocation.MODULE_PATH, Arrays.asList(modules));
            fm.setLocationFromPaths(StandardLocation.CLASS_OUTPUT, Arrays.asList(classes));
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(findJavaFiles(src2));
            CompilationTask t = c.getTask(null, fm, null, null, null, files);
            t.addModules(Arrays.asList("m1x", "m2x"));
            Assert.check(t.call());
        }
    }
}

