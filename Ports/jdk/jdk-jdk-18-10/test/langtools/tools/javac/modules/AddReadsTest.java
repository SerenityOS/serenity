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
 * @summary Test the --add-reads option
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          java.desktop
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask toolbox.JavapTask ModuleTestBase
 * @run main AddReadsTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.JavapTask;
import toolbox.Task;

public class AddReadsTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new AddReadsTest().runTests();
    }

    @Test
    public void testAddReads(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports api; }",
                          "package api; public class Api { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }",
                          "package test; public class Test extends api.Api { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "Test.java:1:41: compiler.err.package.not.visible: api, (compiler.misc.not.def.access.does.not.read: m2x, api, m1x)");

        //test add dependencies:
        new JavacTask(tb)
                .options("--add-reads", "m2x=m1x",
                         "--module-source-path", src.toString(),
                         "-processor", VerifyRequires.class.getName())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        String decompiled = new JavapTask(tb)
                .options("-verbose",
                        classes.resolve("m2x").resolve("module-info.class").toString())
                .run()
                .getOutput(Task.OutputKind.DIRECT);

        if (decompiled.contains("m1x")) {
            throw new Exception("Incorrectly refers to m1x module.");
        }

        //cyclic dependencies OK when created through addReads:
        new JavacTask(tb)
                .options("--add-reads", "m2x=m1x",
                         "--add-reads", "m1x=m2x",
                         "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        tb.writeJavaFiles(src_m2,
                          "module m2x { requires m1x; }");

        new JavacTask(tb)
                .options("--add-reads", "m1x=m2x",
                         "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @SupportedAnnotationTypes("*")
    public static final class VerifyRequires extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            ModuleElement m2Module = processingEnv.getElementUtils().getModuleElement("m2x");
            if (m2Module == null) {
                throw new AssertionError("Cannot find the m2x module!");
            }
            boolean foundM1 = false;
            for (RequiresDirective rd : ElementFilter.requiresIn(m2Module.getDirectives())) {
                foundM1 |= rd.getDependency().getSimpleName().contentEquals("m1x");
            }
            if (!foundM1) {
                throw new AssertionError("Cannot find the dependency on m1x module!");
            }
            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testAddReadsUnnamedModule(Path base) throws Exception {
        Path jar = prepareTestJar(base);

        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m1x { }",
                          "package impl; public class Impl { api.Api api; }");

        new JavacTask(tb)
          .options("--class-path", jar.toString(),
                   "--add-reads", "m1x=ALL-UNNAMED",
                   "-XDrawDiagnostics")
          .outdir(classes)
          .files(findJavaFiles(moduleSrc))
          .run()
          .writeAll();
    }

    @Test
    public void testAddReadsUnnamedModulePackageConflict(Path base) throws Exception {
        Path jar = prepareTestJar(base);

        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(m1,
                          "module m1x { }",
                          "package api; public class Api { public static void test() { } }",
                          "package impl; public class Impl { { api.Api.test(); } }");

        new JavacTask(tb)
          .options("--class-path", jar.toString(),
                   "--module-source-path", moduleSrc.toString(),
                   "--add-reads", "m1x=ALL-UNNAMED",
                   "-XDrawDiagnostics")
          .outdir(classes)
          .files(m1.resolve("impl").resolve("Impl.java"))
          .run()
          .writeAll();
    }

    @Test
    public void testAddReadsUnnamedToJavaBase(Path base) throws Exception {
        Path jar = prepareTestJar(base);
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(src,
                          "package impl; public class Impl { api.Api a; }");

        new JavacTask(tb)
          .options("--class-path", jar.toString(),
                   "--add-reads", "java.base=ALL-UNNAMED",
                   "--patch-module", "java.base=" + src)
          .outdir(classes)
          .files(src.resolve("impl").resolve("Impl.java"))
          .run()
          .writeAll();
    }

    @Test
    public void testAddReadsToJavaBase(Path base) throws Exception {
        Path src = base.resolve("src");
        Path classes = base.resolve("classes");

        Files.createDirectories(classes);

        tb.writeJavaFiles(src,
                          "package impl; public class Impl { javax.swing.JButton b; }");

        new JavacTask(tb)
          .options("--add-modules", "java.desktop",
                   "--add-reads", "java.base=java.desktop",
                   "--patch-module", "java.base=" + src)
          .outdir(classes)
          .files(findJavaFiles(src))
          .run()
          .writeAll();
    }

    private Path prepareTestJar(Path base) throws Exception {
        Path legacySrc = base.resolve("legacy-src");
        tb.writeJavaFiles(legacySrc,
                          "package api; public abstract class Api {}");
        Path legacyClasses = base.resolve("legacy-classes");
        Files.createDirectories(legacyClasses);

        String log = new JavacTask(tb)
                .options()
                .outdir(legacyClasses)
                .files(findJavaFiles(legacySrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty()) {
            throw new Exception("unexpected output: " + log);
        }

        Path lib = base.resolve("lib");

        Files.createDirectories(lib);

        Path jar = lib.resolve("test-api-1.0.jar");

        new JarTask(tb, jar)
          .baseDir(legacyClasses)
          .files("api/Api.class")
          .run();

        return jar;
    }

    @Test
    public void testX(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { provides java.lang.Runnable with impl.Impl; }",
                          "package impl; public class Impl implements Runnable { public void run() { } }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        Path unnamedSrc = base.resolve("unnamed-src");
        Path unnamedClasses = base.resolve("unnamed-classes");

        Files.createDirectories(unnamedClasses);

        tb.writeJavaFiles(unnamedSrc,
                          "package impl; public class Impl { }");

        new JavacTask(tb)
          .options("--add-reads", "m1x=ALL-UNNAMED",
                   "--patch-module", "m1x=" + unnamedSrc,
                   "--module-path", classes.toString())
          .outdir(unnamedClasses)
          .files(findJavaFiles(unnamedSrc))
          .run()
          .writeAll();
    }

    @Test
    public void testAddSelf(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-reads", "m1x=m1x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testEmpty(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        testEmpty(src, classes, "--add-reads", "");
        testEmpty(src, classes, "--add-reads=");
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
            "error: no value for --add-reads option");
    }

    @Test
    public void testEmptyItem(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }",
                          "package p2; class C2 { }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { }",
                          "package p3; class C3 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        testEmptyItem(src, classes, "m3x=,m1x");
        testEmptyItem(src, classes, "m3x=m1x,,m2x");
        testEmptyItem(src, classes, "m3x=m1x,");
    }

    private void testEmptyItem(Path src, Path classes, String option) throws Exception {
        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-reads", option)
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
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }",
                          "package p2; class C2 { }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { }",
                          "package p3; class C3 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        testEmptyList(src, classes, "m3x=");
        testEmptyList(src, classes, "m3x=,");
    }

    private void testEmptyList(Path src, Path classes, String option) throws Exception {
        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("--module-source-path", src.toString(),
                         "--add-reads", option)
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "error: bad value for --add-reads option: '" + option + "'");
    }

    @Test
    public void testMultipleAddReads_DifferentModules(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }",
                          "package p2; class C2 { p1.C1 c1; }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { }",
                          "package p3; class C3 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-reads", "m2x=m1x",
                         "--add-reads", "m3x=m1x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testMultipleAddReads_SameModule(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { exports p2; }",
                          "package p2; public class C2 { }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { }",
                          "package p3; class C3 { p1.C1 c1; p2.C2 c2; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-reads", "m3x=m1x",
                         "--add-reads", "m3x=m2x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testDuplicateAddReads_SameOption(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { exports p2; }",
                          "package p2; class C2 { p1.C1 c1; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-reads", "m2x=m1x,m1x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testDuplicateAddReads_MultipleOptions(Path base) throws Exception {
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

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-reads", "m2x=m1x",
                         "--add-reads", "m2x=m1x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testRepeatedAddReads(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { exports p2; }",
                          "package p2; public class C2 { }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { }",
                          "package p3; class C3 { p1.C1 c1; p2.C2 c2; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "--add-reads", "m3x=m1x",
                         "--add-reads", "m3x=m2x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testNoEquals(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                         "--add-reads", "m1x:m2x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "error: bad value for --add-reads option: 'm1x:m2x'");
    }

    @Test
    public void testBadSourceName(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "class Dummy { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-reads", "bad*Source=m2x")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "- compiler.warn.bad.name.for.option: --add-reads, bad*Source");
    }

    @Test
    public void testBadTargetName(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }",
                          "package p1; class C1 { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-reads", "m1x=badTarget!")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "- compiler.warn.bad.name.for.option: --add-reads, badTarget!");
    }

    @Test
    public void testSourceNameNotFound(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-reads", "missingSource=m")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "- compiler.warn.module.for.option.not.found: --add-reads, missingSource");
    }

    @Test
    public void testTargetNameNotFound(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports p1; }",
                          "package p1; public class C1 { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--add-reads", "m1x=missingTarget")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        checkOutputContains(log,
            "- compiler.warn.module.for.option.not.found: --add-reads, missingTarget");
    }
}
