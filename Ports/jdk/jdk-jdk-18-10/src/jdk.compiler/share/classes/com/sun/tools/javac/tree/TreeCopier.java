/*
 * Copyright (c) 2006, 2019, Oracle and/or its affiliates. All rights reserved.
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

import com.sun.source.tree.*;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.util.DefinedBy;
import com.sun.tools.javac.util.DefinedBy.Api;
import com.sun.tools.javac.util.List;
import com.sun.tools.javac.util.ListBuffer;

/**
 * Creates a copy of a tree, using a given TreeMaker.
 * Names, literal values, etc are shared with the original.
 *
 *  <p><b>This is NOT part of any supported API.
 *  If you write code that depends on this, you do so at your own risk.
 *  This code and its internal interfaces are subject to change or
 *  deletion without notice.</b>
 */
public class TreeCopier<P> implements TreeVisitor<JCTree,P> {
    private TreeMaker M;

    /** Creates a new instance of TreeCopier */
    public TreeCopier(TreeMaker M) {
        this.M = M;
    }

    public <T extends JCTree> T copy(T tree) {
        return copy(tree, null);
    }

    @SuppressWarnings("unchecked")
    public <T extends JCTree> T copy(T tree, P p) {
        if (tree == null)
            return null;
        return (T) (tree.accept(this, p));
    }

    public <T extends JCTree> List<T> copy(List<T> trees) {
        return copy(trees, null);
    }

