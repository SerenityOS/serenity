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

/**
 * A tree node for a binary expression.
 * Use {@link #getKind getKind} to determine the kind of operator.
 *
 * For example:
 * <pre>
 *   <em>leftOperand</em> <em>operator</em> <em>rightOperand</em>
 * </pre>
 *
 * @jls 15.17 Multiplicative Operators
 * @jls 15.18 Additive Operators
 * @jls 15.19 Shift Operators
 * @jls 15.20 Relational Operators
 * @jls 15.21 Equality Operators
 * @jls 15.22 Bitwise and Logical Operators
 * @jls 15.23 Conditional-And Operator {@code &&}
 * @jls 15.24 Conditional-Or Operator {@code ||}
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface BinaryTree extends ExpressionTree {
    /**
     * Returns the left (first) operand of the expression.
     * @return the left operand
     */
    ExpressionTree getLeftOperand();

    /**
     * Returns the right (second) operand of the expression.
     * @return the right operand
     */
    ExpressionTree getRightOperand();
}
