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
import javax.lang.model.element.Name;

/**
 * A tree node for a method or annotation type element declaration.
 *
 * For example:
 * <pre>
 *   <em>modifiers</em> <em>typeParameters</em> <em>type</em> <em>name</em>
 *      ( <em>parameters</em> )
 *      <em>body</em>
 *
 *   <em>modifiers</em> <em>type</em> <em>name</em> () default <em>defaultValue</em>
 * </pre>
 *
 * @jls 8.4 Method Declarations
 * @jls 8.6 Instance Initializers
 * @jls 8.7 Static Initializers
 * @jls 9.4 Method Declarations
 * @jls 9.6.1 Annotation Type Elements
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface MethodTree extends Tree {
    /**
     * Returns the modifiers, including any annotations for the method being declared.
     * @return the modifiers
     */
    ModifiersTree getModifiers();

    /**
     * Returns the name of the method being declared.
     * @return the name
     */
    Name getName();

    /**
     * Returns the return type of the method being declared.
     * Returns {@code null} for a constructor.
     * @return the return type
     */
    Tree getReturnType();

    /**
     * Returns the type parameters of the method being declared.
     * @return the type parameters
     */
    List<? extends TypeParameterTree> getTypeParameters();

    /**
     * Returns the parameters of the method being declared.
     * @return the parameters
     */
    List<? extends VariableTree> getParameters();

    /**
     * Return an explicit receiver parameter ("this" parameter),
     * or {@code null} if none.
     *
     * @return an explicit receiver parameter ("this" parameter)
     * @since 1.8
     */
    VariableTree getReceiverParameter();

    /**
     * Returns the exceptions listed as being thrown by this method.
     * @return the exceptions
     */
    List<? extends ExpressionTree> getThrows();

    /**
     * Returns the method body, or {@code null} if this is an abstract or native method.
     * @return the method body
     */
    BlockTree getBody();

    /**
     * Returns the default value, if this is an element within
     * an annotation type declaration.
     * Returns {@code null} otherwise.
     * @return the default value
     */
    Tree getDefaultValue(); // for annotation types
}
