/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8131025 8141092 8153761 8145263 8131019 8175886 8176184 8176241 8176110 8177466 8197439 8221759 8234896 8240658
 * @summary Test Completion and Documentation
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 *          jdk.jshell/jdk.jshell:open
 * @build toolbox.ToolBox toolbox.JarTask toolbox.JavacTask
 * @build KullaTesting TestingInputStream Compiler
 * @run testng CompletionSuggestionTest
 */

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.HashSet;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import jdk.jshell.Snippet;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jdk.jshell.Snippet.Status.VALID;
import static jdk.jshell.Snippet.Status.OVERWRITTEN;

@Test
public class CompletionSuggestionTest extends KullaTesting {

    private final Compiler compiler = new Compiler();
    private final Path outDir = Paths.get("completion_suggestion_test");

    public void testMemberExpr() {
        assertEval("class Test { static void test() { } }");
        assertCompletion("Test.t|", "test()");
        assertEval("Test ccTestInstance = new Test();");
        assertCompletion("ccTestInstance.t|", "toString()");
        assertCompletion(" ccTe|", "ccTestInstance");
        assertCompletion("String value = ccTestInstance.to|", "toString()");
        assertCompletion("java.util.Coll|", "Collection", "Collections");
        assertCompletion("String.cla|", "class");
        assertCompletion("boolean.cla|", "class");
        assertCompletion("byte.cla|", "class");
        assertCompletion("short.cla|", "class");
        assertCompletion("char.cla|", "class");
        assertCompletion("int.cla|", "class");
        assertCompletion("float.cla|", "class");
        assertCompletion("long.cla|", "class");
        assertCompletion("double.cla|", "class");
        assertCompletion("void.cla|", "class");
        assertCompletion("Object[].|", "class");
        assertCompletion("int[].|", "class");
        assertEval("Object[] ao = null;");
        assertCompletion("int i = ao.|", "length");
        assertEval("int[] ai = null;");
        assertCompletion("int i = ai.|", "length");
        assertCompletionIncludesExcludes("\"\".|",
                new HashSet<>(Collections.emptyList()),
                new HashSet<>(Arrays.asList("String(")));
        assertEval("double d = 0;");
        assertEval("void m() {}");
        assertCompletionIncludesExcludes("d.|",
                new HashSet<>(Collections.emptyList()),
                new HashSet<>(Arrays.asList("class")));
        assertCompletionIncludesExcludes("m().|",
                new HashSet<>(Collections.emptyList()),
                new HashSet<>(Arrays.asList("class")));
        assertEval("class C {class D {} static class E {} enum F {} interface H {} void method() {} int number;}");
        assertCompletionIncludesExcludes("C.|",
                new HashSet<>(Arrays.asList("D", "E", "F", "H", "class")),
                new HashSet<>(Arrays.asList("method()", "number")));
        assertCompletionIncludesExcludes("new C().|",
                new HashSet<>(Arrays.asList("method()", "number")),
                new HashSet<>(Arrays.asList("D", "E", "F", "H", "class")));
        assertCompletionIncludesExcludes("new C() {}.|",
                new HashSet<>(Arrays.asList("method()", "number")),
                new HashSet<>(Arrays.asList("D", "E", "F", "H", "class")));
        assertCompletion("\"\".leng|", "length()");
        assertCompletion("\"\"\"\n\"\"\".leng|", "length()");
    }

    public void testStartOfExpression() {
        assertEval("int ccTest = 0;");
        assertCompletion("System.err.println(cc|", "ccTest");
        assertCompletion("for (int i = cc|", "ccTest");
    }

    public void testParameter() {
        assertCompletion("class C{void method(int num){num|", "num");
    }

    public void testPrimitive() {
        Set<String> primitives = new HashSet<>(Arrays.asList("boolean", "char", "byte", "short", "int", "long", "float", "double"));
        Set<String> onlyVoid = new HashSet<>(Collections.singletonList("void"));
        Set<String> primitivesOrVoid = new HashSet<>(primitives);
        primitivesOrVoid.addAll(onlyVoid);

        assertCompletionIncludesExcludes("|",
                primitivesOrVoid,
                new HashSet<>(Collections.emptyList()));
        assertCompletionIncludesExcludes("int num = |",
                primitivesOrVoid,
                new HashSet<>(Collections.emptyList()));
        assertCompletionIncludesExcludes("num = |",
                primitivesOrVoid,
                new HashSet<>(Collections.emptyList()));
        assertCompletionIncludesExcludes("class C{void m() {|",
                primitivesOrVoid,
                new HashSet<>(Collections.emptyList()));
        assertCompletionIncludesExcludes("void method(|",
                primitives,
                onlyVoid);
        assertCompletionIncludesExcludes("void method(int num, |",
                primitives,
                onlyVoid);
        assertCompletion("new java.util.ArrayList<doub|");
        assertCompletion("class A extends doubl|");
        assertCompletion("class A implements doubl|");
        assertCompletion("interface A extends doubl|");
        assertCompletion("enum A implements doubl|");
        assertCompletion("class A<T extends doubl|");
    }

