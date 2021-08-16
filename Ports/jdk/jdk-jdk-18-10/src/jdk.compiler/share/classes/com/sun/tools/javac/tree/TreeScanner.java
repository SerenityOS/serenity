/*
 * Copyright (c) 2001, 2020, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.tree;

import com.sun.tools.javac.util.*;
import com.sun.tools.javac.tree.JCTree.*;

/** A subclass of Tree.Visitor, this class defines
 *  a general tree scanner pattern. Translation proceeds recursively in
 *  left-to-right order down a tree. There is one visitor method in this class
 *  for every possible kind of tree node.  To obtain a specific
 *  scanner, it suffices to override those visitor methods which
 *  do some interesting work. The scanner class itself takes care of all
 *  navigational aspects.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class TreeScanner extends Visitor {

    /** Visitor method: Scan a single node.
     */
    public void scan(JCTree tree) {
        if(tree!=null) tree.accept(this);
    }

    /** Visitor method: scan a list of nodes.
     */
    public void scan(List<? extends JCTree> trees) {
        if (trees != null)
        for (List<? extends JCTree> l = trees; l.nonEmpty(); l = l.tail)
            scan(l.head);
    }


/* ***************************************************************************
 * Visitor methods
 ****************************************************************************/

    public void visitTopLevel(JCCompilationUnit tree) {
        scan(tree.defs);
    }

    public void visitPackageDef(JCPackageDecl tree) {
        scan(tree.annotations);
        scan(tree.pid);
    }

    @Override
    public void visitModuleDef(JCModuleDecl tree) {
        scan(tree.mods);
        scan(tree.qualId);
        scan(tree.directives);
    }

    @Override
    public void visitExports(JCExports tree) {
        scan(tree.qualid);
        scan(tree.moduleNames);
    }

    @Override
    public void visitOpens(JCOpens tree) {
        scan(tree.qualid);
        scan(tree.moduleNames);
    }

    @Override
    public void visitProvides(JCProvides tree) {
        scan(tree.serviceName);
        scan(tree.implNames);
    }

    @Override
    public void visitRequires(JCRequires tree) {
        scan(tree.moduleName);
    }

    @Override
    public void visitUses(JCUses tree) {
        scan(tree.qualid);
    }

    public void visitImport(JCImport tree) {
        scan(tree.qualid);
    }

    public void visitClassDef(JCClassDecl tree) {
        scan(tree.mods);
        scan(tree.typarams);
        scan(tree.extending);
        scan(tree.implementing);
        scan(tree.permitting);
        scan(tree.defs);
    }

    public void visitMethodDef(JCMethodDecl tree) {
        scan(tree.mods);
        scan(tree.restype);
        scan(tree.typarams);
        scan(tree.recvparam);
        scan(tree.params);
        scan(tree.thrown);
        scan(tree.defaultValue);
        scan(tree.body);
    }

    public void visitVarDef(JCVariableDecl tree) {
        scan(tree.mods);
        scan(tree.vartype);
        scan(tree.nameexpr);
        scan(tree.init);
    }

    public void visitSkip(JCSkip tree) {
    }

    public void visitBlock(JCBlock tree) {
        scan(tree.stats);
    }

    public void visitDoLoop(JCDoWhileLoop tree) {
        scan(tree.body);
        scan(tree.cond);
    }

    public void visitWhileLoop(JCWhileLoop tree) {
        scan(tree.cond);
        scan(tree.body);
    }

    public void visitForLoop(JCForLoop tree) {
        scan(tree.init);
        scan(tree.cond);
        scan(tree.step);
        scan(tree.body);
    }

    public void visitForeachLoop(JCEnhancedForLoop tree) {
        scan(tree.var);
        scan(tree.expr);
        scan(tree.body);
    }

    public void visitLabelled(JCLabeledStatement tree) {
        scan(tree.body);
    }

    public void visitSwitch(JCSwitch tree) {
        scan(tree.selector);
        scan(tree.cases);
    }

    public void visitCase(JCCase tree) {
        scan(tree.labels);
        scan(tree.stats);
    }

    public void visitSwitchExpression(JCSwitchExpression tree) {
        scan(tree.selector);
        scan(tree.cases);
    }

    public void visitSynchronized(JCSynchronized tree) {
        scan(tree.lock);
        scan(tree.body);
    }

    public void visitTry(JCTry tree) {
        scan(tree.resources);
        scan(tree.body);
        scan(tree.catchers);
        scan(tree.finalizer);
    }

    public void visitCatch(JCCatch tree) {
        scan(tree.param);
        scan(tree.body);
    }

    public void visitConditional(JCConditional tree) {
        scan(tree.cond);
        scan(tree.truepart);
        scan(tree.falsepart);
    }

    public void visitIf(JCIf tree) {
        scan(tree.cond);
        scan(tree.thenpart);
        scan(tree.elsepart);
    }

    public void visitExec(JCExpressionStatement tree) {
        scan(tree.expr);
    }

    public void visitBreak(JCBreak tree) {
    }

    public void visitYield(JCYield tree) {
        scan(tree.value);
    }

    public void visitContinue(JCContinue tree) {
    }

    public void visitReturn(JCReturn tree) {
        scan(tree.expr);
    }

    public void visitThrow(JCThrow tree) {
        scan(tree.expr);
    }

    public void visitAssert(JCAssert tree) {
        scan(tree.cond);
        scan(tree.detail);
    }

    public void visitApply(JCMethodInvocation tree) {
        scan(tree.typeargs);
        scan(tree.meth);
        scan(tree.args);
    }

    public void visitNewClass(JCNewClass tree) {
        scan(tree.encl);
        scan(tree.typeargs);
        scan(tree.clazz);
        scan(tree.args);
        scan(tree.def);
    }

    public void visitNewArray(JCNewArray tree) {
        scan(tree.annotations);
        scan(tree.elemtype);
        scan(tree.dims);
        for (List<JCAnnotation> annos : tree.dimAnnotations)
            scan(annos);
        scan(tree.elems);
    }

    public void visitLambda(JCLambda tree) {
        scan(tree.body);
        scan(tree.params);
    }

    public void visitParens(JCParens tree) {
        scan(tree.expr);
    }

    public void visitAssign(JCAssign tree) {
        scan(tree.lhs);
        scan(tree.rhs);
    }

    public void visitAssignop(JCAssignOp tree) {
        scan(tree.lhs);
        scan(tree.rhs);
    }

    public void visitUnary(JCUnary tree) {
        scan(tree.arg);
    }

    public void visitBinary(JCBinary tree) {
        scan(tree.lhs);
        scan(tree.rhs);
    }

    public void visitTypeCast(JCTypeCast tree) {
        scan(tree.clazz);
        scan(tree.expr);
    }

    public void visitTypeTest(JCInstanceOf tree) {
        scan(tree.expr);
        scan(tree.pattern);
    }

    public void visitBindingPattern(JCBindingPattern tree) {
        scan(tree.var);
    }

    @Override
    public void visitDefaultCaseLabel(JCDefaultCaseLabel tree) {
    }

    @Override
    public void visitParenthesizedPattern(JCParenthesizedPattern that) {
        scan(that.pattern);
    }

    @Override
    public void visitGuardPattern(JCGuardPattern that) {
        scan(that.patt);
        scan(that.expr);
    }

    public void visitIndexed(JCArrayAccess tree) {
        scan(tree.indexed);
        scan(tree.index);
    }

    public void visitSelect(JCFieldAccess tree) {
        scan(tree.selected);
    }

    public void visitReference(JCMemberReference tree) {
        scan(tree.expr);
        scan(tree.typeargs);
    }

    public void visitIdent(JCIdent tree) {
    }

    public void visitLiteral(JCLiteral tree) {
    }

    public void visitTypeIdent(JCPrimitiveTypeTree tree) {
    }

    public void visitTypeArray(JCArrayTypeTree tree) {
        scan(tree.elemtype);
    }

    public void visitTypeApply(JCTypeApply tree) {
        scan(tree.clazz);
        scan(tree.arguments);
    }

    public void visitTypeUnion(JCTypeUnion tree) {
        scan(tree.alternatives);
    }

    public void visitTypeIntersection(JCTypeIntersection tree) {
        scan(tree.bounds);
    }

    public void visitTypeParameter(JCTypeParameter tree) {
        scan(tree.annotations);
        scan(tree.bounds);
    }

    @Override
    public void visitWildcard(JCWildcard tree) {
        scan(tree.kind);
        if (tree.inner != null)
            scan(tree.inner);
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind that) {
    }

    public void visitModifiers(JCModifiers tree) {
        scan(tree.annotations);
    }

    public void visitAnnotation(JCAnnotation tree) {
        scan(tree.annotationType);
        scan(tree.args);
    }

    public void visitAnnotatedType(JCAnnotatedType tree) {
        scan(tree.annotations);
        scan(tree.underlyingType);
    }

    public void visitErroneous(JCErroneous tree) {
    }

    public void visitLetExpr(LetExpr tree) {
        scan(tree.defs);
        scan(tree.expr);
    }

    public void visitTree(JCTree tree) {
        Assert.error();
    }
}