    public <T extends JCTree> List<T> copy(List<T> trees, P p) {
        if (trees == null)
            return null;
        ListBuffer<T> lb = new ListBuffer<>();
        for (T tree: trees)
            lb.append(copy(tree, p));
        return lb.toList();
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitAnnotatedType(AnnotatedTypeTree node, P p) {
        JCAnnotatedType t = (JCAnnotatedType) node;
        List<JCAnnotation> annotations = copy(t.annotations, p);
        JCExpression underlyingType = copy(t.underlyingType, p);
        return M.at(t.pos).AnnotatedType(annotations, underlyingType);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitAnnotation(AnnotationTree node, P p) {
        JCAnnotation t = (JCAnnotation) node;
        JCTree annotationType = copy(t.annotationType, p);
        List<JCExpression> args = copy(t.args, p);
        if (t.getKind() == Tree.Kind.TYPE_ANNOTATION) {
            JCAnnotation newTA = M.at(t.pos).TypeAnnotation(annotationType, args);
            newTA.attribute = t.attribute;
            return newTA;
        } else {
            JCAnnotation newT = M.at(t.pos).Annotation(annotationType, args);
            newT.attribute = t.attribute;
            return newT;
        }
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitAssert(AssertTree node, P p) {
        JCAssert t = (JCAssert) node;
        JCExpression cond = copy(t.cond, p);
        JCExpression detail = copy(t.detail, p);
        return M.at(t.pos).Assert(cond, detail);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitAssignment(AssignmentTree node, P p) {
        JCAssign t = (JCAssign) node;
        JCExpression lhs = copy(t.lhs, p);
        JCExpression rhs = copy(t.rhs, p);
        return M.at(t.pos).Assign(lhs, rhs);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        JCAssignOp t = (JCAssignOp) node;
        JCTree lhs = copy(t.lhs, p);
        JCTree rhs = copy(t.rhs, p);
        return M.at(t.pos).Assignop(t.getTag(), lhs, rhs);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitBinary(BinaryTree node, P p) {
        JCBinary t = (JCBinary) node;
        JCExpression lhs = copy(t.lhs, p);
        JCExpression rhs = copy(t.rhs, p);
        return M.at(t.pos).Binary(t.getTag(), lhs, rhs);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitBlock(BlockTree node, P p) {
        JCBlock t = (JCBlock) node;
        List<JCStatement> stats = copy(t.stats, p);
        return M.at(t.pos).Block(t.flags, stats);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitBreak(BreakTree node, P p) {
        JCBreak t = (JCBreak) node;
        return M.at(t.pos).Break(t.label);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitYield(YieldTree node, P p) {
        JCYield t = (JCYield) node;
        JCExpression value = copy(t.value, p);
        return M.at(t.pos).Yield(value);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitCase(CaseTree node, P p) {
        JCCase t = (JCCase) node;
        List<JCCaseLabel> labels = copy(t.labels, p);
        List<JCStatement> stats = copy(t.stats, p);
        JCTree body;
        if (node.getCaseKind() == CaseTree.CaseKind.RULE) {
            body = t.body instanceof JCExpression && t.stats.head.hasTag(Tag.YIELD)
                    ? ((JCYield) t.stats.head).value : t.stats.head;
        } else {
            body = null;
        }
        return M.at(t.pos).Case(t.caseKind, labels, stats, body);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitCatch(CatchTree node, P p) {
        JCCatch t = (JCCatch) node;
        JCVariableDecl param = copy(t.param, p);
        JCBlock body = copy(t.body, p);
        return M.at(t.pos).Catch(param, body);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitClass(ClassTree node, P p) {
        JCClassDecl t = (JCClassDecl) node;
        JCModifiers mods = copy(t.mods, p);
        List<JCTypeParameter> typarams = copy(t.typarams, p);
        JCExpression extending = copy(t.extending, p);
        List<JCExpression> implementing = copy(t.implementing, p);
        List<JCTree> defs = copy(t.defs, p);
        return M.at(t.pos).ClassDef(mods, t.name, typarams, extending, implementing, defs);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitConditionalExpression(ConditionalExpressionTree node, P p) {
        JCConditional t = (JCConditional) node;
        JCExpression cond = copy(t.cond, p);
        JCExpression truepart = copy(t.truepart, p);
        JCExpression falsepart = copy(t.falsepart, p);
        return M.at(t.pos).Conditional(cond, truepart, falsepart);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitContinue(ContinueTree node, P p) {
        JCContinue t = (JCContinue) node;
        return M.at(t.pos).Continue(t.label);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitDoWhileLoop(DoWhileLoopTree node, P p) {
        JCDoWhileLoop t = (JCDoWhileLoop) node;
        JCStatement body = copy(t.body, p);
        JCExpression cond = copy(t.cond, p);
        return M.at(t.pos).DoLoop(body, cond);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitErroneous(ErroneousTree node, P p) {
        JCErroneous t = (JCErroneous) node;
        List<? extends JCTree> errs = copy(t.errs, p);
        return M.at(t.pos).Erroneous(errs);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitExpressionStatement(ExpressionStatementTree node, P p) {
        JCExpressionStatement t = (JCExpressionStatement) node;
        JCExpression expr = copy(t.expr, p);
        return M.at(t.pos).Exec(expr);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        JCEnhancedForLoop t = (JCEnhancedForLoop) node;
        JCVariableDecl var = copy(t.var, p);
        JCExpression expr = copy(t.expr, p);
        JCStatement body = copy(t.body, p);
        return M.at(t.pos).ForeachLoop(var, expr, body);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitForLoop(ForLoopTree node, P p) {
        JCForLoop t = (JCForLoop) node;
        List<JCStatement> init = copy(t.init, p);
        JCExpression cond = copy(t.cond, p);
        List<JCExpressionStatement> step = copy(t.step, p);
        JCStatement body = copy(t.body, p);
        return M.at(t.pos).ForLoop(init, cond, step, body);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitIdentifier(IdentifierTree node, P p) {
        JCIdent t = (JCIdent) node;
        return M.at(t.pos).Ident(t.name);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitIf(IfTree node, P p) {
        JCIf t = (JCIf) node;
        JCExpression cond = copy(t.cond, p);
        JCStatement thenpart = copy(t.thenpart, p);
        JCStatement elsepart = copy(t.elsepart, p);
        return M.at(t.pos).If(cond, thenpart, elsepart);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitImport(ImportTree node, P p) {
        JCImport t = (JCImport) node;
        JCTree qualid = copy(t.qualid, p);
        return M.at(t.pos).Import(qualid, t.staticImport);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitArrayAccess(ArrayAccessTree node, P p) {
        JCArrayAccess t = (JCArrayAccess) node;
        JCExpression indexed = copy(t.indexed, p);
        JCExpression index = copy(t.index, p);
        return M.at(t.pos).Indexed(indexed, index);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitLabeledStatement(LabeledStatementTree node, P p) {
        JCLabeledStatement t = (JCLabeledStatement) node;
        JCStatement body = copy(t.body, p);
        return M.at(t.pos).Labelled(t.label, body);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitLiteral(LiteralTree node, P p) {
        JCLiteral t = (JCLiteral) node;
        return M.at(t.pos).Literal(t.typetag, t.value);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitMethod(MethodTree node, P p) {
        JCMethodDecl t  = (JCMethodDecl) node;
        JCModifiers mods = copy(t.mods, p);
        JCExpression restype = copy(t.restype, p);
        List<JCTypeParameter> typarams = copy(t.typarams, p);
        List<JCVariableDecl> params = copy(t.params, p);
        JCVariableDecl recvparam = copy(t.recvparam, p);
        List<JCExpression> thrown = copy(t.thrown, p);
        JCBlock body = copy(t.body, p);
        JCExpression defaultValue = copy(t.defaultValue, p);
        return M.at(t.pos).MethodDef(mods, t.name, restype, typarams, recvparam, params, thrown, body, defaultValue);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitMethodInvocation(MethodInvocationTree node, P p) {
        JCMethodInvocation t = (JCMethodInvocation) node;
        List<JCExpression> typeargs = copy(t.typeargs, p);
        JCExpression meth = copy(t.meth, p);
        List<JCExpression> args = copy(t.args, p);
        return M.at(t.pos).Apply(typeargs, meth, args);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitModifiers(ModifiersTree node, P p) {
        JCModifiers t = (JCModifiers) node;
        List<JCAnnotation> annotations = copy(t.annotations, p);
        return M.at(t.pos).Modifiers(t.flags, annotations);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitNewArray(NewArrayTree node, P p) {
        JCNewArray t = (JCNewArray) node;
        JCExpression elemtype = copy(t.elemtype, p);
        List<JCExpression> dims = copy(t.dims, p);
        List<JCExpression> elems = copy(t.elems, p);
        return M.at(t.pos).NewArray(elemtype, dims, elems);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitNewClass(NewClassTree node, P p) {
        JCNewClass t = (JCNewClass) node;
        JCExpression encl = copy(t.encl, p);
        List<JCExpression> typeargs = copy(t.typeargs, p);
        JCExpression clazz = copy(t.clazz, p);
        List<JCExpression> args = copy(t.args, p);
        JCClassDecl def = copy(t.def, p);
        return M.at(t.pos).NewClass(encl, typeargs, clazz, args, def);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitLambdaExpression(LambdaExpressionTree node, P p) {
        JCLambda t = (JCLambda) node;
        List<JCVariableDecl> params = copy(t.params, p);
        JCTree body = copy(t.body, p);
        return M.at(t.pos).Lambda(params, body);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitParenthesized(ParenthesizedTree node, P p) {
        JCParens t = (JCParens) node;
        JCExpression expr = copy(t.expr, p);
        return M.at(t.pos).Parens(expr);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitReturn(ReturnTree node, P p) {
        JCReturn t = (JCReturn) node;
        JCExpression expr = copy(t.expr, p);
        return M.at(t.pos).Return(expr);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitMemberSelect(MemberSelectTree node, P p) {
        JCFieldAccess t = (JCFieldAccess) node;
        JCExpression selected = copy(t.selected, p);
        return M.at(t.pos).Select(selected, t.name);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitMemberReference(MemberReferenceTree node, P p) {
        JCMemberReference t = (JCMemberReference) node;
        JCExpression expr = copy(t.expr, p);
        List<JCExpression> typeargs = copy(t.typeargs, p);
        return M.at(t.pos).Reference(t.mode, t.name, expr, typeargs);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitEmptyStatement(EmptyStatementTree node, P p) {
        JCSkip t = (JCSkip) node;
        return M.at(t.pos).Skip();
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitSwitch(SwitchTree node, P p) {
        JCSwitch t = (JCSwitch) node;
        JCExpression selector = copy(t.selector, p);
        List<JCCase> cases = copy(t.cases, p);
        return M.at(t.pos).Switch(selector, cases);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitSwitchExpression(SwitchExpressionTree node, P p) {
        JCSwitchExpression t = (JCSwitchExpression) node;
        JCExpression selector = copy(t.selector, p);
        List<JCCase> cases = copy(t.cases, p);
        return M.at(t.pos).SwitchExpression(selector, cases);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitSynchronized(SynchronizedTree node, P p) {
        JCSynchronized t = (JCSynchronized) node;
        JCExpression lock = copy(t.lock, p);
        JCBlock body = copy(t.body, p);
        return M.at(t.pos).Synchronized(lock, body);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitThrow(ThrowTree node, P p) {
        JCThrow t = (JCThrow) node;
        JCExpression expr = copy(t.expr, p);
        return M.at(t.pos).Throw(expr);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitCompilationUnit(CompilationUnitTree node, P p) {
        JCCompilationUnit t = (JCCompilationUnit) node;
        List<JCTree> defs = copy(t.defs, p);
        return M.at(t.pos).TopLevel(defs);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitPackage(PackageTree node, P p) {
        JCPackageDecl t = (JCPackageDecl) node;
        List<JCAnnotation> annotations = copy(t.annotations, p);
        JCExpression pid = copy(t.pid, p);
        return M.at(t.pos).PackageDecl(annotations, pid);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitTry(TryTree node, P p) {
        JCTry t = (JCTry) node;
        List<JCTree> resources = copy(t.resources, p);
        JCBlock body = copy(t.body, p);
        List<JCCatch> catchers = copy(t.catchers, p);
        JCBlock finalizer = copy(t.finalizer, p);
        return M.at(t.pos).Try(resources, body, catchers, finalizer);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitParameterizedType(ParameterizedTypeTree node, P p) {
        JCTypeApply t = (JCTypeApply) node;
        JCExpression clazz = copy(t.clazz, p);
        List<JCExpression> arguments = copy(t.arguments, p);
        return M.at(t.pos).TypeApply(clazz, arguments);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitUnionType(UnionTypeTree node, P p) {
        JCTypeUnion t = (JCTypeUnion) node;
        List<JCExpression> components = copy(t.alternatives, p);
        return M.at(t.pos).TypeUnion(components);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitIntersectionType(IntersectionTypeTree node, P p) {
        JCTypeIntersection t = (JCTypeIntersection) node;
        List<JCExpression> bounds = copy(t.bounds, p);
        return M.at(t.pos).TypeIntersection(bounds);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitArrayType(ArrayTypeTree node, P p) {
        JCArrayTypeTree t = (JCArrayTypeTree) node;
        JCExpression elemtype = copy(t.elemtype, p);
        return M.at(t.pos).TypeArray(elemtype);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitTypeCast(TypeCastTree node, P p) {
        JCTypeCast t = (JCTypeCast) node;
        JCTree clazz = copy(t.clazz, p);
        JCExpression expr = copy(t.expr, p);
        return M.at(t.pos).TypeCast(clazz, expr);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitPrimitiveType(PrimitiveTypeTree node, P p) {
        JCPrimitiveTypeTree t = (JCPrimitiveTypeTree) node;
        return M.at(t.pos).TypeIdent(t.typetag);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitTypeParameter(TypeParameterTree node, P p) {
        JCTypeParameter t = (JCTypeParameter) node;
        List<JCAnnotation> annos = copy(t.annotations, p);
        List<JCExpression> bounds = copy(t.bounds, p);
        return M.at(t.pos).TypeParameter(t.name, bounds, annos);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitInstanceOf(InstanceOfTree node, P p) {
        JCInstanceOf t = (JCInstanceOf) node;
        JCExpression expr = copy(t.expr, p);
        JCTree pattern = copy(t.pattern, p);
        return M.at(t.pos).TypeTest(expr, pattern);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitBindingPattern(BindingPatternTree node, P p) {
        JCBindingPattern t = (JCBindingPattern) node;
        JCVariableDecl var = copy(t.var, p);
        return M.at(t.pos).BindingPattern(var);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitGuardedPattern(GuardedPatternTree node, P p) {
        JCGuardPattern t = (JCGuardPattern) node;
        JCPattern patt = copy(t.patt, p);
        JCExpression expr = copy(t.expr, p);
        return M.at(t.pos).GuardPattern(patt, expr);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitParenthesizedPattern(ParenthesizedPatternTree node, P p) {
        JCParenthesizedPattern t = (JCParenthesizedPattern) node;
        JCPattern pattern = copy(t.pattern, p);
        return M.at(t.pos).ParenthesizedPattern(pattern);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitDefaultCaseLabel(DefaultCaseLabelTree node, P p) {
        JCDefaultCaseLabel t = (JCDefaultCaseLabel) node;
        return M.at(t.pos).DefaultCaseLabel();
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitUnary(UnaryTree node, P p) {
        JCUnary t = (JCUnary) node;
        JCExpression arg = copy(t.arg, p);
        return M.at(t.pos).Unary(t.getTag(), arg);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitVariable(VariableTree node, P p) {
        JCVariableDecl t = (JCVariableDecl) node;
        JCModifiers mods = copy(t.mods, p);
        JCExpression vartype = copy(t.vartype, p);
        if (t.nameexpr == null) {
            JCExpression init = copy(t.init, p);
            return M.at(t.pos).VarDef(mods, t.name, vartype, init);
        } else {
            JCExpression nameexpr = copy(t.nameexpr, p);
            return M.at(t.pos).ReceiverVarDef(mods, nameexpr, vartype);
        }
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitWhileLoop(WhileLoopTree node, P p) {
        JCWhileLoop t = (JCWhileLoop) node;
        JCStatement body = copy(t.body, p);
        JCExpression cond = copy(t.cond, p);
        return M.at(t.pos).WhileLoop(cond, body);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitWildcard(WildcardTree node, P p) {
        JCWildcard t = (JCWildcard) node;
        TypeBoundKind kind = M.at(t.kind.pos).TypeBoundKind(t.kind.kind);
        JCTree inner = copy(t.inner, p);
        return M.at(t.pos).Wildcard(kind, inner);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitModule(ModuleTree node, P p) {
        JCModuleDecl t = (JCModuleDecl) node;
        JCModifiers mods = copy(t.mods, p);
        JCExpression qualId = copy(t.qualId);
        List<JCDirective> directives = copy(t.directives);
        return M.at(t.pos).ModuleDef(mods, t.getModuleType(), qualId, directives);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCExports visitExports(ExportsTree node, P p) {
        JCExports t = (JCExports) node;
        JCExpression qualId = copy(t.qualid, p);
        List<JCExpression> moduleNames = copy(t.moduleNames, p);
        return M.at(t.pos).Exports(qualId, moduleNames);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCOpens visitOpens(OpensTree node, P p) {
        JCOpens t = (JCOpens) node;
        JCExpression qualId = copy(t.qualid, p);
        List<JCExpression> moduleNames = copy(t.moduleNames, p);
        return M.at(t.pos).Opens(qualId, moduleNames);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCProvides visitProvides(ProvidesTree node, P p) {
        JCProvides t = (JCProvides) node;
        JCExpression serviceName = copy(t.serviceName, p);
        List<JCExpression> implNames = copy(t.implNames, p);
        return M.at(t.pos).Provides(serviceName, implNames);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCRequires visitRequires(RequiresTree node, P p) {
        JCRequires t = (JCRequires) node;
        JCExpression moduleName = copy(t.moduleName, p);
        return M.at(t.pos).Requires(t.isTransitive, t.isStaticPhase, moduleName);
    }

    @Override @DefinedBy(Api.COMPILER_TREE)
    public JCUses visitUses(UsesTree node, P p) {
        JCUses t = (JCUses) node;
        JCExpression serviceName = copy(t.qualid, p);
        return M.at(t.pos).Uses(serviceName);
    }

    @DefinedBy(Api.COMPILER_TREE)
    public JCTree visitOther(Tree node, P p) {
        JCTree tree = (JCTree) node;
        switch (tree.getTag()) {
            case LETEXPR: {
                LetExpr t = (LetExpr) node;
                List<JCStatement> defs = copy(t.defs, p);
                JCExpression expr = copy(t.expr, p);
                return M.at(t.pos).LetExpr(defs, expr);
            }
            default:
                throw new AssertionError("unknown tree tag: " + tree.getTag());
        }
    }

}
