/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8205418 8207229 8207230 8230847 8245786 8247334 8248641 8240658 8246774
 * @summary Test the outcomes from Trees.getScope
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.comp
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 */

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.lang.model.element.Element;
import javax.tools.JavaCompiler;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.tree.AnnotationTree;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Scope;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.TreePath;
import com.sun.source.util.TreePathScanner;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacScope;
import com.sun.tools.javac.api.JavacTaskImpl;

import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.comp.Analyzer;
import com.sun.tools.javac.comp.AttrContext;
import com.sun.tools.javac.comp.Env;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCStatement;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Context.Factory;

import static javax.tools.JavaFileObject.Kind.SOURCE;

public class TestGetScopeResult {
    public static void main(String... args) throws IOException {
        new TestGetScopeResult().run();
        new TestGetScopeResult().testAnalyzerDisabled();
        new TestGetScopeResult().testVariablesInSwitch();
        new TestGetScopeResult().testMemberRefs();
        new TestGetScopeResult().testAnnotations();
        new TestGetScopeResult().testAnnotationsLazy();
        new TestGetScopeResult().testCircular();
        new TestGetScopeResult().testRecord();
        new TestGetScopeResult().testLocalRecordAnnotation();
        new TestGetScopeResult().testRuleCases();
    }

    public void run() throws IOException {
        String[] simpleLambda = {
            "s:java.lang.String",
            "i:Test.I",
            "super:java.lang.Object",
            "this:Test"
        };
        doTest("class Test { void test() { I i = s -> { }; } interface I { public void test(String s); } }",
               simpleLambda);
        doTest("class Test { void test() { I i = s -> { }; } interface I { public int test(String s); } }",
               simpleLambda);
        doTest("class Test { void test() { I i = s -> { }; } interface I { public String test(String s); } }",
               simpleLambda);
        doTest("class Test { void test() { I i; inv(s -> { }); } void inv(I i) { } interface I { public void test(String s); } }",
               simpleLambda);
        doTest("class Test { void test() { I i; inv(s -> { }); } void inv(I i) { } interface I { public int test(String s); } }",
               simpleLambda);
        doTest("class Test { void test() { I i; inv(s -> { }); } void inv(I i) { } interface I { public String test(String s); } }",
               simpleLambda);
        String[] dualLambda = {
            "s:java.lang.String",
            "i:Test.I1",
            "super:java.lang.Object",
            "this:Test",
            "s:java.lang.CharSequence",
            "i:Test.I1",
            "super:java.lang.Object",
            "this:Test"
        };
        doTest("class Test { void test() { I1 i; inv(s -> { }, s -> { }); } void inv(I1 i, I2 i) { } interface I1 { public String test(String s); } interface I2 { public void test(CharSequence s); } }",
               dualLambda);
        doTest("class Test { void test() { I1 i; inv(s -> { }, s -> { }); } void inv(I1 i, I2 i) { } interface I1 { public String test(String s); } interface I2 { public int test(CharSequence s); } }",
               dualLambda);
        String[] brokenType = {
            "s:<any>",
            "u:Undefined",
            "super:java.lang.Object",
            "this:Test"
        };
        doTest("class Test { void test() { Undefined u = s -> { }; } }",
               brokenType);
        String[] multipleCandidates1 = {
            "s:<any>",
            "super:java.lang.Object",
            "this:Test"
        };
        doTest("class Test { void test() { cand1(s -> { }); } void cand1(I1 i) { } void cand1(I2 i) { } interface I1 { public String test(String s); } interface I2 { public int test(CharSequence s); } }",
               multipleCandidates1);
        String[] multipleCandidates2 = {
            "s:java.lang.String",
            "super:java.lang.Object",
            "this:Test"
        };
        doTest("class Test { void test() { cand1(s -> { }); } void cand1(I1 i) { } void cand1(I2 i, int i) { } interface I1 { public String test(String s); } interface I2 { public int test(CharSequence s); } }",
               multipleCandidates2);

        String[] implicitExplicitConflict1 = {
            ":t",
            "s:java.lang.String",
            "super:java.lang.Object",
            "this:Test"
        };

        doTest("class Test { void test() { cand((var s, t) -> \"\"); } void cand(I i) { } interface I { public String test(String s); }  }",
               implicitExplicitConflict1);

        String[] implicitExplicitConflict2 = {
            "s:none",
            ":t",
            "super:java.lang.Object",
            "this:Test"
        };

        doTest("class Test { void test() { cand((t, var s) -> \"\"); } void cand(I i) { } interface I { public String test(String s); }  }",
               implicitExplicitConflict2);

        String[] noFunctionInterface = {
            "s:none",
            ":t",
            "super:java.lang.Object",
            "this:Test"
        };

        doTest("class Test { void test() { cand((t, var s) -> \"\"); } void cand(String s) { } }",
               noFunctionInterface);

        String[] invocationInMethodInvocation = {
            "d2:java.lang.Double",
            "d1:java.lang.Double",
            "super:java.lang.Object",
            "this:Test"
        };

        doTest("""
               class Test {
                   void test() { test(reduce(0.0, (d1, d2) -> 0)); }
                   void test(int i) {}
                   <T> T reduce(T t, BiFunction<T, T, T> f1) {}
                   static interface BiFunction<R, P, Q> {
                       R apply(P p, Q q);
                   }
               }""",
               invocationInMethodInvocation);

        String[] infer = {
            "c:java.lang.String",
            "super:java.lang.Object",
            "this:Test"
        };

        doTest("class Test { void test() { cand(\"\", c -> { }); } <T>void cand(T t, I<T> i) { } interface I<T> { public String test(T s); }  }",
               infer);
        doTest("class Test { void test() { cand(\"\", c -> { }); } <T>void cand(T t, I<T> i, int j) { } interface I<T> { public void test(T s); }  }",
               infer);
        doTest("class Test { void test() { cand(\"\", c -> { }); } <T>void cand(T t, I<T> i, int j) { } interface I<T> { public String test(T s); }  }",
               infer);
    }

