/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.dynalink;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import jdk.dynalink.internal.AccessControlContextFactory;

import static jdk.dynalink.internal.InternalTypeUtilities.canReferenceDirectly;

/**
 * Similar to ClassValue, but lazily associates a computed value with
 * (potentially) every pair of types.
 * @param <T> the value to associate with pairs of types.
 */
final class BiClassValue<T> {
    /**
     * Creates a new BiClassValue that uses the specified binary function to
     * lazily compute the values.
     * @param compute the binary function to compute the values. Ordinarily, it
     *                is invoked at most once for any pair of values. However,
     *                it is possible for it to be invoked concurrently. It can
     *                even be invoked concurrently multiple times for the same
     *                arguments under contention. In that case, it is undefined
     *                which of the computed values will be retained therefore
     *                returning semantically equivalent values is strongly
     *                recommended; the function should ideally be pure.
     *                Additionally, if the pair of types passed as parameters
     *                are from unrelated class loaders, the computed value is
     *                not cached at all and the function might be reinvoked
     *                with the same parameters in the future. Finally, a null
     *                return value is allowed, but not cached.
     * @param <T> the type of the values
     * @return a new BiClassValue that computes the values using the passed
     * function.
     */
    static <T> BiClassValue<T> computing(final BiFunction<Class<?>, Class<?>, T> compute) {
        return new BiClassValue<>(compute);
    }

    /**
     * A type-specific map that stores the values specific to pairs of types
     * which include its class in one of the positions of the pair. Internally,
     * it uses at most two maps named "forward" and "reverse". A BiClassValues
     * for class C1 can store values for (C1, Cy) in its forward map as well
     * as values for (Cx, C1) in its reverse map. The reason for this scheme
     * is to avoid creating unwanted strong references from a parent class
     * loader to a child class loader. If for a pair of classes (C1, C2)
     * either C1 and C2 are in the same class loader, or C2 is in parent of C1,
     * or C2 is a system class, forward map of C1's BiClassValues is used for
     * storing the computed value. If C1 is in parent of C2, or C1 is a system
     * class, reverse map of C2's BiClassValues is used for storing. If the
     * class loaders are unrelated, the computed value is not cached and will
     * be recomputed on every evaluation.
     * NOTE that while every instance of this class is type-specific, it does
     * not store a reference to the type Class object itself. BiClassValuesRoot
     * creates the association from a type Class object to its BiClassValues'.
     * @param <T> the type of the values
     */
    private final static class BiClassValues<T> {
        // These will be used for compareAndExchange on forward and reverse fields.
        private static final VarHandle FORWARD;
        private static final VarHandle REVERSE;
        static {
            final MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                FORWARD = lookup.findVarHandle(BiClassValues.class, "forward", Map.class);
                REVERSE = lookup.findVarHandle(BiClassValues.class, "reverse", Map.class);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new AssertionError(e);
            }
        }

        private Map<Class<?>, T> forward = Map.of();
        private Map<Class<?>, T> reverse = Map.of();

        T getForwardValue(final Class<?> c) {
            return forward.get(c);
        }

        T getReverseValue(final Class<?> c) {
            return reverse.get(c);
        }

        private T compute(final VarHandle mapHandle, final Class<?> c, final Function<Class<?>, T> compute) {
            @SuppressWarnings("unchecked")
            Map<Class<?>, T> map = (Map<Class<?>, T>) mapHandle.getVolatile(this);
            T value;
            T newValue = null;
            while ((value = map.get(c)) == null) {
                if (newValue == null) {
                    newValue = compute.apply(c);
                    if (newValue == null) {
                        break;
                    }
                }
                @SuppressWarnings({"unchecked", "rawtypes"})
                final Map.Entry<Class<?>, T>[] entries = map.entrySet().toArray(new Map.Entry[map.size() + 1]);
                entries[map.size()] = Map.entry(c, newValue);
                final var newMap = Map.ofEntries(entries);
                @SuppressWarnings("unchecked")
                final var witness = (Map<Class<?>, T>) mapHandle.compareAndExchange(this, map, newMap);
                if (witness == map) {
                    value = newValue;
                    break;
                }
                map = witness;
            }
            return value;
        }

        T computeForward(final Class<?> c, Function<Class<?>, T> compute) {
            return compute(FORWARD, c, compute);
        }

        T computeReverse(final Class<?> c, Function<Class<?>, T> compute) {
            return compute(REVERSE, c, compute);
        }
    }

    // A named class used for "root" field so it can be static so it doesn't
    // gain a synthetic this$0 reference as that'd cause a memory leak through
    // unwanted anchoring to a GC root when used with system classes.
    private static final class BiClassValuesRoot<T> extends ClassValue<BiClassValues<T>> {
        @Override protected BiClassValues<T> computeValue(Class<?> type) {
            return new BiClassValues<>();
        }
    }

    private enum RetentionDirection {
        FORWARD,
        REVERSE,
        NEITHER
    }

    private final BiClassValuesRoot<T> root = new BiClassValuesRoot<>();
    private final BiFunction<Class<?>, Class<?>, T> compute;

    private BiClassValue(final BiFunction<Class<?>, Class<?>, T> compute) {
        this.compute = Objects.requireNonNull(compute);
    }

    final T get(final Class<?> c1, final Class<?> c2) {
        // Most likely case: it is in the forward map of c1's BiClassValues
        final BiClassValues<T> cv1 = root.get(c1);
        final T v1 = cv1.getForwardValue(c2);
        if (v1 != null) {
            return v1;
        }

        // Next likely case: it is in the reverse map of c2's BiClassValues
        final BiClassValues<T> cv2 = root.get(c2);
        final T v2 = cv2.getReverseValue(c1);
        if (v2 != null) {
            return v2;
        }

        // Value is uncached, compute it and cache if possible.
        switch (getRetentionDirection(c1, c2)) {
            case FORWARD:
                // loader of c1 can see loader of c2, store value for (c1, c2) in cv1's forward map
                return cv1.computeForward(c2, cy -> compute.apply(c1, cy));
            case REVERSE:
                // loader of c2 can see loader of c1, store value for (c1, c2) in cv2's reverse map
                return cv2.computeReverse(c1, cx -> compute.apply(cx, c2));
            case NEITHER:
                // Class loaders are unrelated; compute and return uncached.
                return compute.apply(c1, c2);
            default:
                throw new AssertionError(); // enum values exhausted
        }
    }

    @SuppressWarnings("removal")
    private static final AccessControlContext GET_CLASS_LOADER_CONTEXT =
        AccessControlContextFactory.createAccessControlContext("getClassLoader");

    @SuppressWarnings("removal")
    private static RetentionDirection getRetentionDirection(Class<?> from, Class<?> to) {
        return AccessController.doPrivileged((PrivilegedAction<RetentionDirection>) () -> {
            final ClassLoader cl1 = from.getClassLoader();
            final ClassLoader cl2 = to.getClassLoader();
            if (canReferenceDirectly(cl1, cl2)) {
                return RetentionDirection.FORWARD;
            } else if (canReferenceDirectly(cl2, cl1)) {
                return RetentionDirection.REVERSE;
            }
            return RetentionDirection.NEITHER;
        }, GET_CLASS_LOADER_CONTEXT);
    }
}
