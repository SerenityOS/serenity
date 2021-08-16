/*
 * Copyright (c) 2003, 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.javadoc.internal.tool;

import com.sun.source.util.TreePath;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.comp.MemberEnter;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.List;

import static com.sun.tools.javac.code.Flags.*;
import static com.sun.tools.javac.code.Kinds.Kind.*;

/**
 *  Javadoc's own memberEnter phase does a few things above and beyond that
 *  done by javac.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class JavadocMemberEnter extends MemberEnter {
    public static JavadocMemberEnter instance0(Context context) {
        MemberEnter instance = context.get(memberEnterKey);
        if (instance == null)
            instance = new JavadocMemberEnter(context);
        return (JavadocMemberEnter)instance;
    }

    public static void preRegister(Context context) {
        context.put(memberEnterKey, (Context.Factory<MemberEnter>)JavadocMemberEnter::new);
    }

    final ToolEnvironment toolEnv;


    protected JavadocMemberEnter(Context context) {
        super(context);
        toolEnv = ToolEnvironment.instance(context);
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        super.visitMethodDef(tree);
        MethodSymbol meth = tree.sym;
        if (meth == null || meth.kind != MTH) return;
        TreePath treePath = toolEnv.getTreePath(env.toplevel, env.enclClass, tree);
        // do not add those methods that may be mandated by the spec,
        // or those that are synthesized, thus if it does not exist in
        // tree best to let other logic determine the TreePath.
        if (env.enclClass.defs.contains(tree)) {
            toolEnv.setElementToTreePath(meth, treePath);
        }
        // release resources
        // handle constructors for record types specially, because of downstream checks
        if ((env.enclClass.mods.flags & Flags.RECORD) != 0 && TreeInfo.isConstructor(tree)) {
            tree.body.stats = List.nil();
        } else {
        tree.body = null;
        }
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        if (tree.init != null) {
            boolean isFinal = (tree.mods.flags & FINAL) != 0
                    || (env.enclClass.mods.flags & INTERFACE) != 0;
            if (!isFinal || containsNonConstantExpression(tree.init)) {
                // Avoid unnecessary analysis and release resources.
                // In particular, remove non-constant expressions
                // which may trigger Attr.attribClass, since
                // method bodies are also removed, in visitMethodDef.
                tree.init = null;
            }
        }
        super.visitVarDef(tree);
        if (tree.sym != null && tree.sym.kind == VAR && !isParameter(tree.sym)) {
            toolEnv.setElementToTreePath(tree.sym, toolEnv.getTreePath(env.toplevel, env.enclClass, tree));
        }
    }

    private static boolean isParameter(VarSymbol var) {
        return (var.flags() & Flags.PARAMETER) != 0;
    }

    /**
     * Simple analysis of an expression tree to see if it contains tree nodes
     * for any non-constant expression. This does not include checking references
     * to other fields which may or may not be constant.
     */
    private static boolean containsNonConstantExpression(JCExpression tree) {
        return new MaybeConstantExpressionScanner().containsNonConstantExpression(tree);
    }

    /**
     * See JLS 15.18, Constant Expression
     */
    private static class MaybeConstantExpressionScanner extends JCTree.Visitor {
        boolean maybeConstantExpr = true;

        public boolean containsNonConstantExpression(JCExpression tree) {
            scan(tree);
            return !maybeConstantExpr;
        }

        public void scan(JCTree tree) {
            // short circuit scan when end result is definitely false
            if (maybeConstantExpr && tree != null)
                tree.accept(this);
        }

        @Override
        /** default for any non-overridden visit method. */
        public void visitTree(JCTree tree) {
            maybeConstantExpr = false;
        }

        @Override
        public void visitBinary(JCBinary tree) {
            switch (tree.getTag()) {
                case MUL: case DIV: case MOD:
                case PLUS: case MINUS:
                case SL: case SR: case USR:
                case LT: case LE: case GT: case GE:
                case EQ: case NE:
                case BITAND: case BITXOR: case BITOR:
                case AND: case OR:
                    break;
                default:
                    maybeConstantExpr = false;
            }
        }

        @Override
        public void visitConditional(JCConditional tree) {
            scan(tree.cond);
            scan(tree.truepart);
            scan(tree.falsepart);
        }

        @Override
        public void visitIdent(JCIdent tree) { }

        @Override
        public void visitLiteral(JCLiteral tree) { }

        @Override
        public void visitParens(JCParens tree) {
            scan(tree.expr);
        }

        @Override
        public void visitSelect(JCTree.JCFieldAccess tree) {
            scan(tree.selected);
        }

        @Override
        public void visitTypeCast(JCTypeCast tree) {
            scan(tree.clazz);
            scan(tree.expr);
        }

        @Override
        public void visitTypeIdent(JCPrimitiveTypeTree tree) { }

        @Override
        public void visitUnary(JCUnary tree) {
            switch (tree.getTag()) {
                case POS: case NEG: case COMPL: case NOT:
                    break;
                default:
                    maybeConstantExpr = false;
            }
        }
    }
}
