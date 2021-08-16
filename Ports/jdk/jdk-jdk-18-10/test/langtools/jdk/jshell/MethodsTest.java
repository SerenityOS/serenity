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
 * @bug 8080357 8167643 8187359 8199762 8080353 8246353 8247456 8267221
 * @summary Tests for EvaluationState.methods
 * @build KullaTesting TestingInputStream ExpectedDiagnostic
 * @run testng MethodsTest
 */

import javax.tools.Diagnostic;

import jdk.jshell.Snippet;
import jdk.jshell.MethodSnippet;
import jdk.jshell.Snippet.Status;
import org.testng.annotations.Test;

import java.util.List;
import java.util.stream.Collectors;

import static jdk.jshell.Snippet.Status.*;
import static org.testng.Assert.assertEquals;

@Test
public class MethodsTest extends KullaTesting {

    public void noMethods() {
        assertNumberOfActiveMethods(0);
    }

    public void testSignature1() {
        MethodSnippet m1 = methodKey(assertEval("void f() { g(); }", added(RECOVERABLE_DEFINED)));
        assertMethodDeclSnippet(m1, "f", "()void", RECOVERABLE_DEFINED, 1, 0);
        MethodSnippet m2 = methodKey(assertEval("void g() { }",
                added(VALID),
                ste(m1, RECOVERABLE_DEFINED, VALID, false, null)));
        assertMethodDeclSnippet(m2, "g", "()void", VALID, 0, 0);
    }

    public void testSignature2() {
        MethodSnippet m1 = (MethodSnippet) assertDeclareFail("void f() { return g(); }", "compiler.err.prob.found.req");
        assertMethodDeclSnippet(m1, "f", "()void", REJECTED, 0, 2);
        MethodSnippet m2 = methodKey(assertEval("int f() { return g(); }",
                added(RECOVERABLE_DEFINED)));
        assertMethodDeclSnippet(m1, "f", "()void", REJECTED, 0, 2);
        assertMethodDeclSnippet(m2, "f", "()int", RECOVERABLE_DEFINED, 1, 0);
    }

    @Test(enabled = false) // TODO 8081690
    public void testSignature3() {
        MethodSnippet m1 = methodKey(assertEval("void f(Bar b) { }", added(RECOVERABLE_NOT_DEFINED)));
        assertMethodDeclSnippet(m1, "f", "(Bar)void", RECOVERABLE_NOT_DEFINED, 1, 0);
        MethodSnippet m2 = methodKey(assertEval("void f(A.Bar b) { }", added(RECOVERABLE_NOT_DEFINED)));
        assertMethodDeclSnippet(m1, "f", "(Bar)void", RECOVERABLE_NOT_DEFINED, 1, 0);
        assertMethodDeclSnippet(m2, "f", "(A.Bar)void", RECOVERABLE_NOT_DEFINED, 1, 0);
        assertDrop(m1, ste(m1, RECOVERABLE_NOT_DEFINED, DROPPED, false, null));
        assertMethodDeclSnippet(m1, "f", "(Bar)void", DROPPED, 1, 0);
    }

    // 8080357
    public void testNonReplUnresolved() {
        // internal case
        assertEval("class CCC {}", added(VALID));
        assertEval("void f1() { CCC.xxxx(); }", added(RECOVERABLE_DEFINED));
        // external case, not recoverable
        assertDeclareFail("void f2() { System.xxxx(); }", "compiler.err.cant.resolve.location.args");
    }

    public void methods() {
        assertEval("int x() { return 10; }");
        assertEval("String y() { return null; }");
        assertEval("long z() { return 0; }");
        assertMethods(method("()int", "x"), method("()String", "y"), method("()long", "z"));
        assertActiveKeys();
    }

    public void methodOverload() {
        assertEval("int m() { return 1; }");
        assertEval("int m(int x) { return 2; }");
        assertEval("int m(String s) { return 3; }");
        assertEval("int m(int x, int y) { return 4; }");
        assertEval("int m(int x, String z) { return 5; }");
        assertEval("int m(int x, String z, long g) { return 6; }");
        assertMethods(
                method("()int", "m"),
                method("(int)int", "m"),
                method("(String)int", "m"),
                method("(int,int)int", "m"),
                method("(int,String)int", "m"),
                method("(int,String,long)int", "m")
        );
        assertEval("m();", "1");
        assertEval("m(3);", "2");
        assertEval("m(\"hi\");", "3");
        assertEval("m(7, 8);", "4");
        assertEval("m(7, \"eight\");", "5");
        assertEval("m(7, \"eight\", 9L);", "6");
        assertActiveKeys();
    }

