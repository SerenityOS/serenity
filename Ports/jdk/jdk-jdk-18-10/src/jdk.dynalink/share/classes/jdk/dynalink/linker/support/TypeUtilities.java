/*
 * Copyright (c) 2010, 2013, Oracle and/or its affiliates. All rights reserved.
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

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file, and Oracle licenses the original version of this file under the BSD
 * license:
 */
/*
   Copyright 2009-2013 Attila Szegedi

   Redistribution and use in source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
   * Redistributions of source code must retain the above copyright
     notice, this list of conditions and the following disclaimer.
   * Redistributions in binary form must reproduce the above copyright
     notice, this list of conditions and the following disclaimer in the
     documentation and/or other materials provided with the distribution.
   * Neither the name of the copyright holder nor the names of
     contributors may be used to endorse or promote products derived from
     this software without specific prior written permission.

   THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
   IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
   TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
   PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL COPYRIGHT HOLDER
   BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
   CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR
   BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
   OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
   ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package jdk.dynalink.linker.support;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import jdk.dynalink.DynamicLinkerFactory;
import jdk.dynalink.linker.MethodTypeConversionStrategy;

/**
 * Various static utility methods for working with Java types.
 */
public final class TypeUtilities {
    private TypeUtilities() {
    }

    private static final Map<Class<?>, Class<?>> WRAPPER_TYPES = createWrapperTypes();
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TYPES = invertMap(WRAPPER_TYPES);
    private static final Map<String, Class<?>> PRIMITIVE_TYPES_BY_NAME = createClassNameMapping(WRAPPER_TYPES.keySet());

    private static Map<Class<?>, Class<?>> createWrapperTypes() {
        final Map<Class<?>, Class<?>> wrapperTypes = new IdentityHashMap<>(8);
        wrapperTypes.put(Void.TYPE, Void.class);
        wrapperTypes.put(Boolean.TYPE, Boolean.class);
        wrapperTypes.put(Byte.TYPE, Byte.class);
        wrapperTypes.put(Character.TYPE, Character.class);
        wrapperTypes.put(Short.TYPE, Short.class);
        wrapperTypes.put(Integer.TYPE, Integer.class);
        wrapperTypes.put(Long.TYPE, Long.class);
        wrapperTypes.put(Float.TYPE, Float.class);
        wrapperTypes.put(Double.TYPE, Double.class);
        return Collections.unmodifiableMap(wrapperTypes);
    }

    private static Map<String, Class<?>> createClassNameMapping(final Collection<Class<?>> classes) {
        final Map<String, Class<?>> map = new HashMap<>();
        for(final Class<?> clazz: classes) {
            map.put(clazz.getName(), clazz);
        }
        return map;
    }

    private static <K, V> Map<V, K> invertMap(final Map<K, V> map) {
        final Map<V, K> inverted = new IdentityHashMap<>(map.size());
        for(final Map.Entry<K, V> entry: map.entrySet()) {
            inverted.put(entry.getValue(), entry.getKey());
        }
        return Collections.unmodifiableMap(inverted);
    }

    /**
     * Determines whether one type can be converted to another type using a method invocation conversion, as per JLS 5.3
     * "Method Invocation Conversion". This is basically all conversions allowed by subtyping (see
     * {@link #isSubtype(Class, Class)}) as well as boxing conversion (JLS 5.1.7) optionally followed by widening
     * reference conversion, and unboxing conversion (JLS 5.1.8) optionally followed by widening primitive conversion.
     *
     * @param sourceType the type being converted from (call site type for parameter types, method type for return types)
     * @param targetType the parameter type being converted to (method type for parameter types, call site type for return types)
     * @return true if source type is method invocation convertible to target type.
     */
    public static boolean isMethodInvocationConvertible(final Class<?> sourceType, final Class<?> targetType) {
        if(targetType.isAssignableFrom(sourceType)) {
            return true;
        }
        if(sourceType.isPrimitive()) {
            if(targetType.isPrimitive()) {
                return isProperPrimitiveSubtype(sourceType, targetType);
            }
            return isBoxingAndWideningReferenceConversion(sourceType, targetType);
        }
        if(targetType.isPrimitive()) {
            final Class<?> unboxedCallSiteType = getPrimitiveType(sourceType);
            return unboxedCallSiteType != null
                    && (unboxedCallSiteType == targetType || isProperPrimitiveSubtype(unboxedCallSiteType, targetType));
        }
        return false;
    }

    private static boolean isBoxingAndWideningReferenceConversion(final Class<?> sourceType, final Class<?> targetType) {
        final Class<?> wrapperType = getWrapperType(sourceType);
        assert wrapperType != null : sourceType.getName();
        return targetType.isAssignableFrom(wrapperType);
    }

    /**
     * Determines whether a type can be converted to another without losing any
     * precision. As a special case, void is considered convertible only to void
     * and {@link Object} (either as {@code null} or as a custom value set in
     * {@link DynamicLinkerFactory#setAutoConversionStrategy(MethodTypeConversionStrategy)}).
     * Somewhat unintuitively, we consider anything to be convertible to void
     * even though converting to void causes the ultimate loss of data. On the
     * other hand, conversion to void essentially means that the value is of no
     * interest and should be discarded, thus there's no expectation of
     * preserving any precision.
     *
     * @param sourceType the source type
     * @param targetType the target type
     * @return true if lossless conversion is possible
     */
    public static boolean isConvertibleWithoutLoss(final Class<?> sourceType, final Class<?> targetType) {
        if(targetType.isAssignableFrom(sourceType) || targetType == void.class) {
            return true;
        }
        if(sourceType.isPrimitive()) {
            if(sourceType == void.class) {
                // Void should be losslessly representable by Object, either as null or as a custom value that
                // can be set with DynamicLinkerFactory.setAutoConversionStrategy.
                return targetType == Object.class;
            }
            if(targetType.isPrimitive()) {
                return isProperPrimitiveLosslessSubtype(sourceType, targetType);
            }
            return isBoxingAndWideningReferenceConversion(sourceType, targetType);
        }
        // Can't convert from any non-primitive type to any primitive type without data loss because of null.
        // Also, can't convert non-assignable reference types.
        return false;
    }

