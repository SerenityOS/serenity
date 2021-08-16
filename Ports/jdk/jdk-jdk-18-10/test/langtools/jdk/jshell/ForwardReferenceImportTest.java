/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8173232
 * @summary Test of forward referencing of snippets (related to import).
 * @build KullaTesting TestingInputStream
 * @run testng ForwardReferenceImportTest
 */

import jdk.jshell.Snippet;
import jdk.jshell.DeclarationSnippet;
import org.testng.annotations.Test;

import static jdk.jshell.Snippet.Status.*;

@Test
public class ForwardReferenceImportTest extends KullaTesting {

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

    public void testForwardSingleImportMethodToMethod() {
        DeclarationSnippet string = methodKey(assertEval("String string() { return format(\"string\"); }",
                added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(string, RECOVERABLE_DEFINED, "method format(java.lang.String)");
        assertEvalUnresolvedException("string();", "string", 1, 0);
        assertEval("import static java.lang.String.format;",
                added(VALID),
                ste(string, RECOVERABLE_DEFINED, VALID, false, null));
        assertEval("string();", "\"string\"");

        assertEval("double format(String s) { return 0; }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(string, VALID, RECOVERABLE_DEFINED, false, null));
        assertEvalUnresolvedException("string();", "string", 0, 1);
        assertActiveKeys();
    }

    public void testForwardImportMethodOnDemandToMethod() {
        DeclarationSnippet string = methodKey(assertEval("String string() { return format(\"string\"); }",
                added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(string, RECOVERABLE_DEFINED, "method format(java.lang.String)");
        assertEvalUnresolvedException("string();", "string", 1, 0);
        assertEval("import static java.lang.String.*;",
                added(VALID),
                ste(string, RECOVERABLE_DEFINED, VALID, false, null));
        assertEval("string();", "\"string\"");

        assertEval("double format(String s) { return 0; }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(string, VALID, RECOVERABLE_DEFINED, false, null));
        assertEvalUnresolvedException("string();", "string", 0, 1);
        assertActiveKeys();
    }

    public void testForwardSingleImportFieldToMethod() {
        DeclarationSnippet pi = methodKey(assertEval("double pi() { return PI; }",
                added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(pi, RECOVERABLE_DEFINED, "variable PI");
        assertEvalUnresolvedException("pi();", "pi", 1, 0);
        assertEval("import static java.lang.Math.PI;",
                added(VALID),
                ste(pi, RECOVERABLE_DEFINED, VALID, false, null));
        assertEval("Math.abs(pi() - 3.1415) < 0.001;", "true");

        assertEval("String PI;",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(pi, VALID, RECOVERABLE_DEFINED, false, null));
        assertEvalUnresolvedException("pi();", "pi", 0, 1);
        assertActiveKeys();
    }

    public void testForwardImportFieldOnDemandToMethod() {
        DeclarationSnippet pi = methodKey(assertEval("double pi() { return PI; }",
                added(RECOVERABLE_DEFINED)));
        assertUnresolvedDependencies1(pi, RECOVERABLE_DEFINED, "variable PI");
        assertEvalUnresolvedException("pi();", "pi", 1, 0);
        assertEval("import static java.lang.Math.*;",
                added(VALID),
                ste(pi, RECOVERABLE_DEFINED, VALID, false, MAIN_SNIPPET));
        assertEval("Math.abs(pi() - 3.1415) < 0.001;", "true");

        assertEval("String PI;",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(pi, VALID, RECOVERABLE_DEFINED, false, MAIN_SNIPPET));
        assertEvalUnresolvedException("pi();", "pi", 0, 1);
        assertActiveKeys();
    }

    public void testForwardSingleImportMethodToClass1() {
        Snippet a = classKey(assertEval("class A { String s = format(\"%d\", 10); }",
                added(RECOVERABLE_DEFINED)));
        assertEvalUnresolvedException("new A();", "A", 1, 0);
        assertEval("import static java.lang.String.format;",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, false, null));
        assertEval("new A().s;", "\"10\"");
        Snippet format = methodKey(assertEval("void format(String s, int d) { }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(a, VALID, RECOVERABLE_DEFINED, false, MAIN_SNIPPET)));
        assertEvalUnresolvedException("new A();", "A", 0, 1);
        assertActiveKeys();
        assertDrop(format,
                ste(format, VALID, DROPPED, true, null),
                ste(a, RECOVERABLE_DEFINED, VALID, false, format));
    }

    public void testForwardSingleImportMethodToClass2() {
        Snippet a = classKey(assertEval("class A { String s() { return format(\"%d\", 10); } }",
                added(RECOVERABLE_DEFINED)));
        assertEvalUnresolvedException("new A();", "A", 1, 0);
        assertEval("import static java.lang.String.format;",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, false, null));
        assertEval("new A().s();", "\"10\"");
        Snippet format = methodKey(assertEval("void format(String s, int d) { }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(a, VALID, RECOVERABLE_DEFINED, false, null)));
        assertEvalUnresolvedException("new A();", "A", 0, 1);
        assertActiveKeys();
        assertDrop(format,
                ste(format, VALID, DROPPED, true, null),
                ste(a, RECOVERABLE_DEFINED, VALID, false, format));
    }

    public void testForwardSingleImportClassToClass1() {
        Snippet a = classKey(assertEval("class A { static List<Integer> list; }",
                added(RECOVERABLE_NOT_DEFINED)));
        assertDeclareFail("new A();", "compiler.err.cant.resolve.location");
        assertEval("import java.util.List;",
                added(VALID),
                ste(a, RECOVERABLE_NOT_DEFINED, VALID, true, null));
        assertEval("import java.util.Arrays;", added(VALID));
        assertEval("A.list = Arrays.asList(1, 2, 3);", "[1, 2, 3]");

        Snippet list = classKey(assertEval("class List {}",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(a, VALID, RECOVERABLE_NOT_DEFINED, true, null)));
        assertDeclareFail("A.list = Arrays.asList(1, 2, 3);", "compiler.err.already.defined.single.import");
        assertActiveKeys();
        assertDrop(list,
                ste(list, VALID, DROPPED, true, null),
                ste(a, RECOVERABLE_NOT_DEFINED, VALID, true, list));
    }

    public void testForwardSingleImportClassToClass2() {
        Snippet clsA = classKey(assertEval("class A extends ArrayList<Integer> { }",
                added(RECOVERABLE_NOT_DEFINED)));
        assertDeclareFail("new A();", "compiler.err.cant.resolve.location");
        assertEval("import java.util.ArrayList;",
                added(VALID),
                ste(clsA, RECOVERABLE_NOT_DEFINED, VALID, true, MAIN_SNIPPET));
        Snippet vara = varKey(assertEval("A a = new A();", "[]"));

        Snippet arraylist = classKey(assertEval("class ArrayList {}",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(clsA, VALID, RECOVERABLE_NOT_DEFINED, true, MAIN_SNIPPET),
                ste(vara, VALID, RECOVERABLE_NOT_DEFINED, true, clsA)));
        assertDeclareFail("A a = new A();", "compiler.err.cant.resolve.location",
                ste(MAIN_SNIPPET, RECOVERABLE_NOT_DEFINED, REJECTED, false, null),
                ste(vara, RECOVERABLE_NOT_DEFINED, OVERWRITTEN, false, MAIN_SNIPPET));
        assertActiveKeys();
        assertDrop(arraylist,
                ste(arraylist, VALID, DROPPED, true, null),
                ste(clsA, RECOVERABLE_NOT_DEFINED, VALID, true, arraylist));
    }

    public void testForwardImportOnDemandMethodToClass1() {
        Snippet a = classKey(assertEval("class A { String s = format(\"%d\", 10); }",
                added(RECOVERABLE_DEFINED)));
        assertEvalUnresolvedException("new A();", "A", 1, 0);
        assertEval("import static java.lang.String.*;",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, false, null));
        assertEval("A x = new A();");
        assertEval("x.s;", "\"10\"");
        Snippet format = methodKey(assertEval("void format(String s, int d) { }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(a, VALID, RECOVERABLE_DEFINED, false, null)));
        assertEvalUnresolvedException("new A();", "A", 0, 1);
        assertActiveKeys();
        assertDrop(format,
                ste(format, VALID, DROPPED, true, null),
                ste(a, RECOVERABLE_DEFINED, VALID, false, format));
        assertEval("x.s;", "\"10\"");
    }

    public void testForwardImportOnDemandMethodToClass2() {
        Snippet a = classKey(assertEval("class A { String s() { return format(\"%d\", 10); } }",
                added(RECOVERABLE_DEFINED)));
        assertEvalUnresolvedException("new A();", "A", 1, 0);
        assertEval("import static java.lang.String.*;",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, false, null));
        assertEval("new A().s();", "\"10\"");
        Snippet format = methodKey(assertEval("void format(String s, int d) { }",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(a, VALID, RECOVERABLE_DEFINED, false, null)));
        assertEvalUnresolvedException("new A();", "A", 0, 1);
        assertActiveKeys();
        assertDrop(format,
                ste(format, VALID, DROPPED, true, null),
                ste(a, RECOVERABLE_DEFINED, VALID, false, format));
    }

    public void testForwardImportOnDemandClassToClass1() {
        Snippet a = classKey(assertEval("class A { static List<Integer> list; }",
                added(RECOVERABLE_NOT_DEFINED)));
        assertDeclareFail("new A();", "compiler.err.cant.resolve.location");
        assertEval("import java.util.*;",
                added(VALID),
                ste(a, RECOVERABLE_NOT_DEFINED, VALID, true, null));
        assertEval("A.list = Arrays.asList(1, 2, 3);", "[1, 2, 3]");

        Snippet list = classKey(assertEval("class List {}",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(a, VALID, RECOVERABLE_NOT_DEFINED, true, null)));
        assertDeclareFail("A.list = Arrays.asList(1, 2, 3);", "compiler.err.cant.resolve.location");
        assertActiveKeys();
        assertDrop(list,
                ste(list, VALID, DROPPED, true, null),
                ste(a, RECOVERABLE_NOT_DEFINED, VALID, true, list));
    }

    public void testForwardImportOnDemandClassToClass2() {
        Snippet clsA = classKey(assertEval("class A extends ArrayList<Integer> { }",
                added(RECOVERABLE_NOT_DEFINED)));
        assertDeclareFail("new A();", "compiler.err.cant.resolve.location");
        assertEval("import java.util.*;",
                added(VALID),
                ste(clsA, RECOVERABLE_NOT_DEFINED, VALID, true, MAIN_SNIPPET));
        Snippet vara = varKey(assertEval("A a = new A();", "[]"));

        Snippet arraylist = classKey(assertEval("class ArrayList {}",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(clsA, VALID, RECOVERABLE_NOT_DEFINED, true, MAIN_SNIPPET),
                ste(vara, VALID, RECOVERABLE_NOT_DEFINED, true, clsA)));
        assertDeclareFail("new A();", "compiler.err.cant.resolve.location");
        assertActiveKeys();
        assertDrop(arraylist,
                ste(arraylist, VALID, DROPPED, true, null),
                ste(clsA, RECOVERABLE_NOT_DEFINED, VALID, true, arraylist),
                ste(vara, RECOVERABLE_NOT_DEFINED, VALID, true, clsA));
    }

    public void testForwardSingleImportFieldToClass1() {
        Snippet a = classKey(assertEval("class A { static double pi() { return PI; } }",
                added(RECOVERABLE_DEFINED)));
        assertEvalUnresolvedException("new A();", "A", 1, 0);
        assertEval("import static java.lang.Math.PI;",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, false, null));
        assertEval("Math.abs(A.pi() - 3.1415) < 0.001;", "true");

        Snippet list = varKey(assertEval("String PI;",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(a, VALID, RECOVERABLE_DEFINED, false, null)));
        assertEvalUnresolvedException("new A();", "A", 0, 1);
        assertActiveKeys();
        assertDrop(list,
                ste(list, VALID, DROPPED, true, null),
                ste(a, RECOVERABLE_DEFINED, VALID, false, list));
    }

    public void testForwardSingleImportFieldToClass2() {
        Snippet a = classKey(assertEval("class A { static double pi = PI; }",
                added(RECOVERABLE_DEFINED)));
        assertEvalUnresolvedException("new A();", "A", 1, 0);
        assertEval("import static java.lang.Math.PI;",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, true, null));
        assertEval("Math.abs(A.pi - 3.1415) < 0.001;", "true");

        Snippet list = varKey(assertEval("String PI;",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(a, VALID, RECOVERABLE_DEFINED, true, null)));
        assertEvalUnresolvedException("new A();", "A", 0, 1);
        assertActiveKeys();
        assertDrop(list,
                ste(list, VALID, DROPPED, true, null),
                ste(a, RECOVERABLE_DEFINED, VALID, true, list));
    }

    public void testForwardImportOnDemandFieldToClass1() {
        Snippet a = classKey(assertEval("class A { static double pi() { return PI; } }",
                added(RECOVERABLE_DEFINED)));
        assertEvalUnresolvedException("new A();", "A", 1, 0);
        assertEval("import static java.lang.Math.*;",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, false, null));
        assertEval("Math.abs(A.pi() - 3.1415) < 0.001;", "true");

        Snippet list = varKey(assertEval("String PI;",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(a, VALID, RECOVERABLE_DEFINED, false, null)));
        assertEvalUnresolvedException("new A();", "A", 0, 1);
        assertActiveKeys();
        assertDrop(list,
                ste(list, VALID, DROPPED, true, null),
                ste(a, RECOVERABLE_DEFINED, VALID, false, list));
    }

    public void testForwardImportOnDemandFieldToClass2() {
        Snippet a = classKey(assertEval("class A { static double pi = PI; }",
                added(RECOVERABLE_DEFINED)));
        assertEvalUnresolvedException("new A();", "A", 1, 0);
        assertEval("import static java.lang.Math.*;",
                added(VALID),
                ste(a, RECOVERABLE_DEFINED, VALID, true, null));
        assertEval("Math.abs(A.pi - 3.1415) < 0.001;", "true");

        Snippet list = varKey(assertEval("String PI;",
                DiagCheck.DIAG_OK,
                DiagCheck.DIAG_ERROR,
                added(VALID),
                ste(a, VALID, RECOVERABLE_DEFINED, true, null)));
        assertEvalUnresolvedException("new A();", "A", 0, 1);
        assertActiveKeys();
        assertDrop(list,
                ste(list, VALID, DROPPED, true, null),
                ste(a, RECOVERABLE_DEFINED, VALID, true, list));
        assertEval("Math.abs(A.pi - 3.1415) < 0.001;", "true");
    }
}
