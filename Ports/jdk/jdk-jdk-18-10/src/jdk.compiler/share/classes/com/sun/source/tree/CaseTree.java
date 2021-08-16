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

import java.util.List;

import jdk.internal.javac.PreviewFeature;

/**
 * A tree node for a {@code case} in a {@code switch} statement or expression.
 *
 * For example:
 * <pre>
 *   case <em>expression</em> :
 *       <em>statements</em>
 *
 *   default :
 *       <em>statements</em>
 * </pre>
 *
 * @jls 14.11 The switch Statement
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface CaseTree extends Tree {
    /**
     * Returns the expression for the case, or
     * {@code null} if this is the default case.
     * If this case has multiple labels, returns the first label.
     * @return the expression for the case, or null
     * @deprecated Please use {@link #getExpressions()}.
     */
    @Deprecated
    ExpressionTree getExpression();

    /**
     * Returns the labels for this case.
     * For default case, returns an empty list.
     *
     * @return labels for this case
     * @since 12
     */
    List<? extends ExpressionTree> getExpressions();

    /**
     * Returns the labels for this case.
     * For {@code default} case return a list with a single element, {@link DefaultCaseLabelTree}.
     *
     * @return labels for this case
     * @since 17
     */
    @PreviewFeature(feature=PreviewFeature.Feature.SWITCH_PATTERN_MATCHING, reflective=true)
    List<? extends CaseLabelTree> getLabels();

    /**
     * For case with kind {@linkplain CaseKind#STATEMENT},
     * returns the statements labeled by the case.
     * Returns {@code null} for case with kind
     * {@linkplain CaseKind#RULE}.
     * @return the statements labeled by the case or null
     */
    List<? extends StatementTree> getStatements();

    /**
     * For case with kind {@linkplain CaseKind#RULE},
     * returns the statement or expression after the arrow.
     * Returns {@code null} for case with kind
     * {@linkplain CaseKind#STATEMENT}.
     *
     * @return case value or null
     * @since 12
     */
    public default Tree getBody() {
        return null;
    }

    /**
     * Returns the kind of this case.
     *
     * @return the kind of this case
     * @since 12
     */
    public default CaseKind getCaseKind() {
        return CaseKind.STATEMENT;
    }

    /**
     * The syntactic form of this case:
     * <ul>
     *     <li>STATEMENT: {@code case <expression>: <statements>}</li>
     *     <li>RULE: {@code case <expression> -> <expression>/<statement>}</li>
     * </ul>
     *
     * @since 12
     */
    public enum CaseKind {
        /**
         * Case is in the form: {@code case <expression>: <statements>}.
         */
        STATEMENT,
        /**
         * Case is in the form: {@code case <expression> -> <expression>}.
         */
        RULE;
    }
}
