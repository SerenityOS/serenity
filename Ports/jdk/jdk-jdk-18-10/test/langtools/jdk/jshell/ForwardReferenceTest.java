/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8173232 8010319
 * @summary Test of forward referencing of snippets.
 * @build KullaTesting TestingInputStream
 * @run testng ForwardReferenceTest
 */

import java.util.List;
import jdk.jshell.Snippet;
import jdk.jshell.MethodSnippet;
import jdk.jshell.VarSnippet;
import jdk.jshell.DeclarationSnippet;
import org.testng.annotations.Test;

import jdk.jshell.SnippetEvent;
import jdk.jshell.UnresolvedReferenceException;
import static org.testng.Assert.assertEquals;
import static jdk.jshell.Snippet.Status.*;
import static org.testng.Assert.assertTrue;

@Test
public class ForwardReferenceTest extends KullaTesting {

    public void testOverwriteMethodForwardReferenceClass() {
        Snippet k1 = methodKey(assertEval("int q(Boo b) { return b.x; }",
                added(RECOVERABLE_NOT_DEFINED)));
        assertUnresolvedDependencies1((MethodSnippet) k1, RECOVERABLE_NOT_DEFINED, "class Boo");
        assertEval("class Boo { int x = 55; }",
                added(VALID),
                ste(k1, RECOVERABLE_NOT_DEFINED, VALID, true, null));
        assertMethodDeclSnippet((MethodSnippet) k1, "q", "(Boo)int", VALID, 0, 0);
        assertEval("q(new Boo());", "55");
        assertActiveKeys();
    }

    public void testOverwriteMethodForwardReferenceClassImport() {
        MethodSnippet k1 = methodKey(assertEval("int ff(List lis) { return lis.size(); }",
                added(RECOVERABLE_NOT_DEFINED)));
        assertUnresolvedDependencies1(k1, RECOVERABLE_NOT_DEFINED, "class List");
        assertEval("import java.util.*;",
                added(VALID),
                ste(k1, RECOVERABLE_NOT_DEFINED, VALID, true, null));
        assertMethodDeclSnippet(k1, "ff", "(List)int", VALID, 0, 0);
        assertEval("ff(new ArrayList());", "0");
        assertActiveKeys();
    }

