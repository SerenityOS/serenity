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
 * @bug 8173777
 * @summary tests for multi-module mode compilation
 * @library /tools/lib
 * @modules
 *      jdk.compiler/com.sun.tools.javac.api
 *      jdk.compiler/com.sun.tools.javac.code
 *      jdk.compiler/com.sun.tools.javac.main
 *      jdk.compiler/com.sun.tools.javac.processing
 * @build toolbox.ToolBox toolbox.JavacTask toolbox.ModuleBuilder ModuleTestBase
 * @run main CompileModulePatchTest
 */

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import toolbox.JavacTask;
import toolbox.ModuleBuilder;
import toolbox.Task;
import toolbox.Task.Expect;

public class CompileModulePatchTest extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        new CompileModulePatchTest().runTests();
    }

    @Test
    public void testCorrectModulePatch(Path base) throws Exception {
        //note: avoiding use of java.base, as that gets special handling on some places:
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package javax.lang.model.element; public interface Extra extends Element { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("--patch-module", "java.compiler=" + src.toString())
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new Exception("expected output not found: " + log);
    }

    @Test
    public void testCorrectModulePatchMultiModule(Path base) throws Exception {
        //note: avoiding use of java.base, as that gets special handling on some places:
        Path src = base.resolve("src");
        Path m1 = src.resolve("m1");
        tb.writeJavaFiles(m1, "package javax.lang.model.element; public interface Extra extends Element { }");
        Path m2 = src.resolve("m2");
        tb.writeJavaFiles(m2, "package com.sun.source.tree; public interface Extra extends Tree { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("--patch-module", "java.compiler=" + m1.toString(),
                         "--patch-module", "jdk.compiler=" + m2.toString(),
                         "--module-source-path", "dummy")
                .outdir(classes)
                .files(findJavaFiles(src))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new Exception("expected output not found: " + log);

        checkFileExists(classes, "java.compiler/javax/lang/model/element/Extra.class");
        checkFileExists(classes, "jdk.compiler/com/sun/source/tree/Extra.class");
    }

    @Test
    public void testCorrectModulePatchMultiModule2(Path base) throws Exception {
        //note: avoiding use of java.base, as that gets special handling on some places:
        Path src = base.resolve("src");
        Path m1 = src.resolve("m1");
        tb.writeJavaFiles(m1,
                          "package javax.lang.model.element; public interface Extra extends Element { }");
        Path m2 = src.resolve("m2");
        tb.writeJavaFiles(m2,
                          "package com.sun.source.tree; public interface Extra extends Tree { }");
        Path msp = base.resolve("msp");
        Path m3 = msp.resolve("m3x");
        tb.writeJavaFiles(m3,
                          "module m3x { }",
                          "package m3; public class Test { }");
        Path m4 = msp.resolve("m4x");
        tb.writeJavaFiles(m4,
                          "module m4x { }",
                          "package m4; public class Test { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("--patch-module", "java.compiler=" + m1.toString(),
                         "--patch-module", "jdk.compiler=" + m2.toString(),
                         "--module-source-path", msp.toString())
                .outdir(classes)
                .files(findJavaFiles(src, msp))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new Exception("expected output not found: " + log);

        checkFileExists(classes, "java.compiler/javax/lang/model/element/Extra.class");
        checkFileExists(classes, "jdk.compiler/com/sun/source/tree/Extra.class");
        checkFileExists(classes, "m3x/m3/Test.class");
        checkFileExists(classes, "m4x/m4/Test.class");
    }

    @Test
    public void testPatchModuleModuleSourcePathConflict(Path base) throws Exception {
        //note: avoiding use of java.base, as that gets special handling on some places:
        Path src = base.resolve("src");
        Path m1 = src.resolve("m1x");
        tb.writeJavaFiles(m1,
                          "module m1x { }",
                          "package m1; public class Test { }");
        Path m2 = src.resolve("m2x");
        tb.writeJavaFiles(m2,
                          "module m2x { }",
                          "package m2; public class Test { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("--patch-module", "m1x=" + m2.toString(),
                         "--module-source-path", src.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(findJavaFiles(src.resolve("m1x").resolve("m1"),
                                     src.resolve("m2x").resolve("m2")))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expectedOut = Arrays.asList(
                "Test.java:1:1: compiler.err.file.patched.and.msp: m1x, m2x",
                "module-info.java:1:1: compiler.err.module.name.mismatch: m2x, m1x",
                "- compiler.err.cant.access: m1x.module-info, (compiler.misc.cant.resolve.modules)",
                "3 errors"
        );

        if (!expectedOut.equals(log))
            throw new Exception("expected output not found: " + log);
    }

    @Test
    public void testSourcePath(Path base) throws Exception {
        //note: avoiding use of java.base, as that gets special handling on some places:
        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package javax.lang.model.element; public interface Extra extends Element, Other { }");
        Path srcPath = base.resolve("src-path");
        tb.writeJavaFiles(srcPath, "package javax.lang.model.element; interface Other { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("--patch-module", "java.compiler=" + src.toString(),
                         "-sourcepath", srcPath.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(src.resolve("javax/lang/model/element/Extra.java"))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expectedOut = Arrays.asList(
                "Extra.java:1:75: compiler.err.cant.resolve: kindname.class, Other, , ",
                "1 error"
        );

        if (!expectedOut.equals(log))
            throw new Exception("expected output not found: " + log);
    }

    @Test
    public void testClassPath(Path base) throws Exception {
        Path cpSrc = base.resolve("cpSrc");
        tb.writeJavaFiles(cpSrc, "package p; public interface Other { }");
        Path cpClasses = base.resolve("cpClasses");
        tb.createDirectories(cpClasses);

        String cpLog = new JavacTask(tb)
                .outdir(cpClasses)
                .files(findJavaFiles(cpSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!cpLog.isEmpty())
            throw new Exception("expected output not found: " + cpLog);

        Path src = base.resolve("src");
        //note: avoiding use of java.base, as that gets special handling on some places:
        tb.writeJavaFiles(src, "package javax.lang.model.element; public interface Extra extends Element, p.Other { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        List<String> log = new JavacTask(tb)
                .options("--patch-module", "java.compiler=" + src.toString(),
                         "--class-path", cpClasses.toString(),
                         "-XDrawDiagnostics")
                .outdir(classes)
                .files(src.resolve("javax/lang/model/element/Extra.java"))
                .run(Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expectedOut = Arrays.asList(
                "Extra.java:1:75: compiler.err.package.not.visible: p, (compiler.misc.not.def.access.does.not.read.unnamed: p, java.compiler)",
                "1 error"
        );

        if (!expectedOut.equals(log))
            throw new Exception("expected output not found: " + log);
    }

    @Test
    public void testWithModulePath(Path base) throws Exception {
        Path modSrc = base.resolve("modSrc");
        Path modules = base.resolve("modules");
        new ModuleBuilder(tb, "m1")
                .classes("package pkg1; public interface E { }")
                .build(modSrc, modules);

        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package p; interface A extends pkg1.E { }");

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("--module-path", modules.toString(),
                        "--patch-module", "m1=" + src.toString())
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        //checks module bounds still exist
        new ModuleBuilder(tb, "m2")
                .classes("package pkg2; public interface D { }")
                .build(modSrc, modules);

        Path src2 = base.resolve("src2");
        tb.writeJavaFiles(src2, "package p; interface A extends pkg2.D { }");

        List<String> log = new JavacTask(tb, Task.Mode.CMDLINE)
                .options("-XDrawDiagnostics",
                        "--module-path", modules.toString(),
                        "--patch-module", "m1=" + src2.toString())
                .files(findJavaFiles(src2))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(Task.OutputKind.DIRECT);

        List<String> expected = Arrays.asList("A.java:1:32: compiler.err.package.not.visible: pkg2, (compiler.misc.not.def.access.does.not.read: m1, pkg2, m2)",
                "1 error");

        if (!expected.equals(log))
            throw new Exception("expected output not found: " + log);
    }

    @Test
    public void testWithUpgradeModulePath(Path base) throws Exception {
        Path modSrc = base.resolve("modSrc");
        Path modules = base.resolve("modules");
        new ModuleBuilder(tb, "m1")
                .classes("package pkg1; public interface E { }")
                .build(modSrc, modules);

        Path upgrSrc = base.resolve("upgradeSrc");
        Path upgrade = base.resolve("upgrade");
        new ModuleBuilder(tb, "m1")
                .classes("package pkg1; public interface D { }")
                .build(upgrSrc, upgrade);

        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package p; interface A extends pkg1.D { }");

        new JavacTask(tb, Task.Mode.CMDLINE)
                .options("--module-path", modules.toString(),
                        "--upgrade-module-path", upgrade.toString(),
                        "--patch-module", "m1=" + src.toString())
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @Test
    public void testUnnamedIsolation(Path base) throws Exception {
        //note: avoiding use of java.base, as that gets special handling on some places:
        Path sourcePath = base.resolve("source-path");
        tb.writeJavaFiles(sourcePath, "package src; public class Src {}");

        Path classPathSrc = base.resolve("class-path-src");
        tb.writeJavaFiles(classPathSrc, "package cp; public class CP { }");
        Path classPath = base.resolve("classPath");
        tb.createDirectories(classPath);

        String cpLog = new JavacTask(tb)
                .outdir(classPath)
                .files(findJavaFiles(classPathSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!cpLog.isEmpty())
            throw new Exception("expected output not found: " + cpLog);

        Path modulePathSrc = base.resolve("module-path-src");
        tb.writeJavaFiles(modulePathSrc,
                          "module m {}",
                          "package m; public class M {}");
        Path modulePath = base.resolve("modulePath");
        tb.createDirectories(modulePath.resolve("m"));

        String modLog = new JavacTask(tb)
                .outdir(modulePath.resolve("m"))
                .files(findJavaFiles(modulePathSrc))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!modLog.isEmpty())
            throw new Exception("expected output not found: " + modLog);

        Path src = base.resolve("src");
        tb.writeJavaFiles(src, "package m; public class Extra { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        String log = new JavacTask(tb)
                .options("--patch-module", "m=" + sourcePath.toString(),
                         "--class-path", classPath.toString(),
                         "--source-path", sourcePath.toString(),
                         "--module-path", modulePath.toString(),
                         "--processor-path", System.getProperty("test.classes"),
                         "-XDaccessInternalAPI=true",
                         "-processor", CheckModuleContentProcessing.class.getName())
                .outdir(classes)
                .files(findJavaFiles(sourcePath))
                .run()
                .writeAll()
                .getOutput(Task.OutputKind.DIRECT);

        if (!log.isEmpty())
            throw new Exception("expected output not found: " + log);
    }

    @SupportedAnnotationTypes("*")
    public static final class CheckModuleContentProcessing extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            Symtab syms = Symtab.instance(((JavacProcessingEnvironment) processingEnv).getContext());
            Elements elements = processingEnv.getElementUtils();
            ModuleElement unnamedModule = syms.unnamedModule;
            ModuleElement mModule = elements.getModuleElement("m");

            assertNonNull("mModule found", mModule);
            assertNonNull("src.Src from m", elements.getTypeElement(mModule, "src.Src"));
            assertNull("cp.CP not from m", elements.getTypeElement(mModule, "cp.CP"));
            assertNull("src.Src not from unnamed", elements.getTypeElement(unnamedModule, "src.Src"));
            assertNonNull("cp.CP from unnamed", elements.getTypeElement(unnamedModule, "cp.CP"));

            return false;
        }

        @Override
        public SourceVersion getSupportedSourceVersion() {
            return SourceVersion.latest();
        }

        private static void assertNonNull(String msg, Object val) {
            if (val == null) {
                throw new AssertionError(msg);
            }
        }

        private static void assertNull(String msg, Object val) {
            if (val != null) {
                throw new AssertionError(msg);
            }
        }
    }

    @Test
    public void testSingleModeIncremental(Path base) throws Exception {
        //note: avoiding use of java.base, as that gets special handling on some places:
        Path src = base.resolve("src");
        tb.writeJavaFiles(src,
                          "package javax.lang.model.element; public interface Extra extends Element { }",
                          "package javax.lang.model.element; public interface Extra2 extends Extra { }");
        Path classes = base.resolve("classes");
        tb.createDirectories(classes);

        Thread.sleep(2000); //ensure newer timestamps on classfiles:

        new JavacTask(tb)
            .options("--patch-module", "java.compiler=" + src.toString())
            .outdir(classes)
            .files(findJavaFiles(src))
            .run()
            .writeAll()
            .getOutput(Task.OutputKind.DIRECT);

        List<String> log = new JavacTask(tb)
            .options("--patch-module", "java.compiler=" + src.toString(),
                     "-verbose")
            .outdir(classes)
            .files(findJavaFiles(src.resolve("javax/lang/model/element/Extra2.java"
                                    .replace("/", src.getFileSystem().getSeparator()))))
            .run()
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT)
            .stream()
            .filter(l -> l.contains("parsing"))
            .collect(Collectors.toList());

        boolean parsesExtra2 = log.stream()
                                  .anyMatch(l -> l.contains("Extra2.java"));
        boolean parsesExtra = log.stream()
                              .anyMatch(l -> l.contains("Extra.java"));

        if (!parsesExtra2 || parsesExtra) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    @Test
    public void testComplexMSPAndPatch(Path base) throws Exception {
        //note: avoiding use of java.base, as that gets special handling on some places:
        Path src1 = base.resolve("src1");
        Path src1ma = src1.resolve("ma");
        tb.writeJavaFiles(src1ma,
                          "module ma { exports ma; }",
                          "package ma; public class C1 { public static void method() { } }",
                          "package ma.impl; public class C2 { }");
        Path src1mb = src1.resolve("mb");
        tb.writeJavaFiles(src1mb,
                          "module mb { requires ma; }",
                          "package mb.impl; public class C2 { public static void method() { } }");
        Path src1mc = src1.resolve("mc");
        tb.writeJavaFiles(src1mc,
                          "module mc { }");
        Path classes1 = base.resolve("classes1");
        tb.createDirectories(classes1);
        tb.cleanDirectory(classes1);

        new JavacTask(tb)
            .options("--module-source-path", src1.toString())
            .files(findJavaFiles(src1))
            .outdir(classes1)
            .run()
            .writeAll();

        //patching:
        Path src2 = base.resolve("src2");
        Path src2ma = src2.resolve("ma");
        tb.writeJavaFiles(src2ma,
                          "package ma.impl; public class C2 { public static void extra() { ma.C1.method(); } }",
                          "package ma.impl; public class C3 { public void test() { C2.extra(); } }");
        Path src2mb = src2.resolve("mb");
        tb.writeJavaFiles(src2mb,
                          "package mb.impl; public class C3 { public void test() { C2.method(); ma.C1.method(); ma.impl.C2.extra(); } }");
        Path src2mc = src2.resolve("mc");
        tb.writeJavaFiles(src2mc,
                          "package mc.impl; public class C2 { public static void test() { } }",
                          //will require --add-reads ma:
                          "package mc.impl; public class C3 { public static void test() { ma.impl.C2.extra(); } }");
        Path src2mt = src2.resolve("mt");
        tb.writeJavaFiles(src2mt,
                          "module mt { requires ma; requires mb; }",
                          "package mt.impl; public class C2 { public static void test() { mb.impl.C2.method(); ma.impl.C2.extra(); } }",
                          "package mt.impl; public class C3 { public static void test() { C2.test(); mc.impl.C2.test(); } }");
        Path classes2 = base.resolve("classes2");
        tb.createDirectories(classes2);
        tb.cleanDirectory(classes2);

        Thread.sleep(2000); //ensure newer timestamps on classfiles:

        new JavacTask(tb)
            .options("--module-path", classes1.toString(),
                     "--patch-module", "ma=" + src2ma.toString(),
                     "--patch-module", "mb=" + src2mb.toString(),
                     "--add-exports", "ma/ma.impl=mb",
                     "--patch-module", "mc=" + src2mc.toString(),
                     "--add-reads", "mc=ma",
                     "--add-exports", "ma/ma.impl=mc",
                     "--add-exports", "ma/ma.impl=mt",
                     "--add-exports", "mb/mb.impl=mt",
                     "--add-exports", "mc/mc.impl=mt",
                     "--add-reads", "mt=mc",
                     "--module-source-path", src2.toString())
            .outdir(classes2)
            .files(findJavaFiles(src2))
            .run()
            .writeAll();

        //incremental compilation (C2 mustn't be compiled, C3 must):
        tb.writeJavaFiles(src2ma,
                          "package ma.impl; public class C3 { public void test() { ma.C1.method(); C2.extra(); } }");
        tb.writeJavaFiles(src2mt,
                          "package mt.impl; public class C3 { public static void test() { mc.impl.C2.test(); C2.test(); } }");

        List<String> log = new JavacTask(tb)
            .options("--module-path", classes1.toString(),
                     "--patch-module", "ma=" + src2ma.toString(),
                     "--patch-module", "mb=" + src2mb.toString(),
                     "--add-exports", "ma/ma.impl=mb",
                     "--patch-module", "mc=" + src2mc.toString(),
                     "--add-reads", "mc=ma",
                     "--add-exports", "ma/ma.impl=mc",
                     "--add-exports", "ma/ma.impl=mt",
                     "--add-exports", "mb/mb.impl=mt",
                     "--add-exports", "mc/mc.impl=mt",
                     "--add-reads", "mt=mc",
                     "--module-source-path", src2.toString(),
                     "--add-modules", "mc",
                     "-verbose")
            .outdir(classes2)
            .files(src2ma.resolve("ma").resolve("impl").resolve("C3.java"),
                   src2mt.resolve("mt").resolve("impl").resolve("C3.java"))
            .run()
            .writeAll()
            .getOutputLines(Task.OutputKind.DIRECT)
            .stream()
            .filter(l -> l.contains("parsing"))
            .collect(Collectors.toList());

        boolean parsesC3 = log.stream()
                              .anyMatch(l -> l.contains("C3.java"));
        boolean parsesC2 = log.stream()
                              .anyMatch(l -> l.contains("C2.java"));

        if (!parsesC3 || parsesC2) {
            throw new AssertionError("Unexpected output: " + log);
        }
    }

    private void checkFileExists(Path dir, String path) {
        Path toCheck = dir.resolve(path.replace("/", dir.getFileSystem().getSeparator()));

        if (!Files.exists(toCheck)) {
            throw new AssertionError(toCheck.toString() + " does not exist!");
        }
    }
}
