/*
 * Copyright (c) 2010, 2016, Oracle and/or its affiliates. All rights reserved.
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

package jdk.dynalink;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.WrongMethodTypeException;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;
import jdk.dynalink.linker.ConversionComparator;
import jdk.dynalink.linker.ConversionComparator.Comparison;
import jdk.dynalink.linker.GuardedInvocation;
import jdk.dynalink.linker.GuardingTypeConverterFactory;
import jdk.dynalink.linker.LinkerServices;
import jdk.dynalink.linker.MethodTypeConversionStrategy;
import jdk.dynalink.linker.support.TypeUtilities;

/**
 * A factory for type converters. This class is the main implementation behind the
 * {@link LinkerServices#asType(MethodHandle, MethodType)}. It manages the known {@link GuardingTypeConverterFactory}
 * instances and creates appropriate converters for method handles.
 */
final class TypeConverterFactory {
    private final ConversionComparator[] comparators;
    private final MethodTypeConversionStrategy autoConversionStrategy;

    private final BiClassValue<MethodHandle> converterMap;
    private final BiClassValue<MethodHandle> converterIdentityMap;
    private final BiClassValue<Boolean> canConvert;

    /**
     * Creates a new type converter factory from the available {@link GuardingTypeConverterFactory} instances.
     *
     * @param factories the {@link GuardingTypeConverterFactory} instances to compose.
     * @param autoConversionStrategy conversion strategy for automatic type conversions. After
     * {@link #asType(java.lang.invoke.MethodHandle, java.lang.invoke.MethodType)} has applied all custom
     * conversions to a method handle, it still needs to effect
     * {@link TypeUtilities#isMethodInvocationConvertible(Class, Class) method invocation conversions} that
     * can usually be automatically applied as per
     * {@link java.lang.invoke.MethodHandle#asType(java.lang.invoke.MethodType)}.
     * However, sometimes language runtimes will want to customize even those conversions for their own call
     * sites. A typical example is allowing unboxing of null return values, which is by default prohibited by
     * ordinary {@code MethodHandles.asType}. In this case, a language runtime can install its own custom
     * automatic conversion strategy, that can deal with null values. Note that when the strategy's
     * {@link MethodTypeConversionStrategy#asType(java.lang.invoke.MethodHandle, java.lang.invoke.MethodType)}
     * is invoked, the custom language conversions will already have been applied to the method handle, so by
     * design the difference between the handle's current method type and the desired final type will always
     * only be ones that can be subjected to method invocation conversions. Can be null, in which case no
     * custom strategy is employed.
     */
    TypeConverterFactory(final Iterable<? extends GuardingTypeConverterFactory> factories,
            final MethodTypeConversionStrategy autoConversionStrategy) {
        final List<GuardingTypeConverterFactory> l = new LinkedList<>();
        final List<ConversionComparator> c = new LinkedList<>();
        for(final GuardingTypeConverterFactory factory: factories) {
            l.add(factory);
            if(factory instanceof ConversionComparator) {
                c.add((ConversionComparator)factory);
            }
        }
        this.comparators = c.toArray(new ConversionComparator[0]);
        this.autoConversionStrategy = autoConversionStrategy;

        final GuardingTypeConverterFactory[] afactories = l.toArray(new GuardingTypeConverterFactory[0]);
        final BiClassValue<MethodHandle> converterMap = BiClassValue.computing(
            (sourceType, targetType) -> createConverter(sourceType, targetType, afactories));

        converterIdentityMap = BiClassValue.computing((sourceType, targetType) -> {
            if(!canAutoConvert(sourceType, targetType)) {
                final MethodHandle converter = getCacheableTypeConverter(sourceType, targetType, converterMap);
                if(converter != IDENTITY_CONVERSION) {
                    return converter;
                }
            }
            return IDENTITY_CONVERSION.asType(MethodType.methodType(targetType, sourceType));
        });

        canConvert = BiClassValue.computing(
            (sourceType, targetType) -> getTypeConverterNull(sourceType, targetType, converterMap) != null);

        this.converterMap = converterMap;
    }

