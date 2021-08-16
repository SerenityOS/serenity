/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
package jdk.dynalink.internal;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import jdk.dynalink.linker.support.TypeUtilities;

/**
 * Various static utility methods for testing type relationships; internal to Dynalink.
 */
public class InternalTypeUtilities {
    private InternalTypeUtilities() {
    }

    /**
     * Returns true if either of the types is assignable from the other.
     * @param c1 one type
     * @param c2 another type
     * @return true if either c1 is assignable from c2 or c2 is assignable from c1.
     */
    public static boolean areAssignable(final Class<?> c1, final Class<?> c2) {
        return c1.isAssignableFrom(c2) || c2.isAssignableFrom(c1);
    }

    /**
     * Return true if it is safe to strongly reference a class from the referred
     * class loader from a class associated with the referring class loader
     * without risking a class loader memory leak. Generally, it is only safe
     * to reference classes from the same or ancestor class loader. {@code null}
     * indicates the system class loader; classes from it can always be
     * directly referenced, and it can only directly reference classes from
     * itself. This method can be used by language runtimes to ensure they are
     * using weak references in their linkages when they need to link to methods
     * in unrelated class loaders.
     *
     * @param referrerLoader the referrer class loader.
     * @param referredLoader the referred class loader
     * @return true if it is safe to strongly reference the class from referred
     * in referred.
     * @throws SecurityException if the caller does not have the
     * {@code RuntimePermission("getClassLoader")} permission and the method
     * needs to traverse the parent class loader chain.
     */
    public static boolean canReferenceDirectly(final ClassLoader referrerLoader, final ClassLoader referredLoader) {
        if(referredLoader == null) {
            // Can always refer directly to a system class
            return true;
        }
        if(referrerLoader == null) {
            // System classes can't refer directly to any non-system class
            return false;
        }
        // Otherwise, can only refer directly to classes residing in same or
        // parent class loader.

        ClassLoader referrer = referrerLoader;
        do {
            if(referrer == referredLoader) {
                return true;
            }
            referrer = referrer.getParent();
        } while(referrer != null);
        return false;
    }

    /**
     * Given two types represented by c1 and c2, returns a type that is their
     * most specific common supertype for purposes of lossless conversions.
     *
     * @param c1 one type
     * @param c2 another type
     * @return their most common superclass or superinterface for purposes of
     * lossless conversions. If they have several unrelated superinterfaces as
     * their most specific common type, or the types themselves are completely
     * unrelated interfaces, {@link java.lang.Object} is returned.
     */
    public static Class<?> getCommonLosslessConversionType(final Class<?> c1, final Class<?> c2) {
        if(c1 == c2) {
            return c1;
        } else if (c1 == void.class || c2 == void.class) {
            return Object.class;
        } else if(TypeUtilities.isConvertibleWithoutLoss(c2, c1)) {
            return c1;
        } else if(TypeUtilities.isConvertibleWithoutLoss(c1, c2)) {
            return c2;
        } else if(c1.isPrimitive() && c2.isPrimitive()) {
            if((c1 == byte.class && c2 == char.class) || (c1 == char.class && c2 == byte.class)) {
                // byte + char = int
                return int.class;
            } else if((c1 == short.class && c2 == char.class) || (c1 == char.class && c2 == short.class)) {
                // short + char = int
                return int.class;
            } else if((c1 == int.class && c2 == float.class) || (c1 == float.class && c2 == int.class)) {
                // int + float = double
                return double.class;
            }
        }
        // For all other cases. This will handle long + (float|double) = Number case as well as boolean + anything = Object case too.
        return getMostSpecificCommonTypeUnequalNonprimitives(c1, c2);
    }

    private static Class<?> getMostSpecificCommonTypeUnequalNonprimitives(final Class<?> c1, final Class<?> c2) {
        final Class<?> npc1 = c1.isPrimitive() ? TypeUtilities.getWrapperType(c1) : c1;
        final Class<?> npc2 = c2.isPrimitive() ? TypeUtilities.getWrapperType(c2) : c2;
        final Set<Class<?>> a1 = getAssignables(npc1, npc2);
        final Set<Class<?>> a2 = getAssignables(npc2, npc1);
        a1.retainAll(a2);
        if(a1.isEmpty()) {
            // Can happen when at least one of the arguments is an interface,
            // as they don't have Object at the root of their hierarchy.
            return Object.class;
        }
        // Gather maximally specific elements. Yes, there can be more than one
        // thank to interfaces. I.e., if you call this method for String.class
        // and Number.class, you'll have Comparable, Serializable, and Object
        // as maximal elements.
        final List<Class<?>> max = new ArrayList<>();
        outer: for(final Class<?> clazz: a1) {
            for(final Iterator<Class<?>> maxiter = max.iterator(); maxiter.hasNext();) {
                final Class<?> maxClazz = maxiter.next();
                if(TypeUtilities.isSubtype(maxClazz, clazz)) {
                    // It can't be maximal, if there's already a more specific
                    // maximal than it.
                    continue outer;
                }
                if(TypeUtilities.isSubtype(clazz, maxClazz)) {
                    // If it's more specific than a currently maximal element,
                    // that currently maximal is no longer a maximal.
                    maxiter.remove();
                }
            }
            // If we get here, no current maximal is more specific than the
            // current class, so it is considered maximal as well
            max.add(clazz);
        }
        if(max.size() > 1) {
            return Object.class;
        }
        return max.get(0);
    }

    private static Set<Class<?>> getAssignables(final Class<?> c1, final Class<?> c2) {
        final Set<Class<?>> s = new HashSet<>();
        collectAssignables(c1, c2, s);
        return s;
    }

    private static void collectAssignables(final Class<?> c1, final Class<?> c2, final Set<Class<?>> s) {
        if(c1.isAssignableFrom(c2)) {
            s.add(c1);
        }
        final Class<?> sc = c1.getSuperclass();
        if(sc != null) {
            collectAssignables(sc, c2, s);
        }
        for(final Class<?> itf: c1.getInterfaces()) {
            collectAssignables(itf, c2, s);
        }
    }
}
