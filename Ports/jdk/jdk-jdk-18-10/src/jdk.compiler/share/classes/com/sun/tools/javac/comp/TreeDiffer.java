/*
 * Copyright (c) 2018, Google LLC. All rights reserved.
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.tools.javac.comp;

import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.JCAnnotatedType;
import com.sun.tools.javac.tree.JCTree.JCAnnotation;
import com.sun.tools.javac.tree.JCTree.JCArrayAccess;
import com.sun.tools.javac.tree.JCTree.JCArrayTypeTree;
import com.sun.tools.javac.tree.JCTree.JCAssert;
import com.sun.tools.javac.tree.JCTree.JCAssign;
import com.sun.tools.javac.tree.JCTree.JCAssignOp;
import com.sun.tools.javac.tree.JCTree.JCBinary;
import com.sun.tools.javac.tree.JCTree.JCBindingPattern;
import com.sun.tools.javac.tree.JCTree.JCBlock;
import com.sun.tools.javac.tree.JCTree.JCBreak;
import com.sun.tools.javac.tree.JCTree.JCCase;
import com.sun.tools.javac.tree.JCTree.JCCatch;
import com.sun.tools.javac.tree.JCTree.JCClassDecl;
import com.sun.tools.javac.tree.JCTree.JCCompilationUnit;
import com.sun.tools.javac.tree.JCTree.JCConditional;
import com.sun.tools.javac.tree.JCTree.JCContinue;
import com.sun.tools.javac.tree.JCTree.JCDefaultCaseLabel;
import com.sun.tools.javac.tree.JCTree.JCDoWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCEnhancedForLoop;
import com.sun.tools.javac.tree.JCTree.JCErroneous;
import com.sun.tools.javac.tree.JCTree.JCExports;
import com.sun.tools.javac.tree.JCTree.JCExpressionStatement;
import com.sun.tools.javac.tree.JCTree.JCFieldAccess;
import com.sun.tools.javac.tree.JCTree.JCForLoop;
import com.sun.tools.javac.tree.JCTree.JCIdent;
import com.sun.tools.javac.tree.JCTree.JCIf;
import com.sun.tools.javac.tree.JCTree.JCImport;
import com.sun.tools.javac.tree.JCTree.JCInstanceOf;
import com.sun.tools.javac.tree.JCTree.JCLabeledStatement;
import com.sun.tools.javac.tree.JCTree.JCLambda;
import com.sun.tools.javac.tree.JCTree.JCLiteral;
import com.sun.tools.javac.tree.JCTree.JCMemberReference;
import com.sun.tools.javac.tree.JCTree.JCMethodDecl;
import com.sun.tools.javac.tree.JCTree.JCMethodInvocation;
import com.sun.tools.javac.tree.JCTree.JCModifiers;
import com.sun.tools.javac.tree.JCTree.JCModuleDecl;
import com.sun.tools.javac.tree.JCTree.JCNewArray;
import com.sun.tools.javac.tree.JCTree.JCNewClass;
import com.sun.tools.javac.tree.JCTree.JCOpens;
import com.sun.tools.javac.tree.JCTree.JCPackageDecl;
import com.sun.tools.javac.tree.JCTree.JCPrimitiveTypeTree;
import com.sun.tools.javac.tree.JCTree.JCProvides;
import com.sun.tools.javac.tree.JCTree.JCRequires;
import com.sun.tools.javac.tree.JCTree.JCReturn;
import com.sun.tools.javac.tree.JCTree.JCSwitch;
import com.sun.tools.javac.tree.JCTree.JCSwitchExpression;
import com.sun.tools.javac.tree.JCTree.JCSynchronized;
import com.sun.tools.javac.tree.JCTree.JCThrow;
import com.sun.tools.javac.tree.JCTree.JCTry;
import com.sun.tools.javac.tree.JCTree.JCTypeApply;
import com.sun.tools.javac.tree.JCTree.JCTypeCast;
import com.sun.tools.javac.tree.JCTree.JCTypeIntersection;
import com.sun.tools.javac.tree.JCTree.JCTypeParameter;
import com.sun.tools.javac.tree.JCTree.JCTypeUnion;
import com.sun.tools.javac.tree.JCTree.JCUnary;
import com.sun.tools.javac.tree.JCTree.JCUses;
import com.sun.tools.javac.tree.JCTree.JCVariableDecl;
import com.sun.tools.javac.tree.JCTree.JCWhileLoop;
import com.sun.tools.javac.tree.JCTree.JCWildcard;
import com.sun.tools.javac.tree.JCTree.JCYield;
import com.sun.tools.javac.tree.JCTree.LetExpr;
import com.sun.tools.javac.tree.JCTree.TypeBoundKind;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/** A visitor that compares two lambda bodies for structural equality. */
public class TreeDiffer extends TreeScanner {

