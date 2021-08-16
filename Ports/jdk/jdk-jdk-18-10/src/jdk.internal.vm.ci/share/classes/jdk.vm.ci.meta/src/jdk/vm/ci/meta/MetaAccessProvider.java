/*
 * Copyright (c) 2012, 2019, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jdk.vm.ci.meta.SpeculationLog.Speculation;

/**
 * Provides access to the metadata of a class typically provided in a class file.
 */
public interface MetaAccessProvider {

    /**
     * Returns the resolved Java type representing a given Java class.
     *
     * @param clazz the Java class object
     * @return the resolved Java type object
     */
    ResolvedJavaType lookupJavaType(Class<?> clazz);

    /**
     * Returns the resolved Java types representing some given Java classes.
     *
     * @param classes the Java class objects
     * @return the resolved Java type objects
     */
    default ResolvedJavaType[] lookupJavaTypes(Class<?>[] classes) {
        ResolvedJavaType[] result = new ResolvedJavaType[classes.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = lookupJavaType(classes[i]);
        }
        return result;
    }

    /**
     * Provides the {@link ResolvedJavaMethod} for a {@link Method} or {@link Constructor} obtained
     * via reflection.
     */
    ResolvedJavaMethod lookupJavaMethod(Executable reflectionMethod);

    /**
     * Provides the {@link ResolvedJavaField} for a {@link Field} obtained via reflection.
     */
    ResolvedJavaField lookupJavaField(Field reflectionField);

    /**
     * Returns the resolved Java type of the given {@link JavaConstant} object.
     *
     * @return {@code null} if {@code constant.isNull() || !constant.kind.isObject()}
     */
    ResolvedJavaType lookupJavaType(JavaConstant constant);

    /**
     * Returns the number of bytes occupied by this constant value or constant object.
     *
     * @param constant the constant whose bytes should be measured
     * @return the number of bytes occupied by this constant
     */
    long getMemorySize(JavaConstant constant);

    /**
     * Parses a
     * <a href="http://docs.oracle.com/javase/specs/jvms/se7/html/jvms-4.html#jvms-4.3.3">method
     * descriptor</a> into a {@link Signature}.
     *
     * @throws IllegalArgumentException if the method descriptor is not well formed
     */
    Signature parseMethodDescriptor(String methodDescriptor);

    /**
     * Encodes a deoptimization action and a deoptimization reason in an integer value.
     *
     * @param debugId an integer that can be used to track the origin of a deoptimization at
     *            runtime. There is no guarantee that the runtime will use this value. The runtime
     *            may even keep fewer than 32 bits.
     *
     * @return the encoded value as an integer
     */
    JavaConstant encodeDeoptActionAndReason(DeoptimizationAction action, DeoptimizationReason reason, int debugId);

    /**
     * Gets a constant that denotes {@code speculation}. The constant can passed to the
     * deoptimization handler (e.g., through a thread local) to indicate a failed speculation.
     */
    JavaConstant encodeSpeculation(Speculation speculation);

    /**
     * Decodes {@code constant} back to a {@link Speculation} object.
     *
     * @throws IllegalArgumentException if {@code constant} can only be decoded through a
     *             {@link SpeculationLog} and {@code speculationLog} does not contain the
     *             speculation denoted by {@code constant}
     */
    Speculation decodeSpeculation(JavaConstant constant, SpeculationLog speculationLog);

    DeoptimizationReason decodeDeoptReason(JavaConstant constant);

    DeoptimizationAction decodeDeoptAction(JavaConstant constant);

    int decodeDebugId(JavaConstant constant);

    int getArrayBaseOffset(JavaKind elementKind);

    int getArrayIndexScale(JavaKind elementKind);
}
