/*
 * Copyright (c) 2005, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * Common interface for all nodes in an abstract syntax tree.
 *
 * <p><b>WARNING:</b> This interface and its sub-interfaces are
 * subject to change as the Java programming language evolves.
 * These interfaces are implemented by the JDK Java compiler (javac)
 * and should not be implemented either directly or indirectly by
 * other applications.
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 *
 * @since 1.6
 */
public interface Tree {

    /**
     * Enumerates all kinds of trees.
     */
    public enum Kind {
        /**
         * Used for instances of {@link AnnotatedTypeTree}
         * representing annotated types.
         */
        ANNOTATED_TYPE(AnnotatedTypeTree.class),

        /**
         * Used for instances of {@link AnnotationTree}
         * representing declaration annotations.
         */
        ANNOTATION(AnnotationTree.class),

        /**
         * Used for instances of {@link AnnotationTree}
         * representing type annotations.
         */
        TYPE_ANNOTATION(AnnotationTree.class),

        /**
         * Used for instances of {@link ArrayAccessTree}.
         */
        ARRAY_ACCESS(ArrayAccessTree.class),

        /**
         * Used for instances of {@link ArrayTypeTree}.
         */
        ARRAY_TYPE(ArrayTypeTree.class),

        /**
         * Used for instances of {@link AssertTree}.
         */
        ASSERT(AssertTree.class),

        /**
         * Used for instances of {@link AssignmentTree}.
         */
        ASSIGNMENT(AssignmentTree.class),

        /**
         * Used for instances of {@link BlockTree}.
         */
        BLOCK(BlockTree.class),

        /**
         * Used for instances of {@link BreakTree}.
         */
        BREAK(BreakTree.class),

        /**
         * Used for instances of {@link CaseTree}.
         */
        CASE(CaseTree.class),

        /**
         * Used for instances of {@link CatchTree}.
         */
        CATCH(CatchTree.class),

        /**
         * Used for instances of {@link ClassTree} representing classes.
         */
        CLASS(ClassTree.class),

        /**
         * Used for instances of {@link CompilationUnitTree}.
         */
        COMPILATION_UNIT(CompilationUnitTree.class),

        /**
         * Used for instances of {@link ConditionalExpressionTree}.
         */
        CONDITIONAL_EXPRESSION(ConditionalExpressionTree.class),

        /**
         * Used for instances of {@link ContinueTree}.
         */
        CONTINUE(ContinueTree.class),

        /**
         * Used for instances of {@link DoWhileLoopTree}.
         */
        DO_WHILE_LOOP(DoWhileLoopTree.class),

        /**
         * Used for instances of {@link EnhancedForLoopTree}.
         */
        ENHANCED_FOR_LOOP(EnhancedForLoopTree.class),

        /**
         * Used for instances of {@link ExpressionStatementTree}.
         */
        EXPRESSION_STATEMENT(ExpressionStatementTree.class),

        /**
         * Used for instances of {@link MemberSelectTree}.
         */
        MEMBER_SELECT(MemberSelectTree.class),

        /**
         * Used for instances of {@link MemberReferenceTree}.
         */
        MEMBER_REFERENCE(MemberReferenceTree.class),

        /**
         * Used for instances of {@link ForLoopTree}.
         */
        FOR_LOOP(ForLoopTree.class),

        /**
         * Used for instances of {@link IdentifierTree}.
         */
        IDENTIFIER(IdentifierTree.class),

        /**
         * Used for instances of {@link IfTree}.
         */
        IF(IfTree.class),

        /**
         * Used for instances of {@link ImportTree}.
         */
        IMPORT(ImportTree.class),

        /**
         * Used for instances of {@link InstanceOfTree}.
         */
        INSTANCE_OF(InstanceOfTree.class),

        /**
         * Used for instances of {@link LabeledStatementTree}.
         */
        LABELED_STATEMENT(LabeledStatementTree.class),

        /**
         * Used for instances of {@link MethodTree}.
         */
        METHOD(MethodTree.class),

