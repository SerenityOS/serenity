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
 * @test
 * @bug 8143964
 * @summary test queries to the JShell that return Streams
 * @build KullaTesting
 * @run testng JShellQueryTest
 */
import jdk.jshell.Snippet;
import org.testng.annotations.Test;

import jdk.jshell.ImportSnippet;
import jdk.jshell.MethodSnippet;
import jdk.jshell.TypeDeclSnippet;
import jdk.jshell.VarSnippet;
import static java.util.stream.Collectors.joining;
import static org.testng.Assert.assertEquals;

@Test
public class JShellQueryTest extends KullaTesting {

    public void testSnippets() {
        assertStreamMatch(getState().snippets());
        VarSnippet sx = varKey(assertEval("int x = 5;"));
        VarSnippet sfoo = varKey(assertEval("String foo;"));
        MethodSnippet smm = methodKey(assertEval("int mm() { return 6; }"));
        MethodSnippet svv = methodKey(assertEval("void vv() { }"));
        assertStreamMatch(getState().snippets(), sx, sfoo, smm, svv);
        TypeDeclSnippet sc = classKey(assertEval("class C { }"));
        TypeDeclSnippet si = classKey(assertEval("interface I { }"));
        ImportSnippet simp = importKey(assertEval("import java.lang.reflect.*;"));
        assertStreamMatch(getState().snippets(), sx, sfoo, smm, svv, sc, si, simp);
    }

    public void testVars() {
        assertStreamMatch(getState().variables());
        VarSnippet sx = varKey(assertEval("int x = 5;"));
        VarSnippet sfoo = varKey(assertEval("String foo;"));
        MethodSnippet smm = methodKey(assertEval("int mm() { return 6; }"));
        MethodSnippet svv = methodKey(assertEval("void vv() { }"));
        assertStreamMatch(getState().variables(), sx, sfoo);
        TypeDeclSnippet sc = classKey(assertEval("class C { }"));
        TypeDeclSnippet si = classKey(assertEval("interface I { }"));
        ImportSnippet simp = importKey(assertEval("import java.lang.reflect.*;"));
        assertStreamMatch(getState().variables(), sx, sfoo);
    }

    public void testMethods() {
        assertStreamMatch(getState().methods());
        VarSnippet sx = varKey(assertEval("int x = 5;"));
        VarSnippet sfoo = varKey(assertEval("String foo;"));
        MethodSnippet smm = methodKey(assertEval("int mm() { return 6; }"));
        MethodSnippet svv = methodKey(assertEval("void vv() { }"));
        TypeDeclSnippet sc = classKey(assertEval("class C { }"));
        TypeDeclSnippet si = classKey(assertEval("interface I { }"));
        ImportSnippet simp = importKey(assertEval("import java.lang.reflect.*;"));
        assertStreamMatch(getState().methods(), smm, svv);
    }

    public void testTypes() {
        assertStreamMatch(getState().types());
        VarSnippet sx = varKey(assertEval("int x = 5;"));
        VarSnippet sfoo = varKey(assertEval("String foo;"));
        MethodSnippet smm = methodKey(assertEval("int mm() { return 6; }"));
        MethodSnippet svv = methodKey(assertEval("void vv() { }"));
        TypeDeclSnippet sc = classKey(assertEval("class C { }"));
        TypeDeclSnippet si = classKey(assertEval("interface I { }"));
        ImportSnippet simp = importKey(assertEval("import java.lang.reflect.*;"));
        assertStreamMatch(getState().types(), sc, si);
    }

    public void testImports() {
        assertStreamMatch(getState().imports());
        VarSnippet sx = varKey(assertEval("int x = 5;"));
        VarSnippet sfoo = varKey(assertEval("String foo;"));
        MethodSnippet smm = methodKey(assertEval("int mm() { return 6; }"));
        MethodSnippet svv = methodKey(assertEval("void vv() { }"));
        TypeDeclSnippet sc = classKey(assertEval("class C { }"));
        TypeDeclSnippet si = classKey(assertEval("interface I { }"));
        ImportSnippet simp = importKey(assertEval("import java.lang.reflect.*;"));
        assertStreamMatch(getState().imports(), simp);
    }

    public void testDiagnostics() {
        Snippet sx = varKey(assertEval("int x = 5;"));
        assertStreamMatch(getState().diagnostics(sx));
        Snippet broken = methodKey(assertEvalFail("int m() { blah(); return \"hello\"; }"));
        String res = getState().diagnostics(broken)
                .map(d -> d.getCode())
                .collect(joining("+"));
        assertEquals(res, "compiler.err.cant.resolve.location.args+compiler.err.prob.found.req");
    }

    public void testUnresolvedDependencies() {
        VarSnippet sx = varKey(assertEval("int x = 5;"));
        assertStreamMatch(getState().unresolvedDependencies(sx));
        MethodSnippet unr = methodKey(getState().eval("void uu() { baz(); zips(); }"));
        assertStreamMatch(getState().unresolvedDependencies(unr), "method zips()", "method baz()");
    }
}
