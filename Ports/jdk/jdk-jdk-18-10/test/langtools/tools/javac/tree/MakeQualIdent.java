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
 * @bug     8023835
 * @summary Verify that TreeMaker.QualIdent(Symbol) field access cascade ends with
 *          the top-level package (when no toplevel is set in TreeMaker)
 * @modules jdk.compiler/com.sun.tools.javac.api
 *          jdk.compiler/com.sun.tools.javac.code
 *          jdk.compiler/com.sun.tools.javac.tree
 *          jdk.compiler/com.sun.tools.javac.util
 * @run main MakeQualIdent
 */

import com.sun.source.tree.IdentifierTree;
import com.sun.source.tree.MemberSelectTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.api.JavacTaskImpl;
import com.sun.tools.javac.api.JavacTool;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.util.Context;
import java.util.ArrayList;

public class MakeQualIdent {
    public static void main(String... args) throws Exception {
        JavacTool tool = JavacTool.create();
        JavacTask task = tool.getTask(null, null, null, new ArrayList<String>(), null, null);
        Context ctx = ((JavacTaskImpl)task).getContext();
        TreeMaker treeMaker = TreeMaker.instance(ctx);
        Symtab syms = Symtab.instance(ctx);

        String stringTree = printTree(treeMaker.QualIdent(syms.stringType.tsym));

        if (!"java.lang.String".equals(stringTree)) {
            throw new IllegalStateException(stringTree);
        }
    }

    private static String printTree(Tree tree) {
        final StringBuilder result = new StringBuilder();

        new TreeScanner<Void, Void>() {
            @Override public Void visitIdentifier(IdentifierTree node, Void p) {
                result.append(node.getName());
                return super.visitIdentifier(node, p);
            }
            @Override public Void visitMemberSelect(MemberSelectTree node, Void p) {
                scan(node.getExpression(), null);
                result.append(".");
                result.append(node.getIdentifier());
                return null;
            }
        }.scan(tree, null);

        return result.toString();
    }
}
