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
 * @bug 8144903 8177466 8191842 8211694 8213725 8239536 8257236 8252409
 * @summary Tests for EvaluationState.variables
 * @library /tools/lib
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jshell
 * @build Compiler KullaTesting TestingInputStream ExpectedDiagnostic
 * @run testng VariablesTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import javax.tools.Diagnostic;

import jdk.jshell.MethodSnippet;
import jdk.jshell.Snippet;
import jdk.jshell.TypeDeclSnippet;
import jdk.jshell.VarSnippet;
import jdk.jshell.Snippet.SubKind;
import jdk.jshell.SnippetEvent;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.util.stream.Collectors.toList;
import static jdk.jshell.Snippet.Status.*;
import static jdk.jshell.Snippet.SubKind.VAR_DECLARATION_SUBKIND;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;

@Test
public class VariablesTest extends KullaTesting {

    public void noVariables() {
        assertNumberOfActiveVariables(0);
    }

    private void badVarValue(VarSnippet key) {
        try {
            getState().varValue(key);
            fail("Expected exception for: " + key.source());
        } catch (IllegalArgumentException e) {
            // ok
        }
    }

    public void testVarValue1() {
        VarSnippet v1 = varKey(assertEval("und1 a;", added(RECOVERABLE_NOT_DEFINED)));
        badVarValue(v1);
        VarSnippet v2 = varKey(assertEval("und2 a;",
                ste(MAIN_SNIPPET, RECOVERABLE_NOT_DEFINED, RECOVERABLE_NOT_DEFINED, false, null),
                ste(v1, RECOVERABLE_NOT_DEFINED, OVERWRITTEN, false, MAIN_SNIPPET)));
        badVarValue(v2);
        TypeDeclSnippet und = classKey(assertEval("class und2 {}",
                added(VALID),
                ste(v2, RECOVERABLE_NOT_DEFINED, VALID, true, MAIN_SNIPPET)));
        assertVarValue(v2, "null");
        assertDrop(und,
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(und, VALID, DROPPED, true, null),
                ste(v2, VALID, RECOVERABLE_NOT_DEFINED, true, und));
        badVarValue(v1);
        badVarValue(v2);
    }

    public void testVarValue2() {
        VarSnippet v1 = (VarSnippet) assertDeclareFail("int a = 0.0;", "compiler.err.prob.found.req");
        badVarValue(v1);
        VarSnippet v2 = varKey(assertEval("int a = 0;", added(VALID)));
        assertDrop(v2, ste(MAIN_SNIPPET, VALID, DROPPED, true, null));
        badVarValue(v2);
    }

    public void testSignature1() {
        VarSnippet v1 = varKey(assertEval("und1 a;", added(RECOVERABLE_NOT_DEFINED)));
        assertVariableDeclSnippet(v1, "a", "und1", RECOVERABLE_NOT_DEFINED, VAR_DECLARATION_SUBKIND, 1, 0);
        VarSnippet v2 = varKey(assertEval("und2 a;",
                ste(MAIN_SNIPPET, RECOVERABLE_NOT_DEFINED, RECOVERABLE_NOT_DEFINED, false, null),
                ste(v1, RECOVERABLE_NOT_DEFINED, OVERWRITTEN, false, MAIN_SNIPPET)));
        assertVariableDeclSnippet(v2, "a", "und2", RECOVERABLE_NOT_DEFINED, VAR_DECLARATION_SUBKIND, 1, 0);
        TypeDeclSnippet und = classKey(assertEval("class und2 {}",
                added(VALID),
                ste(v2, RECOVERABLE_NOT_DEFINED, VALID, true, MAIN_SNIPPET)));
        assertVariableDeclSnippet(v2, "a", "und2", VALID, VAR_DECLARATION_SUBKIND, 0, 0);
        assertDrop(und,
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                ste(und, VALID, DROPPED, true, null),
                ste(v2, VALID, RECOVERABLE_NOT_DEFINED, true, und));
        assertVariableDeclSnippet(v2, "a", "und2", RECOVERABLE_NOT_DEFINED, VAR_DECLARATION_SUBKIND, 1, 0);
    }

