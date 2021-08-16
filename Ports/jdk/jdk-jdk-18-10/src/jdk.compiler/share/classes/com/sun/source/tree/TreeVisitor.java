/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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

package com.sun.source.tree;

import jdk.internal.javac.PreviewFeature;

/**
 * A visitor of trees, in the style of the visitor design pattern.
 * Classes implementing this interface are used to operate
 * on a tree when the kind of tree is unknown at compile time.
 * When a visitor is passed to an tree's {@link Tree#accept
 * accept} method, the <code>visit<i>Xyz</i></code> method most applicable
 * to that tree is invoked.
 *
 * <p> Classes implementing this interface may or may not throw a
 * {@code NullPointerException} if the additional parameter {@code p}
 * is {@code null}; see documentation of the implementing class for
 * details.
 *
 * <p> <b>WARNING:</b> It is possible that methods will be added to
 * this interface to accommodate new, currently unknown, language
 * structures added to future versions of the Java programming
 * language.  Therefore, visitor classes directly implementing this
 * interface may be source incompatible with future versions of the
 * platform.
 *
 * @param <R> the return type of this visitor's methods.  Use {@link
 *            Void} for visitors that do not need to return results.
 * @param <P> the type of the additional parameter to this visitor's
 *            methods.  Use {@code Void} for visitors that do not need an
 *            additional parameter.
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 *
 * @since 1.6
 */
public interface TreeVisitor<R,P> {
    /**
     * Visits an AnnotatedTypeTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitAnnotatedType(AnnotatedTypeTree node, P p);

    /**
     * Visits an AnnotatedTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitAnnotation(AnnotationTree node, P p);

    /**
     * Visits a MethodInvocationTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitMethodInvocation(MethodInvocationTree node, P p);

    /**
     * Visits an AssertTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitAssert(AssertTree node, P p);

    /**
     * Visits an AssignmentTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitAssignment(AssignmentTree node, P p);

    /**
     * Visits a CompoundAssignmentTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitCompoundAssignment(CompoundAssignmentTree node, P p);

    /**
     * Visits a BinaryTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitBinary(BinaryTree node, P p);

    /**
     * Visits a BlockTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitBlock(BlockTree node, P p);

    /**
     * Visits a BreakTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitBreak(BreakTree node, P p);

    /**
     * Visits a CaseTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitCase(CaseTree node, P p);

    /**
     * Visits a CatchTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitCatch(CatchTree node, P p);

    /**
     * Visits a ClassTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitClass(ClassTree node, P p);

    /**
     * Visits a ConditionalExpressionTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitConditionalExpression(ConditionalExpressionTree node, P p);

    /**
     * Visits a ContinueTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitContinue(ContinueTree node, P p);

    /**
     * Visits a DoWhileTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitDoWhileLoop(DoWhileLoopTree node, P p);

    /**
     * Visits an ErroneousTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitErroneous(ErroneousTree node, P p);

    /**
     * Visits an ExpressionStatementTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitExpressionStatement(ExpressionStatementTree node, P p);

    /**
     * Visits an EnhancedForLoopTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitEnhancedForLoop(EnhancedForLoopTree node, P p);

    /**
     * Visits a ForLoopTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitForLoop(ForLoopTree node, P p);

    /**
     * Visits an IdentifierTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitIdentifier(IdentifierTree node, P p);

    /**
     * Visits an IfTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitIf(IfTree node, P p);

    /**
     * Visits an ImportTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitImport(ImportTree node, P p);

    /**
     * Visits an ArrayAccessTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitArrayAccess(ArrayAccessTree node, P p);

    /**
     * Visits a LabeledStatementTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitLabeledStatement(LabeledStatementTree node, P p);

    /**
     * Visits a LiteralTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitLiteral(LiteralTree node, P p);

    /**
     * Visits an BindingPattern node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     * @since 16
     */
    R visitBindingPattern(BindingPatternTree node, P p);

    /**
     * Visits a DefaultCaseLabelTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     * @since 17
     */
    @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
    R visitDefaultCaseLabel(DefaultCaseLabelTree node, P p);

