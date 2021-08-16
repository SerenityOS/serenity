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
 * @bug 8207032
 * @summary Test the --add-exports option
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main AddExportsTest
 */

import java.nio.file.Path;

import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Expect;
import toolbox.Task.OutputKind;

public class AddExportsTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new AddExportsTest().runTests();
    }

    @Test
    public void testEmpty(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);
        testEmpty(src, classes, "--add-exports", "");
        testEmpty(src, classes, "--add-exports=");
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
            "error: no value for --add-exports option");
    }

    @Test
    public void testEmptyItem(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { requires m1x; }",
                          "package p2; class C2 { p1.C1 c1; }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { requires m1x; }",
                          "package p3; class C3 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        testEmptyItem(src, classes, "m1x/p1=,m2x,m3x");
        testEmptyItem(src, classes, "m1x/p1=m2x,,m3x");
        testEmptyItem(src, classes, "m1x/p1=m2x,m3x,");
    }

    void testEmptyItem(Path src, Path classes, String option) throws Exception {
        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-exports", option)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testEmptyList(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        testEmptyList(src, classes, "m1x/p1=");
        testEmptyList(src, classes, "m1x/p1=,");
    }

    void testEmptyList(Path src, Path classes, String option) throws Exception {
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("--module-source-path", src.toString(),
                         "--add-exports", option)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "error: bad value for --add-exports option: '" + option + "'");
    }

    @Test
    public void testMissingSourceParts(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }",
                          "package p2; class C2 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        testMissingSourcePart(src, classes, "=m2x");
        testMissingSourcePart(src, classes, "/=m2x");
        testMissingSourcePart(src, classes, "m1x/=m2x");
        testMissingSourcePart(src, classes, "/p1=m2x");
        testMissingSourcePart(src, classes, "m1xp1=m2x");
    }

    private void testMissingSourcePart(Path src, Path classes, String option) throws Exception {
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("--module-source-path", src.toString(),
                         "--add-exports", option)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "error: bad value for --add-exports option: '" + option + "'");
    }

    @Test
    public void testBadSourceParts(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }",
                          "package p2; class C2 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        testBadSourcePart(src, classes, "m!/p1=m2x", "m!");
        testBadSourcePart(src, classes, "m1x/p!=m2x", "p!");
    }

    private void testBadSourcePart(Path src, Path classes, String option, String badName)
                throws Exception {
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                         "--module-source-path", src.toString(),
                         "--add-exports", option)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "- compiler.warn.bad.name.for.option: --add-exports, " + badName);
    }

    @Test
    public void testBadTarget(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                         "--module-source-path", src.toString(),
                         "--add-exports", "m1x/p1=m!")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "- compiler.warn.bad.name.for.option: --add-exports, m!");
    }

    @Test
    public void testSourceNotFound(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                         "--module-source-path", src.toString(),
                         "--add-exports", "DoesNotExist/p=m1x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "- compiler.warn.module.for.option.not.found: --add-exports, DoesNotExist");
    }

    @Test
    public void testTargetNotFound(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }",
                          "package p1; class C1 { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                         "--module-source-path", src.toString(),
                         "--add-exports", "m1x/p1=DoesNotExist")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "- compiler.warn.module.for.option.not.found: --add-exports, DoesNotExist");
    }

    @Test
    public void testDuplicate(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { requires m1x; }",
                          "package p2; class C2 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-exports", "m1x/p1=m2x,m2x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testRepeated_SameTarget(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { requires m1x; }",
                          "package p2; class C2 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-exports", "m1x/p1=m2x",
                         "--add-exports", "m1x/p1=m2x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testRepeated_DifferentTarget(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { requires m1x; }",
                          "package p2; class C2 { p1.C1 c1; }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { requires m1x; }",
                          "package p3; class C3 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-exports", "m1x/p1=m2x",
                         "--add-exports", "m1x/p1=m3x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testNoReads(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }",
                          "package p2; class C2 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log;

        log = new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Expect.FAIL)
                .writeAll()
                .getOutput(OutputKind.DIRECT);

        checkOutputContains(log,
            "C2.java:1:24: compiler.err.package.not.visible: p1, (compiler.misc.not.def.access.does.not.read: m2x, p1, m1x)");

        log = new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "-XDrawDiagnostics",
                         "--add-exports", "m1x/p1=m2x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Expect.FAIL)
                .writeAll()
                .getOutput(OutputKind.DIRECT);

        checkOutputContains(log,
            "C2.java:1:24: compiler.err.package.not.visible: p1, (compiler.misc.not.def.access.does.not.read: m2x, p1, m1x)");

        Path mp = base.resolve("mp");
        tb.createDirectories(mp);

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "-XDrawDiagnostics",
                         "--add-exports", "m1x/p1=m2x",
                         "--add-reads", "m2x=m1x")
                .outdir(mp)
                .files(findJavaFiles(src))
                .run(Expect.SUCCESS)
                .writeAll();

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-exports", "m1x/p1=m2x",
                         "--add-reads", "m2x=m1x",
                         "--module-path", mp.toString())
                .outdir(classes)
                .files(findJavaFiles(src_m2))
                .run(Expect.FAIL)
                .writeAll()
                .getOutput(OutputKind.DIRECT);

        checkOutputContains(log,
            "C2.java:1:24: compiler.err.package.not.visible: p1, (compiler.misc.not.def.access.does.not.read: m2x, p1, m1x)");
        checkOutputContains(log,
            "- compiler.warn.module.for.option.not.found: --add-reads, m1x");
        checkOutputContains(log,
            "- compiler.warn.module.for.option.not.found: --add-exports, m1x");

        new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-exports", "m1x/p1=m2x",
                         "--add-reads", "m2x=m1x",
                         "--module-path", mp.toString(),
                         "--add-modules", "m1x")
                .outdir(classes)
                .files(findJavaFiles(src_m2))
                .run(Expect.SUCCESS)
                .writeAll()
                .getOutput(OutputKind.DIRECT);
    }
}