    public void testSignature2() {
        VarSnippet v1 = (VarSnippet) assertDeclareFail("int a = 0.0;", "compiler.err.prob.found.req");
        assertVariableDeclSnippet(v1, "a", "int", REJECTED, SubKind.VAR_DECLARATION_WITH_INITIALIZER_SUBKIND, 0, 1);
        VarSnippet v2 = varKey(assertEval("int a = 0;",
                added(VALID)));
        assertVariableDeclSnippet(v2, "a", "int", VALID, SubKind.VAR_DECLARATION_WITH_INITIALIZER_SUBKIND, 0, 0);
        assertDrop(v2, ste(MAIN_SNIPPET, VALID, DROPPED, true, null));
        assertVariableDeclSnippet(v2, "a", "int", DROPPED, SubKind.VAR_DECLARATION_WITH_INITIALIZER_SUBKIND, 0, 0);
    }

    public void variables() {
        VarSnippet snx = varKey(assertEval("int x = 10;"));
        VarSnippet sny = varKey(assertEval("String y = \"hi\";"));
        VarSnippet snz = varKey(assertEval("long z;"));
        assertVariables(variable("int", "x"), variable("String", "y"), variable("long", "z"));
        assertVarValue(snx, "10");
        assertVarValue(sny, "\"hi\"");
        assertVarValue(snz, "0");
        assertActiveKeys();
    }

    public void variablesArray() {
        VarSnippet sn = varKey(assertEval("int[] a = new int[12];"));
        assertEquals(sn.typeName(), "int[]");
        assertEval("int len = a.length;", "12");
        assertVariables(variable("int[]", "a"), variable("int", "len"));
        assertActiveKeys();
    }

    public void variablesArrayOld() {
        VarSnippet sn = varKey(assertEval("int a[] = new int[12];"));
        assertEquals(sn.typeName(), "int[]");
        assertEval("int len = a.length;", "12");
        assertVariables(variable("int[]", "a"), variable("int", "len"));
        assertActiveKeys();
    }