    /**
     * Visits a MethodTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitMethod(MethodTree node, P p);

    /**
     * Visits a ModifiersTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitModifiers(ModifiersTree node, P p);

    /**
     * Visits a NewArrayTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitNewArray(NewArrayTree node, P p);

    /**
     * Visits a GuardPatternTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     * @since 17
     */
    @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
    R visitGuardedPattern(GuardedPatternTree node, P p);

    /**
     * Visits a ParenthesizedPatternTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     * @since 17
     */
    @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
    R visitParenthesizedPattern(ParenthesizedPatternTree node, P p);

    /**
     * Visits a NewClassTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitNewClass(NewClassTree node, P p);

    /**
     * Visits a LambdaExpressionTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitLambdaExpression(LambdaExpressionTree node, P p);

    /**
     * Visits a PackageTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitPackage(PackageTree node, P p);

    /**
     * Visits a ParenthesizedTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitParenthesized(ParenthesizedTree node, P p);

    /**
     * Visits a ReturnTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitReturn(ReturnTree node, P p);

    /**
     * Visits a MemberSelectTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitMemberSelect(MemberSelectTree node, P p);

    /**
     * Visits a MemberReferenceTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitMemberReference(MemberReferenceTree node, P p);

    /**
     * Visits an EmptyStatementTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitEmptyStatement(EmptyStatementTree node, P p);

    /**
     * Visits a SwitchTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitSwitch(SwitchTree node, P p);

    /**
     * Visits a SwitchExpressionTree node.
     *
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     * @since 12
     */
    R visitSwitchExpression(SwitchExpressionTree node, P p);

    /**
     * Visits a SynchronizedTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitSynchronized(SynchronizedTree node, P p);

    /**
     * Visits a ThrowTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitThrow(ThrowTree node, P p);

    /**
     * Visits a CompilationUnitTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitCompilationUnit(CompilationUnitTree node, P p);

    /**
     * Visits a TryTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitTry(TryTree node, P p);

    /**
     * Visits a ParameterizedTypeTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitParameterizedType(ParameterizedTypeTree node, P p);

    /**
     * Visits a UnionTypeTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitUnionType(UnionTypeTree node, P p);

    /**
     * Visits an IntersectionTypeTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitIntersectionType(IntersectionTypeTree node, P p);

    /**
     * Visits an ArrayTypeTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitArrayType(ArrayTypeTree node, P p);

    /**
     * Visits a TypeCastTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitTypeCast(TypeCastTree node, P p);

    /**
     * Visits a PrimitiveTypeTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitPrimitiveType(PrimitiveTypeTree node, P p);

    /**
     * Visits a TypeParameterTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitTypeParameter(TypeParameterTree node, P p);

    /**
     * Visits an InstanceOfTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitInstanceOf(InstanceOfTree node, P p);

    /**
     * Visits a UnaryTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitUnary(UnaryTree node, P p);

    /**
     * Visits a VariableTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitVariable(VariableTree node, P p);

    /**
     * Visits a WhileLoopTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitWhileLoop(WhileLoopTree node, P p);

    /**
     * Visits a WildcardTypeTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitWildcard(WildcardTree node, P p);

    /**
     * Visits a ModuleTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitModule(ModuleTree node, P p);

    /**
     * Visits an ExportsTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitExports(ExportsTree node, P p);

    /**
     * Visits an OpensTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitOpens(OpensTree node, P p);

    /**
     * Visits a ProvidesTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitProvides(ProvidesTree node, P p);

    /**
     * Visits a RequiresTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitRequires(RequiresTree node, P p);

    /**
     * Visits a UsesTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitUses(UsesTree node, P p);

    /**
     * Visits an unknown type of Tree node.
     * This can occur if the language evolves and new kinds
     * of nodes are added to the {@code Tree} hierarchy.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     */
    R visitOther(Tree node, P p);

    /**
     * Visits a YieldTree node.
     * @param node the node being visited
     * @param p a parameter value
     * @return a result value
     * @since 13
     */
    R visitYield(YieldTree node, P p);
}