    public void doTest(String code, String... expected) throws IOException {
        JavaCompiler c = ToolProvider.getSystemJavaCompiler();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            class MyFileObject extends SimpleJavaFileObject {
                MyFileObject() {
                    super(URI.create("myfo:///Test.java"), SOURCE);
                }
                @Override
                public String getCharContent(boolean ignoreEncodingErrors) {
                    return code;
                }
            }
            JavacTask t = (JavacTask) c.getTask(null, fm, null, null, null, List.of(new MyFileObject()));
            CompilationUnitTree cut = t.parse().iterator().next();

            ((JavacTaskImpl)t).enter();

            for (int r = 0; r < 2; r++) {
                List<String> actual = new ArrayList<>();

                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitLambdaExpression(LambdaExpressionTree node, Void p) {
                        Scope scope = Trees.instance(t).getScope(new TreePath(getCurrentPath(), node.getBody()));
                        actual.addAll(dumpScope(scope));
                        return super.visitLambdaExpression(node, p);
                    }
                }.scan(cut, null);

                List<String> expectedList = List.of(expected);

                if (!expectedList.equals(actual)) {
                    throw new IllegalStateException("Unexpected scope content: " + actual + "\n" +
                                                     "expected: " + expectedList);
                }

                t.analyze();
            }
        }
    }

    void testAnalyzerDisabled() throws IOException {
        JavacTool c = JavacTool.create();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            class MyFileObject extends SimpleJavaFileObject {
                MyFileObject() {
                    super(URI.create("myfo:///Test.java"), SOURCE);
                }
                @Override
                public String getCharContent(boolean ignoreEncodingErrors) {
                    return "class Test {" +
                           "    void test() { cand(() -> { System.err.println(); }); }" +
                           "    Runnable r = new Runnable() { public void test() { System.err.println(); } };" +
                           "    void cand(Runnable r) { }" +
                           "}";
                }
            }
            Context ctx = new Context();
            TestAnalyzer.preRegister(ctx);
            JavacTask t = (JavacTask) c.getTask(null, fm, null, List.of("-XDfind=lambda"), null,
                                                List.of(new MyFileObject()), ctx);
            CompilationUnitTree cut = t.parse().iterator().next();
            t.analyze();

            TestAnalyzer analyzer = (TestAnalyzer) TestAnalyzer.instance(ctx);

            if (!analyzer.analyzeCalled) {
                throw new IllegalStateException("Analyzer didn't run!");
            }

            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitLambdaExpression(LambdaExpressionTree node, Void p) {
                    analyzer.analyzeCalled = false;
                    Trees.instance(t).getScope(new TreePath(getCurrentPath(), node.getBody()));
                    if (analyzer.analyzeCalled) {
                        throw new IllegalStateException("Analyzer was run during getScope!");
                    }
                    return super.visitLambdaExpression(node, p);
                }

                @Override
                public Void visitVariable(VariableTree node, Void p) {
                    if (node.getInitializer() != null) {
                        analyzer.analyzeCalled = false;
                        TreePath tp = new TreePath(getCurrentPath(), node.getInitializer());
                        Trees.instance(t).getScope(tp);
                        if (analyzer.analyzeCalled) {
                            throw new IllegalStateException("Analyzer was run during getScope!");
                        }
                    }
                    return super.visitVariable(node, p);
                }
            }.scan(cut, null);
        }
    }

    private static final class TestAnalyzer extends Analyzer {

        public static void preRegister(Context context) {
            context.put(analyzerKey, (Factory<Analyzer>) ctx -> new TestAnalyzer(ctx));
        }

        private boolean analyzeCalled;

        public TestAnalyzer(Context context) {
            super(context);
        }

        @Override
        protected void analyze(JCStatement statement, Env<AttrContext> env) {
            analyzeCalled = true;
            super.analyze(statement, env);
        }
    }

    void testVariablesInSwitch() throws IOException {
        JavacTool c = JavacTool.create();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            class MyFileObject extends SimpleJavaFileObject {
                MyFileObject() {
                    super(URI.create("myfo:///Test.java"), SOURCE);
                }
                @Override
                public String getCharContent(boolean ignoreEncodingErrors) {
                    return "class Test {" +
                           "    void test() {\n" +
                           "        E e = E.A;\n" +
                           "        Object o = E.A;\n" +
                           "        switch (e) {\n" +
                           "            case A:\n" +
                           "                return;\n" +
                           "            case B:\n" +
                           "                test();\n" +
                           "                E ee = null;\n" +
                           "                break;\n" +
                           "        }\n" +
                           "    }\n" +
                           "    enum E {A, B}\n" +
                           "}";
                }
            }
            Context ctx = new Context();
            TestAnalyzer.preRegister(ctx);
            JavacTask t = (JavacTask) c.getTask(null, fm, null, null, null,
                                                List.of(new MyFileObject()), ctx);
            CompilationUnitTree cut = t.parse().iterator().next();
            t.analyze();

            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitMethodInvocation(MethodInvocationTree node, Void p) {
                    Trees.instance(t).getScope(getCurrentPath());
                    return super.visitMethodInvocation(node, p);
                }
            }.scan(cut, null);
        }
    }

    void testMemberRefs() throws IOException {
        JavacTool c = JavacTool.create();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            class MyFileObject extends SimpleJavaFileObject {
                MyFileObject() {
                    super(URI.create("myfo:///Test.java"), SOURCE);
                }
                @Override
                public String getCharContent(boolean ignoreEncodingErrors) {
                    return "class Test {" +
                           "    void test() {\n" +
                           "        Test t = this;\n" +
                           "        Runnable r1 = t::test;\n" +
                           "        Runnable r2 = true ? t::test : t::test;\n" +
                           "        c(t::test);\n" +
                           "        c(true ? t::test : t::test);\n" +
                           "    }\n" +
                           "    void c(Runnable r) {}\n" +
                           "}";
                }
            }
            Context ctx = new Context();
            TestAnalyzer.preRegister(ctx);
            JavacTask t = (JavacTask) c.getTask(null, fm, null, null, null,
                                                List.of(new MyFileObject()), ctx);
            CompilationUnitTree cut = t.parse().iterator().next();
            t.analyze();

            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitConditionalExpression(ConditionalExpressionTree node, Void p) {
                    Trees.instance(t).getScope(new TreePath(getCurrentPath(), node.getCondition()));
                    return super.visitConditionalExpression(node, p);
                }

                @Override
                public Void visitBlock(BlockTree node, Void p) {
                    Trees.instance(t).getScope(getCurrentPath());
                    return super.visitBlock(node, p);
                }
            }.scan(cut, null);
        }
    }

    void testAnnotations() throws IOException {
        JavacTool c = JavacTool.create();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            class MyFileObject extends SimpleJavaFileObject {
                MyFileObject() {
                    super(URI.create("myfo:///Test.java"), SOURCE);
                }
                @Override
                public String getCharContent(boolean ignoreEncodingErrors) {
                    return "class Test {" +
                           "    void test() {\n" +
                           "        new Object() {\n" +
                           "            @A\n" +
                           "            public String t() { return null; }\n" +
                           "        };\n" +
                           "    }\n" +
                           "    @interface A {}\n" +
                           "}";
                }
            }
            Context ctx = new Context();
            TestAnalyzer.preRegister(ctx);
            JavacTask t = (JavacTask) c.getTask(null, fm, null, null, null,
                                                List.of(new MyFileObject()), ctx);
            CompilationUnitTree cut = t.parse().iterator().next();
            t.analyze();

            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitIdentifier(IdentifierTree node, Void p) {
                    if (node.getName().contentEquals("A")) {
                        Trees.instance(t).getScope(getCurrentPath());
                    }
                    return super.visitIdentifier(node, p);
                }

                @Override
                public Void visitMethod(MethodTree node, Void p) {
                    super.visitMethod(node, p);
                    if (node.getReturnType() != null) {
                        Trees.instance(t).getScope(new TreePath(getCurrentPath(), node.getReturnType()));
                    }
                    return null;
                }
            }.scan(cut, null);
        }
    }

    void testAnnotationsLazy() throws IOException {
        JavacTool c = JavacTool.create();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            class MyFileObject extends SimpleJavaFileObject {
                MyFileObject() {
                    super(URI.create("myfo:///Test.java"), SOURCE);
                }
                @Override
                public String getCharContent(boolean ignoreEncodingErrors) {
                    return "import java.lang.annotation.*;\n" +
                           "\n" +
                           "class ClassA {\n" +
                           "    Object o = ClassB.lcv;\n" +
                           "}\n" +
                           "\n" +
                           "class ClassB {\n" +
                           "    static final String[] lcv = new @TA String[0];\n" +
                           "}\n" +
                           "\n" +
                           "class ClassC {\n" +
                           "    static final Object o = (@TA Object) null;\n" +
                           "}\n" +
                           "\n" +
                           "@Target({ElementType.TYPE_USE, ElementType.TYPE_PARAMETER})\n" +
                           "@interface TA {}\n";
                }
            }
            Context ctx = new Context();
            TestAnalyzer.preRegister(ctx);
            JavacTask t = (JavacTask) c.getTask(null, fm, null, null, null,
                                                List.of(new MyFileObject()), ctx);
            t.addTaskListener(new TaskListener() {
                @Override
                public void finished(TaskEvent e) {
                    if (e.getKind() == TaskEvent.Kind.ANALYZE) {
                        new TreePathScanner<Void, Void>() {
                            @Override
                            public Void scan(Tree tree, Void p) {
                                if (tree != null) {
                                    Trees.instance(t).getScope(new TreePath(getCurrentPath(), tree));
                                }
                                return super.scan(tree, p);
                            }
                        }.scan(Trees.instance(t).getPath(e.getTypeElement()), null);
                    }
                }
            });

            t.call();
        }
    }

    void testCircular() throws IOException {
        JavacTool c = JavacTool.create();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            class MyFileObject extends SimpleJavaFileObject {
                MyFileObject() {
                    super(URI.create("myfo:///Test.java"), SOURCE);
                }
                @Override
                public String getCharContent(boolean ignoreEncodingErrors) {
                    return "class Test extends Test {" +
                           "    {\n" +
                           "        int i;\n" +
                           "    }\n" +
                           "}";
                }
            }
            Context ctx = new Context();
            TestAnalyzer.preRegister(ctx);
            JavacTask t = (JavacTask) c.getTask(null, fm, null, null, null,
                                                List.of(new MyFileObject()), ctx);
            CompilationUnitTree cut = t.parse().iterator().next();
            t.analyze();

            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitBlock(BlockTree node, Void p) {
                    Trees.instance(t).getScope(getCurrentPath());
                    return super.visitBlock(node, p);
                }
            }.scan(cut, null);
        }
    }

    void testRecord() throws IOException {
        JavacTool c = JavacTool.create();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            class MyFileObject extends SimpleJavaFileObject {
                MyFileObject() {
                    super(URI.create("myfo:///Test.java"), SOURCE);
                }
                @Override
                public String getCharContent(boolean ignoreEncodingErrors) {
                    return "record Test<T>(int mark) {}";
                }
            }
            Context ctx = new Context();
            TestAnalyzer.preRegister(ctx);
            JavacTask t = (JavacTask) c.getTask(null, fm, null, null, null,
                                                List.of(new MyFileObject()), ctx);
            CompilationUnitTree cut = t.parse().iterator().next();
            t.analyze();

            List<String> actual = new ArrayList<>();

            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitClass(ClassTree node, Void p) {
                    Scope scope = Trees.instance(t).getScope(getCurrentPath());
                    actual.addAll(dumpScope(scope));
                    return super.visitClass(node, p);
                }
            }.scan(cut, null);

            List<String> expected = List.of(
                    "super:java.lang.Record",
                    "this:Test<T>",
                    "T:T"
            );

            if (!expected.equals(actual)) {
                throw new AssertionError("Unexpected Scope content: " + actual);
            }
        }
    }

    void testLocalRecordAnnotation() throws IOException {
        JavacTool c = JavacTool.create();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            class Variant {
                final String code;
                final List<List<String>> expectedScopeContent;
                public Variant(String code, List<List<String>> expectedScopeContent) {
                    this.code = code;
                    this.expectedScopeContent = expectedScopeContent;
                }
            }
            Variant[] variants = new Variant[] {
                new Variant("""
                            class Test {
                                void t() {
                                    record R(@Annotation int i) {
                                        void stop () {}
                                    }
                                }
                            }
                            @interface Annotation {}
                            """,
                            List.of(
                                List.of("super:java.lang.Object", "this:Test"),
                                List.of("super:java.lang.Object", "this:Test")
                            )),
                new Variant("""
                            record Test(@Annotation int i) {}
                            @interface Annotation {}
                            """,
                            List.of(
                                List.of("i:int", "super:java.lang.Record", "this:Test"),
                                List.of("super:java.lang.Record", "this:Test")
                            ))
            };
            for (Variant currentVariant : variants) {
                class MyFileObject extends SimpleJavaFileObject {
                    MyFileObject() {
                        super(URI.create("myfo:///Test.java"), SOURCE);
                    }
                    @Override
                    public String getCharContent(boolean ignoreEncodingErrors) {
                        return currentVariant.code;
                    }
                }
                Context ctx = new Context();
                TestAnalyzer.preRegister(ctx);
                JavacTask t = (JavacTask) c.getTask(null, fm, null, null, null,
                                                    List.of(new MyFileObject()), ctx);
                CompilationUnitTree cut = t.parse().iterator().next();
                t.analyze();

                List<List<String>> actual = new ArrayList<>();

                new TreePathScanner<Void, Void>() {
                    @Override
                    public Void visitAnnotation(AnnotationTree node, Void p) {
                        Scope scope = Trees.instance(t).getScope(getCurrentPath());
                        actual.add(dumpScope(scope));
                        return super.visitAnnotation(node, p);
                    }
                }.scan(cut, null);

                if (!currentVariant.expectedScopeContent.equals(actual)) {
                    throw new AssertionError("Unexpected Scope content: " + actual);
                }
            }
        }
    }

    void testRuleCases() throws IOException {
        JavacTool c = JavacTool.create();
        try (StandardJavaFileManager fm = c.getStandardFileManager(null, null, null)) {
            String code = """
                          class Test {
                              void t(int i) {
                                  long local;
                                  System.err.println(switch (i) {
                                    case 0 -> {
                                        String var;
                                        int scopeHere;
                                        yield "";
                                    }
                                    default -> {
                                        String var;
                                        int scopeHere;
                                        yield "";
                                    }
                                  });
                                  switch (i) {
                                    case 0 -> {
                                        String var;
                                        int scopeHere;
                                    }
                                    default -> {
                                        String var;
                                        int scopeHere;
                                    }
                                  };
                                  switch (i) {
                                    case 0: {
                                        int checkTree;
                                    }
                                  }
                              }
                          }
                          """;
            class MyFileObject extends SimpleJavaFileObject {
                MyFileObject() {
                    super(URI.create("myfo:///Test.java"), SOURCE);
                }
                @Override
                public String getCharContent(boolean ignoreEncodingErrors) {
                    return code;
                }
            }
            Context ctx = new Context();
            TestAnalyzer.preRegister(ctx);
            JavacTask t = (JavacTask) c.getTask(null, fm, null, null, null,
                                                List.of(new MyFileObject()), ctx);
            CompilationUnitTree cut = t.parse().iterator().next();
            t.analyze();

            List<List<String>> actual = new ArrayList<>();

            new TreePathScanner<Void, Void>() {
                @Override
                public Void visitVariable(VariableTree node, Void p) {
                    if (node.getName().contentEquals("scopeHere")) {
                        Scope scope = Trees.instance(t).getScope(getCurrentPath());
                        actual.add(dumpScope(scope));
                        JCTree body = getCaseBody(scope);
                        if (body == null) {
                            throw new AssertionError("Unexpected null body.");
                        }
                    } else if (node.getName().contentEquals("checkTree")) {
                        Scope scope = Trees.instance(t).getScope(getCurrentPath());
                        JCTree body = getCaseBody(scope);
                        if (body != null) {
                            throw new AssertionError("Unexpected body tree: " + body);
                        }
                    }
                    return super.visitVariable(node, p);
                }
                JCTree getCaseBody(Scope scope) {
                    return ((JCCase) ((JavacScope) scope).getEnv().next.next.tree).body;
                }
            }.scan(cut, null);

            List<List<String>> expected =
                    Collections.nCopies(4,
                                        List.of("scopeHere:int",
                                                "var:java.lang.String",
                                                "local:long",
                                                "i:int",
                                                "super:java.lang.Object",
                                                "this:Test"
                                            ));

            if (!expected.equals(actual)) {
                throw new AssertionError("Unexpected Scope content: " + actual);
            }
        }
    }

    private List<String> dumpScope(Scope scope) {
        List<String> content = new ArrayList<>();
        while (scope.getEnclosingClass() != null) {
            for (Element el : scope.getLocalElements()) {
                content.add(el.getSimpleName() + ":" +el.asType().toString());
            }
            scope = scope.getEnclosingScope();
        }
        return content;
    }
}
