/*
 * Copyright (c) 2013, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8024415
 * @summary Pretty printing of JCConditional does not follow the precedence and
 *          associativity rules of JCConditional
 * @modules jdk.compiler/com.sun.tools.javac.file
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @run testng T8024415
 */


import static org.testng.Assert.assertEquals;

import java.io.IOException;
import java.io.StringWriter;

import org.testng.annotations.Test;

import com.sun.tools.javac.file.JavacFileManager;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCExpression;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;


/*
 * Test verifies that the precedence rules of conditional expressions
 * (JCConditional) are correct.
 */
@Test
public class T8024415 {

    TreeMaker maker;
    JCExpression x;


    public T8024415() {
        Context ctx = new Context();
        JavacFileManager.preRegister(ctx);
        maker = TreeMaker.instance(ctx);
        Names names = Names.instance(ctx);
        x = maker.Ident(names.fromString("x"));
    }


    // JLS 15.25: The conditional operator is syntactically right-associative
    // (it groups right-to-left). Thus, a?b:c?d:e?f:g means the same as
    // a?b:(c?d:(e?f:g)).
    public void testAssociativity() throws IOException {

        JCTree left   = maker.Conditional(maker.Conditional(x, x, x), x, x);
        JCTree right  = maker.Conditional(x, x, maker.Conditional(x, x, x));

        String prettyLeft   = prettyPrint(left);
        String prettyRight  = prettyPrint(right);

        assertEquals(prettyLeft.replaceAll("\\s", ""),  "(x?x:x)?x:x");
        assertEquals(prettyRight.replaceAll("\\s", ""), "x?x:x?x:x");

    }


    // The true-part of of a conditional expression is surrounded by ? and :
    // and can thus always be parsed unambiguously without surrounding
    // parentheses.
    public void testPrecedence() throws IOException {

        JCTree left   = maker.Conditional(maker.Assign(x, x), x, x);
        JCTree middle = maker.Conditional(x, maker.Assign(x, x), x);
        JCTree right  = maker.Conditional(x, x, maker.Assign(x, x));

        String prettyLeft   = prettyPrint(left);
        String prettyMiddle = prettyPrint(middle);
        String prettyRight  = prettyPrint(right);

        assertEquals(prettyLeft.replaceAll("\\s", ""),   "(x=x)?x:x");
        assertEquals(prettyMiddle.replaceAll("\\s", ""), "x?x=x:x");
        assertEquals(prettyRight.replaceAll("\\s", ""),  "x?x:(x=x)");

    }


    // Helper method
    private static String prettyPrint(JCTree tree) throws IOException {
        StringWriter sw = new StringWriter();
        new Pretty(sw, true).printExpr(tree);
        return sw.toString();
    }

}
