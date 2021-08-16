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

package javax.lang.model.element;

import java.util.List;
import javax.lang.model.type.*;

/**
 * Represents a method, constructor, or initializer (static or
 * instance) of a class or interface, including annotation type
 * elements.
 *
 * @author Joseph D. Darcy
 * @author Scott Seligman
 * @author Peter von der Ah&eacute;
 * @see ExecutableType
 * @since 1.6
 */
public interface ExecutableElement extends Element, Parameterizable {
    /**
     * {@return the {@linkplain ExecutableType executable type} defined
     * by this executable element}
     *
     * @see ExecutableType
     */
    @Override
    TypeMirror asType();

    /**
     * Returns the formal type parameters of this executable
     * in declaration order.
     *
     * @return the formal type parameters, or an empty list
     * if there are none
     */
    List<? extends TypeParameterElement> getTypeParameters();

    /**
     * {@return the return type of this executable}
     * Returns a {@link NoType} with kind {@link TypeKind#VOID VOID}
     * if this executable is not a method, or is a method that does not
     * return a value.
     */
    TypeMirror getReturnType();

    /**
     * Returns the formal parameters of this executable.
     * They are returned in declaration order.
     *
     * @return the formal parameters,
     * or an empty list if there are none
     */
    List<? extends VariableElement> getParameters();

    /**
     * Returns the receiver type of this executable,
     * or {@link javax.lang.model.type.NoType NoType} with
     * kind {@link javax.lang.model.type.TypeKind#NONE NONE}
     * if the executable has no receiver type.
     *
     * An executable which is an instance method, or a constructor of an
     * inner class, has a receiver type derived from the {@linkplain
     * #getEnclosingElement declaring type}.
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
     * {@return {@code true} if this method or constructor accepts a variable
     * number of arguments and returns {@code false} otherwise}
     */
    boolean isVarArgs();

    /**
     * {@return {@code true} if this method is a default method and
     * returns {@code false} otherwise}
     * @since 1.8
     */
    boolean isDefault();

    /**
     * Returns the exceptions and other throwables listed in this
     * method or constructor's {@code throws} clause in declaration
     * order.
     *
     * @return the exceptions and other throwables listed in the
     * {@code throws} clause, or an empty list if there are none
     */
    List<? extends TypeMirror> getThrownTypes();

    /**
     * Returns the default value if this executable is an annotation
     * interface element.  Returns {@code null} if this method is not
     * an annotation interface element, or if it is an annotation
     * interface element with no default value.
     *
     * @return the default value, or {@code null} if none
     */
    AnnotationValue getDefaultValue();

    /**
     * {@return the simple name of a constructor, method, or
     * initializer}  For a constructor, the name {@code "<init>"} is
     * returned, for a static initializer, the name {@code "<clinit>"}
     * is returned, and for an anonymous class or instance
     * initializer, an <a href=Name.html#empty_name>empty name</a> is
     * returned.
     */
    @Override
    Name getSimpleName();
}
