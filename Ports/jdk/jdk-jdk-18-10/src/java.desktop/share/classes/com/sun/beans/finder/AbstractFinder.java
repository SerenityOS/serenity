/*
 * Copyright (c) 2008, 2013, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.beans.finder;

import java.lang.reflect.Executable;
import java.lang.reflect.Modifier;

import java.util.HashMap;
import java.util.Map;

/**
 * This abstract class provides functionality
 * to find a public method or constructor
 * with specified parameter types.
 * It supports a variable number of parameters.
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
abstract class AbstractFinder<T extends Executable> {
    private final Class<?>[] args;

    /**
     * Creates finder for array of classes of arguments.
     * If a particular element of array equals {@code null},
     * than the appropriate pair of classes
     * does not take into consideration.
     *
     * @param args  array of classes of arguments
     */
    protected AbstractFinder(Class<?>[] args) {
        this.args = args;
    }

    /**
     * Checks validness of the method.
     * At least the valid method should be public.
     *
     * @param method  the object that represents method
     * @return {@code true} if the method is valid,
     *         {@code false} otherwise
     */
    protected boolean isValid(T method) {
        return Modifier.isPublic(method.getModifiers());
    }

    /**
     * Performs a search in the {@code methods} array.
     * The one method is selected from the array of the valid methods.
     * The list of parameters of the selected method shows
     * the best correlation with the list of arguments
     * specified at class initialization.
     * If more than one method is both accessible and applicable
     * to a method invocation, it is necessary to choose one
     * to provide the descriptor for the run-time method dispatch.
     * The most specific method should be chosen.
     *
     * @param methods  the array of methods to search within
     * @return the object that represents found method
     * @throws NoSuchMethodException if no method was found or several
     *                               methods meet the search criteria
     * @see #isAssignable
     */
    final T find(T[] methods) throws NoSuchMethodException {
        Map<T, Class<?>[]> map = new HashMap<T, Class<?>[]>();

        T oldMethod = null;
        Class<?>[] oldParams = null;
        boolean ambiguous = false;

        for (T newMethod : methods) {
            if (isValid(newMethod)) {
                Class<?>[] newParams = newMethod.getParameterTypes();
                if (newParams.length == this.args.length) {
                    PrimitiveWrapperMap.replacePrimitivesWithWrappers(newParams);
                    if (isAssignable(newParams, this.args)) {
                        if (oldMethod == null) {
                            oldMethod = newMethod;
                            oldParams = newParams;
                        } else {
                            boolean useNew = isAssignable(oldParams, newParams);
                            boolean useOld = isAssignable(newParams, oldParams);

                            if (useOld && useNew) {
                                // only if parameters are equal
                                useNew = !newMethod.isSynthetic();
                                useOld = !oldMethod.isSynthetic();
                            }
                            if (useOld == useNew) {
                                ambiguous = true;
                            } else if (useNew) {
                                oldMethod = newMethod;
                                oldParams = newParams;
                                ambiguous = false;
                            }
                        }
                    }
                }
                if (newMethod.isVarArgs()) {
                    int length = newParams.length - 1;
                    if (length <= this.args.length) {
                        Class<?>[] array = new Class<?>[this.args.length];
                        System.arraycopy(newParams, 0, array, 0, length);
                        if (length < this.args.length) {
                            Class<?> type = newParams[length].getComponentType();
                            if (type.isPrimitive()) {
                                type = PrimitiveWrapperMap.getType(type.getName());
                            }
                            for (int i = length; i < this.args.length; i++) {
                                array[i] = type;
                            }
                        }
                        map.put(newMethod, array);
                    }
                }
            }
        }
        for (T newMethod : methods) {
            Class<?>[] newParams = map.get(newMethod);
            if (newParams != null) {
                if (isAssignable(newParams, this.args)) {
                    if (oldMethod == null) {
                        oldMethod = newMethod;
                        oldParams = newParams;
                    } else {
                        boolean useNew = isAssignable(oldParams, newParams);
                        boolean useOld = isAssignable(newParams, oldParams);

                        if (useOld && useNew) {
                            // only if parameters are equal
                            useNew = !newMethod.isSynthetic();
                            useOld = !oldMethod.isSynthetic();
                        }
                        if (useOld == useNew) {
                            if (oldParams == map.get(oldMethod)) {
                                ambiguous = true;
                            }
                        } else if (useNew) {
                            oldMethod = newMethod;
                            oldParams = newParams;
                            ambiguous = false;
                        }
                    }
                }
            }
        }

        if (ambiguous) {
            throw new NoSuchMethodException("Ambiguous methods are found");
        }
        if (oldMethod == null) {
            throw new NoSuchMethodException("Method is not found");
        }
        return oldMethod;
    }

    /**
     * Determines if every class in {@code min} array is either the same as,
     * or is a superclass of, the corresponding class in {@code max} array.
     * The length of every array must equal the number of arguments.
     * This comparison is performed in the {@link #find} method
     * before the first call of the isAssignable method.
     * If an argument equals {@code null}
     * the appropriate pair of classes does not take into consideration.
     *
     * @param min  the array of classes to be checked
     * @param max  the array of classes that is used to check
     * @return {@code true} if all classes in {@code min} array
     *         are assignable from corresponding classes in {@code max} array,
     *         {@code false} otherwise
     *
     * @see Class#isAssignableFrom
     */
    private boolean isAssignable(Class<?>[] min, Class<?>[] max) {
        for (int i = 0; i < this.args.length; i++) {
            if (null != this.args[i]) {
                if (!min[i].isAssignableFrom(max[i])) {
                    return false;
                }
            }
        }
        return true;
    }
}