    public void testEmpty() {
        assertCompletionIncludesExcludes("|",
                new HashSet<>(Arrays.asList("Object", "Void")),
                new HashSet<>(Arrays.asList("$REPL00DOESNOTMATTER")));
        assertCompletionIncludesExcludes("V|",
                new HashSet<>(Collections.singletonList("Void")),
                new HashSet<>(Collections.singletonList("Object")));
        assertCompletionIncludesExcludes("{ |",
                new HashSet<>(Arrays.asList("Object", "Void")),
                new HashSet<>(Arrays.asList("$REPL00DOESNOTMATTER")));
    }

    public void testSmartCompletion() {
        assertEval("int ccTest1 = 0;");
        assertEval("int ccTest2 = 0;");
        assertEval("String ccTest3 = null;");
        assertEval("void method(int i, String str) { }");
        assertEval("void method(String str, int i) { }");
        assertEval("java.util.List<String> list = null;");
        assertCompletion("int ccTest4 = |", true, "ccTest1", "ccTest2");
        assertCompletion("ccTest2 = |", true, "ccTest1", "ccTest2");
        assertCompletion("int ccTest4 = ccTe|", "ccTest1", "ccTest2", "ccTest3");
        assertCompletion("int ccTest4 = ccTest3.len|", true, "length()");
        assertCompletion("method(|", true, "ccTest1", "ccTest2", "ccTest3");
        assertCompletion("method(0, |", true, "ccTest3");
        assertCompletion("list.add(|", true, "ccTest1", "ccTest2", "ccTest3");
        assertCompletion("list.add(0, |", true, "ccTest3");
        assertCompletion("new String(|", true, "ccTest3");
        assertCompletion("new String(new char[0], |", true, "ccTest1", "ccTest2");
        assertCompletionIncludesExcludes("new jav|", new HashSet<>(Arrays.asList("java.", "javax.")), Collections.emptySet());
        assertCompletion("Class<String> clazz = String.c|", true, "class");

        Snippet klass = classKey(assertEval("class Klass {void method(int n) {} private void method(String str) {}}"));
        assertCompletion("new Klass().method(|", true, "ccTest1", "ccTest2");
        Snippet klass2 = classKey(assertEval("class Klass {static void method(int n) {} void method(String str) {}}",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(klass, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
        assertCompletion("Klass.method(|", true, "ccTest1", "ccTest2");
        assertEval("class Klass {Klass(int n) {} private Klass(String str) {}}",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(klass2, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertCompletion("new Klass(|", true, "ccTest1", "ccTest2");
    }

    public void testSmartCompletionInOverriddenMethodInvocation() {
        assertEval("int ccTest1 = 0;");
        assertEval("int ccTest2 = 0;");
        assertEval("String ccTest3 = null;");
        assertCompletion("\"\".wait(|", true, "ccTest1", "ccTest2");
        assertEval("class Base {void method(int n) {}}");
        assertEval("class Extend extends Base {}");
        assertCompletion("new Extend().method(|", true, "ccTest1", "ccTest2");
    }

    public void testSmartCompletionForBoxedType() {
        assertEval("int ccTest1 = 0;");
        assertEval("Integer ccTest2 = 0;");
        assertEval("Object ccTest3 = null;");
        assertEval("int method1(int n) {return n;}");
        assertEval("Integer method2(Integer n) {return n;}");
        assertEval("Object method3(Object o) {return o;}");
        assertCompletion("int ccTest4 = |", true, "ccTest1", "ccTest2", "method1(", "method2(");
        assertCompletion("Integer ccTest4 = |", true, "ccTest1", "ccTest2", "method1(", "method2(");
        assertCompletion("Object ccTest4 = |", true, "ccTest1", "ccTest2", "ccTest3", "method1(", "method2(", "method3(");
        assertCompletion("method1(|", true, "ccTest1", "ccTest2", "method1(", "method2(");
        assertCompletion("method2(|", true, "ccTest1", "ccTest2", "method1(", "method2(");
        assertCompletion("method3(|", true, "ccTest1", "ccTest2", "ccTest3", "method1(", "method2(", "method3(");
    }

    public void testNewClass() {
        assertCompletion("String str = new Strin|", "String(", "StringBuffer(", "StringBuilder(", "StringIndexOutOfBoundsException(");
        assertCompletion("String str = new java.lang.Strin|", "String(", "StringBuffer(", "StringBuilder(", "StringIndexOutOfBoundsException(");
        assertCompletion("String str = new |", true, "String(");
        assertCompletion("String str = new java.lang.|", true, "String(");
        assertCompletion("throw new Strin|", true, "StringIndexOutOfBoundsException(");

        assertEval("class A{class B{} class C {C(int n) {}} static class D {} interface I {}}");
        assertEval("A a;");
        assertCompletion("new A().new |", "B()", "C(");
        assertCompletion("a.new |", "B()", "C(");
        assertCompletion("new A.|", "D()");

        assertEval("enum E{; class A {}}");
        assertEval("interface I{; class A {}}");
        assertCompletion("new E.|", "A()");
        assertCompletion("new I.|", "A()");
        assertCompletion("new String(I.A|", "A");
    }

    public void testFullyQualified() {
        assertCompletion("Optional<String> opt = java.u|", "util.");
        assertCompletionIncludesExcludes("Optional<Strings> opt = java.util.O|", new HashSet<>(Collections.singletonList("Optional")), Collections.emptySet());

        assertEval("void method(java.util.Optional<String> opt) {}");
        assertCompletion("method(java.u|", "util.");

        assertCompletion("Object.notElement.|");
        assertCompletion("Object o = com.su|", "sun.");

        Path p1 = outDir.resolve("dir1");
        compiler.compile(p1,
                "package p1.p2;\n" +
                "public class Test {\n" +
                "}",
                "package p1.p3;\n" +
                "public class Test {\n" +
                "}");
        String jarName = "test.jar";
        compiler.jar(p1, jarName, "p1/p2/Test.class", "p1/p3/Test.class");
        addToClasspath(compiler.getPath(p1.resolve(jarName)));

        assertCompletionIncludesExcludes("|", new HashSet<>(Collections.singletonList("p1.")), Collections.emptySet());
        assertCompletion("p1.|", "p2.", "p3.");
        assertCompletion("p1.p2.|", "Test");
        assertCompletion("p1.p3.|", "Test");
    }

    public void testCheckAccessibility() {
        assertCompletion("java.util.regex.Pattern.co|", "compile(");
    }

    public void testCompletePackages() {
        assertCompletion("java.u|", "util.");
        assertCompletionIncludesExcludes("jav|", new HashSet<>(Arrays.asList("java.", "javax.")), Collections.emptySet());
    }

    public void testImports() {
        assertCompletion("import java.u|", "util.");
        assertCompletionIncludesExcludes("import jav|", new HashSet<>(Arrays.asList("java.", "javax.")), Collections.emptySet());
        assertCompletion("import static java.u|", "util.");
        assertCompletionIncludesExcludes("import static jav|", new HashSet<>(Arrays.asList("java.", "javax.")), Collections.emptySet());
        assertCompletion("import static java.lang.Boolean.g|", "getBoolean");
        assertCompletion("import java.util.*|");
        assertCompletionIncludesExcludes("import java.lang.String.|",
                Collections.emptySet(),
                new HashSet<>(Arrays.asList("CASE_INSENSITIVE_ORDER", "copyValueOf", "format", "join", "valueOf", "class", "length")));
        assertCompletionIncludesExcludes("import static java.lang.String.|",
                new HashSet<>(Arrays.asList("CASE_INSENSITIVE_ORDER", "copyValueOf", "format", "join", "valueOf")),
                new HashSet<>(Arrays.asList("class", "length")));
        assertCompletionIncludesExcludes("import java.util.Map.|",
                new HashSet<>(Arrays.asList("Entry")),
                new HashSet<>(Arrays.asList("class")));
    }

    public void testImportStart() {
        assertCompletionIncludesExcludes("import c|", Set.of("com."), Set.of());
    }

    public void testBrokenClassFile() throws Exception {
        Compiler compiler = new Compiler();
        Path testOutDir = Paths.get("CompletionTestBrokenClassFile");
        String input = "package test.inner; public class Test {}";
        compiler.compile(testOutDir, input);
        addToClasspath(compiler.getPath(testOutDir).resolve("test"));
        assertCompletion("import inner.|");
    }

    public void testDocumentation() throws Exception {
        dontReadParameterNamesFromClassFile();
        assertSignature("System.getProperty(|",
                "String System.getProperty(String key)",
                "String System.getProperty(String key, String def)");
        assertEval("char[] chars = null;");
        assertSignature("new String(chars, |",
                "String(char[], int, int)");
        assertSignature("String.format(|",
                "String String.format(String, Object...)",
                "String String.format(java.util.Locale, String, Object...)");
        assertSignature("\"\".getBytes(\"\"|", "void String.getBytes(int, int, byte[], int)",
                                                    "byte[] String.getBytes(String) throws java.io.UnsupportedEncodingException",
                                                    "byte[] String.getBytes(java.nio.charset.Charset)");
        assertSignature("\"\".getBytes(\"\" |", "void String.getBytes(int, int, byte[], int)",
                                                     "byte[] String.getBytes(String) throws java.io.UnsupportedEncodingException",
                                                     "byte[] String.getBytes(java.nio.charset.Charset)");
        //JDK-8221759:
        Compiler compiler = new Compiler();
        Path testOutDir = Paths.get("WithPrivateField");
        String input = "package field; public class FieldTest { private static String field; private static String field2; }";
        compiler.compile(testOutDir, input);
        addToClasspath(compiler.getPath(testOutDir));
        assertSignature("field.FieldTest.field|");
        assertSignature("field.FieldTest.field2|");
    }

    public void testMethodsWithNoArguments() throws Exception {
        dontReadParameterNamesFromClassFile();
        assertSignature("System.out.println(|",
                "void java.io.PrintStream.println()",
                "void java.io.PrintStream.println(boolean)",
                "void java.io.PrintStream.println(char)",
                "void java.io.PrintStream.println(int)",
                "void java.io.PrintStream.println(long)",
                "void java.io.PrintStream.println(float)",
                "void java.io.PrintStream.println(double)",
                "void java.io.PrintStream.println(char[])",
                "void java.io.PrintStream.println(String)",
                "void java.io.PrintStream.println(Object)");
    }

    public void testErroneous() {
        assertCompletion("Undefined.|");
        assertSignature("does.not.exist|");
    }

    public void testClinit() {
        assertEval("enum E{;}");
        assertEval("class C{static{}}");
        assertCompletionIncludesExcludes("E.|", Collections.emptySet(), new HashSet<>(Collections.singletonList("<clinit>")));
        assertCompletionIncludesExcludes("C.|", Collections.emptySet(), new HashSet<>(Collections.singletonList("<clinit>")));
    }

    public void testMethodHeaderContext() {
        assertCompletion("private void f(Runn|", "Runnable");
        assertCompletion("void f(Runn|", "Runnable");
        assertCompletion("void f(Object o1, Runn|", "Runnable");
        assertCompletion("void f(Object o1) throws Num|", true, "NumberFormatException");
        assertCompletion("void f(Object o1) throws java.lang.Num|", true, "NumberFormatException");
        assertEval("class HogeHoge {static class HogeHogeException extends Exception {}}");
        assertCompletion("void f(Object o1) throws Hoge|", "HogeHoge");
        assertCompletion("void f(Object o1) throws HogeHoge.|", true, "HogeHogeException");
    }

    public void testTypeVariables() {
        assertCompletion("class A<TYPE> { public void test() { TY|", "TYPE");
        assertCompletion("class A<TYPE> { public static void test() { TY|");
        assertCompletion("class A<TYPE> { public <TYPE> void test() { TY|", "TYPE");
        assertCompletion("class A<TYPE> { public static <TYPE> void test() { TY|", "TYPE");
    }

    public void testGeneric() {
        assertEval("import java.util.concurrent.*;");
        assertCompletion("java.util.List<Integ|", "Integer");
        assertCompletion("class A<TYPE extends Call|", "Callable");
        assertCompletion("class A<TYPE extends Callable<TY|", "TYPE");
        assertCompletion("<TYPE> void f(TY|", "TYPE");
        assertCompletion("class A<TYPE extends Callable<? sup|", "super");
        assertCompletion("class A<TYPE extends Callable<? super TY|", "TYPE");
    }

    public void testFields() {
        assertEval("interface Interface { int field = 0; }");
        Snippet clazz = classKey(assertEval("class Clazz {" +
                "static int staticField = 0;" +
                "int field = 0;" +
                " }"));
        assertCompletion("Interface.fiel|", "field");
        assertCompletion("Clazz.staticFiel|", "staticField");
        assertCompletion("new Interface() {}.fiel|");
        assertCompletion("new Clazz().staticFiel|");
        assertCompletion("new Clazz().fiel|", "field");
        assertCompletion("new Clazz() {}.fiel|", "field");
        assertEval("class Clazz implements Interface {}",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(clazz, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertCompletion("Clazz.fiel|", "field");
        assertCompletion("new Clazz().fiel|");
        assertCompletion("new Clazz() {}.fiel|");
    }

    public void testMethods() {
        assertEval("interface Interface {" +
                "default int defaultMethod() { return 0; }" +
                "static int staticMethod() { return 0; }" +
                "}");
        Snippet clazz = classKey(assertEval("class Clazz {" +
                "static int staticMethod() { return 0; }" +
                "int method() { return 0; }" +
                "}"));
        assertCompletion("Interface.staticMeth|", "staticMethod()");
        assertCompletion("Clazz.staticMeth|", "staticMethod()");
        assertCompletion("new Interface() {}.defaultMe||", "defaultMethod()");
        assertCompletion("new Clazz().staticMeth|");
        assertCompletion("new Clazz().meth|", "method()");
        assertEval("class Clazz implements Interface {}",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(clazz, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertCompletion("Clazz.staticMeth|");
        assertCompletion("new Clazz() {}.defaultM|", "defaultMethod()");
    }

    @Test
    public void testUncompletedDeclaration() {
        assertCompletion("class Clazz { Claz|", "Clazz");
        assertCompletion("class Clazz { class A extends Claz|", "Clazz");
        assertCompletion("class Clazz { Clazz clazz; Object o = claz|", "clazz");
        assertCompletion("class Clazz { static Clazz clazz; Object o = claz|", "clazz");
        assertCompletion("class Clazz { Clazz clazz; static Object o = claz|", true);
        assertCompletion("class Clazz { void method(Claz|", "Clazz");
        assertCompletion("class A { int method() { return 0; } int a = meth|", "method()");
        assertCompletion("class A { int field = 0; int method() { return fiel|", "field");
        assertCompletion("class A { static int method() { return 0; } int a = meth|", "method()");
        assertCompletion("class A { static int field = 0; int method() { return fiel|", "field");
        assertCompletion("class A { int method() { return 0; } static int a = meth|", true);
        assertCompletion("class A { int field = 0; static int method() { return fiel|", true);
    }

    @Test
    public void testClassDeclaration() {
        assertEval("void ClazzM() {}");
        assertEval("void InterfaceM() {}");
        assertEval("interface Interface {}");
        assertCompletion("interface A extends Interf|", "Interface");
        assertCompletion("class A implements Interf|", "Interface");
        assertEval("class Clazz {}");
        assertCompletion("class A extends Claz|", "Clazz");
        assertCompletion("class A extends Clazz implements Interf|", "Interface");
        assertEval("interface Interface1 {}");
        assertCompletion("class A extends Clazz implements Interface, Interf|", "Interface", "Interface1");
        assertCompletion("interface A implements Claz|");
        assertCompletion("interface A implements Inter|");
        assertCompletion("class A implements Claz|", true);
        assertCompletion("class A extends Clazz implements Interface, Interf|", true, "Interface1");
        assertCompletion("class A extends Clazz implements Interface, Interf|", true, "Interface1");
        assertEval("class InterfaceClazz {}");
        assertCompletion("class A <T extends Claz|", "Clazz");
        assertCompletion("class A <T extends Interf|", "Interface", "Interface1", "InterfaceClazz");
        assertCompletion("class A <T extends Interface & Interf|", "Interface", "Interface1", "InterfaceClazz");
        assertCompletion("class A <T extends Clazz & Interf|", "Interface", "Interface1", "InterfaceClazz");
        assertCompletion("class A <T extends Claz|", true, "Clazz");
        assertCompletion("class A <T extends Interf|", true, "Interface", "Interface1", "InterfaceClazz");
        assertCompletion("class A <T extends Interface & Interf|", true, "Interface1");
        assertCompletion("class A <T extends Clazz & Interf|", true, "Interface", "Interface1");
    }

    public void testMethodDeclaration() {
        assertEval("void ClazzM() {}");
        assertEval("void InterfaceM() {}");
        assertEval("interface Interface {}");
        assertCompletion("void m(Interf|", "Interface");
        assertCompletion("void m(Interface i1, Interf|", "Interface");
        assertEval("class InterfaceException extends Exception {}");
        assertCompletion("void m(Interface i1) throws Interf|", "Interface", "InterfaceException");
        assertCompletion("void m(Interface i1) throws Interf|", true, "InterfaceException");
    }

    public void testDocumentationOfUserDefinedMethods() {
        assertEval("void f() {}");
        assertSignature("f(|", "void f()");
        assertEval("void f(int i) {}");
        assertSignature("f(|", "void f()", "void f(int i)");
        assertEval("<T> void f(T... ts) {}", DiagCheck.DIAG_WARNING, DiagCheck.DIAG_OK);
        assertSignature("f(|", "void f()", "void f(int i)", "void <T>f(T... ts)");
        assertEval("class A {}");
        assertEval("void f(A a) {}");
        assertSignature("f(|", "void f()", "void f(int i)", "void <T>f(T... ts)", "void f(A a)");
    }

    public void testClass() {
        assertSignature("String|", "java.lang.String");
    }

    public void testDocumentationOfUserDefinedConstructors() {
        Snippet a = classKey(assertEval("class A {}"));
        assertSignature("new A(|", "A()");
        Snippet a2 = classKey(assertEval("class A { A() {} A(int i) {}}",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(a, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
        assertSignature("new A(|", "A()", "A(int i)");
        assertEval("class A<T> { A(T a) {} A(int i) {} <U> A(T t, U u) {}}",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(a2, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertSignature("new A(|", "A<T>(T a)", "A<T>(int i)", "<U> A<T>(T t, U u)");
    }

    public void testDocumentationOfOverriddenMethods() throws Exception {
        dontReadParameterNamesFromClassFile();
        assertSignature("\"\".wait(|",
            "void Object.wait(long) throws InterruptedException",
            "void Object.wait(long, int) throws InterruptedException",
            "void Object.wait() throws InterruptedException");
        assertEval("class Base {void method() {}}");
        Snippet e = classKey(assertEval("class Extend extends Base {}"));
        assertSignature("new Extend().method(|", "void Base.method()");
        assertEval("class Extend extends Base {void method() {}}",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(e, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertSignature("new Extend().method(|", "void Extend.method()");
    }

    public void testDocumentationOfInvisibleMethods() {
        assertSignature("Object.wait(|");
        assertSignature("\"\".indexOfSupplementary(|");
        Snippet a = classKey(assertEval("class A {void method() {}}"));
        assertSignature("A.method(|");
        assertEval("class A {private void method() {}}",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(a, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertSignature("new A().method(|");
    }

    public void testDocumentationOfInvisibleConstructors() {
        assertSignature("new Compiler(|");
        assertEval("class A { private A() {} }");
        assertSignature("new A(|");
    }

    public void testDocumentationWithBoxing() {
        assertEval("int primitive = 0;");
        assertEval("Integer boxed = 0;");
        assertEval("Object object = null;");
        assertEval("void method(int n, Object o) { }");
        assertEval("void method(Object n, int o) { }");
        assertSignature("method(primitive,|",
                "void method(int n, Object o)",
                "void method(Object n, int o)");
        assertSignature("method(boxed,|",
                "void method(int n, Object o)",
                "void method(Object n, int o)");
        assertSignature("method(object,|",
                "void method(Object n, int o)");
    }

    public void testDocumentationWithGenerics() {
        class TestDocumentationWithGenerics {
            private final Function<Integer, String> codeFacotry;
            private final BiFunction<String, Integer, String> evalFormatter;
            private final BiFunction<String, Integer, String> docFormatter;
            int count;

            TestDocumentationWithGenerics(
                    Function<Integer, String> codeFactory,
                    BiFunction<String, Integer, String> evalFormatter,
                    BiFunction<String, Integer, String> documentationFormatter) {
                this.codeFacotry = codeFactory;
                this.evalFormatter = evalFormatter;
                this.docFormatter = documentationFormatter;
            }

            void assertDoc(String generics) {
                assertDoc(generics, generics);
            }

            void assertDoc(String generics, String expectedGenerics) {
                assertEval(evalFormatter.apply(generics, count));
                assertSignature(codeFacotry.apply(count), docFormatter.apply(expectedGenerics, count));
                count++;
            }
        }

        TestDocumentationWithGenerics[] tests = {
            new TestDocumentationWithGenerics(
                    i -> "f" + i + "(|",
                    (g, i) -> "<" + g + "> void f" + i + "() {}",
                    (g, i) -> "void <" + g + ">f" + i + "()"
            ),
            new TestDocumentationWithGenerics(
                    i -> "new C" + i + "().f(|",
                    (g, i) -> "class C" + i + "<" + g + "> { void f() {} }",
                    (g, i) -> "void C" + i + "<" + g + ">.f()"
            )
        };

        Arrays.stream(tests).forEach(t -> {
                t.assertDoc("T");
                t.assertDoc("T extends Object",
                        "T");
                t.assertDoc("T extends String");
                t.assertDoc("T extends java.lang.String",
                        "T extends String");
                t.assertDoc("T extends Number & Comparable<T>");
                t.assertDoc("T extends java.io.Serializable & CharSequence");
                t.assertDoc("K, D, M extends java.util.Map<K, D>",
                        "K, D, M extends java.util.Map<K,D>");
        });
    }

    public void testVarArgs() {
        assertEval("int i = 0;");
        assertEval("class Foo1 { static void m(int... i) { } } ");
        assertCompletion("Foo1.m(|", true, "i");
        assertCompletion("Foo1.m(i, |", true, "i");
        assertCompletion("Foo1.m(i, i, |", true, "i");
        assertEval("class Foo2 { static void m(String s, int... i) { } } ");
        assertCompletion("Foo2.m(|", true);
        assertCompletion("Foo2.m(i, |", true);
        assertCompletion("Foo2.m(\"\", |", true, "i");
        assertCompletion("Foo2.m(\"\", i, |", true, "i");
        assertCompletion("Foo2.m(\"\", i, i, |", true, "i");
        assertEval("class Foo3 { Foo3(String s, int... i) { } } ");
        assertCompletion("new Foo3(|", true);
        assertCompletion("new Foo3(i, |", true);
        assertCompletion("new Foo3(\"\", |", true, "i");
        assertCompletion("new Foo3(\"\", i, |", true, "i");
        assertCompletion("new Foo3(\"\", i, i, |", true, "i");
        assertEval("int[] ia = null;");
        assertCompletion("Foo1.m(ia, |", true);
        assertEval("class Foo4 { static void m(int... i) { } static void m(int[] ia, String str) { } } ");
        assertEval("String str = null;");
        assertCompletion("Foo4.m(ia, |", true, "str");
    }

    public void testConstructorAsMemberOf() {
        assertEval("class Baz<X> { Baz(X x) { } } ");
        assertEval("String str = null;");
        assertEval("Integer i = null;");
        assertCompletion("new Baz(|", true, "i", "str");
        assertCompletion("new Baz<String>(|", true, "str");
        assertCompletion("Baz<String> bz = new Baz<>(|", true, "str");
        assertEval("class Foo { static void m(String str) {} static void m(Baz<String> baz) {} }");
        assertCompletion("Foo.m(new Baz<>(|", true, "str");
    }

    public void testIntersection() {
        assertEval("<Z extends Runnable & CharSequence> Z get() { return null; }");
        assertEval("var v = get();");
        assertCompletionIncludesExcludes("v.|", true, Set.of("run()", "length()"), Set.of());
        assertCompletion("Runnable r = |", true, "get()", "v");
        assertCompletion("CharSequence r = |", true, "get()", "v");
        assertCompletion("Number r = |", true);
    }

    public void testAnonymous() {
        assertEval("var v = new Runnable() { public void run() { } public int length() { return 0; } };");
        assertCompletionIncludesExcludes("v.|", true, Set.of("run()", "length()"), Set.of());
        assertCompletion("Runnable r = |", true, "v");
        assertCompletion("CharSequence r = |", true);
    }

    public void testCompletionInAnonymous() {
        assertCompletionIncludesExcludes("new Undefined() { int i = \"\".l|", Set.of("length()"), Set.of());
    }

    public void testMemberReferences() {
        assertEval("class C {" +
                   "    public static String stat() { return null; }" +
                   "    public static void statVoid(String s) {}" +
                   "    public static Integer statConvert1(String s) { return null; }" +
                   "    public static String statConvert2(Integer s) { return null; }" +
                   "    public static String statConvert3(CharSequence s) { return null; }" +
                   "    public String inst() { return null; }" +
                   "    public void instVoid(String s) { }" +
                   "}");
        assertEval("interface FI { public void t(String s); }");
        assertCompletion("FI fi = C::|", (Boolean) null, "stat", "statConvert1", "statConvert2", "statConvert3", "statVoid");
        assertCompletion("FI fi = C::|", true, "statConvert1", "statConvert3","statVoid");
        assertCompletion("FI fi = new C()::i|", (Boolean) null, "inst", "instVoid");
        assertCompletion("FI fi = new C()::i|", true, "instVoid");
        assertEval("interface FI2<R, P> { public R t(P p); }");
        assertCompletion("FI2<String, Integer> fi = C::|", (Boolean) null, "stat", "statConvert1", "statConvert2", "statConvert3", "statVoid");
        assertCompletion("FI2<String, Integer> fi = C::|", true, "statConvert2");
        assertCompletion("FI2<String, CharSequence> fi = C::|", true, "statConvert3");
        assertCompletion("FI2<String, String> fi = C::|", true, "statConvert3");
        assertCompletion("FI2<Object, String> fi = C::|", true, "statConvert1", "statConvert3");
    }

    public void testBrokenLambdaCompletion() {
        assertEval("interface Consumer<T> { public void consume(T t); }");
        assertEval("interface Function<T, R> { public R convert(T t); }");
        assertEval("<T> void m1(T t, Consumer<T> f) { }");
        assertCompletion("m1(\"\", x -> {x.tri|", "trim()");
        assertEval("<T> void m2(T t, Function<T, String> f) { }");
        assertCompletion("m2(\"\", x -> {x.tri|", "trim()");
        assertEval("<T> void m3(T t, Consumer<T> f, int i) { }");
        assertCompletion("m3(\"\", x -> {x.tri|", "trim()");
        assertEval("<T> void m4(T t, Function<T, String> f, int i) { }");
        assertCompletion("m4(\"\", x -> {x.tri|", "trim()");
        assertEval("<T> T m5(Consumer<T> f) { return null; }");
        assertCompletion("String s = m5(x -> {x.tri|", "trim()");
        assertEval("<T> T m6(Function<T, String> f) { return null; }");
        assertCompletion("String s = m6(x -> {x.tri|", "trim()");
        assertEval("<T> T m7(Consumer<T> f, int i) { return null; }");
        assertCompletion("String s = m7(x -> {x.tri|", "trim()");
        assertEval("<T> T m8(Function<T, String> f, int i) { return null; }");
        assertCompletion("String s = m8(x -> {x.tri|", "trim()");
    }

    @BeforeMethod
    public void setUp() {
        super.setUp();

        Path srcZip = Paths.get("src.zip");

        try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(srcZip))) {
            out.putNextEntry(new JarEntry("java/lang/System.java"));
            out.write(("package java.lang;\n" +
                       "public class System {\n" +
                       "    public String getProperty(String key) { return null; }\n" +
                       "    public String getProperty(String key, String def) { return def; }\n" +
                       "}\n").getBytes());
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }

        try {
            Field availableSources = getAnalysis().getClass().getDeclaredField("availableSources");
            availableSources.setAccessible(true);
            availableSources.set(getAnalysis(), Arrays.asList(srcZip));
        } catch (NoSuchFieldException | IllegalArgumentException | IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private void dontReadParameterNamesFromClassFile() throws Exception {
        Field keepParameterNames = getAnalysis().getClass().getDeclaredField("keepParameterNames");
        keepParameterNames.setAccessible(true);
        keepParameterNames.set(getAnalysis(), new String[0]);
    }

    @Test(enabled = false) //TODO 8171829
    public void testBrokenClassFile2() throws IOException {
        Path broken = outDir.resolve("broken");
        compiler.compile(broken,
                "package p;\n" +
                "public class BrokenA {\n" +
                "}",
                "package p.q;\n" +
                "public class BrokenB {\n" +
                "}",
                "package p;\n" +
                "public class BrokenC {\n" +
                "}");
        Path cp = compiler.getPath(broken);
        Path target = cp.resolve("p").resolve("BrokenB.class");
        Files.deleteIfExists(target);
        Files.move(cp.resolve("p").resolve("q").resolve("BrokenB.class"), target);
        addToClasspath(cp);

        assertEval("import p.*;");
        assertCompletion("Broke|", "BrokenA", "BrokenC");
    }
}