    public TreeDiffer(
            Collection<? extends Symbol> symbols, Collection<? extends Symbol> otherSymbols) {
        this.equiv = equiv(symbols, otherSymbols);
    }

    private static Map<Symbol, Symbol> equiv(
            Collection<? extends Symbol> symbols, Collection<? extends Symbol> otherSymbols) {
        Map<Symbol, Symbol> result = new HashMap<>();
        Iterator<? extends Symbol> it = otherSymbols.iterator();
        for (Symbol symbol : symbols) {
            if (!it.hasNext()) break;
            result.put(symbol, it.next());
        }
        return result;
    }

    private JCTree parameter;
    private boolean result;
    private Map<Symbol, Symbol> equiv = new HashMap<>();

    public boolean scan(JCTree tree, JCTree parameter) {
        if (tree == null || parameter == null) {
            return tree == null && parameter == null;
        }
        tree = TreeInfo.skipParens(tree);
        parameter = TreeInfo.skipParens(parameter);
        if (tree.type != null
                && tree.type.constValue() != null
                && parameter.type != null
                && parameter.type.constValue() != null) {
            return Objects.equals(tree.type.constValue(), parameter.type.constValue());
        }
        if (tree.getTag() != parameter.getTag()) {
            return false;
        }
        JCTree prevParameter = this.parameter;
        boolean prevResult = this.result;
        try {
            this.parameter = parameter;
            tree.accept(this);
            return result;
        } finally {
            this.parameter = prevParameter;
            this.result = prevResult;
        }
    }

    private boolean scan(Iterable<? extends JCTree> xs, Iterable<? extends JCTree> ys) {
        if (xs == null || ys == null) {
            return xs == null && ys == null;
        }
        Iterator<? extends JCTree> x = xs.iterator();
        Iterator<? extends JCTree> y = ys.iterator();
        while (x.hasNext() && y.hasNext()) {
            if (!scan(x.next(), y.next())) {
                return false;
            }
        }
        return !x.hasNext() && !y.hasNext();
    }

    private boolean scanDimAnnotations(List<List<JCAnnotation>> xs, List<List<JCAnnotation>> ys) {
        if (xs == null || ys == null) {
            return xs == null && ys == null;
        }
        Iterator<List<JCAnnotation>> x = xs.iterator();
        Iterator<List<JCAnnotation>> y = ys.iterator();
        while (x.hasNext() && y.hasNext()) {
            if (!scan(x.next(), y.next())) {
                return false;
            }
        }
        return !x.hasNext() && !y.hasNext();
    }

    @Override
    public void visitIdent(JCIdent tree) {
        JCIdent that = (JCIdent) parameter;
        // Identifiers are a special case: we want to ensure the identifiers correspond to the
        // same symbols (rather than just having the same name), but also consider lambdas
        // equal if they differ only in the names of the parameters.
        Symbol symbol = tree.sym;
        Symbol otherSymbol = that.sym;
        if (symbol != null && otherSymbol != null) {
            if (Objects.equals(equiv.get(symbol), otherSymbol)) {
                result = true;
                return;
            }
        }
        result = tree.sym == that.sym;
    }

    @Override
    public void visitSelect(JCFieldAccess tree) {
        JCFieldAccess that = (JCFieldAccess) parameter;
        result = scan(tree.selected, that.selected) && tree.sym == that.sym;
    }

    @Override
    public void visitAnnotatedType(JCAnnotatedType tree) {
        JCAnnotatedType that = (JCAnnotatedType) parameter;
        result =
                scan(tree.annotations, that.annotations)
                        && scan(tree.underlyingType, that.underlyingType);
    }

    @Override
    public void visitAnnotation(JCAnnotation tree) {
        JCAnnotation that = (JCAnnotation) parameter;
        result = scan(tree.annotationType, that.annotationType) && scan(tree.args, that.args);
    }

    @Override
    public void visitApply(JCMethodInvocation tree) {
        JCMethodInvocation that = (JCMethodInvocation) parameter;
        result =
                scan(tree.typeargs, that.typeargs)
                        && scan(tree.meth, that.meth)
                        && scan(tree.args, that.args)
                        && tree.polyKind == that.polyKind;
    }

    @Override
    public void visitAssert(JCAssert tree) {
        JCAssert that = (JCAssert) parameter;
        result = scan(tree.cond, that.cond) && scan(tree.detail, that.detail);
    }