    public void testForwardVarToMethod() {
        DeclarationSnippet t = methodKey(assertEval("int t() { return x; }", added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(t, RECOVERABLE_DEFINED, "variable x");
        assertEvalUnresolvedException("t();", "t", 1, 0);
        Snippet x = varKey(assertEval("int x = 33;", "33",
                added(VALID),
                ste(t, RECOVERABLE_DEFINED, VALID, false, null)));
        assertEval("t();", "33");
        assertEval("double x = 0.88;",
                "0.88", null,
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(x, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(t, VALID, RECOVERABLE_DEFINED, false, MAIN_SNIPPET));
        assertEvalUnresolvedException("t();", "t", 0, 1);
        assertActiveKeys();
    }

    public void testForwardMethodToMethod() {
        Snippet t = methodKey(assertEval("int t() { return f(); }", added(RECOVERABLE_DEFINED)));
        Snippet f = methodKey(assertEval("int f() { return g(); }",
                added(RECOVERABLE_DEFINED),
                ste(t, RECOVERABLE_DEFINED, VALID, false, null)));
        assertUnresolvedDependencies1((DeclarationSnippet) f, RECOVERABLE_DEFINED, "method g()");
        assertEvalUnresolvedException("t();", "f", 1, 0);
        Snippet g = methodKey(assertEval("int g() { return 55; }",
                added(VALID),
                ste(f, RECOVERABLE_DEFINED, VALID, false, null)));
        assertEval("t();", "55");
        assertEval("double g() { return 3.14159; }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(g, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(f, VALID, RECOVERABLE_DEFINED, false, MAIN_SNIPPET));
        DeclarationSnippet exsn = assertEvalUnresolvedException("t();", "f", 0, 1);
        assertTrue(exsn == f, "Identity must not change");
        assertActiveKeys();
    }

    public void testForwardClassToMethod() {
        DeclarationSnippet t = methodKey(assertEval("int t() { return new A().f(); }", added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(t, RECOVERABLE_DEFINED, "class A");
        assertEvalUnresolvedException("t();", "t", 1, 0);
        Snippet a = classKey(assertEval(
                "class A {\n" +
                        "   int f() { return 10; }\n" +
                "}",
                added(VALID),
                ste(t, RECOVERABLE_DEFINED, VALID, false, null)));
        assertEval("t();", "10");
        assertEval(
                "class A {\n" +
                "   double f() { return 88.0; }\n" +
                "}",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(a, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(t, VALID, RECOVERABLE_DEFINED, false, MAIN_SNIPPET));
        assertEvalUnresolvedException("t();", "t", 0, 1);
        assertActiveKeys();
    }

    public void testForwardVarToClass() {
        DeclarationSnippet a = classKey(assertEval("class A { int f() { return g; } }", added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(a, RECOVERABLE_DEFINED, "variable g");
        Snippet g = varKey(assertEval("int g = 10;", "10",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, false, null)));
        assertEval("new A().f();", "10");
        assertEval("double g = 10;", "10.0", null,
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(g, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(a, VALID, RECOVERABLE_DEFINED, false, MAIN_SNIPPET));
        assertUnresolvedDependencies(a, 0);
        assertActiveKeys();
    }

    public void testForwardVarToClassGeneric() {
        DeclarationSnippet a = classKey(assertEval("class A<T> { final T x; A(T v) { this.x = v; } ; T get() { return x; } int core() { return g; } }", added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(a, RECOVERABLE_DEFINED, "variable g");

        List<SnippetEvent> events = assertEval("A<String> as = new A<>(\"hi\");", null,
                UnresolvedReferenceException.class, DiagCheck.DIAG_OK, DiagCheck.DIAG_OK, null);
        SnippetEvent ste = events.get(0);
        Snippet assn = ste.snippet();
        DeclarationSnippet unsn = ((UnresolvedReferenceException) ste.exception()).getSnippet();
        assertEquals(unsn.name(), "A", "Wrong with unresolved");
        assertEquals(getState().unresolvedDependencies(unsn).count(), 1, "Wrong size unresolved");
        assertEquals(getState().diagnostics(unsn).count(), 0L, "Expected no diagnostics");

        Snippet g = varKey(assertEval("int g = 10;", "10",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, false, MAIN_SNIPPET)));
        assertEval("A<String> as = new A<>(\"low\");",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(assn, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("as.get();", "\"low\"");
        assertUnresolvedDependencies(a, 0);
        assertActiveKeys();
    }

   public void testForwardVarToClassExtendsImplements() {
        DeclarationSnippet ik = classKey(assertEval("interface I { default int ii() { return 1; } }", added(VALID)));
        DeclarationSnippet jk = classKey(assertEval("interface J { default int jj() { return 2; } }", added(VALID)));
        DeclarationSnippet ck = classKey(assertEval("class C { int cc() { return 3; } }", added(VALID)));
        DeclarationSnippet dk = classKey(assertEval("class D extends C implements I,J { int dd() { return g; } }", added(RECOVERABLE_DEFINED)));
        DeclarationSnippet ek = classKey(assertEval("class E extends D { int ee() { return 5; } }", added(VALID)));
        assertUnresolvedDependencies1(dk, RECOVERABLE_DEFINED, "variable g");
        assertEvalUnresolvedException("new D();", "D", 1, 0);
        assertEvalUnresolvedException("new E();", "D", 1, 0);
        VarSnippet g = varKey(assertEval("int g = 10;", "10",
                added(VALID),
                ste(dk, RECOVERABLE_DEFINED, VALID, false, MAIN_SNIPPET)));
        assertEval("E e = new E();");
        assertDrop(g,
                ste(g, VALID, DROPPED, true, null),
                ste(dk, VALID, RECOVERABLE_DEFINED, false, g));
        assertEvalUnresolvedException("new D();", "D", 1, 0);
        assertEvalUnresolvedException("new E();", "D", 1, 0);
        assertEval("e.ee();", "5");
        assertEvalUnresolvedException("e.dd();", "D", 1, 0);
        assertEval("e.cc();", "3");
        assertEval("e.jj();", "2");
        assertEval("e.ii();", "1");
        assertActiveKeys();
    }

    public void testForwardVarToInterface() {
        DeclarationSnippet i = classKey(assertEval("interface I { default int f() { return x; } }", added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(i, RECOVERABLE_DEFINED, "variable x");
        DeclarationSnippet c = classKey(assertEval("class C implements I { int z() { return 2; } }", added(VALID)));
        assertEval("C c = new C();");
        assertEval("c.z();", "2");
        assertEvalUnresolvedException("c.f()", "I", 1, 0);
        Snippet g = varKey(assertEval("int x = 55;", "55",
                added(VALID),
                ste(i, RECOVERABLE_DEFINED, VALID, false, null)));
        assertEval("c.f();", "55");
        assertUnresolvedDependencies(i, 0);
        assertActiveKeys();
    }

    public void testForwardVarToEnum() {
        DeclarationSnippet a = classKey(assertEval("enum E { Q, W, E; float ff() { return fff; } }", added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(a, RECOVERABLE_DEFINED, "variable fff");
        Snippet g = varKey(assertEval("float fff = 4.5f;", "4.5",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, false, null)));
        assertEval("E.Q.ff();", "4.5");
        assertEval("double fff = 3.3;", "3.3", null,
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(g, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(a, VALID, RECOVERABLE_DEFINED, false, MAIN_SNIPPET));
        assertUnresolvedDependencies(a, 0);
        assertActiveKeys();
    }

    public void testForwardMethodToClass() {
        DeclarationSnippet a = classKey(assertEval("class A { int f() { return g(); } }", added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(a, RECOVERABLE_DEFINED, "method g()");
        assertEval("A foo() { return null; }");
        assertEvalUnresolvedException("new A();", "A", 1, 0);
        Snippet g = methodKey(assertEval("int g() { return 10; }",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, false, null)));
        assertEval("new A().f();", "10");
        assertEval("double g() { return 10; }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(g, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(a, VALID, RECOVERABLE_DEFINED, false, MAIN_SNIPPET));
        assertUnresolvedDependencies(a, 0);
        assertActiveKeys();
    }

    public void testForwardClassToClass1() {
        Snippet a = classKey(assertEval("class A { B b = new B(); }", added(RECOVERABLE_NOT_DEFINED)));
        assertDeclareFail("new A().b;", "compiler.err.cant.resolve.location");

        Snippet b = classKey(assertEval("class B { public String toString() { return \"B\"; } }",
                added(VALID),
                ste(a, RECOVERABLE_NOT_DEFINED, VALID, true, null)));
        assertEval("new A().b;", "B");
        assertEval("interface B { }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(b, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(a, VALID, RECOVERABLE_DEFINED, true, MAIN_SNIPPET));
        assertEvalUnresolvedException("new A().b;", "A", 0, 1);
        assertActiveKeys();
    }

    public void testForwardClassToClass2() {
        Snippet a = classKey(assertEval("class A extends B { }", added(RECOVERABLE_NOT_DEFINED)));
        assertDeclareFail("new A();", "compiler.err.cant.resolve.location");

        Snippet b = classKey(assertEval("class B { public String toString() { return \"B\"; } }",
                added(VALID),
                ste(a, RECOVERABLE_NOT_DEFINED, VALID, true, null)));
        assertEval("new A();", "B");
        assertEval("interface B { }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(b, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(a, VALID, RECOVERABLE_NOT_DEFINED, true, MAIN_SNIPPET));
        assertDeclareFail("new A();", "compiler.err.cant.resolve.location");
        assertActiveKeys();
    }

    public void testForwardClassToClass3() {
        Snippet a = classKey(assertEval("interface A extends B { static int f() { return 10; } }", added(RECOVERABLE_NOT_DEFINED)));
        assertDeclareFail("A.f();", "compiler.err.cant.resolve.location");

        Snippet b = classKey(assertEval("interface B { }",
                added(VALID),
                ste(a, RECOVERABLE_NOT_DEFINED, VALID, true, null)));
        assertEval("A.f();", "10");
        assertEval("class B { }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(b, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(a, VALID, RECOVERABLE_NOT_DEFINED, true, MAIN_SNIPPET));
        assertDeclareFail("A.f();", "compiler.err.cant.resolve.location");
        assertActiveKeys();
    }

    public void testForwardVariable() {
        assertEval("int f() { return x; }", added(RECOVERABLE_DEFINED));
        assertEvalUnresolvedException("f();", "f", 1, 0);
        assertActiveKeys();
    }

    public void testLocalClassInUnresolved() {
        Snippet f = methodKey(assertEval("void f() { class A {} g(); }", added(RECOVERABLE_DEFINED)));
        assertEval("void g() {}",
                added(VALID),
                ste(f, RECOVERABLE_DEFINED, VALID, true, null));
        assertEval("f();", "");
    }
}
