/*
 * Copyright (c) 2016, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8159602 8170549 8171255 8171322 8254023
 * @summary Test annotations on module declaration.
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.classfile
 * @build toolbox.ToolBox toolbox.JavacTask ModuleTestBase
 * @run main AnnotationsOnModules
 */

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.TypeElement;

import com.sun.tools.classfile.Annotation;
import com.sun.tools.classfile.Attribute;
import com.sun.tools.classfile.ClassFile;
import com.sun.tools.classfile.RuntimeInvisibleAnnotations_attribute;
import com.sun.tools.classfile.RuntimeVisibleAnnotations_attribute;
import toolbox.JavacTask;
import toolbox.Task;
import toolbox.Task.OutputKind;

public class AnnotationsOnModules extends ModuleTestBase {

    public static void main(String... args) throws Exception {
        AnnotationsOnModules t = new AnnotationsOnModules();
        t.runTests();
    }

    @Test
    public void testSimpleAnnotation(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "@Deprecated module m1x { }");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();

        ClassFile cf = ClassFile.read(modulePath.resolve("m1x").resolve("module-info.class"));
        RuntimeVisibleAnnotations_attribute annotations = (RuntimeVisibleAnnotations_attribute) cf.attributes.map.get(Attribute.RuntimeVisibleAnnotations);

