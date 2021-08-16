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

import javax.tools.Diagnostic;

import org.testng.annotations.Test;
import jdk.jshell.VarSnippet;
import java.net.InetAddress;

import static jdk.jshell.Snippet.Status.VALID;
import static jdk.jshell.Snippet.SubKind.*;

public class ExecutionControlTestBase extends KullaTesting {

    String standardListenSpec() {
        String loopback = InetAddress.getLoopbackAddress().getHostAddress();
        return "jdi:hostname(" + loopback + ")";
    }

    String standardLaunchSpec() {
        return "jdi:launch(true)";
    }

    String standardJdiSpec() {
        return "jdi";
    }

    String standardSpecs() {
        return "5(" + standardListenSpec() + "), 6(" + standardLaunchSpec() + "), 7(" + standardJdiSpec() + ")";
    }

    @Test
    public void classesDeclaration() {
        assertEval("interface A { }");
        assertEval("class B implements A { }");
        assertEval("interface C extends A { }");
        assertEval("enum D implements C { }");
        assertEval("@interface E { }");
        assertClasses(
                clazz(KullaTesting.ClassType.INTERFACE, "A"),
                clazz(KullaTesting.ClassType.CLASS, "B"),
                clazz(KullaTesting.ClassType.INTERFACE, "C"),
                clazz(KullaTesting.ClassType.ENUM, "D"),
                clazz(KullaTesting.ClassType.ANNOTATION, "E"));
        assertActiveKeys();
    }

    @Test
    public void interfaceTest() {
        String interfaceSource
                = "interface A {\n"
                + "   default int defaultMethod() { return 1; }\n"
                + "   static int staticMethod() { return 2; }\n"
                + "   int method();\n"
                + "   class Inner1 {}\n"
                + "   static class Inner2 {}\n"
                + "}";
        assertEval(interfaceSource);
        assertEval("A.staticMethod();", "2");
        String classSource
                = "class B implements A {\n"
                + "   public int method() { return 3; }\n"
                + "}";
        assertEval(classSource);
        assertEval("B b = new B();");
        assertEval("b.defaultMethod();", "1");
        assertDeclareFail("B.staticMethod();",
                new ExpectedDiagnostic("compiler.err.cant.resolve.location.args", 0, 14, 1, -1, -1, Diagnostic.Kind.ERROR));
        assertEval("b.method();", "3");
        assertEval("new A.Inner1();");
        assertEval("new A.Inner2();");
        assertEval("new B.Inner1();");
        assertEval("new B.Inner2();");
    }

    @Test
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

    @Test
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

    @Test
    public void testExprSanity() {
        assertEval("int x = 3;", "3");
        assertEval("int y = 4;", "4");
        assertEval("x + y;", "7");
        assertActiveKeys();
    }

    @Test
    public void testImportOnDemand() {
        assertImportKeyMatch("import java.util.*;", "java.util.*", TYPE_IMPORT_ON_DEMAND_SUBKIND, added(VALID));
        assertEval("List<Integer> list = new ArrayList<>();");
        assertEval("list.add(45);");
        assertEval("list.size();", "1");
    }
}