        /**
         * Used for instances of {@link MethodInvocationTree}.
         */
        METHOD_INVOCATION(MethodInvocationTree.class),

        /**
         * Used for instances of {@link ModifiersTree}.
         */
        MODIFIERS(ModifiersTree.class),

        /**
         * Used for instances of {@link NewArrayTree}.
         */
        NEW_ARRAY(NewArrayTree.class),

        /**
         * Used for instances of {@link NewClassTree}.
         */
        NEW_CLASS(NewClassTree.class),

        /**
         * Used for instances of {@link LambdaExpressionTree}.
         */
        LAMBDA_EXPRESSION(LambdaExpressionTree.class),

        /**
         * Used for instances of {@link PackageTree}.
         * @since 9
         */
        PACKAGE(PackageTree.class),

        /**
         * Used for instances of {@link ParenthesizedTree}.
         */
        PARENTHESIZED(ParenthesizedTree.class),

        /**
         * Used for instances of {@link BindingPatternTree}.
         *
         * @since 16
         */
        BINDING_PATTERN(BindingPatternTree.class),

        /**
         * Used for instances of {@link GuardedPatternTree}.
         *
         * @since 17
         */
        @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
        GUARDED_PATTERN(GuardedPatternTree.class),

        /**
         * Used for instances of {@link ParenthesizedPatternTree}.
         *
         * @since 17
         */
        @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
        PARENTHESIZED_PATTERN(ParenthesizedPatternTree.class),

        /**
         * Used for instances of {@link DefaultCaseLabelTree}.
         *
         * @since 17
         */
        @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
        DEFAULT_CASE_LABEL(DefaultCaseLabelTree.class),

        /**
         * Used for instances of {@link PrimitiveTypeTree}.
         */
        PRIMITIVE_TYPE(PrimitiveTypeTree.class),

        /**
         * Used for instances of {@link ReturnTree}.
         */
        RETURN(ReturnTree.class),

        /**
         * Used for instances of {@link EmptyStatementTree}.
         */
        EMPTY_STATEMENT(EmptyStatementTree.class),

        /**
         * Used for instances of {@link SwitchTree}.
         */
        SWITCH(SwitchTree.class),

        /**
         * Used for instances of {@link SwitchExpressionTree}.
         *
         * @since 12
         */
        SWITCH_EXPRESSION(SwitchExpressionTree.class),

        /**
         * Used for instances of {@link SynchronizedTree}.
         */
        SYNCHRONIZED(SynchronizedTree.class),

        /**
         * Used for instances of {@link ThrowTree}.
         */
        THROW(ThrowTree.class),

        /**
         * Used for instances of {@link TryTree}.
         */
        TRY(TryTree.class),

        /**
         * Used for instances of {@link ParameterizedTypeTree}.
         */
        PARAMETERIZED_TYPE(ParameterizedTypeTree.class),

        /**
         * Used for instances of {@link UnionTypeTree}.
         */
        UNION_TYPE(UnionTypeTree.class),

        /**
         * Used for instances of {@link IntersectionTypeTree}.
         */
        INTERSECTION_TYPE(IntersectionTypeTree.class),

        /**
         * Used for instances of {@link TypeCastTree}.
         */
        TYPE_CAST(TypeCastTree.class),

        /**
         * Used for instances of {@link TypeParameterTree}.
         */
        TYPE_PARAMETER(TypeParameterTree.class),

        /**
         * Used for instances of {@link VariableTree}.
         */
        VARIABLE(VariableTree.class),

        /**
         * Used for instances of {@link WhileLoopTree}.
         */
        WHILE_LOOP(WhileLoopTree.class),

        /**
         * Used for instances of {@link UnaryTree} representing postfix
         * increment operator {@code ++}.
         */
        POSTFIX_INCREMENT(UnaryTree.class),

        /**
         * Used for instances of {@link UnaryTree} representing postfix
         * decrement operator {@code --}.
         */
        POSTFIX_DECREMENT(UnaryTree.class),

