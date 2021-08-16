/*
 * Copyright (c) 2003, 2019, Oracle and/or its affiliates. All rights reserved.
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

package pkg;

import java.util.*;
import java.util.function.Supplier;

/**
 * Just a sample class with type parameters.  This is a link to myself:
 * {@link TypeParameters}
 *
 * @param <E> the type parameter for this class.
 * @param <BadClassTypeParam> this should cause a warning.
 * @see TypeParameters
 */

public class TypeParameters<E> implements SubInterface<E> {

    /**
     * This method uses the type parameter of this class.
     * @param param an object that is of type E.
     * @return the parameter itself.
     */
    public E methodThatUsesTypeParameter(E param) {
        return param;
    }

    /**
     * This method has type parameters.  The list of type parameters is long
     * so there should be a line break in the member summary table.
     *
     * @param <T> This is the first type parameter.
     * @param <V> This is the second type parameter.
     * @param <BadMethodTypeParam> this should cause a warning.
     * @param param1 just a parameter.
     * @param param2 just another parameter.
     *
     */
    public <T extends List, V> String[] methodThatHasTypeParameters(T param1,
        V param2) { return null;}

    /**
     * This method has type parameters.  The list of type parameters is short
     * so there should not be a line break in the member summary table.
     *
     * @param <A> This is the first type parameter.
     */
    public <A> void methodThatHasTypeParmaters(A... a) {}

    /**
     * This method returns a TypeParameter array and takes in a TypeParameter array
     * @param e an array of TypeParameters
     * @return an array of TypeParameters
     */
    public E[] methodThatReturnsTypeParameterA(E[] e) { return null;}

    /**
     * Returns TypeParameters
     * @param <T> a typeParameters
     * @param coll a collection
     * @return typeParameters
     */
    public <T extends Object & Comparable<? super T>> T
        methodtThatReturnsTypeParametersB(Collection<? extends T> coll) {
        return null;
    }

    /**
     * Return the contained value, if present, otherwise throw an exception
     * to be created by the provided supplier.
     *
     * @param <X> Type of the exception to be thrown
     * @param exceptionSupplier The supplier which will return the exception to
     * be thrown
     * @return the present value
     * @throws X if there is no value present
     * @throws NullPointerException if no value is present and
     * {@code exceptionSupplier} is null
     */
    public <X extends Throwable> E orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        return null;
    }}