    @Override
    public void visitAssign(JCAssign tree) {
        JCAssign that = (JCAssign) parameter;
        result = scan(tree.lhs, that.lhs) && scan(tree.rhs, that.rhs);
    }

    @Override
    public void visitAssignop(JCAssignOp tree) {
        JCAssignOp that = (JCAssignOp) parameter;
        result =
                scan(tree.lhs, that.lhs)
                        && scan(tree.rhs, that.rhs)
                        && tree.operator == that.operator;
    }

    @Override
    public void visitBinary(JCBinary tree) {
        JCBinary that = (JCBinary) parameter;
        result =
                scan(tree.lhs, that.lhs)
                        && scan(tree.rhs, that.rhs)
                        && tree.operator == that.operator;
    }

    @Override
    public void visitBindingPattern(JCBindingPattern tree) {
        JCBindingPattern that = (JCBindingPattern) parameter;
        result = scan(tree.var, that.var);
        if (!result) {
            return;
        }
    }

    @Override
    public void visitBlock(JCBlock tree) {
        JCBlock that = (JCBlock) parameter;
        result = tree.flags == that.flags && scan(tree.stats, that.stats);
    }

    @Override
    public void visitBreak(JCBreak tree) {
        JCBreak that = (JCBreak) parameter;
        result = tree.label == that.label;
    }

    @Override
    public void visitYield(JCYield tree) {
        JCYield that = (JCYield) parameter;
        result = scan(tree.value, that.value);
    }

    @Override
    public void visitCase(JCCase tree) {
        JCCase that = (JCCase) parameter;
        result = scan(tree.labels, that.labels) && scan(tree.stats, that.stats);
    }

    @Override
    public void visitDefaultCaseLabel(JCDefaultCaseLabel tree) {
        result = true;
    }

    @Override
    public void visitCatch(JCCatch tree) {
        JCCatch that = (JCCatch) parameter;
        result = scan(tree.param, that.param) && scan(tree.body, that.body);
    }

    @Override
    public void visitClassDef(JCClassDecl tree) {
        JCClassDecl that = (JCClassDecl) parameter;
        result =
                scan(tree.mods, that.mods)
                        && tree.name == that.name
                        && scan(tree.typarams, that.typarams)
                        && scan(tree.extending, that.extending)
                        && scan(tree.implementing, that.implementing)
                        && scan(tree.defs, that.defs);
    }

    @Override
    public void visitConditional(JCConditional tree) {
        JCConditional that = (JCConditional) parameter;
        result =
                scan(tree.cond, that.cond)
                        && scan(tree.truepart, that.truepart)
                        && scan(tree.falsepart, that.falsepart);
    }

    @Override
    public void visitContinue(JCContinue tree) {
        JCContinue that = (JCContinue) parameter;
        result = tree.label == that.label;
    }

    @Override
    public void visitDoLoop(JCDoWhileLoop tree) {
        JCDoWhileLoop that = (JCDoWhileLoop) parameter;
        result = scan(tree.body, that.body) && scan(tree.cond, that.cond);
    }

    @Override
    public void visitErroneous(JCErroneous tree) {
        JCErroneous that = (JCErroneous) parameter;
        result = scan(tree.errs, that.errs);
    }

    @Override
    public void visitExec(JCExpressionStatement tree) {
        JCExpressionStatement that = (JCExpressionStatement) parameter;
        result = scan(tree.expr, that.expr);
    }

    @Override
    public void visitExports(JCExports tree) {
        JCExports that = (JCExports) parameter;
        result = scan(tree.qualid, that.qualid) && scan(tree.moduleNames, that.moduleNames);
    }

    @Override
    public void visitForLoop(JCForLoop tree) {
        JCForLoop that = (JCForLoop) parameter;
        result =
                scan(tree.init, that.init)
                        && scan(tree.cond, that.cond)
                        && scan(tree.step, that.step)
                        && scan(tree.body, that.body);
    }

    @Override
    public void visitForeachLoop(JCEnhancedForLoop tree) {
        JCEnhancedForLoop that = (JCEnhancedForLoop) parameter;
        result =
                scan(tree.var, that.var)
                        && scan(tree.expr, that.expr)
                        && scan(tree.body, that.body);
    }

    @Override
    public void visitIf(JCIf tree) {
        JCIf that = (JCIf) parameter;
        result =
                scan(tree.cond, that.cond)
                        && scan(tree.thenpart, that.thenpart)
                        && scan(tree.elsepart, that.elsepart);
    }

    @Override
    public void visitImport(JCImport tree) {
        JCImport that = (JCImport) parameter;
        result = tree.staticImport == that.staticImport && scan(tree.qualid, that.qualid);
    }

