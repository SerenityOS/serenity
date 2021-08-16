/*
 * Copyright (c) 2018, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8187950
 * @summary Handing of BadClassFile exceptions and CompletionFailures
 * @library /tools/javac/lib /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 * @build JavacTestingAbstractProcessor MissingClassFile
 * @run main MissingClassFile
 */

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import toolbox.*;
import toolbox.Task.*;

import com.sun.source.tree.Scope;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.Trees;

@SupportedAnnotationTypes("*")
public class MissingClassFile {
    ToolBox tb = new ToolBox();

    void testPackageContent() throws Exception {
        Path base = Paths.get(".");
        Path libClasses = compileLib(base,
                                     "package pkg;" +
                                     "public class A {" +
                                     "}",
                                     "package pkg;" +
                                     "public class B {" +
                                     "}");

        Files.delete(libClasses.resolve("pkg/B.class"));
        try (OutputStream out = Files.newOutputStream(libClasses.resolve("pkg/B.class"))) {
            out.write(0);
        }

        doRunTest(base,
                  t -> {
                      PackageElement pe = t.getElements().getPackageElement("pkg");
                      for (Element el : pe.getEnclosedElements()) {
                          verifyElement(t, el);
                      }
                  },
                  "",
                  "pkg.B b;");
    }

    void testPackageDirectAPI() throws Exception {
        Path base = Paths.get(".");
        Path libClasses = compileLib(base,
                                     "package pkg;" +
                                     "public class A {" +
                                     "}",
                                     "package pkg;" +
                                     "public class B {" +
                                     "}");

        Files.delete(libClasses.resolve("pkg/B.class"));
        try (OutputStream out = Files.newOutputStream(libClasses.resolve("pkg/B.class"))) {
            out.write(0);
        }

        Path testSrc = base.resolve("test-src");
        tb.createDirectories(testSrc);
        tb.writeJavaFiles(testSrc,
                          "package test;\n" +
                          "public class Test {\n" +
                          "    void t() {\n" +
                          "        pkg.B b;\n" +
                          "    }\n" +
                          "}\n");
        Path testClasses = base.resolve("test-classes");
        tb.createDirectories(testClasses);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        List<String> errors = new ArrayList<>();

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            com.sun.source.util.JavacTask task = (com.sun.source.util.JavacTask)
                    compiler.getTask(null,
                                     null,
                                     d -> errors.add(d.getCode()),
                                     Arrays.asList("-XDrawDiagnostics",
                                                   "-classpath",
                                                   libClasses.toString()),
                                     null,
                                     fm.getJavaFileObjects(tb.findJavaFiles(testSrc)));
            task.parse();
            PackageElement pe = task.getElements().getPackageElement("pkg");
            for (Element el : pe.getEnclosedElements()) {
                verifyElement(task, el);
            }
            task.analyze();
        }

        List<String> expected = Arrays.asList("compiler.err.cant.access");

