/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package java.lang.constant;

import java.lang.invoke.MethodType;
import java.lang.invoke.TypeDescriptor;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * {@linkplain MethodType} constant.
 *
 * @since 12
 */
public sealed interface MethodTypeDesc
        extends ConstantDesc,
                TypeDescriptor.OfMethod<ClassDesc, MethodTypeDesc>
        permits MethodTypeDescImpl {
    /**
     * Creates a {@linkplain MethodTypeDesc} given a method descriptor string.
     *
     * @param descriptor a method descriptor string
     * @return a {@linkplain MethodTypeDesc} describing the desired method type
     * @throws NullPointerException if the argument is {@code null}
     * @throws IllegalArgumentException if the descriptor string is not a valid
     * method descriptor
     * @jvms 4.3.3 Method Descriptors
     */
    static MethodTypeDesc ofDescriptor(String descriptor) {
        return MethodTypeDescImpl.ofDescriptor(descriptor);
    }

    /**
     * Returns a {@linkplain MethodTypeDesc} given the return type and parameter
     * types.
     *
     * @param returnDesc a {@linkplain ClassDesc} describing the return type
     * @param paramDescs {@linkplain ClassDesc}s describing the argument types
     * @return a {@linkplain MethodTypeDesc} describing the desired method type
     * @throws NullPointerException if any argument or its contents are {@code null}
     * @throws IllegalArgumentException if any element of {@code paramDescs} is a
     * {@link ClassDesc} for {@code void}
     */
    static MethodTypeDesc of(ClassDesc returnDesc, ClassDesc... paramDescs) {
        return new MethodTypeDescImpl(returnDesc, paramDescs);
    }

    /**
     * Gets the return type of the method type described by this {@linkplain MethodTypeDesc}.
     *
     * @return a {@link ClassDesc} describing the return type of the method type
     */
    ClassDesc returnType();

    /**
     * Returns the number of parameters of the method type described by
     * this {@linkplain MethodTypeDesc}.
     * @return the number of parameters
     */
    int parameterCount();

    /**
     * Returns the parameter type of the {@code index}'th parameter of the method type
     * described by this {@linkplain MethodTypeDesc}.
     *
     * @param index the index of the parameter to retrieve
     * @return a {@link ClassDesc} describing the desired parameter type
     * @throws IndexOutOfBoundsException if the index is outside the half-open
     * range {[0, parameterCount())}
     */
    ClassDesc parameterType(int index);

    /**
     * Returns the parameter types as an immutable {@link List}.
     *
     * @return a {@link List} of {@link ClassDesc} describing the parameter types
     */
    List<ClassDesc> parameterList();

    /**
     * Returns the parameter types as an array.
     *
     * @return an array of {@link ClassDesc} describing the parameter types
     */
    ClassDesc[] parameterArray();

    /**
     * Returns a {@linkplain MethodTypeDesc} that is identical to
     * this one, except with the specified return type.
     *
     * @param returnType a {@link ClassDesc} describing the new return type
     * @return a {@linkplain MethodTypeDesc} describing the desired method type
     * @throws NullPointerException if the argument is {@code null}
     */
    MethodTypeDesc changeReturnType(ClassDesc returnType);

    /**
     * Returns a {@linkplain MethodTypeDesc} that is identical to this one,
     * except that a single parameter type has been changed to the specified type.
     *
     * @param index the index of the parameter to change
     * @param paramType a {@link ClassDesc} describing the new parameter type
     * @return a {@linkplain MethodTypeDesc} describing the desired method type
     * @throws NullPointerException if any argument is {@code null}
     * @throws IndexOutOfBoundsException if the index is outside the half-open
     * range {[0, parameterCount)}
     */
    MethodTypeDesc changeParameterType(int index, ClassDesc paramType);

    /**
     * Returns a {@linkplain MethodTypeDesc} that is identical to this one,
     * except that a range of parameter types have been removed.
     *
     * @param start the index of the first parameter to remove
     * @param end the index after the last parameter to remove
     * @return a {@linkplain MethodTypeDesc} describing the desired method type
     * @throws IndexOutOfBoundsException if {@code start} is outside the half-open
     * range {@code [0, parameterCount)}, or {@code end} is outside the closed range
     * {@code [0, parameterCount]}, or if {@code start > end}
     */
    MethodTypeDesc dropParameterTypes(int start, int end);

    /**
     * Returns a {@linkplain MethodTypeDesc} that is identical to this one,
     * except that a range of additional parameter types have been inserted.
     *
     * @param pos the index at which to insert the first inserted parameter
     * @param paramTypes {@link ClassDesc}s describing the new parameter types
     *                   to insert
     * @return a {@linkplain MethodTypeDesc} describing the desired method type
     * @throws NullPointerException if any argument or its contents are {@code null}
     * @throws IndexOutOfBoundsException if {@code pos} is outside the closed
     * range {[0, parameterCount]}
     * @throws IllegalArgumentException if any element of {@code paramTypes}
     * is a {@link ClassDesc} for {@code void}
     */
    MethodTypeDesc insertParameterTypes(int pos, ClassDesc... paramTypes);

    /**
     * Returns the method type descriptor string.
     *
     * @return the method type descriptor string
     * @jvms 4.3.3 Method Descriptors
     */
    default String descriptorString() {
        return String.format("(%s)%s",
                             Stream.of(parameterArray())
                                   .map(ClassDesc::descriptorString)
                                   .collect(Collectors.joining()),
                             returnType().descriptorString());
    }

    /**
     * Returns a human-readable descriptor for this method type, using the
     * canonical names for parameter and return types.
     *
     * @return the human-readable descriptor for this method type
     */
    default String displayDescriptor() {
        return String.format("(%s)%s",
                             Stream.of(parameterArray())
                                   .map(ClassDesc::displayName)
                                   .collect(Collectors.joining(",")),
                             returnType().displayName());
    }

    /**
     * Compares the specified object with this descriptor for equality.  Returns
     * {@code true} if and only if the specified object is also a
     * {@linkplain MethodTypeDesc} both have the same arity, their return types
     * are equal, and each pair of corresponding parameter types are equal.
     *
     * @param o the other object
     * @return whether this descriptor is equal to the other object
     */
    boolean equals(Object o);
}
