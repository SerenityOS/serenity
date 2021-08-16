/*
 * Copyright (c) 2016, 2021, Oracle and/or its affiliates. All rights reserved.
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
package java.lang;

import jdk.internal.reflect.ReflectionFactory;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * A collection of most specific public methods. Methods are added to it using
 * {@link #merge(Method)} method. Only the most specific methods for a
 * particular signature are kept.
 */
final class PublicMethods {

    /**
     * a map of (method name, parameter types) -> linked list of Method(s)
     */
    private final Map<Key, MethodList> map = new LinkedHashMap<>();

    /**
     * keeps track of the number of collected methods
     */
    private int methodCount;

    /**
     * Merges new method with existing methods. New method is either
     * ignored (if a more specific method with same signature exists) or added
     * to the collection. When it is added to the collection, it may replace one
     * or more existing methods with same signature if they are less specific
     * than added method.
     * See comments in code...
     */
    void merge(Method method) {
        Key key = new Key(method);
        MethodList existing = map.get(key);
        int xLen = existing == null ? 0 : existing.length();
        MethodList merged = MethodList.merge(existing, method);
        methodCount += merged.length() - xLen;
        // replace if head of list changed
        if (merged != existing) {
            map.put(key, merged);
        }
    }

    /**
     * Dumps methods to array.
     */
    Method[] toArray() {
        Method[] array = new Method[methodCount];
        int i = 0;
        for (MethodList ml : map.values()) {
            for (; ml != null; ml = ml.next) {
                array[i++] = ml.method;
            }
        }
        return array;
    }

    /**
     * Method (name, parameter types) tuple.
     */
    private static final class Key {
        @SuppressWarnings("removal")
        private static final ReflectionFactory reflectionFactory =
            AccessController.doPrivileged(
                new ReflectionFactory.GetReflectionFactoryAction());

        private final String name; // must be interned (as from Method.getName())
        private final Class<?>[] ptypes;

        Key(Method method) {
            name = method.getName();
            ptypes = reflectionFactory.getExecutableSharedParameterTypes(method);
        }

        static boolean matches(Method method,
                               String name, // may not be interned
                               Class<?>[] ptypes) {
            return method.getName().equals(name) &&
                   Arrays.equals(
                       reflectionFactory.getExecutableSharedParameterTypes(method),
                       ptypes
                   );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            //noinspection StringEquality (guaranteed interned String(s))
            return (o instanceof Key that)
                    && name == that.name
                    && Arrays.equals(ptypes, that.ptypes);
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(name) + // guaranteed interned String
                   31 * Arrays.hashCode(ptypes);
        }
    }

    /**
     * Node of a inked list containing Method(s) sharing the same
     * (name, parameter types) tuple.
     */
    static final class MethodList {
        Method method;
        MethodList next;

        private MethodList(Method method) {
            this.method = method;
        }

        /**
         * @return the head of a linked list containing given {@code methods}
         *         filtered by given method {@code name}, parameter types
         *         {@code ptypes} and including or excluding static methods as
         *         requested by {@code includeStatic} flag.
         */
        static MethodList filter(Method[] methods, String name,
                                 Class<?>[] ptypes, boolean includeStatic) {
            MethodList head = null, tail = null;
            for (Method method : methods) {
                if ((includeStatic || !Modifier.isStatic(method.getModifiers())) &&
                    Key.matches(method, name, ptypes)) {
                    if (tail == null) {
                        head = tail = new MethodList(method);
                    } else {
                        tail = tail.next = new MethodList(method);
                    }
                }
            }
            return head;
        }

        /**
         * This method should only be called with the {@code head} (possibly null)
         * of a list of Method(s) that share the same (method name, parameter types)
         * and another {@code methodList} that also contains Method(s) with the
         * same and equal (method name, parameter types) as the 1st list.
         * It modifies the 1st list and returns the head of merged list
         * containing only the most specific methods for each signature
         * (i.e. return type). The returned head of the merged list may or
         * may not be the same as the {@code head} of the given list.
         * The given {@code methodList} is not modified.
         */
        static MethodList merge(MethodList head, MethodList methodList) {
            for (MethodList ml = methodList; ml != null; ml = ml.next) {
                head = merge(head, ml.method);
            }
            return head;
        }

        private static MethodList merge(MethodList head, Method method) {
            Class<?> dclass = method.getDeclaringClass();
            Class<?> rtype = method.getReturnType();
            MethodList prev = null;
            for (MethodList l = head; l != null; l = l.next) {
                // eXisting method
                Method xmethod = l.method;
                // only merge methods with same signature:
                // (return type, name, parameter types) tuple
                // as we only keep methods with same (name, parameter types)
                // tuple together in one list, we only need to check return type
                if (rtype == xmethod.getReturnType()) {
                    Class<?> xdclass = xmethod.getDeclaringClass();
                    if (dclass.isInterface() == xdclass.isInterface()) {
                        // both methods are declared by interfaces
                        // or both by classes
                        if (dclass.isAssignableFrom(xdclass)) {
                            // existing method is the same or overrides
                            // new method - ignore new method
                            return head;
                        }
                        if (xdclass.isAssignableFrom(dclass)) {
                            // new method overrides existing
                            // method - knock out existing method
                            if (prev != null) {
                                prev.next = l.next;
                            } else {
                                head = l.next;
                            }
                            // keep iterating
                        } else {
                            // unrelated (should only happen for interfaces)
                            prev = l;
                            // keep iterating
                        }
                    } else if (dclass.isInterface()) {
                        // new method is declared by interface while
                        // existing method is declared by class -
                        // ignore new method
                        return head;
                    } else /* xdclass.isInterface() */ {
                        // new method is declared by class while
                        // existing method is declared by interface -
                        // knock out existing method
                        if (prev != null) {
                            prev.next = l.next;
                        } else {
                            head = l.next;
                        }
                        // keep iterating
                    }
                } else {
                    // distinct signatures
                    prev = l;
                    // keep iterating
                }
            }
            // append new method to the list
            if (prev == null) {
                head = new MethodList(method);
            } else {
                prev.next = new MethodList(method);
            }
            return head;
        }

        private int length() {
            int len = 1;
            for (MethodList ml = next; ml != null; ml = ml.next) {
                len++;
            }
            return len;
        }

        /**
         * @return 1st method in list with most specific return type
         */
        Method getMostSpecific() {
            Method m = method;
            Class<?> rt = m.getReturnType();
            for (MethodList ml = next; ml != null; ml = ml.next) {
                Method m2 = ml.method;
                Class<?> rt2 = m2.getReturnType();
                if (rt2 != rt && rt.isAssignableFrom(rt2)) {
                    // found more specific return type
                    m = m2;
                    rt = rt2;
                }
            }
            return m;
        }
    }
}
