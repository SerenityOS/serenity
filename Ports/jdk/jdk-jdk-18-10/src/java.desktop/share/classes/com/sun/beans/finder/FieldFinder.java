/*
 * Copyright (c) 2008, 2012, Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static sun.reflect.misc.ReflectUtil.isPackageAccessible;

/**
 * This utility class provides {@code static} methods
 * to find a public field with specified name
 * in specified class.
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
public final class FieldFinder {

    /**
     * Finds public field (static or non-static)
     * that is declared in public class.
     *
     * @param type  the class that can have field
     * @param name  the name of field to find
     * @return object that represents found field
     * @throws NoSuchFieldException if field is not found
     * @see Class#getField
     */
    public static Field findField(Class<?> type, String name) throws NoSuchFieldException {
        if (name == null) {
            throw new IllegalArgumentException("Field name is not set");
        }
        if (!FinderUtils.isExported(type)) {
            throw new NoSuchFieldException("Field '" + name + "' is not accessible");
        }
        Field field = type.getField(name);
        if (!Modifier.isPublic(field.getModifiers())) {
            throw new NoSuchFieldException("Field '" + name + "' is not public");
        }
        type = field.getDeclaringClass();
        if (!Modifier.isPublic(type.getModifiers()) || !isPackageAccessible(type)) {
            throw new NoSuchFieldException("Field '" + name + "' is not accessible");
        }
        return field;
    }

    /**
     * Finds public non-static field
     * that is declared in public class.
     *
     * @param type  the class that can have field
     * @param name  the name of field to find
     * @return object that represents found field
     * @throws NoSuchFieldException if field is not found
     * @see Class#getField
     */
    public static Field findInstanceField(Class<?> type, String name) throws NoSuchFieldException {
        Field field = findField(type, name);
        if (Modifier.isStatic(field.getModifiers())) {
            throw new NoSuchFieldException("Field '" + name + "' is static");
        }
        return field;
    }

    /**
     * Finds public static field
     * that is declared in public class.
     *
     * @param type  the class that can have field
     * @param name  the name of field to find
     * @return object that represents found field
     * @throws NoSuchFieldException if field is not found
     * @see Class#getField
     */
    public static Field findStaticField(Class<?> type, String name) throws NoSuchFieldException {
        Field field = findField(type, name);
        if (!Modifier.isStatic(field.getModifiers())) {
            throw new NoSuchFieldException("Field '" + name + "' is not static");
        }
        return field;
    }

    /**
     * Disable instantiation.
     */
    private FieldFinder() {
    }
}
