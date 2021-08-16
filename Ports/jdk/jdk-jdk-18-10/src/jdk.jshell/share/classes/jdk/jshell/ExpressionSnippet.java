/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jshell;

import jdk.jshell.Key.ExpressionKey;

/**
 * Snippet for an assignment or variable-value expression.
 * The Kind is {@link jdk.jshell.Snippet.Kind#EXPRESSION}.
 * <p>
 * <code>ExpressionSnippet</code> is immutable: an access to
 * any of its methods will always return the same result.
 * and thus is thread-safe.
 *
 * @since 9
 * @jls 15 Expressions
 */
public class ExpressionSnippet extends Snippet {

    ExpressionSnippet(ExpressionKey key, String userSource, Wrap guts, String name, SubKind subkind) {
        super(key, userSource, guts, name, subkind, null);
    }

    /**
     * Variable name which is the value of the expression. Since the expression
     * is either just a variable identifier or it is an assignment
     * to a variable, there is always a variable which is the subject of the
     * expression. All other forms of expression become temporary variables
     * which are instead referenced by a {@link VarSnippet}.
     * @return the name of the variable which is the subject of the expression.
     */
    @Override
    public String name() {
        return key().name();
    }

    /**
     * Type of the expression
     * @return String representation of the type of the expression.
     */
    public String typeName() {
        return key().typeName();
    }

    /**** internal access ****/

    @Override
    ExpressionKey key() {
        return (ExpressionKey) super.key();
    }
}