    @Override
    public void visitIndexed(JCArrayAccess tree) {
        JCArrayAccess that = (JCArrayAccess) parameter;
        result = scan(tree.indexed, that.indexed) && scan(tree.index, that.index);
    }

    @Override
    public void visitLabelled(JCLabeledStatement tree) {
        JCLabeledStatement that = (JCLabeledStatement) parameter;
        result = tree.label == that.label && scan(tree.body, that.body);
    }

    @Override
    public void visitLambda(JCLambda tree) {
        JCLambda that = (JCLambda) parameter;
        result =
                scan(tree.params, that.params)
                        && scan(tree.body, that.body)
                        && tree.paramKind == that.paramKind;
    }

    @Override
    public void visitLetExpr(LetExpr tree) {
        LetExpr that = (LetExpr) parameter;
        result = scan(tree.defs, that.defs) && scan(tree.expr, that.expr);
    }

    @Override
    public void visitLiteral(JCLiteral tree) {
        JCLiteral that = (JCLiteral) parameter;
        result = tree.typetag == that.typetag && Objects.equals(tree.value, that.value);
    }

    @Override
    public void visitMethodDef(JCMethodDecl tree) {
        JCMethodDecl that = (JCMethodDecl) parameter;
        result =
                scan(tree.mods, that.mods)
                        && tree.name == that.name
                        && scan(tree.restype, that.restype)
                        && scan(tree.typarams, that.typarams)
                        && scan(tree.recvparam, that.recvparam)
                        && scan(tree.params, that.params)
                        && scan(tree.thrown, that.thrown)
                        && scan(tree.body, that.body)
                        && scan(tree.defaultValue, that.defaultValue);
    }

    @Override
    public void visitModifiers(JCModifiers tree) {
        JCModifiers that = (JCModifiers) parameter;
        result = tree.flags == that.flags && scan(tree.annotations, that.annotations);
    }

    @Override
    public void visitModuleDef(JCModuleDecl tree) {
        JCModuleDecl that = (JCModuleDecl) parameter;
        result =
                scan(tree.mods, that.mods)
                        && scan(tree.qualId, that.qualId)
                        && scan(tree.directives, that.directives);
    }

    @Override
    public void visitNewArray(JCNewArray tree) {
        JCNewArray that = (JCNewArray) parameter;
        result =
                scan(tree.elemtype, that.elemtype)
                        && scan(tree.dims, that.dims)
                        && scan(tree.annotations, that.annotations)
                        && scanDimAnnotations(tree.dimAnnotations, that.dimAnnotations)
                        && scan(tree.elems, that.elems);
    }

    @Override
    public void visitNewClass(JCNewClass tree) {
        JCNewClass that = (JCNewClass) parameter;
        result =
                scan(tree.encl, that.encl)
                        && scan(tree.typeargs, that.typeargs)
                        && scan(tree.clazz, that.clazz)
                        && scan(tree.args, that.args)
                        && scan(tree.def, that.def)
                        && tree.constructor == that.constructor;
    }

    @Override
    public void visitOpens(JCOpens tree) {
        JCOpens that = (JCOpens) parameter;
        result = scan(tree.qualid, that.qualid) && scan(tree.moduleNames, that.moduleNames);
    }

    @Override
    public void visitPackageDef(JCPackageDecl tree) {
        JCPackageDecl that = (JCPackageDecl) parameter;
        result =
                scan(tree.annotations, that.annotations)
                        && scan(tree.pid, that.pid)
                        && tree.packge == that.packge;
    }

    @Override
    public void visitProvides(JCProvides tree) {
        JCProvides that = (JCProvides) parameter;
        result = scan(tree.serviceName, that.serviceName) && scan(tree.implNames, that.implNames);
    }

    @Override
    public void visitReference(JCMemberReference tree) {
        JCMemberReference that = (JCMemberReference) parameter;
        result =
                tree.mode == that.mode
                        && tree.kind == that.kind
                        && tree.name == that.name
                        && scan(tree.expr, that.expr)
                        && scan(tree.typeargs, that.typeargs);
    }

    @Override
    public void visitRequires(JCRequires tree) {
        JCRequires that = (JCRequires) parameter;
        result =
                tree.isTransitive == that.isTransitive
                        && tree.isStaticPhase == that.isStaticPhase
                        && scan(tree.moduleName, that.moduleName);
    }

    @Override
    public void visitReturn(JCReturn tree) {
        JCReturn that = (JCReturn) parameter;
        result = scan(tree.expr, that.expr);
    }

