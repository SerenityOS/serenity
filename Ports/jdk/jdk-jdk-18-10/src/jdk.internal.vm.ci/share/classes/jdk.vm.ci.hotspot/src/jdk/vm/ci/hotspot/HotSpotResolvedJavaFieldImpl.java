/*
 * Copyright (c) 2011, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.hotspot;

import static jdk.vm.ci.hotspot.HotSpotJVMCIRuntime.runtime;
import static jdk.vm.ci.hotspot.HotSpotVMConfig.config;
import static jdk.vm.ci.hotspot.UnsafeAccess.UNSAFE;
import static jdk.internal.misc.Unsafe.ADDRESS_SIZE;

import java.lang.annotation.Annotation;

import jdk.internal.vm.annotation.Stable;

import jdk.vm.ci.meta.JavaConstant;
import jdk.vm.ci.meta.JavaType;
import jdk.vm.ci.meta.ResolvedJavaType;
import jdk.vm.ci.meta.UnresolvedJavaType;

/**
 * Represents a field in a HotSpot type.
 */
class HotSpotResolvedJavaFieldImpl implements HotSpotResolvedJavaField {

    private final HotSpotResolvedObjectTypeImpl holder;
    private JavaType type;

    /**
     * Value of {@code fieldDescriptor::access_flags()}.
     */
    private final int offset;

    /**
     * Value of {@code fieldDescriptor::index()}.
     */
    private final short index;

    /**
     * This value contains all flags as stored in the VM including internal ones.
     */
    private final int modifiers;

    HotSpotResolvedJavaFieldImpl(HotSpotResolvedObjectTypeImpl holder, JavaType type, long offset, int modifiers, int index) {
        this.holder = holder;
        this.type = type;
        this.index = (short) index;
        assert this.index == index;
        assert offset != -1;
        assert offset == (int) offset : "offset larger than int";
        this.offset = (int) offset;
        this.modifiers = modifiers;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof HotSpotResolvedJavaFieldImpl) {
            HotSpotResolvedJavaFieldImpl that = (HotSpotResolvedJavaFieldImpl) obj;
            if (that.offset != this.offset || that.isStatic() != this.isStatic()) {
                return false;
            } else if (this.holder.equals(that.holder)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public int hashCode() {
        return holder.hashCode() ^ offset;
    }

    @Override
    public int getModifiers() {
        return modifiers & HotSpotModifiers.jvmFieldModifiers();
    }

    @Override
    public boolean isInternal() {
        return (modifiers & config().jvmAccFieldInternal) != 0;
    }

    /**
     * Determines if a given object contains this field.
     *
     * @return true iff this is a non-static field and its declaring class is assignable from
     *         {@code object}'s class
     */
    @Override
    public boolean isInObject(JavaConstant object) {
        if (isStatic()) {
            return false;
        }
        HotSpotObjectConstant constant = (HotSpotObjectConstant) object;
        return getDeclaringClass().isAssignableFrom(constant.getType());
    }

    @Override
    public HotSpotResolvedObjectTypeImpl getDeclaringClass() {
        return holder;
    }

    @Override
    public String getName() {
        return holder.createFieldInfo(index).getName();
    }

    @Override
    public JavaType getType() {
        // Pull field into local variable to prevent a race causing
        // a ClassCastException below
        JavaType currentType = type;
        if (currentType instanceof UnresolvedJavaType) {
            // Don't allow unresolved types to hang around forever
            UnresolvedJavaType unresolvedType = (UnresolvedJavaType) currentType;
            JavaType resolved = HotSpotJVMCIRuntime.runtime().lookupType(unresolvedType.getName(), holder, false);
            if (resolved instanceof ResolvedJavaType) {
                type = resolved;
            }
        }
        return type;

    }

    @Override
    public int getOffset() {
        return offset;
    }

    /**
     * Gets the value of this field's index (i.e. {@code fieldDescriptor::index()} in the encoded
     * fields of the declaring class.
     */
    int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        return format("HotSpotResolvedJavaFieldImpl<%H.%n %t:") + offset + ">";
    }

    @Override
    public boolean isSynthetic() {
        return (config().jvmAccSynthetic & modifiers) != 0;
    }

    /**
     * Checks if this field has the {@code Stable} annotation.
     *
     * @return true if field has {@code Stable} annotation, false otherwise
     */
    @Override
    public boolean isStable() {
        return (config().jvmAccFieldStable & modifiers) != 0;
    }

    private boolean hasAnnotations() {
        if (!isInternal()) {
            HotSpotVMConfig config = config();
            final long metaspaceAnnotations = UNSAFE.getAddress(holder.getMetaspaceKlass() + config.instanceKlassAnnotationsOffset);
            if (metaspaceAnnotations != 0) {
                long fieldsAnnotations = UNSAFE.getAddress(metaspaceAnnotations + config.annotationsFieldAnnotationsOffset);
                if (fieldsAnnotations != 0) {
                    long fieldAnnotations = UNSAFE.getAddress(fieldsAnnotations + config.fieldsAnnotationsBaseOffset + (ADDRESS_SIZE * index));
                    return fieldAnnotations != 0;
                }
            }
        }
        return false;
    }

    @Override
    public Annotation[] getAnnotations() {
        if (!hasAnnotations()) {
            return new Annotation[0];
        }
        return runtime().reflection.getFieldAnnotations(this);
    }

    @Override
    public Annotation[] getDeclaredAnnotations() {
        if (!hasAnnotations()) {
            return new Annotation[0];
        }
        return runtime().reflection.getFieldDeclaredAnnotations(this);
    }

    @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (!hasAnnotations()) {
            return null;
        }
        return runtime().reflection.getFieldAnnotation(this, annotationClass);
    }
}
