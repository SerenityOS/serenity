/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
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

package jdk.experimental.bytecode;

import java.util.Iterator;

/**
 * Helper to create and manipulate type descriptors of T.
 *
 * @param <S> the type of symbols
 * @param <T> the type of type descriptors
 */
public interface TypeHelper<S, T> {
    /**
     * Return the type descriptor of an element given the type
     * descriptor of an array.
     *
     * @param t the type descriptor of the array
     * @return the element type
     */
    T elemtype(T t);

    /**
     * Return the type descriptor of an array given the type descriptor
     * of an element.
     *
     * @param t the type descriptor of the element
     * @return the type descriptor of the array
     */
    T arrayOf(T t);

    /**
     * Return an iterator over the type descriptors of the parameters of a
     * method.
       *
     * @param t the method type descriptor
     * @return an iterator over the type descriptors of the parameters
     */
    Iterator<T> parameterTypes(T t);

    /**
     * Return the type descriptor of a {@code TypeTag}.
     *
     * @param tag the {@code TypeTag} of a primitive type
     * @return the type descriptor of the primitive type
     */
    T fromTag(TypeTag tag);

    /**
     * Return the return type descriptor of a method.
     *
     * @param t the method type descriptor
     * @return the return type descriptor
     */
    T returnType(T t);

    /**
     * Return the type descriptor for a symbol.
     *
     * @param s the symbol
     * @return the type descriptor
     */
    T type(S s);

    /**
     * Return the symbol corresponding to a type descriptor.
     *
     * @param type the type descriptor
     * @return the symbol
     */
    S symbol(T type);

    /**
     * Return the {@code TypeTag} corresponding to a type descriptor.  Reference
     * types return {@code TypeTag.A}.
     *
     * @param t a type descriptor
     * @return the corresponding {@code TypeTag}
     */
    TypeTag tag(T t);

    /**
     * Return the symbol corresponding to a JVM type descriptor string.
     *
     * @param s a JVM type descriptor string
     * @return the corresponding symbol
     */
    S symbolFrom(String s);

    /**
     * Return the common supertype descriptor of two type descriptors.
     *
     * @param t1 a type descriptor
     * @param t2 a type descriptor
     * @return the common supertype descriptor
     */
    T commonSupertype(T t1, T t2);

    /**
     * Return the type descriptor for the null type.
     *
     * @return the type descriptor for the null type
     */
    T nullType();
}
