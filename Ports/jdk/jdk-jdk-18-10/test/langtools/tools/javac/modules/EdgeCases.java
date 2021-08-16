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

/*
 * @test
 * @bug 8154283 8167320 8171098 8172809 8173068 8173117 8176045 8177311 8241519
 * @summary tests for multi-module mode compilation
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.processing
 *      jdk.compiler/com.sun.tools.javac.util
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask ModuleTestBase
 * @run main EdgeCases
 */

import java.io.BufferedWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.ModuleElement.RequiresDirective;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.ElementFilter;
import javax.lang.model.util.Elements;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.CompilationUnitTree;
//import com.sun.source.util.JavacTask; // conflicts with toolbox.JavacTask
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.code.Symbol.ModuleSymbol;
import com.sun.tools.javac.code.Symtab;

import toolbox.JarTask;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.Expect;
import toolbox.Task.OutputKind;

public class EdgeCases extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new EdgeCases().runTests();
    }

    @Test
    public void testAddExportUndefinedModule(Path base) throws Exception {
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package test; import undefPackage.Any; public class Test {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("--add-exports", "undefModule/undefPackage=ALL-UNNAMED",
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList("- compiler.warn.module.for.option.not.found: --add-exports, undefModule",
                                              "Test.java:1:34: compiler.err.doesnt.exist: undefPackage",
                                              "1 error", "1 warning");

        if (!expected.equals(log))
            throw new Exception("expected output not found: " + log);
    }

    @Test
    public void testModuleSymbolOutterMostClass(Path base) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            Path moduleSrc = base.resolve("module-src");
            Path m1 = moduleSrc.resolve("m1x");

            tb.writeJavaFiles(m1, "module m1x { }");

            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(findJavaFiles(moduleSrc));
            com.sun.source.util.JavacTask task =
                (com.sun.source.util.JavacTask) compiler.getTask(null, fm, null, null, null, files);

            task.analyze();

            ModuleSymbol msym = (ModuleSymbol) task.getElements().getModuleElement("m1x");

            msym.outermostClass();
        }
    }

    @Test
    public void testParseEnterAnalyze(Path base) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            Path moduleSrc = base.resolve("module-src");
            Path m1 = moduleSrc.resolve("m1x");

            tb.writeJavaFiles(m1, "module m1x { }",
                                  "package p;",
                                  "package p; class T { }");

            Path classes = base.resolve("classes");
            Iterable<? extends JavaFileObject> files = fm.getJavaFileObjects(findJavaFiles(moduleSrc));
            List<String> options = Arrays.asList("-d", classes.toString(), "-Xpkginfo:always");
            JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, fm, null, options, null, files);

            Iterable<? extends CompilationUnitTree> parsed = task.parse();
            Iterable<? extends Element> entered = task.enter(parsed);
            Iterable<? extends Element> analyzed = task.analyze(entered);
            Iterable<? extends JavaFileObject> generatedFiles = task.generate(analyzed);

            Set<String> generated = new HashSet<>();

            for (JavaFileObject jfo : generatedFiles) {
                generated.add(jfo.getName());
            }

            Set<String> expected = new HashSet<>(
                    Arrays.asList(Paths.get("testParseEnterAnalyze", "classes", "p", "package-info.class").toString(),
                                  Paths.get("testParseEnterAnalyze", "classes", "module-info.class").toString(),
                                  Paths.get("testParseEnterAnalyze", "classes", "p", "T.class").toString())
            );

            if (!Objects.equals(expected, generated))
                throw new AssertionError("Incorrect generated files: " + generated);
        }
    }

    @Test
    public void testModuleImplicitModuleBoundaries(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports api1; }",
                          "package api1; public class Api1 { public void call() { } }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { requires m1x; exports api2; }",
                          "package api2; public class Api2 { public static api1.Api1 get() { return null; } }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { requires m2x; }",
                          "package test; public class Test { { api2.Api2.get().call(); api2.Api2.get().toString(); } }");
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

        if (!log.contains("Test.java:1:52: compiler.err.not.def.access.class.intf.cant.access.reason: call(), api1.Api1, api1, (compiler.misc.not.def.access.does.not.read: m3x, api1, m1x)") ||
            !log.contains("Test.java:1:76: compiler.err.not.def.access.class.intf.cant.access: toString(), java.lang.Object"))
            throw new Exception("expected output not found");
    }

    @Test
    public void testAssignClassToAutomaticModule(Path base) throws Exception {
        //check that if a ClassSymbol belongs to an automatic module, it is properly assigned and not
        //duplicated when being accessed through a classfile.
        Path automaticSrc = base.resolve("automaticSrc");
        tb.writeJavaFiles(automaticSrc, "package api1; public class Api1 {}");
        Path automaticClasses = base.resolve("automaticClasses");
        tb.createDirectories(automaticClasses);

        String automaticLog = new JavacTask(tb)
                                .outdir(automaticClasses)
                                .files(findJavaFiles(automaticSrc))
                                .run()
                                .writeAll()
                                .getOutput(Task.OutputKind.DIRECT);

        if (!automaticLog.isEmpty())
            throw new Exception("expected output not found: " + automaticLog);

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        Path automaticJar = modulePath.resolve("a-1.0.jar");

        new JarTask(tb, automaticJar)
          .baseDir(automaticClasses)
          .files("api1/Api1.class")
          .run();

        Path src = base.resolve("src");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { requires a; exports api2; }",
                          "package api2; public class Api2 { public static api1.Api1 get() { return null; } }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { requires a; requires m2x; }",
                          "package test; public class Test { { api2.Api2.get(); api1.Api1 a1; } }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src_m2))
                .run()
                .writeAll();

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src_m3))
                .run()
                .writeAll();
    }

    @Test
    public void testEmptyImplicitModuleInfo(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        Files.createDirectories(src_m1);
        try (Writer w = Files.newBufferedWriter(src_m1.resolve("module-info.java"))) {}
        tb.writeJavaFiles(src_m1,
                          "package test; public class Test {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("--source-path", src_m1.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src_m1.resolve("test")))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
                "- compiler.err.cant.access: module-info, (compiler.misc.bad.source.file.header: module-info.java, (compiler.misc.file.does.not.contain.module))",
                "1 error");

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        tb.writeJavaFiles(src_m1,
                          "module m1x {}");

        new JavacTask(tb)
                .options("--source-path", src_m1.toString())
                .outdir(classes)
                .files(findJavaFiles(src_m1.resolve("test")))
                .run()
                .writeAll();

    }

    @Test
    public void testClassPackageClash(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { exports test.m1x; }",
                          """
                              package test.m1x;
                              public class Test {}
                              """);
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { requires m1x; }",
                          """
                              package test;
                              public class m1x {}
                              """);
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
            "m1x.java:2:8: compiler.err.clash.with.pkg.of.same.name: kindname.class, test.m1x",
            "1 error"
        );

        if (!expected.equals(log)) {
            throw new IllegalStateException(log.toString());
        }
    }

    @Test
    public void testImplicitJavaBase(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_java_base = src.resolve("java.base");
        Files.createDirectories(src_java_base);
        tb.writeJavaFiles(src_java_base, "module java.base { exports java.lang; }");
        tb.writeJavaFiles(src_java_base,
                          "package java.lang; public class Object {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        //module-info from source:
        new JavacTask(tb)
            .options("-sourcepath", src_java_base.toString())
            .outdir(classes)
            .files(findJavaFiles(src_java_base.resolve("java").resolve("lang").resolve("Object.java")))
            .run()
            .writeAll();

        //module-info from class:
        if (!Files.exists(classes.resolve("module-info.class"))) {
            throw new AssertionError("module-info.class not created!");
        }

        new JavacTask(tb)
            .outdir(classes)
            .files(findJavaFiles(src_java_base.resolve("java").resolve("lang").resolve("Object.java")))
            .run()
            .writeAll();

        //broken module-info.class:
        Files.newOutputStream(classes.resolve("module-info.class")).close();

        List<String> log = new JavacTask(tb)
            .options("-XDrawDiagnostics")
            .outdir(classes)
            .files(findJavaFiles(src_java_base.resolve("java").resolve("lang").resolve("Object.java")))
            .run(Expect.FAIL)
            .writeAll()
            .getOutputLines(OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
                "- compiler.err.cant.access: <error>.module-info, (compiler.misc.bad.class.file.header: module-info.class, (compiler.misc.illegal.start.of.class.file))",
                "1 error");

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }

        //broken module-info.java:
        Files.delete(classes.resolve("module-info.class"));

        try (Writer out = Files.newBufferedWriter(src_java_base.resolve("module-info.java"))) {
            out.write("class Broken {}");
        }

        log = new JavacTask(tb)
            .options("-sourcepath", src_java_base.toString(),
                                "-XDrawDiagnostics")
            .outdir(classes)
            .files(findJavaFiles(src_java_base.resolve("java").resolve("lang").resolve("Object.java")))
            .run(Expect.FAIL)
            .writeAll()
            .getOutputLines(OutputKind.DIRECT);

        expected = Arrays.asList("X");

        if (expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testModuleInfoNameMismatchSource(Path base) throws Exception {
        Path src = base.resolve("src");
        Path m1 = src.resolve("m1x");
        Files.createDirectories(m1);
        tb.writeJavaFiles(m1, "module other { }",
                              "package test; public class Test {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log = new JavacTask(tb)
            .options("--module-source-path", src.toString(),
                     "-XDrawDiagnostics")
            .outdir(classes)
            .files(findJavaFiles(m1.resolve("test").resolve("Test.java")))
            .run(Expect.FAIL)
            .writeAll()
            .getOutputLines(OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
                "module-info.java:1:1: compiler.err.module.name.mismatch: other, m1x",
                "- compiler.err.cant.access: m1x.module-info, (compiler.misc.cant.resolve.modules)",
                "2 errors");

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testModuleInfoNameMismatchClass(Path base) throws Exception {
        Path src = base.resolve("src");
        Files.createDirectories(src);
        tb.writeJavaFiles(src, "module other { }",
                               "package test; public class Test {}");
        Path classes = base.resolve("classes");
        Path m1Classes = classes.resolve("m1x");
        tb.createDirectories(m1Classes);

        new JavacTask(tb)
            .outdir(m1Classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll()
            .getOutputLines(OutputKind.DIRECT);

        Path src2 = base.resolve("src2");
        Files.createDirectories(src2);
        tb.writeJavaFiles(src2, "module use { requires m1x; }");

        Path classes2 = base.resolve("classes2");
        tb.createDirectories(classes2);

        List<String> log = new JavacTask(tb)
            .options("--module-path", classes.toString(),
                     "-XDrawDiagnostics")
            .outdir(classes2)
            .files(findJavaFiles(src2))
            .run(Expect.FAIL)
            .writeAll()
            .getOutputLines(OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
                "- compiler.err.cant.access: m1x.module-info, (compiler.misc.bad.class.file.header: module-info.class, (compiler.misc.module.name.mismatch: other, m1x))",
                "module-info.java:1:1: compiler.err.module.not.found: m1x",
                "2 errors");

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testGetDirectivesComplete(Path base) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        JavacTaskImpl task = (JavacTaskImpl) compiler.getTask(null, null, null, null, null, null);
        Symtab syms = Symtab.instance(task.getContext());

        syms.java_base.getDirectives();
    }

    @Test
    public void testPackageInModuleInfo(Path base) throws Exception {
        Path src = base.resolve("src");
        Files.createDirectories(src);
        tb.writeJavaFiles(src, "package p; module foo { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log = new JavacTask(tb)
            .options("-XDrawDiagnostics", "-XDshould-stop.ifError=FLOW")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Expect.FAIL)
            .writeAll()
            .getOutputLines(OutputKind.DIRECT);

        List<String> expected = Arrays.asList(
                "module-info.java:1:1: compiler.err.no.pkg.in.module-info.java",
                "1 error");

        if (!expected.equals(log)) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testInvisibleClassVisiblePackageClash(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }",
                          """
                              package m1x;
                              import m1x.a.*; public class Test { A a; }
                              """,
                          """
                              package m1x.a;
                              public class A { }
                              """);
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }",
                          """
                              package m1x;
                              public class a { public static class A { } }
                              """);
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        new JavacTask(tb)
            .options("--module-source-path", src.toString(),
                     "-XDrawDiagnostics")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll();
    }

    @Test
    public void testStripUnknownRequired(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "module m1x { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { }");
        Path src_m4 = src.resolve("m4x");
        tb.writeJavaFiles(src_m4,
                          "module m4x { }");
        Path src_test = src.resolve("test");
        tb.writeJavaFiles(src_test,
                          "module test { requires m1x; requires m2x; requires java.base; requires m3x; requires m4x; }");
        Path src_compile = src.resolve("compile");
        tb.writeJavaFiles(src_compile,
                          "module compile { exports p to test; }",
                          "package p; public class Test { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("-processor", ListRequires.class.getName(),
                         "--module-source-path", src.toString(),
                         "--limit-modules", "compile",
                         "-XDaccessInternalAPI=true")
                .outdir(classes)
                .files(findJavaFiles(src_compile))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.STDOUT);

        List<String> expected = Arrays.asList(
                "from directives:",
                "java.base",
                "from requires:",
                "java.base"
        );
        if (!Objects.equals(log, expected))
            throw new AssertionError("Unexpected output: " + log);
    }

    @SupportedAnnotationTypes("*")
    @SupportedOptions("expectedEnclosedElements")
    public static final class ListRequires extends AbstractProcessor {

        private int round;

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (round++ == 0) {
                ModuleElement compileE = processingEnv.getElementUtils().getModuleElement("compile");
                ModuleElement testE = ElementFilter.exportsIn(compileE.getDirectives()).get(0).getTargetModules().get(0);

                System.out.println("from directives:");
                for (RequiresDirective rd : ElementFilter.requiresIn(testE.getDirectives())) {
                    System.out.println(rd.getDependency().getQualifiedName());
                }

                System.out.println("from requires:");
                for (RequiresDirective rd : ((ModuleSymbol) testE).requires) {
                    System.out.println(rd.getDependency().getQualifiedName());
                }
            }

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

    }

    @Test
    public void testOnDemandCompletionModuleInfoJava(Path base) throws Exception {
        Path src = base.resolve("src");
        Path src_m1 = src.resolve("m1x");
        tb.writeJavaFiles(src_m1,
                          "@Deprecated module m1x { }");
        Path src_m2 = src.resolve("m2x");
        tb.writeJavaFiles(src_m2,
                          "module m2x { requires m1x; }");
        Path src_m3 = src.resolve("m3x");
        tb.writeJavaFiles(src_m3,
                          "module m3x { requires m2x; requires m1x; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected;

        log = new JavacTask(tb)
                .options("--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src_m1))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList("");

        if (!expected.equals(log)) {
            throw new IllegalStateException(log.toString());
        }

        log = new JavacTask(tb)
                .options("--module-source-path", src.toString(),
                         "-XDrawDiagnostics",
                         "-Xlint:deprecation")
                .outdir(classes)
                .files(findJavaFiles(src_m3))
                .run()
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        expected = Arrays.asList(
                "module-info.java:1:23: compiler.warn.has.been.deprecated.module: m1x",
                "module-info.java:1:37: compiler.warn.has.been.deprecated.module: m1x",
                "2 warnings"
        );

        if (!expected.equals(log)) {
            throw new IllegalStateException(log.toString());
        }
    }

    @Test
    public void testUnnamedPackage(Path base) throws Exception {
        List<String> out;
        List<String> expected;

        //-source 8:
        Path src8 = base.resolve("src8");
        Files.createDirectories(src8);
        tb.writeJavaFiles(src8,
                          "package test; public class Test {}");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        out = new JavacTask(tb)
                .options("--source-path", src8.toString(),
                         "-processor", UnnamedPackageProcessor.class.getName(),
                         "-source", "8")
                .outdir(classes)
                .files(findJavaFiles(src8))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.STDOUT);

        expected = Arrays.asList("noModule");

        if (!expected.equals(out)) {
            throw new AssertionError("Unexpected output: " + out);
        }

        //-source 9, unnamed:
        Path srcUnnamed = base.resolve("srcUnnamed");
        Files.createDirectories(srcUnnamed);
        tb.writeJavaFiles(srcUnnamed,
                          "public class Test {}");
        Path classesUnnamed = base.resolve("classesUnnamed");
        tb.createDirectories(classesUnnamed);

        out = new JavacTask(tb)
                .options("--source-path", srcUnnamed.toString(),
                         "-processor", UnnamedPackageProcessor.class.getName())
                .outdir(classesUnnamed)
                .files(findJavaFiles(srcUnnamed))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.STDOUT);

        expected = Arrays.asList("unnamedModule");

        if (!expected.equals(out)) {
            throw new AssertionError("Unexpected output: " + out);
        }

        //-source 9, named:
        Path srcNamed = base.resolve("srcNamed");
        Files.createDirectories(srcNamed);
        tb.writeJavaFiles(srcNamed,
                          "module m {}",
                          "public class Test {}");
        Path classesNamed = base.resolve("classesNamed");
        tb.createDirectories(classesNamed);

        out = new JavacTask(tb)
                .options("--source-path", srcNamed.toString(),
                         "-classpath", "",
                         "-processorpath", System.getProperty("test.class.path"),
                         "-processor", UnnamedPackageProcessor.class.getName())
                .outdir(classesNamed)
                .files(findJavaFiles(srcNamed))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.STDOUT);

        expected = Arrays.asList("m");

        if (!expected.equals(out)) {
            throw new AssertionError("Unexpected output: " + out);
        }

        //-source 9, conflict:
        Path srcNamed2 = base.resolve("srcNamed2");
        Path srcNamed2m1 = srcNamed2.resolve("m1x");
        Files.createDirectories(srcNamed2m1);
        tb.writeJavaFiles(srcNamed2m1,
                          "module m1x {}",
                          "public class Test {}");
        Path srcNamed2m2 = srcNamed2.resolve("m2x");
        Files.createDirectories(srcNamed2m2);
        tb.writeJavaFiles(srcNamed2m2,
                          "module m2x {}",
                          "public class Test {}");
        Path classesNamed2 = base.resolve("classesNamed2");
        tb.createDirectories(classesNamed2);

        out = new JavacTask(tb)
                .options("--module-source-path", srcNamed2.toString(),
                         "-classpath", "",
                         "-processorpath", System.getProperty("test.class.path"),
                         "-processor", UnnamedPackageProcessor.class.getName(),
                         "-XDshould-stop.ifError=FLOW")
                .outdir(classesNamed2)
                .files(findJavaFiles(srcNamed2))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.STDOUT);

        expected = Arrays.asList("null",
                                 "m1x: true",
                                 "m2x: true");

        if (!expected.equals(out)) {
            throw new AssertionError("Unexpected output: " + out);
        }
    }

    @SupportedAnnotationTypes("*")
    public static final class UnnamedPackageProcessor extends AbstractProcessor {

        int round = 0;

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            if (round++ != 0)
                return false;

            Elements elements = processingEnv.getElementUtils();
            PackageElement pe = elements.getPackageElement("");

            if (pe == null) {
                System.out.println("null");
            } else {
                ModuleElement mod = (ModuleElement) pe.getEnclosingElement();
                if (mod == null) {
                    System.out.println("noModule");
                } else if (mod.isUnnamed()) {
                    System.out.println("unnamedModule");
                } else {
                    System.out.println(mod);
                }
            }

            ModuleElement m1x = elements.getModuleElement("m1x");
            ModuleElement m2x = elements.getModuleElement("m2x");

            if (m1x != null && m2x != null) {
                System.out.println("m1x: " + (elements.getPackageElement(m1x, "") != null));
                System.out.println("m2x: " + (elements.getPackageElement(m2x, "") != null));
            }

            return false;
        }

    }

    @Test
    public void testEmptyInExportedPackage(Path base) throws Exception {
        Path src = base.resolve("src");
        Path m = src.resolve("m");
        tb.writeJavaFiles(m,
                          "module m { exports api; }");
        Path apiFile = m.resolve("api").resolve("Api.java");
        Files.createDirectories(apiFile.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(apiFile)) {
            w.write("//no package decl");
        }
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;
        List<String> expected =
                Arrays.asList("module-info.java:1:20: compiler.err.package.empty.or.not.found: api",
                              "1 error");

        System.err.println("file explicitly specified:");

        log = new JavacTask(tb)
            .options("-XDrawDiagnostics",
                     "--module-source-path", src.toString())
            .outdir(classes)
            .files(findJavaFiles(src))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log))
            throw new Exception("expected output not found: " + log);

        System.err.println("file not specified:");

        tb.cleanDirectory(classes);

        log = new JavacTask(tb)
            .options("-XDrawDiagnostics",
                     "--module-source-path", src.toString())
            .outdir(classes)
            .files(findJavaFiles(m.resolve("module-info.java")))
            .run(Task.Expect.FAIL)
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log))
            throw new Exception("expected output not found: " + log);
    }

    @Test
    public void testJustPackageInExportedPackage(Path base) throws Exception {
        Path src = base.resolve("src");
        Path m = src.resolve("m");
        tb.writeJavaFiles(m,
                          "module m { exports api; }");
        Path apiFile = m.resolve("api").resolve("Api.java");
        Files.createDirectories(apiFile.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(apiFile)) {
            w.write("package api;");
        }
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        System.err.println("file explicitly specified:");

        new JavacTask(tb)
            .options("-XDrawDiagnostics",
                     "--module-source-path", src.toString())
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll();

        System.err.println("file not specified:");

        tb.cleanDirectory(classes);

        new JavacTask(tb)
            .options("-XDrawDiagnostics",
                     "--module-source-path", src.toString())
            .outdir(classes)
            .files(findJavaFiles(m.resolve("module-info.java")))
            .run()
            .writeAll();
    }

    @Test
    public void testWrongPackageInExportedPackage(Path base) throws Exception {
        Path src = base.resolve("src");
        Path m = src.resolve("m");
        tb.writeJavaFiles(m,
                          "module m { exports api; }");
        Path apiFile = m.resolve("api").resolve("Api.java");
        Files.createDirectories(apiFile.getParent());
        try (BufferedWriter w = Files.newBufferedWriter(apiFile)) {
            w.write("package impl; public class Api { }");
        }
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log;

        List<String> expected =
                Arrays.asList("module-info.java:1:20: compiler.err.package.empty.or.not.found: api",
                              "1 error");

        System.err.println("file explicitly specified:");

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log))
            throw new Exception("expected output not found: " + log);

        System.err.println("file not specified:");

        tb.cleanDirectory(classes);

        log = new JavacTask(tb)
                .options("-XDrawDiagnostics",
                         "--module-source-path", src.toString())
                .outdir(classes)
                .files(findJavaFiles(m.resolve("module-info.java")))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        if (!expected.equals(log))
            throw new Exception("expected output not found: " + log);
    }

    @Test
    public void testDependOnUnnamedAccessibility(Path base) throws Exception {
        Path unnamedSrc = base.resolve("unnamed-src");
        tb.writeJavaFiles(unnamedSrc,
                          "package p1; public class First { public static p2.Second get() { return null; } }",
                          "package p2; public class Second { public void test() { } }");
        Path unnamedClasses = base.resolve("unnamed-classes");
        tb.createDirectories(unnamedClasses);

        System.err.println("compiling unnamed sources:");

        new JavacTask(tb)
                .outdir(unnamedClasses)
                .files(findJavaFiles(unnamedSrc))
                .run()
                .writeAll();

        //test sources:
        Path src = base.resolve("src");
        Path m = src.resolve("m");
        tb.writeJavaFiles(m,
                          "module m { }",
                          "package p; public class Test { { p1.First.get().test(); } }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        System.err.println("compiling test module:");

        new JavacTask(tb)
            .options("-classpath", unnamedClasses.toString(),
                     "--add-reads", "m=ALL-UNNAMED")
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll();
    }

    @Test
    public void testMisnamedModuleInfoClass(Path base) throws Exception {
        Path src = base.resolve("src");
        Path a = src.resolve("a");
        tb.writeJavaFiles(a,
                          "module a {}");
        Path b = src.resolve("b");
        tb.writeJavaFiles(b,
                          "module b { uses com.example.c; }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);
        Path aClasses = classes.resolve("x");
        tb.createDirectories(aClasses);

        new JavacTask(tb)
                .outdir(aClasses)
                .files(findJavaFiles(a))
                .run()
                .writeAll();

        Path bClasses = classes.resolve("b");
        tb.createDirectories(bClasses);

        List<String> log;

        log = new JavacTask(tb)
                .outdir(bClasses)
                .options("-p", classes.toString(),
                         "-XDrawDiagnostics")
                .files(findJavaFiles(b))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = List.of("module-info.java:1:28: compiler.err.doesnt.exist: com.example",
                                        "1 error");

        if (!expected.equals(log))
            throw new Exception("expected output not found: " + log);
    }

}