        /**
         * Used for instances of {@link UnaryTree} representing prefix
         * increment operator {@code ++}.
         */
        PREFIX_INCREMENT(UnaryTree.class),

        /**
         * Used for instances of {@link UnaryTree} representing prefix
         * decrement operator {@code --}.
         */
        PREFIX_DECREMENT(UnaryTree.class),

        /**
         * Used for instances of {@link UnaryTree} representing unary plus
         * operator {@code +}.
         */
        UNARY_PLUS(UnaryTree.class),

        /**
         * Used for instances of {@link UnaryTree} representing unary minus
         * operator {@code -}.
         */
        UNARY_MINUS(UnaryTree.class),

        /**
         * Used for instances of {@link UnaryTree} representing bitwise
         * complement operator {@code ~}.
         */
        BITWISE_COMPLEMENT(UnaryTree.class),

        /**
         * Used for instances of {@link UnaryTree} representing logical
         * complement operator {@code !}.
         */
        LOGICAL_COMPLEMENT(UnaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * multiplication {@code *}.
         */
        MULTIPLY(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * division {@code /}.
         */
        DIVIDE(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * remainder {@code %}.
         */
        REMAINDER(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * addition or string concatenation {@code +}.
         */
        PLUS(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * subtraction {@code -}.
         */
        MINUS(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * left shift {@code <<}.
         */
        LEFT_SHIFT(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * right shift {@code >>}.
         */
        RIGHT_SHIFT(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * unsigned right shift {@code >>>}.
         */
        UNSIGNED_RIGHT_SHIFT(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * less-than {@code <}.
         */
        LESS_THAN(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * greater-than {@code >}.
         */
        GREATER_THAN(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * less-than-equal {@code <=}.
         */
        LESS_THAN_EQUAL(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * greater-than-equal {@code >=}.
         */
        GREATER_THAN_EQUAL(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * equal-to {@code ==}.
         */
        EQUAL_TO(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * not-equal-to {@code !=}.
         */
        NOT_EQUAL_TO(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * bitwise and logical "and" {@code &}.
         */
        AND(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * bitwise and logical "xor" {@code ^}.
         */
        XOR(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * bitwise and logical "or" {@code |}.
         */
        OR(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * conditional-and {@code &&}.
         */
        CONDITIONAL_AND(BinaryTree.class),

        /**
         * Used for instances of {@link BinaryTree} representing
         * conditional-or {@code ||}.
         */
        CONDITIONAL_OR(BinaryTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * multiplication assignment {@code *=}.
         */
        MULTIPLY_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * division assignment {@code /=}.
         */
        DIVIDE_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * remainder assignment {@code %=}.
         */
        REMAINDER_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * addition or string concatenation assignment {@code +=}.
         */
        PLUS_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * subtraction assignment {@code -=}.
         */
        MINUS_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * left shift assignment {@code <<=}.
         */
        LEFT_SHIFT_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * right shift assignment {@code >>=}.
         */
        RIGHT_SHIFT_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * unsigned right shift assignment {@code >>>=}.
         */
        UNSIGNED_RIGHT_SHIFT_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * bitwise and logical "and" assignment {@code &=}.
         */
        AND_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * bitwise and logical "xor" assignment {@code ^=}.
         */
        XOR_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link CompoundAssignmentTree} representing
         * bitwise and logical "or" assignment {@code |=}.
         */
        OR_ASSIGNMENT(CompoundAssignmentTree.class),

        /**
         * Used for instances of {@link LiteralTree} representing
         * an integral literal expression of type {@code int}.
         */
        INT_LITERAL(LiteralTree.class),

        /**
         * Used for instances of {@link LiteralTree} representing
         * an integral literal expression of type {@code long}.
         */
        LONG_LITERAL(LiteralTree.class),

        /**
         * Used for instances of {@link LiteralTree} representing
         * a floating-point literal expression of type {@code float}.
         */
        FLOAT_LITERAL(LiteralTree.class),

        /**
         * Used for instances of {@link LiteralTree} representing
         * a floating-point literal expression of type {@code double}.
         */
        DOUBLE_LITERAL(LiteralTree.class),

        /**
         * Used for instances of {@link LiteralTree} representing
         * a boolean literal expression of type {@code boolean}.
         */
        BOOLEAN_LITERAL(LiteralTree.class),

        /**
         * Used for instances of {@link LiteralTree} representing
         * a character literal expression of type {@code char}.
         */
        CHAR_LITERAL(LiteralTree.class),

        /**
         * Used for instances of {@link LiteralTree} representing
         * a string literal expression of type {@link String}.
         */
        STRING_LITERAL(LiteralTree.class),

        /**
         * Used for instances of {@link LiteralTree} representing
         * the use of {@code null}.
         */
        NULL_LITERAL(LiteralTree.class),

        /**
         * Used for instances of {@link WildcardTree} representing
         * an unbounded wildcard type argument.
         */
        UNBOUNDED_WILDCARD(WildcardTree.class),

        /**
         * Used for instances of {@link WildcardTree} representing
         * an extends bounded wildcard type argument.
         */
        EXTENDS_WILDCARD(WildcardTree.class),

        /**
         * Used for instances of {@link WildcardTree} representing
         * a super bounded wildcard type argument.
         */
        SUPER_WILDCARD(WildcardTree.class),

        /**
         * Used for instances of {@link ErroneousTree}.
         */
        ERRONEOUS(ErroneousTree.class),

        /**
         * Used for instances of {@link ClassTree} representing interfaces.
         */
        INTERFACE(ClassTree.class),

        /**
         * Used for instances of {@link ClassTree} representing enums.
         */
        ENUM(ClassTree.class),

        /**
         * Used for instances of {@link ClassTree} representing annotation types.
         */
        ANNOTATION_TYPE(ClassTree.class),

        /**
         * Used for instances of {@link ModuleTree} representing module declarations.
         */
        MODULE(ModuleTree.class),

        /**
         * Used for instances of {@link ExportsTree} representing
         * exports directives in a module declaration.
         */
        EXPORTS(ExportsTree.class),

        /**
         * Used for instances of {@link ExportsTree} representing
         * opens directives in a module declaration.
         */
        OPENS(OpensTree.class),

        /**
         * Used for instances of {@link ProvidesTree} representing
         * provides directives in a module declaration.
         */
        PROVIDES(ProvidesTree.class),

        /**
         * Used for instances of {@link ClassTree} representing records.
         * @since 16
         */
        RECORD(ClassTree.class),

        /**
         * Used for instances of {@link RequiresTree} representing
         * requires directives in a module declaration.
         */
        REQUIRES(RequiresTree.class),

        /**
         * Used for instances of {@link UsesTree} representing
         * uses directives in a module declaration.
         */
        USES(UsesTree.class),

        /**
         * An implementation-reserved node. This is the not the node
         * you are looking for.
         */
        OTHER(null),

        /**
         * Used for instances of {@link YieldTree}.
         *
         * @since 13
         */
        YIELD(YieldTree.class);


        Kind(Class<? extends Tree> intf) {
            associatedInterface = intf;
        }

        /**
         * Returns the associated interface type that uses this kind.
         * @return the associated interface
         */
        public Class<? extends Tree> asInterface() {
            return associatedInterface;
        }

        private final Class<? extends Tree> associatedInterface;
    }

    /**
     * Returns the kind of this tree.
     *
     * @return the kind of this tree
     */
    Kind getKind();

    /**
     * Accept method used to implement the visitor pattern.  The
     * visitor pattern is used to implement operations on trees.
     *
     * @param <R> the result type of this operation
     * @param <D> the type of additional data
     * @param visitor the visitor to be called
     * @param data a value to be passed to the visitor
     * @return the result returned from calling the visitor
     */
    <R,D> R accept(TreeVisitor<R,D> visitor, D data);
}
