/*
 * Copyright (c) 2011, 2017, Oracle and/or its affiliates. All rights reserved.
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

/**
 * A tree node for a lambda expression.
 *
 * For example:
 * <pre>{@code
 *   ()->{}
 *   (List<String> ls)->ls.size()
 *   (x,y)-> { return x + y; }
 * }</pre>
 */
public interface LambdaExpressionTree extends ExpressionTree {

    /**
     * Lambda expressions come in two forms:
     * <ul>
     * <li> expression lambdas, whose body is an expression, and
     * <li> statement lambdas, whose body is a block
     * </ul>
     */
    public enum BodyKind {
        /** enum constant for expression lambdas */
        EXPRESSION,
        /** enum constant for statement lambdas */
        STATEMENT
    }

    /**
     * Returns the parameters of this lambda expression.
     * @return the parameters
     */
    List<? extends VariableTree> getParameters();

    /**
     * Returns the body of the lambda expression.
     * @return the body
     */
    Tree getBody();

    /**
     * Returns the kind of the body of the lambda expression.
     * @return the kind of the body
     */
    BodyKind getBodyKind();
}
