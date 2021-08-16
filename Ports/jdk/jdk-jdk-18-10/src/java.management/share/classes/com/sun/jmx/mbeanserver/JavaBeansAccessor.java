/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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
package com.sun.jmx.mbeanserver;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import jdk.internal.access.JavaBeansAccess;
import jdk.internal.access.SharedSecrets;

/**
 * A centralized place for gaining access to java.beans related functionality -
 * if available.
 */
class JavaBeansAccessor {
    static {
        // ensure that java.beans.Introspector is initialized (if present)
        // it will fill in the SharedSecrets
        try {
            Class.forName("java.beans.Introspector", true,
                          JavaBeansAccessor.class.getClassLoader());
        } catch (ClassNotFoundException ignore) { }
    }

    private static JavaBeansAccess getJavaBeansAccess() {
        return SharedSecrets.getJavaBeansAccess();
    }

    static boolean isAvailable() {
        return getJavaBeansAccess() != null;
    }

    /**
     * Returns the getter method for a property of the given name
     * @param clazz The JavaBeans class
     * @param property The property name
     * @return The resolved property getter name or null
     * @throws Exception
     */
    static Method getReadMethod(Class<?> clazz, String property) throws Exception {
        JavaBeansAccess jba = getJavaBeansAccess();
        return jba != null ? jba.getReadMethod(clazz, property) : null;
    }

    /**
     * Return the <b>value</b> attribute of the associated
     * <code>@ConstructorProperties</code> annotation if that is present.
     * @param ctr The constructor to extract the annotation value from
     * @return The {@code value} attribute of the <code>@ConstructorProperties</code>
     *         annotation or {@code null} if the constructor is not annotated by
     *         this annotation or the annotation is not accessible.
     */
    static String[] getConstructorPropertiesValue(Constructor<?> ctr) {
        JavaBeansAccess jba = getJavaBeansAccess();
        return jba != null ? jba.getConstructorPropertiesValue(ctr) : null;
    }
}
