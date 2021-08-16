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

package javax.lang.model.type;


import java.util.List;

import javax.lang.model.element.ExecutableElement;

/**
 * Represents the type of an executable.  An <i>executable</i>
 * is a method, constructor, or initializer.
 *
 * <p> The executable is
 * represented as when viewed as a method (or constructor or
 * initializer) of some reference type.
 * If that reference type is parameterized, then its actual
 * type arguments are substituted into any types returned by the methods of
 * this interface.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see ExecutableElement
 * @since 1.6
 */
public interface ExecutableType extends TypeMirror {

    /**
     * Returns the type variables declared by the formal type parameters
     * of this executable.
     *
     * @return the type variables declared by the formal type parameters,
     *          or an empty list if there are none
     */
    List<? extends TypeVariable> getTypeVariables();

    /**
     * {@return the return type of this executable}
     * Returns a {@link NoType} with kind {@link TypeKind#VOID VOID}
     * if this executable is not a method, or is a method that does not
     * return a value.
     */
    TypeMirror getReturnType();

    /**
     * Returns the types of this executable's formal parameters.
     *
     * @return the types of this executable's formal parameters,
     *          or an empty list if there are none
     */
    List<? extends TypeMirror> getParameterTypes();

    /**
     * Returns the receiver type of this executable,
     * or {@link javax.lang.model.type.NoType NoType} with
     * kind {@link javax.lang.model.type.TypeKind#NONE NONE}
     * if the executable has no receiver type.
     *
     * An executable which is an instance method, or a constructor of an
     * inner class, has a receiver type derived from the {@linkplain
     * ExecutableElement#getEnclosingElement declaring type}.
     *
     * An executable which is a static method, or a constructor of a
     * non-inner class, or an initializer (static or instance), has no
     * receiver type.
     *
     * @return the receiver type of this executable
     * @since 1.8
     *
     * @jls 8.4 Method Declarations
     * @jls 8.4.1 Formal Parameters
     * @jls 8.8 Constructor Declarations
     */
    TypeMirror getReceiverType();

    /**
     * Returns the exceptions and other throwables listed in this
     * executable's {@code throws} clause.
     *
     * @return the exceptions and other throwables listed in this
     *          executable's {@code throws} clause,
     *          or an empty list if there are none.
     */
    List<? extends TypeMirror> getThrownTypes();
}