    /**
     * Similar to {@link MethodHandle#asType(MethodType)} except it also hooks in method handles produced by
     * {@link GuardingTypeConverterFactory} implementations, providing for language-specific type coercing of
     * parameters. For all conversions that are not a JLS method invocation conversion it'll insert
     * {@link MethodHandles#filterArguments(MethodHandle, int, MethodHandle...)} with composite filters
     * provided by {@link GuardingTypeConverterFactory} implementations. For the remaining JLS method invocation
     * conversions, it will invoke {@link MethodTypeConversionStrategy#asType(MethodHandle, MethodType)} first
     * if an automatic conversion strategy was specified in the
     * {@link #TypeConverterFactory(Iterable, MethodTypeConversionStrategy) constructor}, and finally apply
     * {@link MethodHandle#asType(MethodType)} for any remaining conversions.
     *
     * @param handle target method handle
     * @param fromType the types of source arguments
     * @return a method handle that is a suitable combination of {@link MethodHandle#asType(MethodType)},
     * {@link MethodTypeConversionStrategy#asType(MethodHandle, MethodType)}, and
     * {@link MethodHandles#filterArguments(MethodHandle, int, MethodHandle...)} with
     * {@link GuardingTypeConverterFactory} produced type converters as filters.
     */
    MethodHandle asType(final MethodHandle handle, final MethodType fromType) {
        MethodHandle newHandle = handle;
        final MethodType toType = newHandle.type();
        final int l = toType.parameterCount();
        if(l != fromType.parameterCount()) {
            throw new WrongMethodTypeException("Parameter counts differ: " + handle.type() + " vs. " + fromType);
        }
        int pos = 0;
        final List<MethodHandle> converters = new LinkedList<>();
        for(int i = 0; i < l; ++i) {
            final Class<?> fromParamType = fromType.parameterType(i);
            final Class<?> toParamType = toType.parameterType(i);
            if(canAutoConvert(fromParamType, toParamType)) {
                newHandle = applyConverters(newHandle, pos, converters);
            } else {
                final MethodHandle converter = getTypeConverterNull(fromParamType, toParamType, converterMap);
                if(converter != null) {
                    if(converters.isEmpty()) {
                        pos = i;
                    }
                    converters.add(converter);
                } else {
                    newHandle = applyConverters(newHandle, pos, converters);
                }
            }
        }
        newHandle = applyConverters(newHandle, pos, converters);

        // Convert return type
        final Class<?> fromRetType = fromType.returnType();
        final Class<?> toRetType = toType.returnType();
        if(fromRetType != Void.TYPE && toRetType != Void.TYPE) {
            if(!canAutoConvert(toRetType, fromRetType)) {
                final MethodHandle converter = getTypeConverterNull(toRetType, fromRetType, converterMap);
                if(converter != null) {
                    newHandle = MethodHandles.filterReturnValue(newHandle, converter);
                }
            }
        }

        // Give change to automatic conversion strategy, if one is present.
        final MethodHandle autoConvertedHandle =
                autoConversionStrategy != null ? autoConversionStrategy.asType(newHandle, fromType) : newHandle;

        // Do a final asType for any conversions that remain.
        return autoConvertedHandle.asType(fromType);
    }

    private static MethodHandle applyConverters(final MethodHandle handle, final int pos, final List<MethodHandle> converters) {
        if(converters.isEmpty()) {
            return handle;
        }
        final MethodHandle newHandle =
                MethodHandles.filterArguments(handle, pos, converters.toArray(new MethodHandle[0]));
        converters.clear();
        return newHandle;
    }

    /**
     * Returns true if there might exist a conversion between the requested types (either an automatic JVM conversion,
     * or one provided by any available {@link GuardingTypeConverterFactory}), or false if there definitely does not
     * exist a conversion between the requested types. Note that returning true does not guarantee that the conversion
     * will succeed at runtime (notably, if the "from" or "to" types are sufficiently generic), but returning false
     * guarantees that it would fail.
     *
     * @param from the source type for the conversion
     * @param to the target type for the conversion
     * @return true if there can be a conversion, false if there can not.
     */
    boolean canConvert(final Class<?> from, final Class<?> to) {
        return canAutoConvert(from, to) || canConvert.get(from, to);
    }

    /**
     * Determines which of the two type conversions from a source type to the two target types is preferred. This is
     * used for dynamic overloaded method resolution. If the source type is convertible to exactly one target type with
     * a method invocation conversion, it is chosen, otherwise available {@link ConversionComparator}s are consulted.
     * @param sourceType the source type.
     * @param targetType1 one potential target type
     * @param targetType2 another potential target type.
     * @return one of Comparison constants that establish which - if any - of the target types is preferable for the
     * conversion.
     */
    Comparison compareConversion(final Class<?> sourceType, final Class<?> targetType1, final Class<?> targetType2) {
        for(final ConversionComparator comparator: comparators) {
            final Comparison result = comparator.compareConversion(sourceType, targetType1, targetType2);
            if(result != Comparison.INDETERMINATE) {
                return result;
            }
        }
        if(TypeUtilities.isMethodInvocationConvertible(sourceType, targetType1)) {
            if(!TypeUtilities.isMethodInvocationConvertible(sourceType, targetType2)) {
                return Comparison.TYPE_1_BETTER;
            }
        } else if(TypeUtilities.isMethodInvocationConvertible(sourceType, targetType2)) {
            return Comparison.TYPE_2_BETTER;
        }
        return Comparison.INDETERMINATE;
    }

