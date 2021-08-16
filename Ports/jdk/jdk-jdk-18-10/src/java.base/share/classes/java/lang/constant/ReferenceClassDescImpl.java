/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.constant.ConstantUtils.dropFirstAndLastChar;
import static java.lang.constant.ConstantUtils.internalToBinary;
import static java.util.Objects.requireNonNull;

/**
 * A <a href="package-summary.html#nominal">nominal descriptor</a> for a class,
 * interface, or array type.  A {@linkplain ReferenceClassDescImpl} corresponds to a
 * {@code Constant_Class_info} entry in the constant pool of a classfile.
 */
final class ReferenceClassDescImpl implements ClassDesc {
    private final String descriptor;

    /**
     * Creates a {@linkplain ClassDesc} from a descriptor string for a class or
     * interface type
     *
     * @param descriptor a field descriptor string for a class or interface type
     * @throws IllegalArgumentException if the descriptor string is not a valid
     * field descriptor string, or does not describe a class or interface type
     * @jvms 4.3.2 Field Descriptors
     */
    ReferenceClassDescImpl(String descriptor) {
        requireNonNull(descriptor);
        int len = ConstantUtils.skipOverFieldSignature(descriptor, 0, descriptor.length(), false);
        if (len == 0 || len == 1
            || len != descriptor.length())
            throw new IllegalArgumentException(String.format("not a valid reference type descriptor: %s", descriptor));
        this.descriptor = descriptor;
    }

    @Override
    public String descriptorString() {
        return descriptor;
    }

    @Override
    public Class<?> resolveConstantDesc(MethodHandles.Lookup lookup)
            throws ReflectiveOperationException {
        ClassDesc c = this;
        int depth = ConstantUtils.arrayDepth(descriptorString());
        for (int i=0; i<depth; i++)
            c = c.componentType();

        if (c.isPrimitive())
            return lookup.findClass(descriptorString());
        else {
            Class<?> clazz = lookup.findClass(internalToBinary(dropFirstAndLastChar(c.descriptorString())));
            for (int i = 0; i < depth; i++)
                clazz = clazz.arrayType();
            return clazz;
        }
    }

    /**
     * Returns {@code true} if this {@linkplain ReferenceClassDescImpl} is
     * equal to another {@linkplain ReferenceClassDescImpl}.  Equality is
     * determined by the two class descriptors having equal class descriptor
     * strings.
     *
     * @param o the {@code ClassDesc} to compare to this
     *       {@code ClassDesc}
     * @return {@code true} if the specified {@code ClassDesc}
     *      is equal to this {@code ClassDesc}.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ClassDesc constant = (ClassDesc) o;
        return descriptor.equals(constant.descriptorString());
    }

    @Override
    public int hashCode() {
        return descriptor.hashCode();
    }

    @Override
    public String toString() {
        return String.format("ClassDesc[%s]", displayName());
    }
}
