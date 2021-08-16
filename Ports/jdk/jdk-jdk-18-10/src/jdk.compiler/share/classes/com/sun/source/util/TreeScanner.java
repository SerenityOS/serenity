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
 * A TreeVisitor that visits all the child tree nodes.
 * To visit nodes of a particular type, just override the
 * corresponding visitXYZ method.
 * Inside your method, call super.visitXYZ to visit descendant
 * nodes.
 *
 * <p>Here is an example to count the number of identifier nodes in a tree:
 * <pre>
 *   class CountIdentifiers extends TreeScanner&lt;Integer,Void&gt; {
 *      {@literal @}Override
 *      public Integer visitIdentifier(IdentifierTree node, Void p) {
 *          return 1;
 *      }
 *      {@literal @}Override
 *      public Integer reduce(Integer r1, Integer r2) {
 *          return (r1 == null ? 0 : r1) + (r2 == null ? 0 : r2);
 *      }
 *   }
 * </pre>
 *
 * @implSpec
 * <p>The default implementation of the visitXYZ methods will determine
 * a result as follows:
 * <ul>
 * <li>If the node being visited has no children, the result will be {@code null}.
 * <li>If the node being visited has one child, the result will be the
 * result of calling {@code scan} with that child. The child may be a simple node
 * or itself a list of nodes.
 * <li>If the node being visited has more than one child, the result will
 * be determined by calling {@code scan} with each child in turn, and then combining the
 * result of each scan after the first with the cumulative result
 * so far, as determined by the {@link #reduce} method. Each child may be either
 * a simple node or a list of nodes. The default behavior of the {@code reduce}
 * method is such that the result of the visitXYZ method will be the result of
 * the last child scanned.
 * </ul>
 *
 * @param <R> the return type of this visitor's methods.  Use {@link
 *            Void} for visitors that do not need to return results.
 * @param <P> the type of the additional parameter to this visitor's
 *            methods.  Use {@code Void} for visitors that do not need an
 *            additional parameter.
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public class TreeScanner<R,P> implements TreeVisitor<R,P> {
    /**
     * Constructs a {@code TreeScanner}.
     */
    public TreeScanner() {}

    /**
     * Scans a single node.
     * @param tree the node to be scanned
     * @param p a parameter value passed to the visit method
     * @return the result value from the visit method
     */
    public R scan(Tree tree, P p) {
        return (tree == null) ? null : tree.accept(this, p);
    }

    private R scanAndReduce(Tree node, P p, R r) {
        return reduce(scan(node, p), r);
    }

    /**
     * Scans a sequence of nodes.
     * @param nodes the nodes to be scanned
     * @param p a parameter value to be passed to the visit method for each node
     * @return the combined return value from the visit methods.
     *      The values are combined using the {@link #reduce reduce} method.
     */
    public R scan(Iterable<? extends Tree> nodes, P p) {
        R r = null;
        if (nodes != null) {
            boolean first = true;
            for (Tree node : nodes) {
                r = (first ? scan(node, p) : scanAndReduce(node, p, r));
                first = false;
            }
        }
        return r;
    }

    private R scanAndReduce(Iterable<? extends Tree> nodes, P p, R r) {
        return reduce(scan(nodes, p), r);
    }

    /**
     * Reduces two results into a combined result.
     * The default implementation is to return the first parameter.
     * The general contract of the method is that it may take any action whatsoever.
     * @param r1 the first of the values to be combined
     * @param r2 the second of the values to be combined
     * @return the result of combining the two parameters
     */
    public R reduce(R r1, R r2) {
        return r1;
    }


/* ***************************************************************************
 * Visitor methods
 ****************************************************************************/

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitCompilationUnit(CompilationUnitTree node, P p) {
        R r = scan(node.getPackage(), p);
        r = scanAndReduce(node.getImports(), p, r);
        r = scanAndReduce(node.getTypeDecls(), p, r);
        r = scanAndReduce(node.getModule(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitPackage(PackageTree node, P p) {
        R r = scan(node.getAnnotations(), p);
        r = scanAndReduce(node.getPackageName(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitImport(ImportTree node, P p) {
        return scan(node.getQualifiedIdentifier(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitClass(ClassTree node, P p) {
        R r = scan(node.getModifiers(), p);
        r = scanAndReduce(node.getTypeParameters(), p, r);
        r = scanAndReduce(node.getExtendsClause(), p, r);
        r = scanAndReduce(node.getImplementsClause(), p, r);
        r = scanAndReduce(node.getPermitsClause(), p, r);
        r = scanAndReduce(node.getMembers(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitMethod(MethodTree node, P p) {
        R r = scan(node.getModifiers(), p);
        r = scanAndReduce(node.getReturnType(), p, r);
        r = scanAndReduce(node.getTypeParameters(), p, r);
        r = scanAndReduce(node.getParameters(), p, r);
        r = scanAndReduce(node.getReceiverParameter(), p, r);
        r = scanAndReduce(node.getThrows(), p, r);
        r = scanAndReduce(node.getBody(), p, r);
        r = scanAndReduce(node.getDefaultValue(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitVariable(VariableTree node, P p) {
        R r = scan(node.getModifiers(), p);
        r = scanAndReduce(node.getType(), p, r);
        r = scanAndReduce(node.getNameExpression(), p, r);
        r = scanAndReduce(node.getInitializer(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitEmptyStatement(EmptyStatementTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitBlock(BlockTree node, P p) {
        return scan(node.getStatements(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitDoWhileLoop(DoWhileLoopTree node, P p) {
        R r = scan(node.getStatement(), p);
        r = scanAndReduce(node.getCondition(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitWhileLoop(WhileLoopTree node, P p) {
        R r = scan(node.getCondition(), p);
        r = scanAndReduce(node.getStatement(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitForLoop(ForLoopTree node, P p) {
        R r = scan(node.getInitializer(), p);
        r = scanAndReduce(node.getCondition(), p, r);
        r = scanAndReduce(node.getUpdate(), p, r);
        r = scanAndReduce(node.getStatement(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitEnhancedForLoop(EnhancedForLoopTree node, P p) {
        R r = scan(node.getVariable(), p);
        r = scanAndReduce(node.getExpression(), p, r);
        r = scanAndReduce(node.getStatement(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitLabeledStatement(LabeledStatementTree node, P p) {
        return scan(node.getStatement(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitSwitch(SwitchTree node, P p) {
        R r = scan(node.getExpression(), p);
        r = scanAndReduce(node.getCases(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitSwitchExpression(SwitchExpressionTree node, P p) {
        R r = scan(node.getExpression(), p);
        r = scanAndReduce(node.getCases(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitCase(CaseTree node, P p) {
        R r = scan(node.getExpressions(), p);
        if (node.getCaseKind() == CaseTree.CaseKind.RULE)
            r = scanAndReduce(node.getBody(), p, r);
        else
            r = scanAndReduce(node.getStatements(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitSynchronized(SynchronizedTree node, P p) {
        R r = scan(node.getExpression(), p);
        r = scanAndReduce(node.getBlock(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitTry(TryTree node, P p) {
        R r = scan(node.getResources(), p);
        r = scanAndReduce(node.getBlock(), p, r);
        r = scanAndReduce(node.getCatches(), p, r);
        r = scanAndReduce(node.getFinallyBlock(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitCatch(CatchTree node, P p) {
        R r = scan(node.getParameter(), p);
        r = scanAndReduce(node.getBlock(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitConditionalExpression(ConditionalExpressionTree node, P p) {
        R r = scan(node.getCondition(), p);
        r = scanAndReduce(node.getTrueExpression(), p, r);
        r = scanAndReduce(node.getFalseExpression(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitIf(IfTree node, P p) {
        R r = scan(node.getCondition(), p);
        r = scanAndReduce(node.getThenStatement(), p, r);
        r = scanAndReduce(node.getElseStatement(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitExpressionStatement(ExpressionStatementTree node, P p) {
        return scan(node.getExpression(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitBreak(BreakTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitContinue(ContinueTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitReturn(ReturnTree node, P p) {
        return scan(node.getExpression(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitThrow(ThrowTree node, P p) {
        return scan(node.getExpression(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitAssert(AssertTree node, P p) {
        R r = scan(node.getCondition(), p);
        r = scanAndReduce(node.getDetail(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitMethodInvocation(MethodInvocationTree node, P p) {
        R r = scan(node.getTypeArguments(), p);
        r = scanAndReduce(node.getMethodSelect(), p, r);
        r = scanAndReduce(node.getArguments(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitNewClass(NewClassTree node, P p) {
        R r = scan(node.getEnclosingExpression(), p);
        r = scanAndReduce(node.getIdentifier(), p, r);
        r = scanAndReduce(node.getTypeArguments(), p, r);
        r = scanAndReduce(node.getArguments(), p, r);
        r = scanAndReduce(node.getClassBody(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitNewArray(NewArrayTree node, P p) {
        R r = scan(node.getType(), p);
        r = scanAndReduce(node.getDimensions(), p, r);
        r = scanAndReduce(node.getInitializers(), p, r);
        r = scanAndReduce(node.getAnnotations(), p, r);
        for (Iterable< ? extends Tree> dimAnno : node.getDimAnnotations()) {
            r = scanAndReduce(dimAnno, p, r);
        }
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitLambdaExpression(LambdaExpressionTree node, P p) {
        R r = scan(node.getParameters(), p);
        r = scanAndReduce(node.getBody(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitParenthesized(ParenthesizedTree node, P p) {
        return scan(node.getExpression(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitAssignment(AssignmentTree node, P p) {
        R r = scan(node.getVariable(), p);
        r = scanAndReduce(node.getExpression(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitCompoundAssignment(CompoundAssignmentTree node, P p) {
        R r = scan(node.getVariable(), p);
        r = scanAndReduce(node.getExpression(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitUnary(UnaryTree node, P p) {
        return scan(node.getExpression(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitBinary(BinaryTree node, P p) {
        R r = scan(node.getLeftOperand(), p);
        r = scanAndReduce(node.getRightOperand(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitTypeCast(TypeCastTree node, P p) {
        R r = scan(node.getType(), p);
        r = scanAndReduce(node.getExpression(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitInstanceOf(InstanceOfTree node, P p) {
        R r = scan(node.getExpression(), p);
        if (node.getPattern() != null) {
            r = scanAndReduce(node.getPattern(), p, r);
        } else {
            r = scanAndReduce(node.getType(), p, r);
        }
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     * @since 14
     */
    @Override
    public R visitBindingPattern(BindingPatternTree node, P p) {
        return scan(node.getVariable(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     * @since 17
     */
    @Override
    @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
    public R visitDefaultCaseLabel(DefaultCaseLabelTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitArrayAccess(ArrayAccessTree node, P p) {
        R r = scan(node.getExpression(), p);
        r = scanAndReduce(node.getIndex(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitMemberSelect(MemberSelectTree node, P p) {
        return scan(node.getExpression(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     * @since 17
     */
    @Override
    @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
    public R visitParenthesizedPattern(ParenthesizedPatternTree node, P p) {
        return scan(node.getPattern(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     * @since 17
     */
    @Override
    @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
    public R visitGuardedPattern(GuardedPatternTree node, P p) {
        R r = scan(node.getPattern(), p);
        return scanAndReduce(node.getExpression(), p, r);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitMemberReference(MemberReferenceTree node, P p) {
        R r = scan(node.getQualifierExpression(), p);
        r = scanAndReduce(node.getTypeArguments(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitIdentifier(IdentifierTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitLiteral(LiteralTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitPrimitiveType(PrimitiveTypeTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitArrayType(ArrayTypeTree node, P p) {
        return scan(node.getType(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitParameterizedType(ParameterizedTypeTree node, P p) {
        R r = scan(node.getType(), p);
        r = scanAndReduce(node.getTypeArguments(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitUnionType(UnionTypeTree node, P p) {
        return scan(node.getTypeAlternatives(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitIntersectionType(IntersectionTypeTree node, P p) {
        return scan(node.getBounds(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitTypeParameter(TypeParameterTree node, P p) {
        R r = scan(node.getAnnotations(), p);
        r = scanAndReduce(node.getBounds(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitWildcard(WildcardTree node, P p) {
        return scan(node.getBound(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitModifiers(ModifiersTree node, P p) {
        return scan(node.getAnnotations(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitAnnotation(AnnotationTree node, P p) {
        R r = scan(node.getAnnotationType(), p);
        r = scanAndReduce(node.getArguments(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitAnnotatedType(AnnotatedTypeTree node, P p) {
        R r = scan(node.getAnnotations(), p);
        r = scanAndReduce(node.getUnderlyingType(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitModule(ModuleTree node, P p) {
        R r = scan(node.getAnnotations(), p);
        r = scanAndReduce(node.getName(), p, r);
        r = scanAndReduce(node.getDirectives(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitExports(ExportsTree node, P p) {
        R r = scan(node.getPackageName(), p);
        r = scanAndReduce(node.getModuleNames(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitOpens(OpensTree node, P p) {
        R r = scan(node.getPackageName(), p);
        r = scanAndReduce(node.getModuleNames(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitProvides(ProvidesTree node, P p) {
        R r = scan(node.getServiceName(), p);
        r = scanAndReduce(node.getImplementationNames(), p, r);
        return r;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitRequires(RequiresTree node, P p) {
        return scan(node.getModuleName(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitUses(UsesTree node, P p) {
        return scan(node.getServiceName(), p);
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitOther(Tree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation returns {@code null}.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitErroneous(ErroneousTree node, P p) {
        return null;
    }

    /**
     * {@inheritDoc}
     *
     * @implSpec This implementation scans the children in left to right order.
     *
     * @param node  {@inheritDoc}
     * @param p  {@inheritDoc}
     * @return the result of scanning
     */
    @Override
    public R visitYield(YieldTree node, P p) {
        return scan(node.getValue(), p);
    }
}
