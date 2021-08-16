/*
 * Copyright (c) 2006, 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.util.HashMap;
import java.util.Map;

/**
 * This utility class associates
 * name of primitive type with appropriate class.
 *
 * @since 1.7
 *
 * @author Sergey A. Malenkov
 */
final class PrimitiveTypeMap {

    /**
     * Returns primitive type class by its name.
     *
     * @param name  the name of primitive type
     * @return found primitive type class,
     *         or {@code null} if not found
     */
    static Class<?> getType(String name) {
        return map.get(name);
    }

    private static final Map<String, Class<?>> map = new HashMap<String, Class<?>>(9);

    static {
        map.put(boolean.class.getName(), boolean.class);
        map.put(char.class.getName(), char.class);
        map.put(byte.class.getName(), byte.class);
        map.put(short.class.getName(), short.class);
        map.put(int.class.getName(), int.class);
        map.put(long.class.getName(), long.class);
        map.put(float.class.getName(), float.class);
        map.put(double.class.getName(), double.class);
        map.put(void.class.getName(), void.class);
    }

    /**
     * Disable instantiation.
     */
    private PrimitiveTypeMap() {
    }
}
