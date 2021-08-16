/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.misc;

import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;

/**
 * A thread-local variable that is notified when a thread terminates and
 * it has been initialized in the terminating thread (even if it was
 * initialized with a null value).
 */
public class TerminatingThreadLocal<T> extends ThreadLocal<T> {

    @Override
    public void set(T value) {
        super.set(value);
        register(this);
    }

    @Override
    public void remove() {
        super.remove();
        unregister(this);
    }

    /**
     * Invoked by a thread when terminating and this thread-local has an associated
     * value for the terminating thread (even if that value is null), so that any
     * native resources maintained by the value can be released.
     *
     * @param value current thread's value of this thread-local variable
     *              (may be null but only if null value was explicitly initialized)
     */
    protected void threadTerminated(T value) {
    }

    // following methods and field are implementation details and should only be
    // called from the corresponding code int Thread/ThreadLocal class.

    /**
     * Invokes the TerminatingThreadLocal's {@link #threadTerminated()} method
     * on all instances registered in current thread.
     */
    public static void threadTerminated() {
        for (TerminatingThreadLocal<?> ttl : REGISTRY.get()) {
            ttl._threadTerminated();
        }
    }

    private void _threadTerminated() { threadTerminated(get()); }

    /**
     * Register given TerminatingThreadLocal
     *
     * @param tl the ThreadLocal to register
     */
    public static void register(TerminatingThreadLocal<?> tl) {
        REGISTRY.get().add(tl);
    }

    /**
     * Unregister given TerminatingThreadLocal
     *
     * @param tl the ThreadLocal to unregister
     */
    private static void unregister(TerminatingThreadLocal<?> tl) {
        REGISTRY.get().remove(tl);
    }

    /**
     * a per-thread registry of TerminatingThreadLocal(s) that have been registered
     * but later not unregistered in a particular thread.
     */
    public static final ThreadLocal<Collection<TerminatingThreadLocal<?>>> REGISTRY =
        new ThreadLocal<>() {
            @Override
            protected Collection<TerminatingThreadLocal<?>> initialValue() {
                return Collections.newSetFromMap(new IdentityHashMap<>(4));
            }
        };
}
