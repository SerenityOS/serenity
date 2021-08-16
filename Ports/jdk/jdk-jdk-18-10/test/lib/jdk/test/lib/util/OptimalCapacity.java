/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;

/**
 * Utility functions to check that the static storages are pre-sized
 * optimally.
 */
public final class OptimalCapacity {

    private OptimalCapacity() {}

    /**
     * Checks adequacy of the initial capacity of a static field
     * of type {@code ArrayList}.
     *
     * Having
     * <pre>
     * class XClass {
     *     static ArrayList theList = new ArrayList(N);
     * }
     * </pre>
     *
     * you should call from the test
     *
     * <pre>
     * OptimalCapacity.assertProperlySized(XClass.class, "theList", N);
     * </pre>
     */
    public static void ofArrayList(Class<?> clazz, String fieldName,
            int initialCapacity)
    {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object obj = field.get(null);
            if (!ArrayList.class.equals(obj.getClass())) {
                throw new RuntimeException("'" + field +
                    "' expected to be of type ArrayList");
            }
            ArrayList<?> list = (ArrayList<?>)obj;

            // For ArrayList the optimal capacity is its final size
            if (list.size() != initialCapacity) {
                throw new RuntimeException("Size of '" + field +
                    "' is " + list.size() +
                    ", but expected to be " + initialCapacity);
            }
            if (internalArraySize(list) != initialCapacity) {
                throw new RuntimeException("Capacity of '" + field +
                    "' is " + internalArraySize(list) +
                    ", but expected to be " + initialCapacity);
            }
        } catch (ReflectiveOperationException roe) {
            throw new RuntimeException(roe);
        }
    }

    /**
     * Checks adequacy of the initial capacity of a static field
     * of type {@code HashMap}.
     *
     * Having
     * <pre>
     * class XClass {
     *     static HashMap theMap = new HashMap(N);
     * }
     * </pre>
     *
     * you should call from the test
     *
     * <pre>
     * OptimalCapacity.ofHashMap(XClass.class, "theMap", N);
     * </pre>
     */
    public static void ofHashMap(Class<?> clazz, String fieldName,
            int initialCapacity)
    {
        ofHashMap(clazz, null, fieldName, initialCapacity);
    }

    /**
     * Checks adequacy of the initial capacity of a non-static field
     * of type {@code HashMap}.
     *
     * Having
     * <pre>
     * class XClass {
     *     HashMap theMap = new HashMap(N);
     * }
     * XClass instance = ...
     * </pre>
     *
     * you should call from the test
     *
     * <pre>
     * OptimalCapacity.ofHashMap(XClass.class, instance, "theMap", N);
     * </pre>
     */
    public static void ofHashMap(Class<?> clazz, Object instance,
            String fieldName, int initialCapacity)
    {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object obj = field.get(instance);
            if (!HashMap.class.equals(obj.getClass())) {
                throw new RuntimeException(field +
                    " expected to be of type HashMap");
            }
            HashMap<?,?> map = (HashMap<?,?>)obj;

            // Check that the map allocates only necessary amount of space
            HashMap<Object, Object> tmp = new HashMap<>(map);
            if (internalArraySize(map) != internalArraySize(tmp)) {
                throw new RuntimeException("Final capacity of '" + field +
                    "' is " + internalArraySize(map) +
                    ", which exceeds necessary minimum " + internalArraySize(tmp));
            }

            // Check that map is initially properly sized
            tmp = new HashMap<>(initialCapacity);
            tmp.put(new Object(), new Object()); // trigger storage init
            if (internalArraySize(map) != internalArraySize(tmp)) {
                throw new RuntimeException("Requested capacity of '" + field +
                    "' was " + initialCapacity +
                    ", which resulted in final capacity " + internalArraySize(tmp) +
                    ", which differs from necessary minimum " + internalArraySize(map));
            }

        } catch (ReflectiveOperationException roe) {
            throw new RuntimeException(roe);
        }
    }

    /**
     * Checks adequacy of the expected maximum size of a static field
     * of type {@code IdentityHashMap}.
     *
     * Having
     * <pre>
     * class XClass {
     *     static IdentityHashMap theMap = new IdentityHashMap(M);
     * }
     * </pre>
     *
     * you should call from the test
     *
     * <pre>
     * OptimalCapacity.ofIdentityHashMap(XClass.class, "theMap", M);
     * </pre>
     */
    public static void ofIdentityHashMap(Class<?> clazz, String fieldName,
            int expectedMaxSize)
    {
        try {
            Field field = clazz.getDeclaredField(fieldName);
            field.setAccessible(true);
            Object obj = field.get(null);
            if (!IdentityHashMap.class.equals(obj.getClass())) {
                throw new RuntimeException("'" + field +
                    "' expected to be of type IdentityHashMap");
            }
            IdentityHashMap<?,?> map = (IdentityHashMap<?,?>)obj;

            // Check that size of map is what was expected
            if (map.size() != expectedMaxSize) {
                throw new RuntimeException("Size of '" + field +
                    "' is " + map.size() +
                    ", which differs from expected " + expectedMaxSize);
            }

            // Check that the map allocated only necessary amount of memory
            IdentityHashMap<Object, Object> tmp = new IdentityHashMap<>(map);
            if (internalArraySize(map) != internalArraySize(tmp)) {
                throw new RuntimeException("Final capacity of '" + field +
                    "' is " + internalArraySize(map) +
                    ", which exceeds necessary minimum " + internalArraySize(tmp));
            }

            // Check that map was initially properly sized
            tmp = new IdentityHashMap<>(expectedMaxSize);
            tmp.put(new Object(), new Object()); // trigger storage init
            if (internalArraySize(map) != internalArraySize(tmp)) {
                throw new RuntimeException("Requested number of elements in '" + field +
                    "' was " + expectedMaxSize +
                    ", which resulted in final capacity " + internalArraySize(tmp) +
                    ", which differs from necessary minimum " + internalArraySize(map));
            }
        } catch (ReflectiveOperationException roe) {
            throw new RuntimeException(roe);
        }
    }

    /**
     * Returns size of the internal storage.
     */
    private static int internalArraySize(Object container)
            throws ReflectiveOperationException {
        Field field;
        if (ArrayList.class.equals(container.getClass())) {
            field = ArrayList.class.getDeclaredField("elementData");
        } else if (HashMap.class.equals(container.getClass())) {
            field = HashMap.class.getDeclaredField("table");
        } else if (IdentityHashMap.class.equals(container.getClass())) {
            field = IdentityHashMap.class.getDeclaredField("table");
        } else {
            throw new RuntimeException("Unexpected class " +
                    container.getClass());
        }
        field.setAccessible(true);
        return ((Object[])field.get(container)).length;
    }
}