        if (!expected.equals(errors)) {
            throw new IllegalStateException("Expected error not found!");
        }
    }

    void testSuperClass() throws Exception {
        doTestCombo("class Test {" +
                    "}",
                    "package pkg;" +
                    "public class A extends # {" +
                    "}",
                    "pkg.A x;",
                    "# a = null; a.toString();",
                    (fqn, t) -> {
                        TypeElement a = t.getElements()
                                         .getTypeElement(t.getElements()
                                                          .getModuleElement(""),
                                                         "pkg.A");
                        TypeMirror superclass = a.getSuperclass();
                        verifyTypeMirror(t, superclass);
                        assertEquals(TypeKind.ERROR, superclass.getKind());
                        Element superclassEl = ((DeclaredType) superclass).asElement();
                        assertEquals(ElementKind.CLASS, superclassEl.getKind());
                        assertEquals(TypeKind.ERROR, superclassEl.asType().getKind());
                        TypeMirror originalType = Trees.instance(t).getOriginalType((ErrorType) superclass);
                        assertEquals(TypeKind.DECLARED, originalType.getKind());
                        assertEquals(superclassEl, ((DeclaredType) originalType).asElement());
                  });
        doTestCombo("interface Test {" +
                    "}",
                    "package pkg;" +
                    "public class A implements # {" +
                    "}",
                    "pkg.A x;",
                    "# a = null; a.toString();",
                    (fqn, t) -> {
                        TypeElement a = t.getElements().getTypeElement("pkg.A");
                        TypeMirror superintf = a.getInterfaces().get(0);
                        verifyTypeMirror(t, superintf);
                        assertEquals(TypeKind.ERROR, superintf.getKind());
                        Element superintfEl = ((DeclaredType) superintf).asElement();
                        //superintfEl.getKind() may be either CLASS or INTERFACE, depending on which class is missing
                        assertEquals(TypeKind.ERROR, superintfEl.asType().getKind());
                        TypeMirror originalType = Trees.instance(t).getOriginalType((ErrorType) superintf);
                        assertEquals(TypeKind.DECLARED, originalType.getKind());
                        assertEquals(superintfEl, ((DeclaredType) originalType).asElement());
                  });
        doTestCombo("class Test {" +
                    "}",
                    "package pkg;" +
                    "public class A extends # {" +
                    "}",
                    "pkg.A x;",
                    "# a = null; a.toString();",
                    (fqn, t) -> {
                        TypeElement a = t.getElements()
                                         .getTypeElement(t.getElements()
                                                          .getModuleElement(""),
                                                         "pkg.A");
                        DeclaredType superclass = (DeclaredType) a.getSuperclass();
                        superclass.getTypeArguments();
                  });
        doTestCombo("class Test {" +
                    "}",
                    "package pkg;" +
                    "public class A extends # {" +
                    "}",
                    "pkg.A x;",
                    "# a = null; a.toString();",
                    (fqn, t) -> {
                        TypeElement a = t.getElements()
                                         .getTypeElement(t.getElements()
                                                          .getModuleElement(""),
                                                         "pkg.A");
                        DeclaredType superclass = (DeclaredType) a.getSuperclass();
                        superclass.getEnclosingType();
                  });
    }

    void testAnnotation() throws Exception {
        doTestCombo("@interface Test {" +
                    "}",
                    "package pkg;" +
                    "@#\n" +
                    "public class A {" +
                    "}",
                    "",
                    "# a = null; a.toString();",
                    (fqn, t) -> {
                      TypeElement a = t.getElements().getTypeElement("pkg.A");
                      for (AnnotationMirror am : a.getAnnotationMirrors()) {
                          verifyTypeMirror(t, am.getAnnotationType());
                      }
                  });
        doTestCombo("@interface Test {" +
                    "    public Class<?> value();" +
                    "}",
                    "package pkg;" +
                    "@#(Object.class)\n" +
                    "public class A {" +
                    "}",
                    "",
                    "# a = null; a.toString();",
                    (fqn, t) -> {
                      TypeElement a = t.getElements().getTypeElement("pkg.A");
                      for (AnnotationMirror am : a.getAnnotationMirrors()) {
                          verifyTypeMirror(t, am.getAnnotationType());
                          if (am.getAnnotationType().toString().equals(fqn)) {
                              verifyTypeMirror(t, (TypeMirror) am.getElementValues().values()
                                                                 .iterator().next().getValue());
                          }
                      }
                  });
        doTestCombo("class Test { }",
                    "package pkg;" +
                    "@Ann(#.class)\n" +
                    "public class A {" +
                    "}" +
                    "@interface Ann {" +
                    "    public Class<?> value();" +
                    "}",
                    "",
                    "# a = null; a.toString();",
                    (fqn, t) -> {
                      TypeElement a = t.getElements().getTypeElement("pkg.A");
                      for (AnnotationMirror am : a.getAnnotationMirrors()) {
                          verifyTypeMirror(t, am.getAnnotationType());
                          if (am.getAnnotationType().toString().equals(fqn)) {
                              verifyTypeMirror(t, (TypeMirror) am.getElementValues().values()
                                                                 .iterator().next().getValue());
                          }
                      }
                  });
    }

    void testMethod() throws Exception {
        doTestCombo("class Test {" +
                    "}",
                    "package pkg;" +
                    "public class A {" +
                    "    public void m1(# t) { }" +
                    "    public # m2() { return null; }" +
                    "}",
                    "",
                    "pkg.A a = null; a.m2().toString();",
                    (fqn, t) -> {
                      TypeElement a = t.getElements().getTypeElement("pkg.A");
                      List<? extends Element> members = a.getEnclosedElements();
                      if (members.size() != 3)
                          throw new AssertionError("Unexpected number of members, " +
                                                   "received members: " + members);
                      for (Element e : members) {
                          verifyElement(t, e);
                      }
                  });
    }

    void testAnnotationProcessing() throws Exception {
        boolean[] superClass = new boolean[1];
        boolean[] inInit = new boolean[1];
        class TestAP extends AbstractProcessor {

            @Override
            public void init(ProcessingEnvironment processingEnv) {
                super.init(processingEnv);
                if (inInit[0])
                    doCheck();
            }

            @Override
            public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
                if (!inInit[0])
                    doCheck();
                return false;
            }

            private void doCheck() {
                com.sun.source.util.JavacTask t = com.sun.source.util.JavacTask.instance(processingEnv);
                TypeElement a = t.getElements().getTypeElement("pkg.A");
                if (superClass[0]) {
                    verifyTypeMirror(t, a.getSuperclass());
                } else {
                    verifyTypeMirror(t, a.getInterfaces().get(0));
                }
            }

            @Override
            public Set<String> getSupportedAnnotationTypes() {
                return Set.of("*");
            }

            @Override
            public SourceVersion getSupportedSourceVersion() {
                return SourceVersion.latest();
            }
        }

        for (boolean supClass : new boolean[] {false, true}) {
            for (boolean init : new boolean[] {false, true}) {
                String decl = supClass ? "class Test { }" : "interface Test { }";
                String snip = supClass ? "extends #" : "implements #";

                superClass[0] = supClass;
                inInit[0] = init;

                doTestComboCallBack(decl,
                                    "package pkg;" +
                                    "public class A " + snip + " {" +
                                    "}",
                                    "",
                                    "# a = null; a.toString();",
                                    (fqn, t) -> t.setProcessors(List.of(new TestAP())));
            }
        }
    }

    void testGetTypeElement() throws Exception {
        doTestCombo("class Test { }",
                    "package pkg;" +
                    "public class A extends # {" +
                    "}",
                    "",
                    "pkg.A a = null; a.toString();", //should be generalized/in variant?
                    (fqn, t) -> {
                          TypeElement a = t.getElements().getTypeElement(fqn);
                          if (a != null) {
                              throw new IllegalStateException();
                          }
                      });
    }

    void testScope() throws Exception {
        class Variant {
            private final String code;
            private final String fqn;
            public Variant(String code, String fqn) {
                this.code = code;
                this.fqn  = fqn;
            }
        }
        Path base = Paths.get(".");
        Path libClasses = compileLib(base,
                                     "package pkg;" +
                                     "public class A {" +
                                     "    public static class I {}" +
                                     "}",
                                     "package pkg;" +
                                     "public class B {" +
                                     "}");
        try (OutputStream out = Files.newOutputStream(libClasses.resolve("pkg/B.class"))) {
            out.write(0);
        }
        try (OutputStream out = Files.newOutputStream(libClasses.resolve("pkg/A$I.class"))) {
            out.write(0);
        }

        Path testSrc = base.resolve("test-src");
        tb.createDirectories(testSrc);
        Path testClasses = base.resolve("test-classes");
        tb.createDirectories(testClasses);

        Variant[] variants = new Variant[] {
            //JDK-8198378:
//            new Variant("package test;\n" +
//                        "import pkg.B;\n" +
//                        "public class Test {}\n",
//                        "test.Test"),
            new Variant("package test;\n" +
                        "import pkg.*;\n" +
                        "public class Test {}\n",
                        "test.Test"),
            new Variant("package test;\n" +
                        "import pkg.A.*;\n" +
                        "public class Test extends I {}\n",
                        "test.Test"),
            new Variant("package test;\n" +
                        "import static pkg.A.*;\n" +
                        "public class Test extends I {}\n",
                        "test.Test"),
            new Variant("package pkg;\n" +
                        "public class Test {}\n",
                        "pkg.Test")
        };
        for (Variant variant : variants) {
            System.err.println("variant: " + variant.code);
            tb.writeJavaFiles(testSrc, variant.code);
            tb.cleanDirectory(testClasses);

            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            List<String> errors = new ArrayList<>();

            try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
                com.sun.source.util.JavacTask task = (com.sun.source.util.JavacTask)
                        compiler.getTask(null,
                                         null,
                                         d -> errors.add(d.getCode()),
                                         Arrays.asList("-XDrawDiagnostics",
                                                       "-classpath",
                                                       libClasses.toString()),
                                         null,
                                         fm.getJavaFileObjects(tb.findJavaFiles(testSrc)));
                task.analyze();
                TypeElement a = task.getElements()
                                    .getTypeElement(task.getElements()
                                                        .getModuleElement(""),
                                                    variant.fqn);
                Trees trees = Trees.instance(task);
                TreePath tpA = trees.getPath(a);
                Scope scope = trees.getScope(tpA);
                while (scope != null) {
                    for (Element el : scope.getLocalElements()) {
                        verifyElement(task, el);
                    }
                    scope = scope.getEnclosingScope();
                }
            }
        }
    }

    void testGetEnclosingOnMissingType() throws Exception {
        Path base = Paths.get(".", "testGetEnclosingOnMissingType");
        Path libClasses = compileLib(base,
                                     "package pkg;\n" +
                                     "public class A<E> {\n" +
                                     "    public static class N<E> {}\n" +
                                     "}\n",
                                     "package pkg;\n" +
                                     "public class T<E> {\n" +
                                     "    T<A<T>> n;\n" +
                                     "}\n");
        try (OutputStream out = Files.newOutputStream(libClasses.resolve("pkg/A.class"))) {
            out.write(0);
        }

        Path testSrc = base.resolve("test-src");
        tb.createDirectories(testSrc);
        Path testClasses = base.resolve("test-classes");
        tb.createDirectories(testClasses);

        tb.writeJavaFiles(testSrc, "class Test { }");
        tb.cleanDirectory(testClasses);

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

        List<Consumer<DeclaredType>> validators = Arrays.asList(
                dt -> { if (dt.getEnclosingType().getKind() != TypeKind.NONE)
                            throw new AssertionError("Unexpected enclosing type: " +
                                                     dt.getEnclosingType());
                },
                dt -> { if (!"pkg.T<pkg.A<pkg.T>>".equals(dt.toString()))
                            throw new AssertionError("Unexpected toString: " +
                                                     dt.toString());
                }
        );

        try (StandardJavaFileManager fm = compiler.getStandardFileManager(null, null, null)) {
            for (Consumer<DeclaredType> validator : validators) {
                com.sun.source.util.JavacTask task = (com.sun.source.util.JavacTask)
                        compiler.getTask(null,
                                         null,
                                         null,
                                         Arrays.asList("-XDrawDiagnostics",
                                                       "-classpath",
                                                       libClasses.toString()),
                                         null,
                                         fm.getJavaFileObjects(tb.findJavaFiles(testSrc)));
                task.analyze();
                TypeElement a = task.getElements()
                                    .getTypeElement(task.getElements()
                                                        .getModuleElement(""),
                                                    "pkg.T");
                DeclaredType type = (DeclaredType) a.getEnclosedElements().get(0).asType();
                validator.accept(type);
            }
        }
    }

    private Path compileLib(Path base, String... sources) throws Exception {
        Path libSrc = base.resolve("lib-src");
        tb.createDirectories(libSrc);
        tb.writeJavaFiles(libSrc, sources);
        Path libClasses = base.resolve("lib-classes");
        tb.createDirectories(libClasses);
        new JavacTask(tb).outdir(libClasses.toString())
                         .sourcepath(libSrc.toString())
                         .files(tb.findJavaFiles(libSrc))
                         .run()
                         .writeAll();

        return libClasses;
    }

    private void doTestCombo(String decl,
                             String use,
                             String snippetInClass,
                             String snippetInMethod,
                             BiConsumer<String, com.sun.source.util.JavacTask> test) throws Exception {
        doTestComboCallBack(decl,
                            use,
                            snippetInClass,
                            snippetInMethod,
                            (fqn, t) -> {
            t.addTaskListener(new TaskListener() {
                @Override
                public void finished(TaskEvent e) {
                    if (e.getKind() == TaskEvent.Kind.ENTER) {
                        test.accept(fqn, t);
                    }
                }
            });
        });
    }

    private void doTestComboCallBack(String decl,
                                     String use,
                                     String snippetInClass,
                                     String snippetInMethod,
                                     BiConsumer<String, com.sun.source.util.JavacTask> callback) throws Exception {
        List<TestVariant> variants = List.of(
                new TestVariant("package pkg; public #", "pkg.Test", "pkg/Test.class"),
                new TestVariant("package pkg; public class O { public static # }", "pkg.O.Test", "pkg/O$Test.class"),
                new TestVariant("package pkg; public class O { public static # }", "pkg.O.Test", "pkg/O.class"),
                new TestVariant("package pkg; public class O { public static class N { public static # } }", "pkg.O.N.Test", "pkg/O$N$Test.class"),
                new TestVariant("package pkg; public class O { public static class N { public static # } }", "pkg.O.N.Test", "pkg/O$N.class"),
                new TestVariant("package pkg; public class O { public static class N { public static # } }", "pkg.O.N.Test", "pkg/O.class")
        );

        Path base = Paths.get(".");

        for (TestVariant v : variants) {
            System.err.println("-----------------------------------------------------------------------");
            System.err.println("variant: " + v.declarationStub + ", " + v.fqn + ", " + v.path);
            Path libClasses = compileLib(base,
                                         use.replace("#", v.fqn),
                                         v.declarationStub.replace("#", decl));

            Files.delete(libClasses.resolve(v.path));

            doRunTestFullCallback(base,
                                  t -> callback.accept(v.fqn, t),
                                  snippetInClass.replace("#", v.fqn),
                                  snippetInMethod.replace("#", v.fqn));
        }
    }

    private void doRunTest(Path base,
                           Consumer<com.sun.source.util.JavacTask> test,
                           String snippetInClass,
                           String snippetInMethod) throws Exception {
        doRunTestFullCallback(base, t -> {
            t.addTaskListener(new TaskListener() {
                @Override
                public void finished(TaskEvent e) {
                    if (e.getKind() == TaskEvent.Kind.ENTER) {
                        test.accept(t);
                    }
                }
            });
        }, snippetInClass, snippetInMethod);
    }

    private void doRunTestFullCallback(Path base,
                                       Consumer<com.sun.source.util.JavacTask> callback,
                                       String snippetInClass,
                                       String snippetInMethod) throws Exception {
        Path libClasses = base.resolve("lib-classes");
        Path testSrc = base.resolve("test-src");
        tb.createDirectories(testSrc);
        tb.writeJavaFiles(testSrc,
                          "package test;\n" +
                          "public class Test {\n" +
                          snippetInClass + "\n" +
                          "    void t() {\n" +
                          snippetInMethod + "\n" +
                          "    }\n" +
                          "}\n");
        System.err.println("content: " + "package test;\n" +
                          "public class Test {\n" +
                          snippetInClass + "\n" +
                          "    void t() {\n" +
                          snippetInMethod + "\n" +
                          "    }\n" +
                          "}\n");
        Path testClasses = base.resolve("test-classes");
        tb.createDirectories(testClasses);

        var expectedErrors = new JavacTask(tb).outdir(testClasses.toString())
                                              .options("-XDrawDiagnostics",
                                                       "-classpath",
                                                       libClasses.toString())
                                              .sourcepath(testSrc.toString())
                                              .files(tb.findJavaFiles(testSrc))
                                              .run(Expect.FAIL)
                                              .writeAll()
                                              .getOutputLines(OutputKind.DIRECT,
                                                              OutputKind.STDERR,
                                                              OutputKind.STDOUT);

        var errors = new JavacTask(tb).outdir(testClasses.toString())
                                      .options("-XDrawDiagnostics",
                                               "-classpath",
                                               libClasses.toString())
                                      .sourcepath(testSrc.toString())
                                      .files(tb.findJavaFiles(testSrc))
                                      .callback(callback)
                                      .run(Expect.FAIL)
                                      .writeAll()
                                      .getOutputLines(OutputKind.DIRECT,
                                                      OutputKind.STDERR,
                                                      OutputKind.STDOUT);

        if (!expectedErrors.equals(errors)) {
            throw new IllegalStateException("Expected error not found!");
        }
    }

    private void verifyTypeMirror(com.sun.source.util.JavacTask t, TypeMirror type) {
        Element el = t.getTypes().asElement(type);

        if (el != null) {
            verifyElement(t, el);
        }
    }

    private void verifyElement(com.sun.source.util.JavacTask t, Element el) {
        el.getKind(); //forces completion
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Unexpected value, expected: " + expected + ", actual: " + actual);
        }
    }

    public static void main(String... args) throws Exception {
        MissingClassFile t = new MissingClassFile();
        t.testPackageContent();
        t.testPackageDirectAPI();
        t.testSuperClass();
        t.testAnnotation();
        t.testAnnotationProcessing();
        t.testGetTypeElement();
        t.testScope();
        t.testGetEnclosingOnMissingType();
    }

    static class TestVariant {
        public final String declarationStub;
        public final String fqn;
        public final String path;

        public TestVariant(String declarationStub, String fqn, String path) {
            this.declarationStub = declarationStub;
            this.fqn = fqn;
            this.path = path;
        }

    }
}
