/*
 * Copyright (c) 2006, 2012, Oracle and/or its affiliates. All rights reserved.
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

import static sun.reflect.misc.ReflectUtil.checkPackageAccess;

/**
 * This is utility class that provides {@code static} methods
 * to find a class with the specified name using the specified class loader.
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
public final class ClassFinder {

    /**
     * Returns the {@code Class} object associated
     * with the class or interface with the given string name,
     * using the default class loader.
     * <p>
     * The {@code name} can denote an array class
     * (see {@link Class#getName} for details).
     *
     * @param name  fully qualified name of the desired class
     * @return class object representing the desired class
     *
     * @throws ClassNotFoundException  if the class cannot be located
     *                                 by the specified class loader
     *
     * @see Class#forName(String)
     * @see Class#forName(String,boolean,ClassLoader)
     * @see ClassLoader#getSystemClassLoader()
     * @see Thread#getContextClassLoader()
     */
    public static Class<?> findClass(String name) throws ClassNotFoundException {
        checkPackageAccess(name);
        try {
            ClassLoader loader = Thread.currentThread().getContextClassLoader();
            if (loader == null) {
                // can be null in IE (see 6204697)
                loader = ClassLoader.getSystemClassLoader();
            }
            if (loader != null) {
                return Class.forName(name, false, loader);
            }

        } catch (ClassNotFoundException exception) {
            // use current class loader instead
        } catch (SecurityException exception) {
            // use current class loader instead
        }
        return Class.forName(name);
    }

    /**
     * Returns the {@code Class} object associated with
     * the class or interface with the given string name,
     * using the given class loader.
     * <p>
     * The {@code name} can denote an array class
     * (see {@link Class#getName} for details).
     * <p>
     * If the parameter {@code loader} is null,
     * the class is loaded through the default class loader.
     *
     * @param name    fully qualified name of the desired class
     * @param loader  class loader from which the class must be loaded
     * @return class object representing the desired class
     *
     * @throws ClassNotFoundException  if the class cannot be located
     *                                 by the specified class loader
     *
     * @see #findClass(String,ClassLoader)
     * @see Class#forName(String,boolean,ClassLoader)
     */
    public static Class<?> findClass(String name, ClassLoader loader) throws ClassNotFoundException {
        checkPackageAccess(name);
        if (loader != null) {
            try {
                return Class.forName(name, false, loader);
            } catch (ClassNotFoundException exception) {
                // use default class loader instead
            } catch (SecurityException exception) {
                // use default class loader instead
            }
        }
        return findClass(name);
    }

    /**
     * Returns the {@code Class} object associated
     * with the class or interface with the given string name,
     * using the default class loader.
     * <p>
     * The {@code name} can denote an array class
     * (see {@link Class#getName} for details).
     * <p>
     * This method can be used to obtain
     * any of the {@code Class} objects
     * representing {@code void} or primitive Java types:
     * {@code char}, {@code byte}, {@code short},
     * {@code int}, {@code long}, {@code float},
     * {@code double} and {@code boolean}.
     *
     * @param name  fully qualified name of the desired class
     * @return class object representing the desired class
     *
     * @throws ClassNotFoundException  if the class cannot be located
     *                                 by the specified class loader
     *
     * @see #resolveClass(String,ClassLoader)
     */
    public static Class<?> resolveClass(String name) throws ClassNotFoundException {
        return resolveClass(name, null);
    }

    /**
     * Returns the {@code Class} object associated with
     * the class or interface with the given string name,
     * using the given class loader.
     * <p>
     * The {@code name} can denote an array class
     * (see {@link Class#getName} for details).
     * <p>
     * If the parameter {@code loader} is null,
     * the class is loaded through the default class loader.
     * <p>
     * This method can be used to obtain
     * any of the {@code Class} objects
     * representing {@code void} or primitive Java types:
     * {@code char}, {@code byte}, {@code short},
     * {@code int}, {@code long}, {@code float},
     * {@code double} and {@code boolean}.
     *
     * @param name    fully qualified name of the desired class
     * @param loader  class loader from which the class must be loaded
     * @return class object representing the desired class
     *
     * @throws ClassNotFoundException  if the class cannot be located
     *                                 by the specified class loader
     *
     * @see #findClass(String,ClassLoader)
     * @see PrimitiveTypeMap#getType(String)
     */
    public static Class<?> resolveClass(String name, ClassLoader loader) throws ClassNotFoundException {
        Class<?> type = PrimitiveTypeMap.getType(name);
        return (type == null)
                ? findClass(name, loader)
                : type;
    }

    /**
     * Disable instantiation.
     */
    private ClassFinder() {
    }
}
