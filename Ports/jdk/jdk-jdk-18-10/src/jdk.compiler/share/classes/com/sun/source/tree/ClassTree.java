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

package com.sun.source.tree;

import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Name;

/**
 * A tree node for a class, interface, enum, record, or annotation
 * type declaration.
 *
 * For example:
 * <pre>
 *   <em>modifiers</em> class <em>simpleName</em> <em>typeParameters</em>
 *       extends <em>extendsClause</em>
 *       implements <em>implementsClause</em>
 *   {
 *       <em>members</em>
 *   }
 * </pre>
 *
 * @jls 8.1 Class Declarations
 * @jls 8.9 Enum Types
 * @jls 8.10 Record Types
 * @jls 9.1 Interface Declarations
 * @jls 9.6 Annotation Types
 *
 * @author Peter von der Ah&eacute;
 * @author Jonathan Gibbons
 * @since 1.6
 */
public interface ClassTree extends StatementTree {
    /**
     * Returns the modifiers, including any annotations,
     * for this type declaration.
     * @return the modifiers
     */
    ModifiersTree getModifiers();

    /**
     * Returns the simple name of this type declaration.
     * @return the simple name
     */
    Name getSimpleName();

    /**
     * Returns any type parameters of this type declaration.
     * @return the type parameters
     */
    List<? extends TypeParameterTree> getTypeParameters();

    /**
     * Returns the supertype of this type declaration,
     * or {@code null} if none is provided.
     * @return the supertype
     */
    Tree getExtendsClause();

    /**
     * Returns the interfaces implemented by this type declaration.
     * @return the interfaces
     */
    List<? extends Tree> getImplementsClause();

    /**
     * Returns the subclasses permitted by this type declaration.
     *
     * @implSpec this implementation returns an empty list
     *
     * @return the subclasses
     *
     * @since 17
     */
    default List<? extends Tree> getPermitsClause() {
        return List.of();
    }

    /**
     * Returns the members declared in this type declaration.
     * @return the members
     */
    List<? extends Tree> getMembers();
}