    /**
     * Determines whether one type is a subtype of another type, as per JLS
     * 4.10 "Subtyping". Note: this is not strict or proper subtype, therefore
     * true is also returned for identical types; to be completely precise, it
     * allows identity conversion (JLS 5.1.1), widening primitive conversion
     * (JLS 5.1.2) and widening reference conversion (JLS 5.1.5).
     *
     * @param subType the supposed subtype
     * @param superType the supposed supertype of the subtype
     * @return true if subType can be converted by identity conversion, widening primitive conversion, or widening
     * reference conversion to superType.
     */
    public static boolean isSubtype(final Class<?> subType, final Class<?> superType) {
        // Covers both JLS 4.10.2 "Subtyping among Class and Interface Types"
        // and JLS 4.10.3 "Subtyping among Array Types", as well as primitive
        // type identity.
        if(superType.isAssignableFrom(subType)) {
            return true;
        }
        // JLS 4.10.1 "Subtyping among Primitive Types". Note we don't test for
        // identity, as identical types were taken care of in the
        // isAssignableFrom test. As per 4.10.1, the supertype relation is as
        // follows:
        // double > float
        // float > long
        // long > int
        // int > short
        // int > char
        // short > byte
        if(superType.isPrimitive() && subType.isPrimitive()) {
            return isProperPrimitiveSubtype(subType, superType);
        }
        return false;
    }

    /**
     * Returns true if a supposed primitive subtype is a proper subtype ( meaning, subtype and not identical) of the
     * supposed primitive supertype
     *
     * @param subType the supposed subtype
     * @param superType the supposed supertype
     * @return true if subType is a proper (not identical to) primitive subtype of the superType
     */
    private static boolean isProperPrimitiveSubtype(final Class<?> subType, final Class<?> superType) {
        if(superType == boolean.class || subType == boolean.class) {
            return false;
        }
        if(subType == byte.class) {
            return superType != char.class;
        }
        if(subType == char.class) {
            return superType != short.class && superType != byte.class;
        }
        if(subType == short.class) {
            return superType != char.class && superType != byte.class;
        }
        if(subType == int.class) {
            return superType == long.class || superType == float.class || superType == double.class;
        }
        if(subType == long.class) {
            return superType == float.class || superType == double.class;
        }
        if(subType == float.class) {
            return superType == double.class;
        }
        return false;
    }

    /**
     * Similar to {@link #isProperPrimitiveSubtype(Class, Class)}, except it disallows conversions from int and long to
     * float, and from long to double, as those can lose precision. It also disallows conversion from and to char and
     * anything else (similar to boolean) as char is not meant to be an arithmetic type.
     * @param subType the supposed subtype
     * @param superType the supposed supertype
     * @return true if subType is a proper (not identical to) primitive subtype of the superType that can be represented
     * by the supertype without no precision loss.
     */
    private static boolean isProperPrimitiveLosslessSubtype(final Class<?> subType, final Class<?> superType) {
        if(superType == boolean.class || subType == boolean.class) {
            return false;
        }
        if(superType == char.class || subType == char.class) {
            return false;
        }
        if(subType == byte.class) {
            return true;
        }
        if(subType == short.class) {
            return superType != byte.class;
        }
        if(subType == int.class) {
            return superType == long.class || superType == double.class;
        }
        if(subType == float.class) {
            return superType == double.class;
        }
        return false;
    }

    /**
     * Given a name of a primitive type returns the class representing it. I.e.
     * when invoked with "int", returns {@link Integer#TYPE}.
     * @param name the name of the primitive type
     * @return the class representing the primitive type, or null if the name
     * does not correspond to a primitive type.
     */
    public static Class<?> getPrimitiveTypeByName(final String name) {
        return PRIMITIVE_TYPES_BY_NAME.get(name);
    }

    /**
     * When passed a class representing a wrapper for a primitive type, returns
     * the class representing the corresponding primitive type. I.e. calling it
     * with {@code Integer.class} will return {@code Integer.TYPE}. If passed a
     * class that is not a wrapper for primitive type, returns null.
     * @param wrapperType the class object representing a wrapper for a
     * primitive type.
     * @return the class object representing the primitive type, or null if the
     * passed class is not a primitive wrapper.
     */
    public static Class<?> getPrimitiveType(final Class<?> wrapperType) {
        return PRIMITIVE_TYPES.get(wrapperType);
    }

    /**
     * When passed a class representing a primitive type, returns the class representing the corresponding
     * wrapper type. I.e. calling it with {@code int.class} will return {@code Integer.class}. If passed a class
     * that is not a primitive type, returns null.
     * @param primitiveType the class object representing a primitive type
     * @return the class object representing the wrapper type, or null if the passed class is not a primitive.
     */
    public static Class<?> getWrapperType(final Class<?> primitiveType) {
        return WRAPPER_TYPES.get(primitiveType);
    }

    /**
     * Returns true if the passed type is a wrapper for a primitive type.
     * @param type the examined type
     * @return true if the passed type is a wrapper for a primitive type.
     */
    public static boolean isWrapperType(final Class<?> type) {
        return PRIMITIVE_TYPES.containsKey(type);
    }
}
