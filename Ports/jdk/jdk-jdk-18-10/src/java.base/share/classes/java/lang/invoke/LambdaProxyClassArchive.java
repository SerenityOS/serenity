/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 */
package java.lang.invoke;

import jdk.internal.loader.BuiltinClassLoader;
import jdk.internal.misc.CDS;

final class LambdaProxyClassArchive {
    /**
     * Check if the class is loaded by a built-in class loader.
     */
    static boolean loadedByBuiltinLoader(Class<?> cls) {
        ClassLoader cl = cls.getClassLoader();
        return (cl == null || (cl instanceof BuiltinClassLoader)) ? true : false;
    }

    private static native void addToArchive(Class<?> caller,
                                            String interfaceMethodName,
                                            MethodType factoryType,
                                            MethodType interfaceMethodType,
                                            MemberName implementationMember,
                                            MethodType dynamicMethodType,
                                            Class<?> lambdaProxyClass);

    private static native Class<?> findFromArchive(Class<?> caller,
                                                   String interfaceMethodName,
                                                   MethodType factoryType,
                                                   MethodType interfaceMethodType,
                                                   MemberName implementationMember,
                                                   MethodType dynamicMethodType);

    /**
     * Registers the lambdaProxyClass into CDS archive.
     * The VM will store the lambdaProxyClass into a hash table
     * using the first six argumennts as the key.
     *
     * CDS only archives lambda proxy class if it's not serializable
     * and no marker interfaces and no additional bridges, and if it is
     * loaded by a built-in class loader.
     */
    static boolean register(Class<?> caller,
                            String interfaceMethodName,
                            MethodType factoryType,
                            MethodType interfaceMethodType,
                            MethodHandle implementation,
                            MethodType dynamicMethodType,
                            boolean isSerializable,
                            Class<?>[] altInterfaces,
                            MethodType[] altMethods,
                            Class<?> lambdaProxyClass) {
        if (!CDS.isDumpingArchive())
            throw new IllegalStateException("should only register lambda proxy class at dump time");

        if (loadedByBuiltinLoader(caller) &&
            !isSerializable && altInterfaces.length == 0 && altMethods.length == 0) {
            addToArchive(caller, interfaceMethodName, factoryType, interfaceMethodType,
                         implementation.internalMemberName(), dynamicMethodType,
                         lambdaProxyClass);
            return true;
        }
        return false;
    }

    /**
     * Lookup a lambda proxy class from the CDS archive using the first
     * six arguments as the key.
     *
     * CDS only archives lambda proxy class if it's not serializable
     * and no marker interfaces and no additional bridges, and if it is
     * loaded by a built-in class loader.
     */
    static Class<?> find(Class<?> caller,
                         String interfaceMethodName,
                         MethodType factoryType,
                         MethodType interfaceMethodType,
                         MethodHandle implementation,
                         MethodType dynamicMethodType,
                         boolean isSerializable,
                         Class<?>[] altInterfaces,
                         MethodType[] altMethods) {
        if (CDS.isDumpingArchive())
            throw new IllegalStateException("cannot load class from CDS archive at dump time");

        if (!loadedByBuiltinLoader(caller) ||
            !CDS.isSharingEnabled() || isSerializable || altInterfaces.length > 0 || altMethods.length > 0)
            return null;

        return findFromArchive(caller, interfaceMethodName, factoryType, interfaceMethodType,
                               implementation.internalMemberName(), dynamicMethodType);
    }
}
