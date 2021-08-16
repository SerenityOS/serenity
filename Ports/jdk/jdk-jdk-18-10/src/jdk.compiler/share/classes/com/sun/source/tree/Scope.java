/*
 * Copyright (c) 2006, 2014, Oracle and/or its affiliates. All rights reserved.
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

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Interface for determining locally available program elements, such as
 * local variables and imports.
 * Upon creation, a Scope is associated with a given program position;
 * for example, a {@linkplain Tree tree node}. This position may be used to
 * infer an enclosing method and/or class.
 *
 * <p>A Scope does not itself contain the details of the elements corresponding
 * to the parameters, methods and fields of the methods and classes containing
 * its position. However, these elements can be determined from the enclosing
 * elements.
 *
 * <p>Scopes may be contained in an enclosing scope. The outermost scope contains
 * those elements available via "star import" declarations; the scope within that
 * contains the top level elements of the compilation unit, including any named
 * imports.
 *
 * @since 1.6
 */
public interface Scope {
    /**
     * Returns the enclosing scope.
     * @return the enclosing scope
     */
    public Scope getEnclosingScope();

    /**
     * Returns the innermost type element containing the position of this scope.
     * @return the innermost enclosing type element
     */
    public TypeElement getEnclosingClass();

    /**
     * Returns the innermost executable element containing the position of this scope.
     * @return the innermost enclosing method declaration
     */
    public ExecutableElement getEnclosingMethod();

    /**
     * Returns the elements directly contained in this scope.
     * @return the elements contained in this scope
     */
    public Iterable<? extends Element> getLocalElements();
}
