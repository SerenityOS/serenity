/*
 * Copyright (c) 2005, 2015, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.jittester.utils;

import jdk.test.lib.jittester.BuiltInType;
import jdk.test.lib.jittester.Type;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utility functions for type system
 */
public class TypeUtil {
    /**
     * Gets a list of implicitly castable types to a given one from the collection of types
     *
     * @param types a collection to get types from
     * @param type  a target type which result type could be implicitly cast to
     * @return      a result collection of types that match given conditions
     */
    public static Collection<Type> getImplicitlyCastable(Collection<Type> types, Type type) {
        return types.stream()
                .filter(t -> t.canImplicitlyCastTo(type))
                .collect(Collectors.toList());
    }

    /**
     * Gets a list of explicitly castable types to a given one from the collection of types
     *
     * @param types a collection to get types from
     * @param type  a target type which result type could be explicitly cast to
     * @return      a result collection of types that match given conditions
     */
    public static Collection<Type> getExplicitlyCastable(Collection<Type> types, Type type) {
        return types.stream()
                .filter(t -> t.canExplicitlyCastTo(type))
                .collect(Collectors.toList());
    }

    /**
     * Gets a list of more capacious types than a given one from the collection of types
     *
     * @param types a collection to get types from
     * @param type  a type to filter given types by capacity
     * @return      a result collection of types that match given conditions
     */
    public static List<Type> getMoreCapaciousThan(Collection<Type> types, BuiltInType type) {
        return types.stream()
                .filter(t -> ((BuiltInType) t).isMoreCapaciousThan(type))
                .collect(Collectors.toList());
    }

    /**
     * Gets a list of less or equal capacious types than a given one from the collection of types
     *
     * @param types a collection to get types from
     * @param type  a type to filter given types by capacity
     * @return      a result collection of types that match given conditions
     */
    public static List<Type> getLessCapaciousOrEqualThan(Collection<Type> types, BuiltInType type) {
        return types.stream()
                .filter(t -> !((BuiltInType) t).isMoreCapaciousThan(type) || t.equals(type))
                .collect(Collectors.toList());
    }
}
