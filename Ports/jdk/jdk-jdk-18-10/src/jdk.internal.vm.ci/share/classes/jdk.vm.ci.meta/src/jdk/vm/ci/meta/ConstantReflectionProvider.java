/*
 * Copyright (c) 2012, 2015, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.meta;

import java.lang.invoke.MethodHandle;

/**
 * Reflection operations on values represented as {@linkplain JavaConstant constants}. All methods
 * in this interface require the VM to access the actual object encapsulated in
 * {@link JavaKind#Object object} constants. This access is not always possible, depending on kind
 * of VM and the state that the VM is in. Therefore, all methods can return {@code null} at any
 * time, to indicate that the result is not available at this point. The caller is responsible to
 * check for {@code null} results and handle them properly, e.g., not perform an optimization.
 */
public interface ConstantReflectionProvider {

    /**
     * Compares two constants for equality. The equality relationship is symmetric. Returns
     * {@link Boolean#TRUE true} if the two constants represent the same run time value,
     * {@link Boolean#FALSE false} if they are different. Returns {@code null} if the constants
     * cannot be compared at this point.
     */
    Boolean constantEquals(Constant x, Constant y);

    /**
     * Returns the length of the array constant. Returns {@code null} if the constant is not an
     * array, or if the array length is not available at this point.
     */
    Integer readArrayLength(JavaConstant array);

    /**
     * Reads a value from the given array at the given index. Returns {@code null} if the constant
     * is not an array, if the index is out of bounds, or if the value is not available at this
     * point.
     */
    JavaConstant readArrayElement(JavaConstant array, int index);

    /**
     * Gets the current value of this field for a given object, if available.
     *
     * There is no guarantee that the same value will be returned by this method for a field unless
     * the field is considered to be constant by the runtime.
     *
     * @param receiver object from which this field's value is to be read. This value is ignored if
     *            this field is static.
     * @return the value of this field or {@code null} if the value is not available (e.g., because
     *         the field holder is not yet initialized).
     */
    JavaConstant readFieldValue(ResolvedJavaField field, JavaConstant receiver);

    /**
     * Converts the given {@link JavaKind#isPrimitive() primitive} constant to a boxed
     * {@link JavaKind#Object object} constant, according to the Java boxing rules. Returns
     * {@code null} if the source is is not a primitive constant, or the boxed value is not
     * available at this point.
     */
    JavaConstant boxPrimitive(JavaConstant source);

    /**
     * Converts the given {@link JavaKind#Object object} constant to a {@link JavaKind#isPrimitive()
     * primitive} constant, according to the Java unboxing rules. Returns {@code null} if the source
     * is is not an object constant that can be unboxed, or the unboxed value is not available at
     * this point.
     */
    JavaConstant unboxPrimitive(JavaConstant source);

    /**
     * Gets a string as a {@link JavaConstant}.
     */
    JavaConstant forString(String value);

    /**
     * Returns the {@link ResolvedJavaType} for a {@link Class} object (or any other object regarded
     * as a class by the VM) encapsulated in the given constant. Returns {@code null} if the
     * constant does not encapsulate a class, or if the type is not available at this point.
     */
    ResolvedJavaType asJavaType(Constant constant);

    /**
     * Gets access to the internals of {@link MethodHandle}.
     */
    MethodHandleAccessProvider getMethodHandleAccess();

    /**
     * Gets raw memory access.
     */
    MemoryAccessProvider getMemoryAccessProvider();

    /**
     * Gets the runtime representation of the {@link Class} object of this type.
     */
    JavaConstant asJavaClass(ResolvedJavaType type);

    /**
     * Gets the runtime representation of the "hub" of this type--that is, the closest part of the
     * type representation which is typically stored in the object header.
     */
    Constant asObjectHub(ResolvedJavaType type);
}
