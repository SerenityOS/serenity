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
 * @test
 * @bug 8141415
 * @summary Test imports
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @library /tools/lib
 * @build KullaTesting TestingInputStream toolbox.Task.ExpectedDiagnostic
 * @run testng ImportTest
 */

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.tools.Diagnostic;

import jdk.jshell.Snippet;
import org.testng.annotations.Test;

import static jdk.jshell.Snippet.Status.VALID;
import static jdk.jshell.Snippet.Status.OVERWRITTEN;
import static jdk.jshell.Snippet.SubKind.SINGLE_TYPE_IMPORT_SUBKIND;
import static jdk.jshell.Snippet.SubKind.SINGLE_STATIC_IMPORT_SUBKIND;
import static jdk.jshell.Snippet.SubKind.TYPE_IMPORT_ON_DEMAND_SUBKIND;
import static jdk.jshell.Snippet.SubKind.STATIC_IMPORT_ON_DEMAND_SUBKIND;

@Test
public class ImportTest extends KullaTesting {

    public void testImport() {
        assertImportKeyMatch("import java.util.List;", "List", SINGLE_TYPE_IMPORT_SUBKIND, added(VALID));
        assertImportKeyMatch("import java.util.ArrayList;", "ArrayList", SINGLE_TYPE_IMPORT_SUBKIND, added(VALID));
        assertEval("List<Integer> list = new ArrayList<>();");
        assertEval("list.add(45);");
        assertEval("list.size();", "1");
    }

    public void testImportOnDemand() {
        assertImportKeyMatch("import java.util.*;", "java.util.*", TYPE_IMPORT_ON_DEMAND_SUBKIND, added(VALID));
        assertEval("List<Integer> list = new ArrayList<>();");
        assertEval("list.add(45);");
        assertEval("list.size();", "1");
    }

    public void testImportStatic() {
        assertImportKeyMatch("import static java.lang.Math.PI;", "PI", SINGLE_STATIC_IMPORT_SUBKIND, added(VALID));
        assertEval("Double.valueOf(PI).toString().substring(0, 16).equals(\"3.14159265358979\");", "true");
    }

    public void testImportStaticOnDemand() {
        assertImportKeyMatch("import static java.lang.Math.*;", "java.lang.Math.*", STATIC_IMPORT_ON_DEMAND_SUBKIND, added(VALID));
        assertEval("abs(cos(PI / 2)) < 0.00001;", "true");
    }

    @Test(enabled = false) // TODO 8129418
    public void testUnknownPackage() {
        assertDeclareFail("import unknown.qqq;",
                new ExpectedDiagnostic("compiler.err.doesnt.exist", 7, 18, 14, -1, -1, Diagnostic.Kind.ERROR));
        assertDeclareFail("import unknown.*;",
                new ExpectedDiagnostic("compiler.err.doesnt.exist", 7, 15, 7, -1, -1, Diagnostic.Kind.ERROR));
    }

    public void testBogusImportIgnoredInFuture() {
        assertDeclareFail("import unknown.qqq;", "compiler.err.doesnt.exist");
        assertDeclareFail("import unknown.*;", "compiler.err.doesnt.exist");
        assertEval("2 + 2;");
    }

    public void testBadImport() {
        assertDeclareFail("import static java.lang.reflect.Modifier;",
                new ExpectedDiagnostic("compiler.err.cant.resolve.location", 14, 31, 23, -1, -1, Diagnostic.Kind.ERROR));
    }

    public void testBadSyntaxImport() {
        assertDeclareFail("import not found.*;",
                new ExpectedDiagnostic("compiler.err.expected", 10, 10, 10, -1, -1, Diagnostic.Kind.ERROR));
    }

    public void testImportRedefinition() {
        Compiler compiler = new Compiler();
        Path path = Paths.get("testImport");
        compiler.compile(path, "package util; public class ArrayList { public String toString() { return \"MyList\"; } }");
        compiler.compile(path, "package util; public class A { public static class ArrayList {\n" +
                "public String toString() { return \"MyInnerList\"; } } }");
        addToClasspath(compiler.getPath(path));

        assertImportKeyMatch("import util.*;", "util.*", TYPE_IMPORT_ON_DEMAND_SUBKIND, added(VALID));
        assertEval("new ArrayList();", "MyList", added(VALID));

        Snippet import0 = assertImportKeyMatch("import java.util.ArrayList;", "ArrayList", SINGLE_TYPE_IMPORT_SUBKIND, added(VALID));
        assertEval("new ArrayList();", "[]");

        Snippet import1 = assertImportKeyMatch("import util.ArrayList;", "ArrayList", SINGLE_TYPE_IMPORT_SUBKIND,
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(import0, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("new ArrayList();", "MyList");

        Snippet import2 = assertImportKeyMatch("import java.util.ArrayList;", "ArrayList", SINGLE_TYPE_IMPORT_SUBKIND,
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(import1, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("new ArrayList();", "[]");

        Snippet import3 = assertImportKeyMatch("import util.A.ArrayList;", "ArrayList", SINGLE_TYPE_IMPORT_SUBKIND,
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(import2, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("new ArrayList();", "MyInnerList");

        Snippet import4 = assertImportKeyMatch("import java.util.ArrayList;", "ArrayList", SINGLE_TYPE_IMPORT_SUBKIND,
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(import3, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("new ArrayList();", "[]");

        Snippet import5 = assertImportKeyMatch("import static util.A.ArrayList;", "ArrayList", SINGLE_STATIC_IMPORT_SUBKIND,
                ste(MAIN_SNIPPET, VALID, VALID, false, null),
                ste(import4, VALID, OVERWRITTEN, false, MAIN_SNIPPET));
        assertEval("new ArrayList();", "MyInnerList");
    }

    public void testImportMemberRedefinition() {
        Compiler compiler = new Compiler();
        Path path = Paths.get("testImport");
        compiler.compile(path, "package util; public class A {" +
                "public static String field = \"A\";" +
                "public static String method() { return \"A\"; } }");
        compiler.compile(path, "package util; public class B {" +
                "public static String field = \"B\";" +
                "public static String method() { return \"B\"; } }");
        addToClasspath(compiler.getPath(path));

        assertImportKeyMatch("import static util.B.*;", "util.B.*", STATIC_IMPORT_ON_DEMAND_SUBKIND, added(VALID));
        assertEval("field;", "\"B\"");
        assertEval("method();", "\"B\"");

        assertImportKeyMatch("import static util.A.method;", "method", SINGLE_STATIC_IMPORT_SUBKIND, added(VALID));
        assertEval("field;", "\"B\"");
        assertEval("method();", "\"A\"");

        assertImportKeyMatch("import static util.A.field;", "field", SINGLE_STATIC_IMPORT_SUBKIND, added(VALID));
        assertEval("field;", "\"A\"");
        assertEval("method();", "\"A\"");
    }

    public void testImportWithComment() {
        assertImportKeyMatch("import java.util.List;//comment", "List", SINGLE_TYPE_IMPORT_SUBKIND, added(VALID));
        assertEval("List l = null;");
    }
}