    @Override
    public void visitSwitch(JCSwitch tree) {
        JCSwitch that = (JCSwitch) parameter;
        result = scan(tree.selector, that.selector) && scan(tree.cases, that.cases);
    }

    @Override
    public void visitSwitchExpression(JCSwitchExpression tree) {
        JCSwitchExpression that = (JCSwitchExpression) parameter;
        result = scan(tree.selector, that.selector) && scan(tree.cases, that.cases);
    }

    @Override
    public void visitSynchronized(JCSynchronized tree) {
        JCSynchronized that = (JCSynchronized) parameter;
        result = scan(tree.lock, that.lock) && scan(tree.body, that.body);
    }

    @Override
    public void visitThrow(JCThrow tree) {
        JCThrow that = (JCThrow) parameter;
        result = scan(tree.expr, that.expr);
    }

    @Override
    public void visitTopLevel(JCCompilationUnit tree) {
        JCCompilationUnit that = (JCCompilationUnit) parameter;
        result =
                scan(tree.defs, that.defs)
                        && tree.modle == that.modle
                        && tree.packge == that.packge;
    }

    @Override
    public void visitTry(JCTry tree) {
        JCTry that = (JCTry) parameter;
        result =
                scan(tree.body, that.body)
                        && scan(tree.catchers, that.catchers)
                        && scan(tree.finalizer, that.finalizer)
                        && scan(tree.resources, that.resources);
    }

    @Override
    public void visitTypeApply(JCTypeApply tree) {
        JCTypeApply that = (JCTypeApply) parameter;
        result = scan(tree.clazz, that.clazz) && scan(tree.arguments, that.arguments);
    }

    @Override
    public void visitTypeArray(JCArrayTypeTree tree) {
        JCArrayTypeTree that = (JCArrayTypeTree) parameter;
        result = scan(tree.elemtype, that.elemtype);
    }

    @Override
    public void visitTypeBoundKind(TypeBoundKind tree) {
        TypeBoundKind that = (TypeBoundKind) parameter;
        result = tree.kind == that.kind;
    }

    @Override
    public void visitTypeCast(JCTypeCast tree) {
        JCTypeCast that = (JCTypeCast) parameter;
        result = scan(tree.clazz, that.clazz) && scan(tree.expr, that.expr);
    }

    @Override
    public void visitTypeIdent(JCPrimitiveTypeTree tree) {
        JCPrimitiveTypeTree that = (JCPrimitiveTypeTree) parameter;
        result = tree.typetag == that.typetag;
    }

    @Override
    public void visitTypeIntersection(JCTypeIntersection tree) {
        JCTypeIntersection that = (JCTypeIntersection) parameter;
        result = scan(tree.bounds, that.bounds);
    }

    @Override
    public void visitTypeParameter(JCTypeParameter tree) {
        JCTypeParameter that = (JCTypeParameter) parameter;
        result =
                tree.name == that.name
                        && scan(tree.bounds, that.bounds)
                        && scan(tree.annotations, that.annotations);
    }

    @Override
    public void visitTypeTest(JCInstanceOf tree) {
        JCInstanceOf that = (JCInstanceOf) parameter;
        result = scan(tree.expr, that.expr) && scan(tree.pattern, that.pattern);
    }

    @Override
    public void visitTypeUnion(JCTypeUnion tree) {
        JCTypeUnion that = (JCTypeUnion) parameter;
        result = scan(tree.alternatives, that.alternatives);
    }

    @Override
    public void visitUnary(JCUnary tree) {
        JCUnary that = (JCUnary) parameter;
        result = scan(tree.arg, that.arg) && tree.operator == that.operator;
    }

    @Override
    public void visitUses(JCUses tree) {
        JCUses that = (JCUses) parameter;
        result = scan(tree.qualid, that.qualid);
    }

    @Override
    public void visitVarDef(JCVariableDecl tree) {
        JCVariableDecl that = (JCVariableDecl) parameter;
        result =
                scan(tree.mods, that.mods)
                        && tree.name == that.name
                        && scan(tree.nameexpr, that.nameexpr)
                        && scan(tree.vartype, that.vartype)
                        && scan(tree.init, that.init);
        if (!result) {
            return;
        }
        equiv.put(tree.sym, that.sym);
    }

    @Override
    public void visitWhileLoop(JCWhileLoop tree) {
        JCWhileLoop that = (JCWhileLoop) parameter;
        result = scan(tree.cond, that.cond) && scan(tree.body, that.body);
    }

    @Override
    public void visitWildcard(JCWildcard tree) {
        JCWildcard that = (JCWildcard) parameter;
        result = scan(tree.kind, that.kind) && scan(tree.inner, that.inner);
    }
}
