/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.foreign;

import jdk.incubator.foreign.ResourceScope;
import jdk.internal.vm.annotation.ForceInline;

import java.lang.ref.Cleaner;
import java.lang.ref.Reference;

/**
 * A confined scope, which features an owner thread. The liveness check features an additional
 * confinement check - that is, calling any operation on this scope from a thread other than the
 * owner thread will result in an exception. Because of this restriction, checking the liveness bit
 * can be performed in plain mode.
 */
final class ConfinedScope extends ResourceScopeImpl {

    private boolean closed; // = false
    private int lockCount = 0;
    private final Thread owner;

    public ConfinedScope(Thread owner, Cleaner cleaner) {
        super(cleaner, new ConfinedResourceList());
        this.owner = owner;
    }

    @ForceInline
    public final void checkValidState() {
        if (owner != Thread.currentThread()) {
            throw new IllegalStateException("Attempted access outside owning thread");
        }
        if (closed) {
            throw new IllegalStateException("Already closed");
        }
    }

    @Override
    public boolean isAlive() {
        return !closed;
    }

    @Override
    public HandleImpl acquire() {
        checkValidState();
        lockCount++;
        return new ConfinedHandle();
    }

    void justClose() {
        this.checkValidState();
        if (lockCount == 0) {
            closed = true;
        } else {
            throw new IllegalStateException("Scope is acquired by " + lockCount + " locks");
        }
    }

    @Override
    public Thread ownerThread() {
        return owner;
    }

    /**
     * A confined resource list; no races are possible here.
     */
    static final class ConfinedResourceList extends ResourceList {
        @Override
        void add(ResourceCleanup cleanup) {
            if (fst != ResourceCleanup.CLOSED_LIST) {
                cleanup.next = fst;
                fst = cleanup;
            } else {
                throw new IllegalStateException("Already closed!");
            }
        }

        @Override
        void cleanup() {
            if (fst != ResourceCleanup.CLOSED_LIST) {
                ResourceCleanup prev = fst;
                fst = ResourceCleanup.CLOSED_LIST;
                cleanup(prev);
            } else {
                throw new IllegalStateException("Attempt to cleanup an already closed resource list");
            }
        }
    }

    /**
     * A confined resource scope handle; no races are possible here.
     */
    final class ConfinedHandle implements HandleImpl {
        boolean released = false;

        @Override
        public ResourceScopeImpl scope() {
            return ConfinedScope.this;
        }

        @Override
        public void release() {
            checkValidState(); // thread check
            if (!released) {
                released = true;
                lockCount--;
            }
        }
    }
}