    /**
     * Determines whether it's safe to perform an automatic conversion between the source and target class.
     *
     * @param fromType convert from this class
     * @param toType convert to this class
     * @return true if it's safe to let MethodHandles.convertArguments() to handle this conversion.
     */
    /*private*/ static boolean canAutoConvert(final Class<?> fromType, final Class<?> toType) {
        return TypeUtilities.isMethodInvocationConvertible(fromType, toType);
    }

    /*private*/ static MethodHandle getCacheableTypeConverterNull(final Class<?> sourceType, final Class<?> targetType, final BiClassValue<MethodHandle> converterMap) {
        final MethodHandle converter = getCacheableTypeConverter(sourceType, targetType, converterMap);
        return converter == IDENTITY_CONVERSION ? null : converter;
    }

    /*private*/ static MethodHandle getTypeConverterNull(final Class<?> sourceType, final Class<?> targetType, final BiClassValue<MethodHandle> converterMap) {
        try {
            return getCacheableTypeConverterNull(sourceType, targetType, converterMap);
        } catch(final NotCacheableConverter e) {
            return e.converter;
        }
    }

    /*private*/ static MethodHandle getCacheableTypeConverter(final Class<?> sourceType, final Class<?> targetType, final BiClassValue<MethodHandle> converterMap) {
        return converterMap.get(sourceType, targetType);
    }

    /**
     * Given a source and target type, returns a method handle that converts between them. Never returns null; in worst
     * case it will return an identity conversion (that might fail for some values at runtime). You can use this method
     * if you have a piece of your program that is written in Java, and you need to reuse existing type conversion
     * machinery in a non-invokedynamic context.
     * @param sourceType the type to convert from
     * @param targetType the type to convert to
     * @return a method handle performing the conversion.
     */
    MethodHandle getTypeConverter(final Class<?> sourceType, final Class<?> targetType) {
        try {
            return converterIdentityMap.get(sourceType, targetType);
        } catch(final NotCacheableConverter e) {
            return e.converter;
        }
    }

    private static class LookupSupplier implements Supplier<MethodHandles.Lookup> {
        volatile boolean returnedLookup;
        volatile boolean closed;

        @Override
        public Lookup get() {
            if (closed) {
                // Something held on to this supplier and tried to invoke it
                // after we're done with it.
                throw new IllegalStateException();
            }
            final Lookup lookup = LinkerServicesImpl.getCurrentLookup();
            returnedLookup = true;
            return lookup;
        }
    }

    /*private*/ static MethodHandle createConverter(final Class<?> sourceType, final Class<?> targetType, GuardingTypeConverterFactory[] factories) {
        final MethodType type = MethodType.methodType(targetType, sourceType);
        final MethodHandle identity = IDENTITY_CONVERSION.asType(type);
        MethodHandle last = identity;

        final LookupSupplier lookupSupplier = new LookupSupplier();
        try {
            for(int i = factories.length; i-- > 0;) {
                final GuardedInvocation next = factories[i].convertToType(sourceType, targetType, lookupSupplier);
                if(next != null) {
                    last = next.compose(last);
                }
            }
        } catch (final RuntimeException e) {
            throw e;
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            lookupSupplier.closed = true;
        }

        if(last == identity) {
            return IDENTITY_CONVERSION;
        }
        if(!lookupSupplier.returnedLookup) {
            return last;
        }
        // At least one of the consulted converter factories obtained the
        // lookup, so we must presume the created converter is sensitive to the
        // lookup class and thus we will not cache it.
        throw new NotCacheableConverter(last);
    }

    /*private*/ static final MethodHandle IDENTITY_CONVERSION = MethodHandles.identity(Object.class);

    @SuppressWarnings("serial")
    private static class NotCacheableConverter extends RuntimeException {
        final MethodHandle converter;

        NotCacheableConverter(final MethodHandle converter) {
            super("", null, false, false);
            this.converter = converter;
        }
    }
}
