/*
 * Copyright (c) 2005, 2014, Oracle and/or its affiliates. All rights reserved.
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
 * A tree node to declare a new instance of a class.
 *
 * For example:
 * <pre>
 *   new <em>identifier</em> ( )
 *
 *   new <em>identifier</em> ( <em>arguments</em> )
 *
 *   new <em>typeArguments</em> <em>identifier</em> ( <em>arguments</em> )
 *       <em>classBody</em>
 *
 *   <em>enclosingExpression</em>.new <em>identifier</em> ( <em>arguments</em> )
 * </pre>
 *
 * @jls 15.9 Class Instance Creation Expressions
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface NewClassTree extends ExpressionTree {
    /**
     * Returns the enclosing expression, or {@code null} if none.
     * @return the enclosing expression
     */
    ExpressionTree getEnclosingExpression();

    /**
     * Returns the type arguments for the object being created.
     * @return the type arguments
     */
    List<? extends Tree> getTypeArguments();

    /**
     * Returns the name of the class being instantiated.
     * @return the name
     */
    ExpressionTree getIdentifier();

    /**
     * Returns the arguments for the constructor to be invoked.
     * @return the arguments
     */
    List<? extends ExpressionTree> getArguments();

    /**
     * Returns the class body if an anonymous class is being
     * instantiated, and {@code null} otherwise.
     * @return the class body
     */
    ClassTree getClassBody();
}
