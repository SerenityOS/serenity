/*
 * Copyright (c) 2017, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @test 8190939 8191842
 * @summary test expressions whose type is inaccessible
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.main
 *          jdk.jdeps/com.sun.tools.javap
 * @library /tools/lib
 * @build KullaTesting Compiler
 * @run testng InaccessibleExpressionTest
 */


import java.nio.file.Path;
import java.nio.file.Paths;

import org.testng.annotations.BeforeMethod;
import jdk.jshell.VarSnippet;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

@Test
public class InaccessibleExpressionTest extends KullaTesting {

    @BeforeMethod
    @Override
    public void setUp() {
        Path path = Paths.get("eit");
        Compiler compiler = new Compiler();
        compiler.compile(path,
                "package priv;\n" +
                "\n" +
                "import java.util.function.Supplier;\n" +
                "import java.util.ArrayList;\n" +
                "\n" +
                "public class GetPriv {\n" +
                "   private enum Count { One };\n" +
                "   public static Packp down() { return new Packp(); }\n" +
                "   public static MyList list() { return new MyList(); }\n" +
                "   public static Count priv() { return Count.One; }\n" +
                "}\n" +
                "\n" +
                "class Packp extends Packp2 {\n" +
                        "public String toString() { return \"Packp\"; } }\n" +
                "\n" +
                "class Packp2 implements Supplier<Integer>  {" +
                        "public Integer get() { return 5; }}\n" +
                "\n" +
                "class MyList extends ArrayList<Integer> {}");
        String tpath = compiler.getPath(path).toString();
        setUp(b -> b
                .remoteVMOptions("--class-path", tpath)
                .compilerOptions("--class-path", tpath));
    }

    public void testExternal() {
        assertEval("import static priv.GetPriv.*;");
        VarSnippet down = varKey(assertEval("down()", "Packp"));
        assertEquals(down.typeName(), "priv.Packp");
        assertEval(down.name() + ".get()", "5");
        VarSnippet list = varKey(assertEval("list()", "[]"));
        assertEquals(list.typeName(), "priv.MyList");
        assertEval(list.name() + ".size()", "0");
        VarSnippet one = varKey(assertEval("priv()", "One"));
        assertEquals(one.typeName(), "priv.GetPriv.Count");
        assertEval("var v = down();", "Packp");
        assertDeclareFail("v.toString()", "compiler.err.not.def.access.class.intf.cant.access");
    }

    public void testInternal() {
        assertEval(
                "class Top {" +
                "    private class Inner {" +
                "        public String toString() { return \"Inner\"; }" +
                "    }" +
                "    Inner n = new Inner(); }");
        VarSnippet n = varKey(assertEval("new Top().n", "Inner"));
        assertEquals(n.typeName(), "Top.Inner");
    }

}
