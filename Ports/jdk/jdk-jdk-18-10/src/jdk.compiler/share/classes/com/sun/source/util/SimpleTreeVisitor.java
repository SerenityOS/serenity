/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.source.util;

import com.sun.source.tree.*;
import jdk.internal.javac.PreviewFeature;

/**
 * A simple visitor for tree nodes.
 *
 * @param <R> the return type of this visitor's methods.  Use {@link
 *            Void} for visitors that do not need to return results.
 * @param <P> the type of the additional parameter to this visitor's
 *            methods.  Use {@code Void} for visitors that do not need an
 *            additional parameter.
 *
 * @author Peter von der Ah&eacute;
 * @since 1.6
 */
public class SimpleTreeVisitor <R,P> implements TreeVisitor<R,P> {
    /**
     * The default value, returned by the {@link #defaultAction default action}.
     */
    protected final R DEFAULT_VALUE;

    /**
     * Creates a visitor, with a DEFAULT_VALUE of {@code null}.
     */
    protected SimpleTreeVisitor() {
        DEFAULT_VALUE = null;
    }

    /**
     * Creates a visitor, with a specified DEFAULT_VALUE.
     * @param defaultValue the default value to be returned by the default action
     */
    protected SimpleTreeVisitor(R defaultValue) {
        DEFAULT_VALUE = defaultValue;
    }

    /**
     * The default action, used by all visit methods that are not overridden.
     * @param node the node being visited
     * @param p the parameter value passed to the visit method
     * @return the result value to be returned from the visit method
     */
    protected R defaultAction(Tree node, P p) {
        return DEFAULT_VALUE;
    }

    /**
     * Invokes the appropriate visit method specific to the type of the node.
     * @param node the node on which to dispatch
     * @param p a parameter to be passed to the appropriate visit method
     * @return the value returns from the appropriate visit method
     */
    public final R visit(Tree node, P p) {
        return (node == null) ? null : node.accept(this, p);
    }

    /**
     * Invokes the appropriate visit method on each of a sequence of nodes.
     * @param nodes the nodes on which to dispatch
     * @param p a parameter value to be passed to each appropriate visit method
     * @return the value return from the last of the visit methods, or null
     *      if none were called
     */
    public final R visit(Iterable<? extends Tree> nodes, P p) {
        R r = null;
        if (nodes != null)
            for (Tree node : nodes)
                r = visit(node, p);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitCompilationUnit(CompilationUnitTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitPackage(PackageTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitImport(ImportTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitClass(ClassTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitMethod(MethodTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitVariable(VariableTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitEmptyStatement(EmptyStatementTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitBlock(BlockTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitDoWhileLoop(DoWhileLoopTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitWhileLoop(WhileLoopTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitForLoop(ForLoopTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitLabeledStatement(LabeledStatementTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitSwitch(SwitchTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitSwitchExpression(SwitchExpressionTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitCase(CaseTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitSynchronized(SynchronizedTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitTry(TryTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitCatch(CatchTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitConditionalExpression(ConditionalExpressionTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitIf(IfTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitExpressionStatement(ExpressionStatementTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitBreak(BreakTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitContinue(ContinueTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitReturn(ReturnTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitThrow(ThrowTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitAssert(AssertTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitNewClass(NewClassTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitNewArray(NewArrayTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitLambdaExpression(LambdaExpressionTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitParenthesized(ParenthesizedTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitAssignment(AssignmentTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitUnary(UnaryTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitBinary(BinaryTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitTypeCast(TypeCastTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitInstanceOf(InstanceOfTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     * @since 14
     */
    @Override
    public R visitBindingPattern(BindingPatternTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     * @since 17
     */
    @Override
    @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
    public R visitDefaultCaseLabel(DefaultCaseLabelTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitArrayAccess(ArrayAccessTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitMemberSelect(MemberSelectTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     * @since 17
     */
    @Override
    @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
    public R visitParenthesizedPattern(ParenthesizedPatternTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     * @since 17
     */
    @Override
    @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
    public R visitGuardedPattern(GuardedPatternTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitMemberReference(MemberReferenceTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitIdentifier(IdentifierTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitLiteral(LiteralTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitPrimitiveType(PrimitiveTypeTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitArrayType(ArrayTypeTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitParameterizedType(ParameterizedTypeTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitUnionType(UnionTypeTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitIntersectionType(IntersectionTypeTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitTypeParameter(TypeParameterTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitWildcard(WildcardTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitModifiers(ModifiersTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitAnnotation(AnnotationTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitAnnotatedType(AnnotatedTypeTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitModule(ModuleTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitExports(ExportsTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitOpens(OpensTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitProvides(ProvidesTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitRequires(RequiresTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitUses(UsesTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitErroneous(ErroneousTree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitOther(Tree node, P p) {
        return defaultAction(node, p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation calls {@code defaultAction}.
     *
     * @param node {@inheritDoc}
     * @param p {@inheritDoc}
     * @return  the result of {@code defaultAction}
     */
    @Override
    public R visitYield(YieldTree node, P p) {
        return defaultAction(node, p);
    }
}