        if (annotations == null || annotations.annotations.length != 1) {
            throw new AssertionError("Annotations not correct!");
        }
    }

    @Test
    public void testSimpleJavadocDeprecationTag(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("src1/A");

        tb.writeJavaFiles(m1,
                "/** @deprecated */ module A { }");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        List<String> warning = new JavacTask(tb)
                .options("--module-source-path", m1.getParent().toString(),
                        "-XDrawDiagnostics")
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of(
                "module-info.java:1:20: compiler.warn.missing.deprecated.annotation",
                "1 warning");
        if (!warning.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }

        Path m2 = base.resolve("src2/B");

        tb.writeJavaFiles(m2,
                "module B { requires A; }");
        String log = new JavacTask(tb)
                .options("--module-source-path", m2.getParent().toString(),
                        "--module-path", modulePath.toString(),
                        "-XDrawDiagnostics")
                .outdir(modulePath)
                .files(findJavaFiles(m2))
                .run()
                .writeAll()
                .getOutput(OutputKind.DIRECT);

        if (!log.isEmpty()) {
            throw new AssertionError("Output is not empty. Expected no output and no warnings.");
        }

        ClassFile cf = ClassFile.read(modulePath.resolve("A").resolve("module-info.class"));
        RuntimeVisibleAnnotations_attribute annotations = (RuntimeVisibleAnnotations_attribute) cf.attributes.map.get(Attribute.RuntimeVisibleAnnotations);

        if (annotations != null && annotations.annotations.length > 0) {
            throw new AssertionError("Found annotation attributes. Expected no annotations for javadoc @deprecated tag.");
        }

        if (cf.attributes.map.get(Attribute.Deprecated) != null) {
            throw new AssertionError("Found Deprecated attribute. Expected no Deprecated attribute for javadoc @deprecated tag.");
        }
    }

    @Test
    public void testEnhancedDeprecatedAnnotation(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("src1/A");

        tb.writeJavaFiles(m1,
                "@Deprecated(since=\"10.X\", forRemoval=true) module A { }");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", m1.getParent().toString())
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();

        Path m2 = base.resolve("src2/B");

        tb.writeJavaFiles(m2,
                "module B { requires A; }");
        List<String> log = new JavacTask(tb)
                .options("--module-source-path", m2.getParent().toString(),
                        "--module-path", modulePath.toString(),
                        "-XDrawDiagnostics")
                .outdir(modulePath)
                .files(findJavaFiles(m2))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of("module-info.java:1:21: compiler.warn.has.been.deprecated.for.removal.module: A",
                "1 warning");
        if (!log.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }

        ClassFile cf = ClassFile.read(modulePath.resolve("A").resolve("module-info.class"));
        RuntimeVisibleAnnotations_attribute annotations = (RuntimeVisibleAnnotations_attribute) cf.attributes.map.get(Attribute.RuntimeVisibleAnnotations);

        if (annotations == null ) {
            throw new AssertionError("Annotations not found!");
        }
        int length = annotations.annotations.length;
        if (length != 1 ) {
            throw new AssertionError("Incorrect number of annotations: " + length);
        }
        int pairsCount = annotations.annotations[0].num_element_value_pairs;
        if (pairsCount != 2) {
            throw new AssertionError("Incorrect number of key-value pairs in annotation: " + pairsCount + " Expected two: forRemoval and since.");
        }
    }

    @Test
    public void testDeprecatedModuleRequiresDeprecatedForRemovalModule(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("src1/A");

        tb.writeJavaFiles(m1,
                "@Deprecated(forRemoval=true) module A { }");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", m1.getParent().toString())
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();

        Path m2 = base.resolve("src2/B");

        tb.writeJavaFiles(m2,
                "@Deprecated(forRemoval=false) module B { requires A; }");
        List<String> log = new JavacTask(tb)
                .options("--module-source-path", m2.getParent().toString(),
                        "--module-path", modulePath.toString(),
                        "-XDrawDiagnostics")
                .outdir(modulePath)
                .files(findJavaFiles(m2))
                .run()
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of("module-info.java:1:51: compiler.warn.has.been.deprecated.for.removal.module: A",
                "1 warning");
        if (!log.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }
    }

    @Test
    public void testExportsAndOpensToDeprecatedModule(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");


        tb.writeJavaFiles(moduleSrc.resolve("B"),
                "@Deprecated module B { }");
        tb.writeJavaFiles(moduleSrc.resolve("C"),
                "@Deprecated(forRemoval=true) module C { }");

        Path modulePath = base.resolve("module-path");
        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll();

        Path m1 = base.resolve("src1/A");

        tb.writeJavaFiles(m1,
                """
                    module A {
                        exports p1 to B; opens p1 to B;
                        exports p2 to C; opens p2 to C;
                        exports p3 to B,C; opens p3 to B,C;
                    }""",
                "package p1; public class A { }",
                "package p2; public class A { }",
                "package p3; public class A { }");
        String log = new JavacTask(tb)
                .options("--module-source-path", m1.getParent().toString(),
                        "--module-path", modulePath.toString(),
                        "-XDrawDiagnostics")
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll()
                .getOutput(OutputKind.DIRECT);

        if (!log.isEmpty()) {
            throw new AssertionError("Output is not empty! " + log);
        }
    }

    @Test
    public void testAnnotationWithImport(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "import m1x.A; @A module m1x { }",
                          "package m1x; import java.lang.annotation.*; @Target(ElementType.MODULE) public @interface A {}");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();

        ClassFile cf = ClassFile.read(modulePath.resolve("m1x").resolve("module-info.class"));
        RuntimeInvisibleAnnotations_attribute annotations = (RuntimeInvisibleAnnotations_attribute) cf.attributes.map.get(Attribute.RuntimeInvisibleAnnotations);

        if (annotations == null || annotations.annotations.length != 1) {
            throw new AssertionError("Annotations not correct!");
        }
    }

    @Test
    public void testAnnotationWithImportFromAnotherModule(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("src1/A");

        tb.writeJavaFiles(m1,
                "module A { exports p1; exports p2; }",
                "package p1; import java.lang.annotation.*; @Target(ElementType.MODULE) public @interface A { }",
                "package p2; import java.lang.annotation.*; @Target(ElementType.MODULE) public @interface B { }");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", m1.getParent().toString())
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();

        Path m2 = base.resolve("src2/B");

        tb.writeJavaFiles(m2,
                "import p1.A; @A @p2.B module B { requires A; }");
        new JavacTask(tb)
                .options("--module-source-path", m2.getParent().toString(),
                        "--module-path", modulePath.toString()
                )
                .outdir(modulePath)
                .files(findJavaFiles(m2))
                .run()
                .writeAll();

        ClassFile cf = ClassFile.read(modulePath.resolve("B").resolve("module-info.class"));
        RuntimeInvisibleAnnotations_attribute annotations = (RuntimeInvisibleAnnotations_attribute) cf.attributes.map.get(Attribute.RuntimeInvisibleAnnotations);

        if (annotations == null ) {
            throw new AssertionError("Annotations not found!");
        }
        int length = annotations.annotations.length;
        if (length != 2 ) {
            throw new AssertionError("Incorrect number of annotations: " + length);
        }
    }

    @Test
    public void testAnnotationWithImportAmbiguity(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("src1/A");

        tb.writeJavaFiles(m1,
                "module A { exports p1; exports p2; }",
                "package p1; import java.lang.annotation.*; @Target(ElementType.MODULE) public @interface AAA { }",
                "package p2; import java.lang.annotation.*; @Target(ElementType.MODULE) public @interface AAA { }");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", m1.getParent().toString())
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();

        Path m2 = base.resolve("src2/B");

        tb.writeJavaFiles(m2,
                "import p1.*; import p2.*; @AAA module B { requires A; }");
        List<String> log = new JavacTask(tb)
                .options("--module-source-path", m2.getParent().toString(),
                        "--module-path", modulePath.toString(),
                        "-XDrawDiagnostics"
                )
                .outdir(modulePath)
                .files(findJavaFiles(m2))
                .run(Task.Expect.FAIL)
                .writeAll()
                .getOutputLines(OutputKind.DIRECT);

        List<String> expected = List.of("module-info.java:1:28: compiler.err.ref.ambiguous: AAA, kindname.class, p2.AAA, p2, kindname.class, p1.AAA, p1",
                "1 error");
        if (!log.containsAll(expected)) {
            throw new AssertionError("Expected output not found. Expected: " + expected);
        }

    }

    @Test
    public void testAnnotationWithoutTarget(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "@test.A module m1x { exports test; }",
                          "package test; public @interface A { }");

        Path classes = base.resolve("classes");
        Files.createDirectories(classes);

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(classes)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();

        ClassFile cf = ClassFile.read(classes.resolve("m1x").resolve("module-info.class"));
        var invisibleAnnotations = (RuntimeInvisibleAnnotations_attribute) cf.attributes.map.get(Attribute.RuntimeInvisibleAnnotations);

        if (invisibleAnnotations == null) {
            throw new AssertionError("Annotations not found!");
        }
        int length = invisibleAnnotations.annotations.length;
        if (length != 1) {
            throw new AssertionError("Incorrect number of annotations: " + length);
        }
        Annotation annotation = invisibleAnnotations.annotations[0];
        String annotationName = cf.constant_pool.getUTF8Value(annotation.type_index).toString();
        if (!"Ltest/A;".equals(annotationName)) {
            throw new AssertionError("Incorrect annotation name: " + annotationName);
        }
    }

    @Test
    public void testModuleInfoAnnotationsInAPI(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "import m1x.*; @A @Deprecated @E @E module m1x { }",
                          "package m1x; import java.lang.annotation.*; @Target(ElementType.MODULE) public @interface A {}",
                          "package m1x; import java.lang.annotation.*; @Target(ElementType.MODULE) @Repeatable(C.class) public @interface E {}",
                          "package m1x; import java.lang.annotation.*; @Target(ElementType.MODULE) public @interface C { public E[] value(); }");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString(),
                         "-processor", AP.class.getName())
                .outdir(modulePath)
                .files(findJavaFiles(m1))
                .run()
                .writeAll();

        Path src = base.resolve("src");

        tb.writeJavaFiles(src,
                          "class T {}");

        Path out = base.resolve("out");

        Files.createDirectories(out);

        new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--add-modules", "m1x",
                         "-processor", AP.class.getName())
                .outdir(out)
                .files(findJavaFiles(src))
                .run()
                .writeAll();

        new JavacTask(tb)
                .options("--module-path", modulePath.toString() + File.pathSeparator + out.toString(),
                         "--add-modules", "m1x",
                         "-processor", AP.class.getName(),
                         "-proc:only")
                .classes("m1x/m1x.A")
                .files(findJavaFiles(src))
                .run()
                .writeAll();
    }

    @SupportedAnnotationTypes("*")
    public static final class AP extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            ModuleElement m1 = processingEnv.getElementUtils().getModuleElement("m1x");
            Set<String> actualAnnotations = new HashSet<>();
            Set<String> expectedAnnotations =
                    new HashSet<>(Arrays.asList("@m1x.A", "@java.lang.Deprecated", "@m1x.C({@m1x.E, @m1x.E})"));

            for (AnnotationMirror am : m1.getAnnotationMirrors()) {
                actualAnnotations.add(am.toString());
            }

            if (!expectedAnnotations.equals(actualAnnotations)) {
                throw new AssertionError("Incorrect annotations: " + actualAnnotations);
            }

            return false;
        }

    }

    @Test
    public void testModuleDeprecation(Path base) throws Exception {
        Path moduleSrc = base.resolve("module-src");
        Path m1 = moduleSrc.resolve("m1x");

        tb.writeJavaFiles(m1,
                          "@Deprecated module m1x { }");

        Path m2 = moduleSrc.resolve("m2x");

        tb.writeJavaFiles(m2,
                          "@Deprecated module m2x { }");

        Path m3 = moduleSrc.resolve("m3x");

        Path modulePath = base.resolve("module-path");

        Files.createDirectories(modulePath);

        List<String> actual;
        List<String> expected;

        String DEPRECATED_JAVADOC = "/** @deprecated */";
        for (String suppress : new String[] {"", DEPRECATED_JAVADOC, "@Deprecated ", "@SuppressWarnings(\"deprecation\") "}) {
            tb.writeJavaFiles(m3,
                              suppress + """
                                  module m3x {
                                      requires m1x;
                                      exports api to m1x, m2x;
                                  }""",
                              "package api; public class Api { }");
            System.err.println("compile m3x");
            actual = new JavacTask(tb)
                    .options("--module-source-path", moduleSrc.toString(),
                             "-XDrawDiagnostics")
                    .outdir(modulePath)
                    .files(findJavaFiles(moduleSrc))
                    .run()
                    .writeAll()
                    .getOutputLines(OutputKind.DIRECT);

            if (suppress.isEmpty()) {
                expected = Arrays.asList(
                        "- compiler.note.deprecated.filename: module-info.java",
                        "- compiler.note.deprecated.recompile");
            } else if (suppress.equals(DEPRECATED_JAVADOC)) {
                expected = Arrays.asList(
                        "module-info.java:1:19: compiler.warn.missing.deprecated.annotation",
                        "- compiler.note.deprecated.filename: module-info.java",
                        "- compiler.note.deprecated.recompile",
                        "1 warning");
            } else {
                expected = Arrays.asList("");
            }

            if (!expected.equals(actual)) {
                throw new AssertionError("Unexpected output: " + actual + "; suppress: " + suppress);
            }

            System.err.println("compile m3x with -Xlint:-deprecation");
            actual = new JavacTask(tb)
                    .options("--module-source-path", moduleSrc.toString(),
                             "-XDrawDiagnostics",
                             "-Xlint:deprecation")
                    .outdir(modulePath)
                    .files(findJavaFiles(moduleSrc))
                    .run()
                    .writeAll()
                    .getOutputLines(OutputKind.DIRECT);

            if (suppress.isEmpty()) {
                expected = Arrays.asList(
                        "module-info.java:2:14: compiler.warn.has.been.deprecated.module: m1x",
                        "1 warning");
            } else if (suppress.equals(DEPRECATED_JAVADOC)) {
                expected = Arrays.asList(
                        "module-info.java:1:19: compiler.warn.missing.deprecated.annotation",
                        "module-info.java:2:14: compiler.warn.has.been.deprecated.module: m1x",
                        "2 warnings");
            } else {
                expected = Arrays.asList("");
            }

            if (!expected.equals(actual)) {
                throw new AssertionError("Unexpected output: " + actual + "; suppress: " + suppress);
            }

            //load the deprecated module-infos from classfile:
            System.err.println("compile m3x with -Xlint:-deprecation, loading deprecated modules from classes");
            actual = new JavacTask(tb)
                    .options("--module-path", modulePath.toString(),
                             "-XDrawDiagnostics",
                             "-Xlint:deprecation")
                    .outdir(modulePath.resolve("m3x"))
                    .files(findJavaFiles(moduleSrc.resolve("m3x")))
                    .run()
                    .writeAll()
                    .getOutputLines(OutputKind.DIRECT);

            if (!expected.equals(actual)) {
                throw new AssertionError("Unexpected output: " + actual + "; suppress: " + suppress);
            }
        }
    }

    @Test
    public void testAttributeValues(Path base) throws Exception {
        class TestCase {
            public final String extraDecl;
            public final String decl;
            public final String use;
            public final String expectedAnnotations;

            public TestCase(String extraDecl, String decl, String use, String expectedAnnotations) {
                this.extraDecl = extraDecl;
                this.decl = decl;
                this.use = use;
                this.expectedAnnotations = expectedAnnotations;
            }
        }

        TestCase[] testCases = new TestCase[] {
            new TestCase("package test; public enum E {A, B;}",
                         "public E value();",
                         "test.E.A",
                         "@test.A(A)"),
            new TestCase("package test; public enum E {A, B;}",
                         "public E[] value();",
                         "{test.E.A, test.E.B}",
                         "@test.A({A, B})"),
            new TestCase("package test; public class Extra {}",
                         "public Class value();",
                         "test.Extra.class",
                         "@test.A(test.Extra.class)"),
            new TestCase("package test; public class Extra {}",
                         "public Class[] value();",
                         "{test.Extra.class, String.class}",
                         "@test.A({test.Extra.class, java.lang.String.class})"),
            new TestCase("package test; public @interface Extra { public Class value(); }",
                         "public test.Extra value();",
                         "@test.Extra(String.class)",
                         "@test.A(@test.Extra(java.lang.String.class))"),
            new TestCase("package test; public @interface Extra { public Class value(); }",
                         "public test.Extra[] value();",
                         "{@test.Extra(String.class), @test.Extra(Integer.class)}",
                         "@test.A({@test.Extra(java.lang.String.class), @test.Extra(java.lang.Integer.class)})"),
            new TestCase("package test; public class Any { }",
                         "public int value();",
                         "1",
                         "@test.A(1)"),
            new TestCase("package test; public class Any { }",
                         "public int[] value();",
                         "{1, 2}",
                         "@test.A({1, 2})"),
            new TestCase("package test; public enum E {A;}",
                        "int integer(); boolean flag(); double value(); String string(); E enumeration(); ",
                        "enumeration = test.E.A, integer = 42, flag = true, value = 3.5, string = \"Text\"",
                        "@test.A(enumeration=A, integer=42, flag=true, value=3.5, string=\"Text\")"),
        };

        Path extraSrc = base.resolve("extra-src");
        tb.writeJavaFiles(extraSrc,
                          "class Any {}");

        int count = 0;

        for (TestCase tc : testCases) {
            Path testBase = base.resolve(String.valueOf(count));
            Path moduleSrc = testBase.resolve("module-src");
            Path m = moduleSrc.resolve("m");

            tb.writeJavaFiles(m,
                              "@test.A(" + tc.use + ") module m { }",
                              "package test; @java.lang.annotation.Target(java.lang.annotation.ElementType.MODULE) public @interface A { " + tc.decl + "}",
                              tc.extraDecl);

            Path modulePath = testBase.resolve("module-path");

            Files.createDirectories(modulePath);

            new JavacTask(tb)
                .options("--module-source-path", moduleSrc.toString())
                .outdir(modulePath)
                .files(findJavaFiles(moduleSrc))
                .run()
                .writeAll();

            Path classes = testBase.resolve("classes");

            Files.createDirectories(classes);

            new JavacTask(tb)
                .options("--module-path", modulePath.toString(),
                         "--add-modules", "m",
                         "-processorpath", System.getProperty("test.classes"),
                         "-processor", ProxyTypeValidator.class.getName(),
                         "-A" + OPT_EXPECTED_ANNOTATIONS + "=" + tc.expectedAnnotations)
                .outdir(classes)
                .files(findJavaFiles(extraSrc))
                .run()
                .writeAll();
        }
    }

    private static final String OPT_EXPECTED_ANNOTATIONS = "expectedAnnotations";

    @SupportedAnnotationTypes("*")
    @SupportedOptions(OPT_EXPECTED_ANNOTATIONS)
    public static final class ProxyTypeValidator extends AbstractProcessor {

        @Override
        public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
            ModuleElement m = processingEnv.getElementUtils().getModuleElement("m");
            String actualTypes = m.getAnnotationMirrors()
                                  .stream()
                                  .map(am -> am.toString())
                                  .collect(Collectors.joining(", "));
            if (!Objects.equals(actualTypes, processingEnv.getOptions().get(OPT_EXPECTED_ANNOTATIONS))) {
                throw new IllegalStateException("Expected annotations not found, actual: " + actualTypes);
            }
            return false;
        }

    }

}
