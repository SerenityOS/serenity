/*
 * Copyright (c) 2015, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8080069 8152925
 * @summary Test of Snippet redefinition and replacement.
 * @build KullaTesting TestingInputStream
 * @run testng ReplaceTest
 */

import java.util.Iterator;
import java.util.stream.Stream;
import jdk.jshell.Snippet;
import jdk.jshell.MethodSnippet;
import jdk.jshell.TypeDeclSnippet;
import jdk.jshell.VarSnippet;
import org.testng.annotations.Test;

import static org.testng.Assert.assertFalse;
import static jdk.jshell.Snippet.Status.*;
import static jdk.jshell.Snippet.SubKind.*;
import static org.testng.Assert.assertTrue;

@Test
public class ReplaceTest extends KullaTesting {

    public void testRedefine() {
        Snippet vx = varKey(assertEval("int x;"));
        Snippet mu = methodKey(assertEval("int mu() { return x * 4; }"));
        Snippet c = classKey(assertEval("class C { String v() { return \"#\" + mu(); } }"));
        assertEval("C c0  = new C();");
        assertEval("c0.v();", "\"#0\"");
        assertEval("int x = 10;", "10",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(vx, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("c0.v();", "\"#40\"");
        assertEval("C c = new C();");
        assertEval("c.v();", "\"#40\"");
        assertEval("int mu() { return x * 3; }",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(mu, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("c.v();", "\"#30\"");
        assertEval("class C { String v() { return \"@\" + mu(); } }",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(c, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("c0.v();", "\"@30\"");
        assertEval("c = new C();");
        assertEval("c.v();", "\"@30\"");
        assertActiveKeys();
    }

    public void testReplaceClassToVar() {
        Snippet oldA = classKey(assertEval("class A { public String toString() { return \"old\"; } }"));
        Snippet v = varKey(assertEval("A a = new A();", "old"));
        assertEval("a;", "old");
        Snippet midA = classKey(assertEval("class A { public String toString() { return \"middle\"; } }",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(oldA, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
        assertEval("a;", "middle");
        assertEval("class A { int x; public String toString() { return \"new\"; } }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(midA, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(v, VALID, VALID, true, MAIN_SNIPPET));
        assertEval("a;", "null");
        assertActiveKeys();
    }

    private <T extends Snippet> void identityMatch(Stream<T> got, T expected) {
        Iterator<T> it = got.iterator();
        assertTrue(it.hasNext(), "expected exactly one");
        assertTrue(expected == it.next(), "Identity must not change");
        assertFalse(it.hasNext(), "expected exactly one");
    }

    public void testReplaceVarToMethod() {
        Snippet x = varKey(assertEval("int x;"));
        MethodSnippet musn = methodKey(assertEval("double mu() { return x * 4; }"));
        assertEval("x == 0;", "true");
        assertEval("mu() == 0.0;", "true");
        assertEval("double x = 2.5;",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(x, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        identityMatch(getState().methods(), musn);
        assertEval("x == 2.5;", "true");
        assertEval("mu() == 10.0;", "true");  // Auto redefine
        assertActiveKeys();
    }

    public void testReplaceMethodToMethod() {
        Snippet a = methodKey(assertEval("double a() { return 2; }"));
        Snippet b = methodKey(assertEval("double b() { return a() * 10; }"));
        assertEval("double c() { return b() * 3; }");
        assertEval("double d() { return c() + 1000; }");
        assertEval("d();", "1060.0");
        assertEval("int a() { return 5; }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(a, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("d();", "1150.0");
        assertActiveKeys();
    }

    public void testReplaceClassToMethod() {
        Snippet c = classKey(assertEval("class C { int f() { return 7; } }"));
        Snippet m = methodKey(assertEval("int m() { return new C().f(); }"));
        assertEval("m();", "7");
        assertEval("class C { int x = 99; int f() { return x; } }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(c, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("m();", "99");
        assertActiveKeys();
    }

    public void testReplaceVarToClass() {
        Snippet x = varKey(assertEval("int x;"));
        TypeDeclSnippet c = classKey(assertEval("class A { double a = 4 * x; }"));
        assertEval("x == 0;", "true");
        assertEval("new A().a == 0.0;", "true");
        assertEval("double x = 2.5;",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(x, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        identityMatch(getState().types(), c);
        assertEval("x == 2.5;", "true");
        assertEval("new A().a == 10.0;", "true");
        assertActiveKeys();
    }

    public void testReplaceMethodToClass() {
        Snippet x = methodKey(assertEval("int x() { return 0; }"));
        TypeDeclSnippet c = classKey(assertEval("class A { double a = 4 * x(); }"));
        assertEval("x() == 0;", "true");
        assertEval("new A().a == 0.0;", "true");
        assertEval("double x() { return 2.5; }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(x, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("x();", "2.5");
        identityMatch(getState().types(), c);
        assertEval("x() == 2.5;", "true");
        assertEval("new A().a == 10.0;", "true");
        assertActiveKeys();
    }

    public void testReplaceClassToClass() {
        TypeDeclSnippet a = classKey(assertEval("class A {}"));
        assertTypeDeclSnippet(a, "A", VALID, CLASS_SUBKIND, 0, 0);
        TypeDeclSnippet b = classKey(assertEval("class B extends A {}"));
        TypeDeclSnippet c = classKey(assertEval("class C extends B {}"));
        TypeDeclSnippet d = classKey(assertEval("class D extends C {}"));
        assertEval("class A { int x; public String toString() { return \"NEW\"; } }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(a, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(b, VALID, VALID, true, MAIN_SNIPPET),
                ste(c, VALID, VALID, true, b),
                ste(d, VALID, VALID, true, c));
        assertTypeDeclSnippet(b, "B", VALID, CLASS_SUBKIND, 0, 0);
        assertTypeDeclSnippet(c, "C", VALID, CLASS_SUBKIND, 0, 0);
        assertTypeDeclSnippet(d, "D", VALID, CLASS_SUBKIND, 0, 0);
        assertEval("new D();", "NEW");
        assertActiveKeys();
    }

    public void testOverwriteReplaceMethod() {
        MethodSnippet k1 = methodKey(assertEval("String m(Integer i) { return i.toString(); }"));
        MethodSnippet k2 = methodKey(assertEval("String m(java.lang.Integer i) { return \"java.lang.\" + i.toString(); }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(k1, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
        assertMethodDeclSnippet(k1, "m", "(Integer)String", OVERWRITTEN, 0, 0);
        assertEval("m(6);", "\"java.lang.6\"");
        assertEval("String m(Integer i) { return i.toString(); }",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(k2, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertMethodDeclSnippet(k2, "m", "(java.lang.Integer)String", OVERWRITTEN, 0, 0);
        assertEval("m(6);", "\"6\"");
        assertActiveKeys();
    }

    public void testImportDeclare() {
        Snippet singleImport = importKey(assertEval("import java.util.List;", added(VALID)));
        Snippet importOnDemand = importKey(assertEval("import java.util.*;", added(VALID)));
        Snippet singleStaticImport = importKey(assertEval("import static java.lang.Math.abs;", added(VALID)));
        Snippet staticImportOnDemand = importKey(assertEval("import static java.lang.Math.*;", added(VALID)));
        assertEval("import java.util.List; //again",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(singleImport, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("import java.util.*; //again",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(importOnDemand, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("import static java.lang.Math.abs; //again",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(singleStaticImport, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("import static java.lang.Math.*; //again",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(staticImportOnDemand, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertActiveKeys();
    }

    @Test(enabled = false) // TODO 8129420
    public void testLocalClassEvolve() {
        Snippet j = methodKey(assertEval("Object j() { return null; }", added(VALID)));
        assertEval("Object j() { class B {}; return null; }",
                ste(MAIN_SNIPPET, VALID, VALID, false, null));
        assertEval("Object j() { class B {}; return new B(); }",
                ste(MAIN_SNIPPET, VALID, VALID, false, null));
        assertEval("j().getClass().getSimpleName();", "\"B\"");
        assertEval("Object j() { class B { int p; public String toString() { return \"Yep\";} }; return new B(); }",
                ste(MAIN_SNIPPET, VALID, VALID, false, null));
        assertEval("j().getClass().getSimpleName();", "\"B\"");
        assertEval("j();", "Yep");
    }

    public void testReplaceCausesMethodReferenceError() {
        Snippet l = classKey(assertEval("interface Logger { public void log(String message); }", added(VALID)));
        Snippet v = varKey(assertEval("Logger l = System.out::println;", added(VALID)));
        assertEval("interface Logger { public boolean accept(String message);  }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(l, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(v, VALID, RECOVERABLE_NOT_DEFINED, true, MAIN_SNIPPET));
    }

    public void testReplaceCausesClassCompilationError() {
        Snippet l = classKey(assertEval("interface L { }", added(VALID)));
        Snippet c = classKey(assertEval("class C implements L { }", added(VALID)));
        assertEval("interface L { void m(); }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(l, VALID, OVERWRITTEN, false, MAIN_SNIPPET),
                ste(c, VALID, RECOVERABLE_NOT_DEFINED, true, MAIN_SNIPPET));
    }

    public void testOverwriteNoUpdate() {
        String xsi = "int x = 5;";
        String xsd = "double x = 3.14159;";
        VarSnippet xi = varKey(assertEval(xsi, added(VALID)));
        String ms1 = "double m(Integer i) { return i + x; }";
        String ms2 = "double m(java.lang.Integer i) { return i + x; }";
        MethodSnippet k1 = methodKey(assertEval(ms1, added(VALID)));
        VarSnippet xd = varKey(assertEval(xsd,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(xi, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
        MethodSnippet k2 = methodKey(assertEval(ms2,
                ste(MAIN_SNIPPET, VALID, VALID, true, null), //TODO: technically, should be false
                ste(k1, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
        VarSnippet xi2 = varKey(assertEval(xsi,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(xd, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
        varKey(assertEval(xsd,
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(xi2, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
    }
}