    public void variablesRedefinition() {
        Snippet x = varKey(assertEval("int x = 10;"));
        Snippet y = varKey(assertEval("String y = \"\";", added(VALID)));
        assertVariables(variable("int", "x"), variable("String", "y"));
        assertActiveKeys();
        assertEval("long x;",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(x, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertVariables(variable("long", "x"), variable("String", "y"));
        assertActiveKeys();
        assertEval("String y;",
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(y, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertVariables(variable("long", "x"), variable("String", "y"));
        assertActiveKeys();
    }

    public void variablesTemporary() {
        assertEval("int $1 = 10;", added(VALID));
        assertEval("2 * $1;", added(VALID));
        assertVariables(variable("int", "$1"), variable("int", "$2"));
        assertActiveKeys();
        assertEval("String y;", added(VALID));
        assertVariables(variable("int", "$1"), variable("int", "$2"), variable("String", "y"));
        assertActiveKeys();
    }

    public void variablesTemporaryNull() {
        assertEval("null;", added(VALID));
        assertVariables(variable("Object", "$1"));
        assertEval("(String) null;", added(VALID));
        assertVariables(variable("Object", "$1"), variable("String", "$2"));
        assertActiveKeys();
        assertEval("\"\";", added(VALID));
        assertVariables(
                variable("Object", "$1"),
                variable("String", "$2"),
                variable("String", "$3"));
        assertActiveKeys();
    }

    public void variablesTemporaryArrayOfCapturedType() {
        assertEval("class Test<T> { T[][] get() { return null; } }", added(VALID));
        assertEval("Test<? extends String> test() { return new Test<>(); }", added(VALID));
        assertEval("test().get()", added(VALID));
        assertVariables(variable("String[][]", "$1"));
        assertEval("\"\".getClass().getEnumConstants()", added(VALID));
        assertVariables(variable("String[][]", "$1"), variable("String[]", "$2"));
        assertActiveKeys();
    }

    public void variablesClassReplace() {
        assertEval("import java.util.*;", added(VALID));
        Snippet var = varKey(assertEval("List<Integer> list = new ArrayList<>();", "[]",
                added(VALID)));
        assertVariables(variable("List<Integer>", "list"));
        assertEval("class List {}",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(var, VALID, RECOVERABLE_NOT_DEFINED, true, MAIN_SNIPPET));
        assertVariables();
        assertEval("List list = new List();",
                DiagCheck.DIAG_OK, DiagCheck.DIAG_IGNORE,
                ste(MAIN_SNIPPET, RECOVERABLE_NOT_DEFINED, VALID, true, null),
                ste(var, RECOVERABLE_NOT_DEFINED, OVERWRITTEN, false, MAIN_SNIPPET));
        assertVariables(variable("List", "list"));
        assertActiveKeys();
    }

    public void variablesErrors() {
        assertDeclareFail("String;", new ExpectedDiagnostic("compiler.err.cant.resolve.location", 0, 6, 0, -1, -1, Diagnostic.Kind.ERROR));
        assertNumberOfActiveVariables(0);
        assertActiveKeys();
    }

    public void variablesUnresolvedActiveFailed() {
        VarSnippet key = varKey(assertEval("und x;", added(RECOVERABLE_NOT_DEFINED)));
        assertVariableDeclSnippet(key, "x", "und", RECOVERABLE_NOT_DEFINED, VAR_DECLARATION_SUBKIND, 1, 0);
        assertUnresolvedDependencies1(key, RECOVERABLE_NOT_DEFINED, "class und");
        assertNumberOfActiveVariables(1);
        assertActiveKeys();
    }

    public void variablesUnresolvedError() {
        assertDeclareFail("und y = null;", new ExpectedDiagnostic("compiler.err.cant.resolve.location", 0, 3, 0, -1, -1, Diagnostic.Kind.ERROR));
        assertNumberOfActiveVariables(0);
        assertActiveKeys();
    }

    public void variablesMultiByteCharacterType() {
        assertEval("class \u3042 {}");
        assertEval("\u3042 \u3042 = null;", added(VALID));
        assertVariables(variable("\u3042", "\u3042"));
        assertEval("new \u3042()", added(VALID));
        assertVariables(variable("\u3042", "\u3042"), variable("\u3042", "$1"));

        assertEval("class \u3042\u3044\u3046\u3048\u304a {}");
        assertEval("\u3042\u3044\u3046\u3048\u304a \u3042\u3044\u3046\u3048\u304a = null;", added(VALID));
        assertVariables(variable("\u3042", "\u3042"), variable("\u3042", "$1"),
                variable("\u3042\u3044\u3046\u3048\u304a", "\u3042\u3044\u3046\u3048\u304a"));
        assertEval("new \u3042\u3044\u3046\u3048\u304a();");
        assertVariables(variable("\u3042", "\u3042"), variable("\u3042", "$1"),
                variable("\u3042\u3044\u3046\u3048\u304a", "\u3042\u3044\u3046\u3048\u304a"),
                variable("\u3042\u3044\u3046\u3048\u304a", "$2"));
        assertActiveKeys();
    }

    @Test(enabled = false) // TODO 8081689
    public void methodVariablesAreNotVisible() {
        Snippet foo = varKey(assertEval("int foo() {" +
                        "int x = 10;" +
                        "int y = 2 * x;" +
                        "return x * y;" +
                        "}", added(VALID)));
        assertNumberOfActiveVariables(0);
        assertActiveKeys();
        assertEval("int x = 10;", "10");
        assertEval("int foo() {" +
                        "int y = 2 * x;" +
                        "return x * y;" +
                        "}",
                ste(foo, VALID, VALID, false, null));
        assertVariables(variable("int", "x"));
        assertActiveKeys();
        assertEval("foo();", "200");
        assertVariables(variable("int", "x"), variable("int", "$1"));
        assertActiveKeys();
    }

    @Test(enabled = false) // TODO 8081689
    public void classFieldsAreNotVisible() {
        Snippet key = classKey(assertEval("class clazz {" +
                        "int x = 10;" +
                        "int y = 2 * x;" +
                        "}"));
        assertNumberOfActiveVariables(0);
        assertEval("int x = 10;", "10");
        assertActiveKeys();
        assertEval(
                "class clazz {" +
                        "int y = 2 * x;" +
                        "}",
                ste(key, VALID, VALID, true, null));
        assertVariables(variable("int", "x"));
        assertEval("new clazz().y;", "20");
        assertVariables(variable("int", "x"), variable("int", "$1"));
        assertActiveKeys();
    }

    public void multiVariables() {
        List<SnippetEvent> abc = assertEval("int a, b, c = 10;",
                DiagCheck.DIAG_OK, DiagCheck.DIAG_OK,
                chain(added(VALID)),
                chain(added(VALID)),
                chain(added(VALID)));
        Snippet a = abc.get(0).snippet();
        Snippet b = abc.get(1).snippet();
        Snippet c = abc.get(2).snippet();
        assertVariables(variable("int", "a"), variable("int", "b"), variable("int", "c"));
        assertEval("double a = 1.4, b = 8.8;", DiagCheck.DIAG_OK, DiagCheck.DIAG_OK,
                chain(ste(MAIN_SNIPPET, VALID, VALID, true, null), ste(a, VALID, OVERWRITTEN, false, MAIN_SNIPPET)),
                chain(ste(MAIN_SNIPPET, VALID, VALID, true, null), ste(b, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
        assertVariables(variable("double", "a"), variable("double", "b"), variable("int", "c"));
        assertEval("double c = a + b;",
                ste(MAIN_SNIPPET, VALID, VALID, true, null),
                ste(c, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertVariables(variable("double", "a"), variable("double", "b"), variable("double", "c"));
        assertActiveKeys();
    }

    public void syntheticVariables() {
        assertEval("assert false;");
        assertNumberOfActiveVariables(0);
        assertActiveKeys();
    }

    public void undefinedReplaceVariable() {
        Snippet key = varKey(assertEval("int d = 234;", "234"));
        assertVariables(variable("int", "d"));
        String src = "undefined d;";
        Snippet undefKey = varKey(assertEval(src,
                ste(MAIN_SNIPPET, VALID, RECOVERABLE_NOT_DEFINED, true, null),
                ste(key, VALID, OVERWRITTEN, false, MAIN_SNIPPET)));
        //assertEquals(getState().source(snippet), src);
        //assertEquals(snippet, undefKey);
        assertEquals(getState().status(undefKey), RECOVERABLE_NOT_DEFINED);
        List<String> unr = getState().unresolvedDependencies((VarSnippet) undefKey).collect(toList());;
        assertEquals(unr.size(), 1);
        assertEquals(unr.get(0), "class undefined");
        assertVariables(variable("undefined", "d"));
    }

    public void lvti() {
        assertEval("var d = 234;", "234");
        assertEval("class Test<T> { T[][] get() { return null; } }", added(VALID));
        assertEval("Test<? extends String> test() { return new Test<>(); }", added(VALID));
        assertEval("var t = test().get();", added(VALID));
        assertEval("<Z extends Runnable & CharSequence> Z get1() { return null; }", added(VALID));
        assertEval("var i1 = get1();", added(VALID));
        assertEval("void t1() { i1.run(); i1.length(); }", added(VALID));
        assertEval("i1 = 1;", DiagCheck.DIAG_ERROR, DiagCheck.DIAG_OK, ste(MAIN_SNIPPET, NONEXISTENT, REJECTED, false, null));
        assertEval("<Z extends Number & CharSequence> Z get2() { return null; }", added(VALID));
        assertEval("var i2 = get2();", added(VALID));
        assertEval("void t2() { i2.length(); }", added(VALID));
        assertEval("var r1 = new Runnable() { public void run() { } public String get() { return \"good\"; } };", added(VALID));
        assertEval("Runnable r2 = r1;");
        assertEval("r1.get()", "\"good\"");
        assertEval("var v = r1.get();", "\"good\"");
        assertEval("var r3 = new java.util.ArrayList<String>(42) { public String get() { return \"good\"; } };", added(VALID));
        assertEval("r3.get()", "\"good\"");
        assertEval("class O { public class Inner { public String test() { return \"good\"; } } }");
        assertEval("var r4 = new O().new Inner() { public String get() { return \"good\"; } };");
        assertEval("r4.get()", "\"good\"");
        assertEval("class O2 { public class Inner { public Inner(int i) { } public String test() { return \"good\"; } } }");
        assertEval("var r5 = new O2().new Inner(1) { public String get() { return \"good\"; } };");
        assertEval("r5.get()", "\"good\"");
        assertEval("<Z> Z identity(Z z) { return z; }");
        assertEval("var r6 = identity(new Object() { String s = \"good\"; });");
        assertEval("r6.s", "\"good\"");
        assertEval("interface I<B, C> { C get(B b); }");
        assertEval("<A, B, C> C cascade(A a, I<A, B> c1, I<B, C> c2) { return c2.get(c1.get(a)); }");
        assertEval("var r7 = cascade(\"good\", a -> new Object() { String s = a; }, b -> new java.util.ArrayList<String>(5) { String s = b.s; });");
        assertEval("r7.s", "\"good\"");
        assertEval("var r8 = cascade(\"good\", a -> new Object() { String s = a; public String getS() { return s; } }, b -> new java.util.ArrayList<String>(5) { String s = b.getS(); public String getS() { return s; } });");
        assertEval("r8.getS()", "\"good\"");
        assertEval("var r9 = new Object() { class T { class Inner { public String g() { return outer(); } } public String outer() { return \"good\"; } public String test() { return new Inner() {}.g(); } } public String test() { return new T().test(); } };");
        assertEval("r9.test()", "\"good\"");
        assertEval("var nested1 = new Object() { class N { public String get() { return \"good\"; } } };");
        assertEval("nested1.new N().get()", "\"good\"");
        assertEval("var nested2 = cascade(\"good\", a -> new Object() { abstract class G { abstract String g(); } G g = new G() { String g() { return a; } }; }, b -> new java.util.ArrayList<String>(5) { String s = b.g.g(); });");
        assertEval("nested2.s", "\"good\"");
        assertEval("<A, B> B convert(A a, I<A, B> c) { return c.get(a); }");
        assertEval("var r10 = convert(\"good\", a -> new api.C(12) { public String val = \"\" + i + s + l + a; } );");
        assertEval("r10.val", "\"12empty[empty]good\"");
        assertEval("var r11 = convert(\"good\", a -> new api.C(\"a\") { public String val = \"\" + i + s + l + a; } );");
        assertEval("r11.val", "\"3a[empty]good\"");
        assertEval("import api.C;");
        assertEval("var r12 = convert(\"good\", a -> new C(java.util.List.of(\"a\")) { public String val = \"\" + i + s + l + a; } );");
        assertEval("r12.val", "\"4empty[a]good\"");
        assertEval("var r13 = convert(\"good\", a -> new api.G<String>(java.util.List.of(\"b\")) { public String val = \"\" + l + a; } );");
        assertEval("r13.val", "\"[b]good\"");
        assertEval("var r14 = convert(\"good\", a -> new api.J<String>() { public java.util.List<String> get() { return java.util.List.of(a, \"c\"); } } );");
        assertEval("r14.get()", "[good, c]");
        assertEval("var r15a = new java.util.ArrayList<String>();");
        assertEval("r15a.add(\"a\");");
        assertEval("var r15b = r15a.get(0);");
        assertEval("r15b", "\"a\"");
        assertEval("class Z { }");
        assertEval("var r16a = new Z();");
        assertEval("var r16b = (Runnable) () -> {int r16b_; int r16b__;};");
        assertEval("class $ { }");
        assertEval("var r16c = new $();");
        assertEval("$ r16d() { return null; }");
        assertEval("var r16d = r16d();");
    }

    public void test8191842() {
        assertEval("import java.util.stream.*;");
        assertEval("var list = Stream.of(1, 2, 3).map(j -> new Object() { int i = j; }).collect(Collectors.toList());");
        assertEval("list.stream().map(a -> String.valueOf(a.i)).collect(Collectors.joining(\", \"));", "\"1, 2, 3\"");
    }

    public void lvtiRecompileDependentsWithIntersectionTypes() {
        assertEval("<Z extends Runnable & CharSequence> Z get1() { return null; }", added(VALID));
        assertEval("var i1 = get1();", added(VALID));
        MethodSnippet get2 = methodKey(assertEval("<Z extends Runnable & Stream> Z get2() { return null; }",
            ste(MAIN_SNIPPET, NONEXISTENT, RECOVERABLE_NOT_DEFINED, false, null)));
        assertEval("import java.util.stream.*;", added(VALID),
                                                 ste(get2, RECOVERABLE_NOT_DEFINED, VALID, true, MAIN_SNIPPET));
        assertEval("void t1() { i1.run(); i1.length(); }", added(VALID));
        assertEval("var i2 = get2();", added(VALID));
        assertEval("void t2() { i2.run(); i2.count(); }", added(VALID));
    }

    public void arrayInit() {
        assertEval("int[] d = {1, 2, 3};");
    }

    public void testAnonymousVar() {
        assertEval("new Object() { public String get() { return \"a\"; } }");
        assertEval("$1.get()", "\"a\"");
    }

    public void testIntersectionVar() {
        assertEval("<Z extends Runnable & CharSequence> Z get() { return null; }", added(VALID));
        assertEval("get();", added(VALID));
        assertEval("void t1() { $1.run(); $1.length(); }", added(VALID));
    }

    public void multipleCaptures() {
        assertEval("class D { D(int foo, String bar) { this.foo = foo; this.bar = bar; } int foo; String bar; } ");
        assertEval("var d = new D(34, \"hi\") { String z = foo + bar; };");
        assertEval("d.z", "\"34hi\"");
    }

    public void multipleAnonymous() {
        VarSnippet v1 = varKey(assertEval("new Object() { public int i = 42; public int i1 = i; public int m1() { return i1; } };"));
        VarSnippet v2 = varKey(assertEval("new Object() { public int i = 42; public int i2 = i; public int m2() { return i2; } };"));
        assertEval(v1.name() + ".i", "42");
        assertEval(v1.name() + ".i1", "42");
        assertEval(v1.name() + ".m1()", "42");
        assertDeclareFail(v1.name() + ".i2",
                          new ExpectedDiagnostic("compiler.err.cant.resolve.location", 0, 5, 2,
                                                 -1, -1, Diagnostic.Kind.ERROR));
        assertEval(v2.name() + ".i", "42");
        assertEval(v2.name() + ".i2", "42");
        assertEval(v2.name() + ".m2()", "42");
        assertDeclareFail(v2.name() + ".i1",
                          new ExpectedDiagnostic("compiler.err.cant.resolve.location", 0, 5, 2,
                                                 -1, -1, Diagnostic.Kind.ERROR));
    }

    public void displayName() {
        assertVarDisplayName("var v1 = 234;", "int");
        assertVarDisplayName("var v2 = new int[] {234};", "int[]");
        assertEval("<Z extends Runnable & CharSequence> Z get() { return null; }", added(VALID));
        assertVarDisplayName("var v3 = get();", "CharSequence&Runnable");
        assertVarDisplayName("var v4a = new java.util.ArrayList<String>();", "java.util.ArrayList<String>");
        assertEval("v4a.add(\"a\");");
        assertVarDisplayName("var v4b = v4a.get(0);", "String");
        assertVarDisplayName("var v5 = new Object() { };", "<anonymous class extending Object>");
        assertVarDisplayName("var v6 = new Runnable() { public void run() { } };", "<anonymous class implementing Runnable>");
    }

    public void varType() {
        assertEval("import java.util.*;");
        var firstVar = varKey(assertEval("var v1 = List.of(1);", added(VALID)));
        assertEval("import list.List;", DiagCheck.DIAG_OK, DiagCheck.DIAG_ERROR, added(VALID),
                                        ste(firstVar, VALID, RECOVERABLE_NOT_DEFINED, true, MAIN_SNIPPET));
        assertEval("var v2 = java.util.List.of(1);", added(VALID));
        assertEval("v2", "[1]");
    }

    public void varDeclNoInit() {
        assertVarDeclNoInit("byte", "b",  "0");
        assertVarDeclNoInit("short", "h",  "0");
        assertVarDeclNoInit("int", "i",  "0");
        assertVarDeclNoInit("long", "l",  "0");
        assertVarDeclNoInit("float", "f",  "0.0");
        assertVarDeclNoInit("double", "d",  "0.0");
        assertVarDeclNoInit("boolean", "n",  "false");
        assertVarDeclNoInit("char", "c",  "'\\000'");
        assertVarDeclNoInit("Object", "o",  "null");
        assertVarDeclNoInit("String", "s", "null");
    }

    public void varDeclRedefNoInit() {
        assertVarDeclRedefNoInit("byte", "b", "1", "0");
        assertVarDeclRedefNoInit("short", "h", "2", "0");
        assertVarDeclRedefNoInit("int", "i", "3", "0");
        assertVarDeclRedefNoInit("long", "l", "4L", IGNORE_VALUE, "0");
        assertVarDeclRedefNoInit("float", "f", "3.14f", IGNORE_VALUE, "0.0");
        assertVarDeclRedefNoInit("double", "d", "3.1415926", "0.0");
        assertVarDeclRedefNoInit("boolean", "n", "true", "false");
        assertVarDeclRedefNoInit("char", "c", "'x'", "'\\000'");
        assertVarDeclRedefNoInit("Object", "o", "new Object()", IGNORE_VALUE, "null");
        assertVarDeclRedefNoInit("String", "s", "\"hi\"", "null");
    }

    public void badPkgVarDecl() {
        Compiler compiler = new Compiler();
        Path nopkgdirpath = Paths.get("cp", "xyz");
        compiler.compile(nopkgdirpath,
                "public class TestZ { public static int V = 0; }\n");
        assertDeclareFail("import static xyz.TestZ.V;",
                        "compiler.err.cant.access");


        VarSnippet v1 = varKey(assertEval("var v = xyz.TestZ.V;", IGNORE_VALUE, null,
                DiagCheck.DIAG_ERROR, DiagCheck.DIAG_OK, added(RECOVERABLE_NOT_DEFINED)));
        assertVariableDeclSnippet(v1, "v", "java.lang.Object", RECOVERABLE_NOT_DEFINED, SubKind.VAR_DECLARATION_WITH_INITIALIZER_SUBKIND, 0, 1);
        assertEval("1+1", "2");
    }

    private void assertVarDeclRedefNoInit(String typeName, String name, String value, String dvalue) {
        assertVarDeclRedefNoInit(typeName, name, value, value, dvalue);
    }

    private void assertVarDeclRedefNoInit(String typeName, String name, String value, String rvalue, String dvalue) {
        VarSnippet vs = varKey(assertEval(typeName + " " + name + " = " + value + ";", rvalue));
        assertVarDeclNoInit(typeName,  name, dvalue,
                ste(vs, VALID, VALID, false, null),
                ste(vs, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
    }

    private VarSnippet assertVarDeclNoInit(String typeName, String name, String dvalue) {
        return assertVarDeclNoInit(typeName, name, dvalue, added(VALID));
    }

    private VarSnippet assertVarDeclNoInit(String typeName, String name, String dvalue, STEInfo mainInfo, STEInfo... updates) {
        VarSnippet vs = varKey(assertEval(typeName + " " + name + ";", dvalue, mainInfo, updates));
        assertEquals(vs.typeName(), typeName);
        assertEval(name, dvalue, added(VALID));
        return vs;
    }

    private void assertVarDisplayName(String var, String typeName) {
        assertEquals(varKey(assertEval(var)).typeName(), typeName);
    }

    @BeforeMethod
    @Override
    public void setUp() {
        Path path = Paths.get("cp");
        Compiler compiler = new Compiler();
        compiler.compile(path,
                "package api;\n" +
                "\n" +
                "import java.util.List;\n" +
                "\n" +
                "public class C {\n" +
                "   public int i;\n" +
                "   public String s;\n" +
                "   public List<String> l;\n" +
                "   public C(int i) {\n" +
                "       this.i = i;\n" +
                "       this.s = \"empty\";\n" +
                "       this.l = List.of(\"empty\");\n" +
                "   }\n" +
                "   public C(String s) {\n" +
                "       this.i = 3;\n" +
                "       this.s = s;\n" +
                "       this.l = List.of(\"empty\");\n" +
                "   }\n" +
                "   public C(List<String> l) {\n" +
                "       this.i = 4;\n" +
                "       this.s = \"empty\";\n" +
                "       this.l = l;\n" +
                "   }\n" +
                "}\n",
                "package api;\n" +
                "\n" +
                "import java.util.List;\n" +
                "\n" +
                "public class G<T> {\n" +
                "   public List<T> l;\n" +
                "   public G(List<T> l) {\n" +
                "       this.l = l;\n" +
                "   }\n" +
                "}\n",
                "package api;\n" +
                "\n" +
                "import java.util.List;\n" +
                "\n" +
                "public interface J<T> {\n" +
                "   public List<T> get();\n" +
                "}\n",
                "package list;\n" +
                "\n" +
                "public class List {\n" +
                "}\n");
        String tpath = compiler.getPath(path).toString();
        setUp(b -> b
                .remoteVMOptions("--class-path", tpath)
                .compilerOptions("--class-path", tpath));
    }

    public void varIntersection() {
        assertEval("interface Marker {}");
        assertEval("var v = (Marker & Runnable) () -> {};", added(VALID));
        assertEval("v.run()");
    }

}
