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

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

import static java.lang.constant.ConstantDescs.CD_void;
import static java.lang.constant.DirectMethodHandleDesc.Kind.CONSTRUCTOR;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a
 * {@link MethodHandle} constant.
 *
 * @since 12
 */
public sealed interface MethodHandleDesc
        extends ConstantDesc
        permits AsTypeMethodHandleDesc,
                DirectMethodHandleDesc {

    /**
     * Creates a {@linkplain MethodHandleDesc} corresponding to an invocation of a
     * declared method, invocation of a constructor, or access to a field.
     *
     * <p>The lookup descriptor string has the same format as for the various
     * variants of {@code CONSTANT_MethodHandle_info} and for the lookup
     * methods on {@link MethodHandles.Lookup}.  For a method or constructor
     * invocation, it is interpreted as a method type descriptor; for field
     * access, it is interpreted as a field descriptor.  If {@code kind} is
     * {@code CONSTRUCTOR}, the {@code name} parameter is ignored and the return
     * type of the lookup descriptor must be {@code void}.  If {@code kind}
     * corresponds to a virtual method invocation, the lookup type includes the
     * method parameters but not the receiver type.
     *
     * @param kind The kind of method handle to be described
     * @param owner a {@link ClassDesc} describing the class containing the
     *              method, constructor, or field
     * @param name the unqualified name of the method or field (ignored if
     *             {@code kind} is {@code CONSTRUCTOR})
     * @param lookupDescriptor a method descriptor string the lookup type,
     *                         if the request is for a method invocation, or
     *                         describing the invocation type, if the request is
     *                         for a field or constructor
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException if any of the non-ignored arguments are null
     * @throws IllegalArgumentException if the descriptor string is not a valid
     * method or field descriptor
     * @jvms 4.4.8 The CONSTANT_MethodHandle_info Structure
     * @jvms 4.2.2 Unqualified Names
     * @jvms 4.3.2 Field Descriptors
     * @jvms 4.3.3 Method Descriptors
     */
    static DirectMethodHandleDesc of(DirectMethodHandleDesc.Kind kind,
                                     ClassDesc owner,
                                     String name,
                                     String lookupDescriptor) {
        switch (kind) {
            case GETTER:
            case SETTER:
            case STATIC_GETTER:
            case STATIC_SETTER:
                return ofField(kind, owner, name, ClassDesc.ofDescriptor(lookupDescriptor));
            default:
                return new DirectMethodHandleDescImpl(kind, owner, name, MethodTypeDesc.ofDescriptor(lookupDescriptor));
        }
    }

    /**
     * Creates a {@linkplain MethodHandleDesc} corresponding to an invocation of a
     * declared method or constructor.
     *
     * <p>The lookup descriptor string has the same format as for the lookup
     * methods on {@link MethodHandles.Lookup}.  If {@code kind} is
     * {@code CONSTRUCTOR}, the name is ignored and the return type of the lookup
     * type must be {@code void}.  If {@code kind} corresponds to a virtual method
     * invocation, the lookup type includes the method parameters but not the
     * receiver type.
     *
     * @param kind The kind of method handle to be described; must be one of
     *             {@code SPECIAL, VIRTUAL, STATIC, INTERFACE_SPECIAL,
     *             INTERFACE_VIRTUAL, INTERFACE_STATIC, CONSTRUCTOR}
     * @param owner a {@link ClassDesc} describing the class containing the
     *              method or constructor
     * @param name the unqualified name of the method (ignored if {@code kind}
     *             is {@code CONSTRUCTOR})
     * @param lookupMethodType a {@link MethodTypeDesc} describing the lookup type
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException if any non-ignored arguments are null
     * @throws IllegalArgumentException if the {@code name} has the incorrect
     * format, or the kind is invalid
     * @jvms 4.2.2 Unqualified Names
     */
    static DirectMethodHandleDesc ofMethod(DirectMethodHandleDesc.Kind kind,
                                           ClassDesc owner,
                                           String name,
                                           MethodTypeDesc lookupMethodType) {
        switch (kind) {
            case GETTER:
            case SETTER:
            case STATIC_GETTER:
            case STATIC_SETTER:
                throw new IllegalArgumentException(kind.toString());
            case VIRTUAL:
            case SPECIAL:
            case INTERFACE_VIRTUAL:
            case INTERFACE_SPECIAL:
            case INTERFACE_STATIC:
            case STATIC:
            case CONSTRUCTOR:
                return new DirectMethodHandleDescImpl(kind, owner, name, lookupMethodType);
            default:
                throw new IllegalArgumentException(kind.toString());
        }
    }

    /**
     * Creates a {@linkplain MethodHandleDesc} corresponding to a method handle
     * that accesses a field.
     *
     * @param kind the kind of the method handle to be described; must be one of {@code GETTER},
     *             {@code SETTER}, {@code STATIC_GETTER}, or {@code STATIC_SETTER}
     * @param owner a {@link ClassDesc} describing the class containing the field
     * @param fieldName the unqualified name of the field
     * @param fieldType a {@link ClassDesc} describing the type of the field
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException if any of the arguments are null
     * @throws IllegalArgumentException if the {@code kind} is not one of the
     * valid values or if the field name is not valid
     * @jvms 4.2.2 Unqualified Names
     */
    static DirectMethodHandleDesc ofField(DirectMethodHandleDesc.Kind kind,
                                          ClassDesc owner,
                                          String fieldName,
                                          ClassDesc fieldType) {
        MethodTypeDesc mtr = switch (kind) {
            case GETTER        -> MethodTypeDesc.of(fieldType, owner);
            case SETTER        -> MethodTypeDesc.of(CD_void, owner, fieldType);
            case STATIC_GETTER -> MethodTypeDesc.of(fieldType);
            case STATIC_SETTER -> MethodTypeDesc.of(CD_void, fieldType);
            default -> throw new IllegalArgumentException(kind.toString());
        };
        return new DirectMethodHandleDescImpl(kind, owner, fieldName, mtr);
    }

    /**
     * Returns a {@linkplain MethodHandleDesc} corresponding to invocation of a constructor
     *
     * @param owner a {@link ClassDesc} describing the class containing the
     *              constructor
     * @param paramTypes {@link ClassDesc}s describing the parameter types of
     *                   the constructor
     * @return the {@linkplain MethodHandleDesc}
     * @throws NullPointerException if any argument or its contents is {@code null}
     */
    static DirectMethodHandleDesc ofConstructor(ClassDesc owner,
                                                ClassDesc... paramTypes) {
        return MethodHandleDesc.ofMethod(CONSTRUCTOR, owner, ConstantDescs.DEFAULT_NAME,
                                         MethodTypeDesc.of(CD_void, paramTypes));
    }

    /**
     * Returns a {@linkplain MethodHandleDesc} that describes this method handle
     * adapted to a different type, as if by {@link MethodHandle#asType(MethodType)}.
     *
     * @param type a {@link MethodHandleDesc} describing the new method type
     * @return a {@linkplain MethodHandleDesc} for the adapted method handle
     * @throws NullPointerException if the argument is {@code null}
     */
    default MethodHandleDesc asType(MethodTypeDesc type) {
        return (invocationType().equals(type)) ? this : new AsTypeMethodHandleDesc(this, type);
    }

    /**
     * Returns a {@link MethodTypeDesc} describing the invocation type of the
     * method handle described by this nominal descriptor.  The invocation type
     * describes the full set of stack values that are consumed by the invocation
     * (including the receiver, if any).
     *
     * @return a {@linkplain MethodHandleDesc} describing the method handle type
     */
    MethodTypeDesc invocationType();

    /**
     * Compares the specified object with this descriptor for equality.  Returns
     * {@code true} if and only if the specified object is also a
     * {@linkplain MethodHandleDesc}, and both encode the same nominal description
     * of a method handle.
     *
     * @param o the other object
     * @return whether this descriptor is equal to the other object
     */
    boolean equals(Object o);
}
