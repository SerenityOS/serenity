/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
package jdk.vm.ci.hotspot;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

import jdk.vm.ci.common.NativeImageReinitialize;
import jdk.vm.ci.meta.ResolvedJavaType;

/**
 * A cleaner tracks a referent object and includes some {@linkplain #doCleanup() cleanup code} that
 * is run some time after the referent object has become weakly-reachable.
 *
 * This is like {@link java.lang.ref.Cleaner} but with weak semantics instead of phantom. Objects
 * referenced by this might be referenced by {@link ResolvedJavaType} which is kept alive by a
 * {@link WeakReference} so we need equivalent reference strength.
 */
abstract class Cleaner extends WeakReference<Object> {

    /**
     * Head of linked list of cleaners.
     */
    @NativeImageReinitialize private static Cleaner first;

    /**
     * Linked list pointers.
     */
    private Cleaner next = null;
    private Cleaner prev = null;

    Cleaner(Object referent) {
        super(referent, queue);
        add(this);
    }

    private static synchronized Cleaner add(Cleaner cl) {
        if (first != null) {
            clean();
        }
        if (first != null) {
            cl.next = first;
            first.prev = cl;
        }
        first = cl;
        return cl;
    }

    /**
     * Removes {@code cl} from the linked list of cleaners.
     */
    private static synchronized void remove(Cleaner cl) {
        // If already removed, do nothing
        if (cl.next == cl) {
            return;
        }

        // Update list
        if (first == cl) {
            if (cl.next != null) {
                first = cl.next;
            } else {
                first = cl.prev;
            }
        }
        if (cl.next != null) {
            cl.next.prev = cl.prev;
        }
        if (cl.prev != null) {
            cl.prev.next = cl.next;
        }

        // Indicate removal by pointing the cleaner to itself
        cl.next = cl;
        cl.prev = cl;
    }

    /**
     * Performs the cleanup action now that this object's referent has become weakly reachable.
     */
    abstract void doCleanup();

    /**
     * Remove the cleaners whose referents have become weakly reachable.
     */
    static void clean() {
        Cleaner c = (Cleaner) queue.poll();
        while (c != null) {
            remove(c);
            c.doCleanup();
            c = (Cleaner) queue.poll();
        }
    }

    /**
     * The {@link ReferenceQueue} to which {@link Cleaner}s are enqueued once their referents'
     * become unreachable.
     */
    private static final ReferenceQueue<Object> queue = new ReferenceQueue<>();
}