    /***
    public void methodOverloadDependent() {
        assertEval("String m(String s) { return s + s; }");
        assertEval("String m(double d) { return m(\"#\" + d); }");
        assertEval("String m(int x) { return m(2.25 * x); }");
        assertEval("String m() { return m(3); }");
        assertMethods(
                method("(String)String", "m"),
                method("(double)String", "m"),
                method("(int)String", "m"),
                method("()String", "m")
        );
        assertEval("m();", "\"#6.75#6.75\"");
        assertEval("m(2);", "\"#4.5#4.5\"");
        assertEval("m(3.14);", "\"#3.14#3.14\"");
        assertEval("m(\"hi\");", "\"hihi\"");
        assertActiveKeys();
    }
    ***/

    public void methodsRedeclaration1() {
        Snippet x = methodKey(assertEval("int x() { return 10; }"));
        Snippet y = methodKey(assertEval("String y() { return \"\"; }"));
        assertMethods(method("()int", "x"), method("()String", "y"));
        assertActiveKeys();

        assertEval("long x() { return 0; }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(x, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertMethods(method("()long", "x"), method("()String", "y"));
        assertActiveKeys();

        assertEval("String y() { return null; }",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(y, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertMethods(method("()long", "x"), method("()String", "y"));
        assertActiveKeys();
    }

    public void methodsRedeclaration2() {
        assertEval("int a() { return 1; }");
        assertMethods(method("()int", "a"));
        assertActiveKeys();

        Snippet b = methodKey(assertEval("Integer b() { return a(); }"));
        assertMethods(method("()int", "a"), method("()Integer", "b"));
        assertActiveKeys();

        Snippet c = methodKey(assertEval("double c() { return b(); }"));
        assertMethods(method("()int", "a"), method("()Integer", "b"), method("()double", "c"));
        assertActiveKeys();

        assertEval("double b() { return 3.14159; }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(b, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertMethods(method("()int", "a"), method("()double", "b"), method("()double", "c"));
        assertEval("c();", "3.14159");
        assertActiveKeys();
    }

    public void methodsRedeclaration3() {
        Snippet x = methodKey(assertEval("int x(Object...a) { return 10; }"));
        assertMethods(method("(Object...)int", "x"));
        assertActiveKeys();

        assertEval("int x(Object[]a) { return 10; }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(x, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertMethods(method("(Object[])int", "x"));
        assertActiveKeys();
    }


    public void methodsRedeclaration4() {
        Snippet a = methodKey(assertEval("int foo(int a) { return a; }"));
        assertEval("int x = foo(10);");
        assertActiveKeys();
        assertMethods(method("(int)int", "foo"));
        assertEval("int foo(int a) { return a * a; }",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(a, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertActiveKeys();
    }

    // 8199762
    public void methodsRedeclaration5() {
        Snippet m1 = methodKey(assertEval("int m(Object o) { return 10; }"));
        assertMethods(method("(Object)int", "m"));

        Snippet m2 = methodKey(assertEval("int m(Object o) { return 30; }",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(m1, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));

        assertEval("<T> int m(T o) { return 30; }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(m2, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertMethods(method("(T)int", "m"));
        assertEval("m(null)", "30");
        assertActiveKeys();
    }

    public void methodsAbstract() {
        MethodSnippet m1 = methodKey(assertEval("abstract String f();",
                ste(MAIN_SNIPPET, NONEXISTENT, RECOVERABLE_DEFINED, true, null)));
        assertEquals(getState().unresolvedDependencies(m1).collect(Collectors.toList()),
                List.of("method f()"));
        MethodSnippet m2 = methodKey(assertEval("abstract int mm(Blah b);",
                ste(MAIN_SNIPPET, NONEXISTENT, RECOVERABLE_NOT_DEFINED, false, null)));
        List<String> unr = getState().unresolvedDependencies(m2).collect(Collectors.toList());
        assertEquals(unr.size(), 2);
        unr.remove("class Blah");
        unr.remove("method mm(Blah)");
        assertEquals(unr.size(), 0, "unexpected entry: " + unr);
        assertNumberOfActiveMethods(2);
        assertActiveKeys();
    }

    public void methodsErrors() {
        assertDeclareFail("String f();",
                new ExpectedDiagnostic("compiler.err.missing.meth.body.or.decl.abstract", 0, 11, 7, -1, -1, Diagnostic.Kind.ERROR));
        assertNumberOfActiveMethods(0);
        assertActiveKeys();

        assertDeclareFail("native String f();",
                new ExpectedDiagnostic("jdk.eval.error.illegal.modifiers", 0, 6, 0, -1, -1, Diagnostic.Kind.ERROR));
        assertNumberOfActiveMethods(0);
        assertActiveKeys();

        assertDeclareFail("synchronized String f() {return null;}",
                new ExpectedDiagnostic("jdk.eval.error.illegal.modifiers", 0, 12, 0, -1, -1, Diagnostic.Kind.ERROR));
        assertNumberOfActiveMethods(0);
        assertActiveKeys();

        assertDeclareFail("default void f() { }",
                new ExpectedDiagnostic("jdk.eval.error.illegal.modifiers", 0, 7, 0, -1, -1, Diagnostic.Kind.ERROR));
        assertNumberOfActiveMethods(0);
        assertActiveKeys();

        assertDeclareFail("int f() {}", "compiler.err.missing.ret.stmt",
                added(REJECTED));
        assertNumberOfActiveMethods(0);
        assertActiveKeys();

        assertEval("String x() { return \"\"; };");
        assertMethods(method("()String", "x"));
        assertActiveKeys();
    }

    public void objectMethodNamedMethodsErrors() {
        assertDeclareFail("boolean equals(double d1, double d2) {  return d1 == d2; }",
                new ExpectedDiagnostic("jdk.eval.error.object.method", 8, 14, 8, -1, -1, Diagnostic.Kind.ERROR));
        assertNumberOfActiveMethods(0);
        assertActiveKeys();

        assertDeclareFail("void          wait() { }",
                new ExpectedDiagnostic("jdk.eval.error.object.method", 14, 18, 14, -1, -1, Diagnostic.Kind.ERROR));
        assertNumberOfActiveMethods(0);
        assertActiveKeys();

        assertDeclareFail("  String   toString() throws NullPointerException{ }",
                new ExpectedDiagnostic("jdk.eval.error.object.method", 11, 19, 11, -1, -1, Diagnostic.Kind.ERROR));
        assertNumberOfActiveMethods(0);
        assertActiveKeys();

    }


    public void methodsAccessModifierIgnored() {
        Snippet f = methodKey(assertEval("public String f() {return null;}",
                added(VALID)));
        assertNumberOfActiveMethods(1);
        assertActiveKeys();

        f = methodKey(assertEval("protected String f() {return null;}",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(f, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
        assertNumberOfActiveMethods(1);
        assertActiveKeys();

        assertEval("private String f() {return null;}",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(f, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertNumberOfActiveMethods(1);
        assertActiveKeys();
    }

    public void methodsIgnoredModifiers() {
        Snippet f = methodKey(assertEval("static String f() {return null;}"));
        assertNumberOfActiveMethods(1);
        assertActiveKeys();

        assertDeclareWarn1("final String f() {return null;}",
                null,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(f, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertNumberOfActiveMethods(1);
        assertActiveKeys();
    }

    public void methodSignatureUnresolved() {
        MethodSnippet key = (MethodSnippet) methodKey(assertEval("und m() { return new und(); }", added(RECOVERABLE_NOT_DEFINED)));
        assertMethodDeclSnippet(key, "m", "()und", RECOVERABLE_NOT_DEFINED, 1, 0);
        assertUnresolvedDependencies1(key, Status.RECOVERABLE_NOT_DEFINED, "class und");
        assertEval("class und {}",
                added(VALID),
                ste(key, RECOVERABLE_NOT_DEFINED, VALID, true, null));
        assertMethodDeclSnippet(key, "m", "()und", Status.VALID, 0, 0);
        assertNumberOfActiveMethods(1);
        assertActiveKeys();
    }

    @Test(enabled = false) // TODO 8081689
    public void classMethodsAreNotVisible() {
        assertEval(
            "class A {" +
                "int foo() {" +
                    "int x = 10;" +
                    "int y = 2 * x;" +
                    "return x * y;" +
                "}" +
            "}");
        assertNumberOfActiveMethods(0);
        assertEval("int x = 10;", "10");
        assertEval("int foo() {" +
                        "int y = 2 * x;" +
                        "return x * y;" +
                        "}");
        assertMethods(method("()int", "foo"));
        assertEval("foo();", "200");
        assertActiveKeys();
    }

    public void lambdas() {
        assertEval("class Inner1 implements Runnable {" +
                "public Runnable lambda1 = () -> {};" +
                "public void function() {}" +
                "public void run() {}" +
                "}");

        assertEval("class Inner2 {" +
                "private Runnable lambda1 = () -> {};" +
                "private static void staticFunction() {}" +
                "}");

        // the following method references and lambda functions
        // generate synthetic methods
        assertEval("Runnable run = () -> {};");
        assertEval("Inner1 inner = new Inner1();");
        assertEval("Runnable l1 = inner::function;");
        assertEval("Runnable l2 = Inner1::new;");
        assertEval("inner.lambda1 = inner::function;");
        assertEval("java.util.stream.IntStream.of(2).mapToObj(int[]::new);");
        assertNumberOfActiveMethods(0);
        assertActiveKeys();
    }

    //JDK-8267221:
    public void testMethodArrayParameters() {
        MethodSnippet m1 = methodKey(assertEval("void m1(int... p) { }", added(VALID)));
        assertEquals(m1.parameterTypes(), "int...");
        MethodSnippet m2 = methodKey(assertEval("void m2(int[]... p) { }", added(VALID)));
        assertEquals(m2.parameterTypes(), "int[]...");
        MethodSnippet m3 = methodKey(assertEval("void m3(int[][] p) { }", added(VALID)));
        assertEquals(m3.parameterTypes(), "int[][]");
    }
}
