/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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

package jdk.jpackage.internal;

import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * BundlerParamInfo<T>
 *
 * A BundlerParamInfo encapsulates an individual bundler parameter of type <T>.
 */
class BundlerParamInfo<T> {

    /**
     * The command line and hashmap name of the parameter
     */
    String id;

    /**
     * Type of the parameter
     */
    Class<T> valueType;

    /**
     * Indicates if value was set using default value function
     */
    boolean isDefaultValue;

    /**
     * If the value is not set, and no fallback value is found,
     * the parameter uses the value returned by the producer.
     */
    Function<Map<String, ? super Object>, T> defaultValueFunction;

    /**
     * An optional string converter for command line arguments.
     */
    BiFunction<String, Map<String, ? super Object>, T> stringConverter;

    String getID() {
        return id;
    }

    Class<T> getValueType() {
        return valueType;
    }

    boolean getIsDefaultValue() {
        return isDefaultValue;
    }

    Function<Map<String, ? super Object>, T> getDefaultValueFunction() {
        return defaultValueFunction;
    }

    BiFunction<String, Map<String, ? super Object>,T>
            getStringConverter() {
        return stringConverter;
    }

    @SuppressWarnings("unchecked")
    final T fetchFrom(Map<String, ? super Object> params) {
        return fetchFrom(params, true);
    }

    @SuppressWarnings("unchecked")
    final T fetchFrom(Map<String, ? super Object> params,
            boolean invokeDefault) {
        Object o = params.get(getID());
        if (o instanceof String && getStringConverter() != null) {
            return getStringConverter().apply((String)o, params);
        }

        Class<T> klass = getValueType();
        if (klass.isInstance(o)) {
            return (T) o;
        }
        if (o != null) {
            throw new IllegalArgumentException("Param " + getID()
                    + " should be of type " + getValueType()
                    + " but is a " + o.getClass());
        }
        if (params.containsKey(getID())) {
            // explicit nulls are allowed
            return null;
        }

        if (invokeDefault && (getDefaultValueFunction() != null)) {
            T result =  getDefaultValueFunction().apply(params);
            if (result != null) {
                params.put(getID(), result);
                isDefaultValue = true;
            }
            return result;
        }

        // ultimate fallback
        return null;
    }
}
