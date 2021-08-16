/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.invoke.MethodHandles;

import sun.invoke.util.Wrapper;

import static java.util.Objects.requireNonNull;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for the class
 * constant corresponding to a primitive type (e.g., {@code int.class}).
 */
final class PrimitiveClassDescImpl
        extends DynamicConstantDesc<Class<?>> implements ClassDesc {

    private final String descriptor;

    /**
     * Creates a {@linkplain ClassDesc} given a descriptor string for a primitive
     * type.
     *
     * @param descriptor the descriptor string, which must be a one-character
     * string corresponding to one of the nine base types
     * @throws IllegalArgumentException if the descriptor string does not
     * describe a valid primitive type
     * @jvms 4.3 Descriptors
     */
    PrimitiveClassDescImpl(String descriptor) {
        super(ConstantDescs.BSM_PRIMITIVE_CLASS, requireNonNull(descriptor), ConstantDescs.CD_Class);
        if (descriptor.length() != 1
            || "VIJCSBFDZ".indexOf(descriptor.charAt(0)) < 0)
            throw new IllegalArgumentException(String.format("not a valid primitive type descriptor: %s", descriptor));
        this.descriptor = descriptor;
    }

    @Override
    public String descriptorString() {
        return descriptor;
    }

    @Override
    public Class<?> resolveConstantDesc(MethodHandles.Lookup lookup) {
        return Wrapper.forBasicType(descriptorString().charAt(0)).primitiveType();
    }

    @Override
    public String toString() {
        return String.format("PrimitiveClassDesc[%s]", displayName());
    }
}
