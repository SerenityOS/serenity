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
import jdk.internal.misc.ScopedMemoryAccess;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.lang.ref.Cleaner;
import java.lang.ref.Reference;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * A shared scope, which can be shared across multiple threads. Closing a shared scope has to ensure that
 * (i) only one thread can successfully close a scope (e.g. in a close vs. close race) and that
 * (ii) no other thread is accessing the memory associated with this scope while the segment is being
 * closed. To ensure the former condition, a CAS is performed on the liveness bit. Ensuring the latter
 * is trickier, and require a complex synchronization protocol (see {@link jdk.internal.misc.ScopedMemoryAccess}).
 * Since it is the responsibility of the closing thread to make sure that no concurrent access is possible,
 * checking the liveness bit upon access can be performed in plain mode, as in the confined case.
 */
class SharedScope extends ResourceScopeImpl {

    private static final ScopedMemoryAccess SCOPED_MEMORY_ACCESS = ScopedMemoryAccess.getScopedMemoryAccess();

    private static final int ALIVE = 0;
    private static final int CLOSING = -1;
    private static final int CLOSED = -2;
    private static final int MAX_FORKS = Integer.MAX_VALUE;

    private int state = ALIVE;

    private static final VarHandle STATE;

    static {
        try {
            STATE = MethodHandles.lookup().findVarHandle(jdk.internal.foreign.SharedScope.class, "state", int.class);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    SharedScope(Cleaner cleaner) {
        super(cleaner, new SharedResourceList());
    }

    @Override
    public Thread ownerThread() {
        return null;
    }

    @Override
    public void checkValidState() {
        if (state < ALIVE) {
            throw ScopedAccessError.INSTANCE;
        }
    }

    @Override
    public HandleImpl acquire() {
        int value;
        do {
            value = (int) STATE.getVolatile(this);
            if (value < ALIVE) {
                //segment is not alive!
                throw new IllegalStateException("Already closed");
            } else if (value == MAX_FORKS) {
                //overflow
                throw new IllegalStateException("Segment acquire limit exceeded");
            }
        } while (!STATE.compareAndSet(this, value, value + 1));
        return new SharedHandle();
    }

    void justClose() {
        int prevState = (int) STATE.compareAndExchange(this, ALIVE, CLOSING);
        if (prevState < 0) {
            throw new IllegalStateException("Already closed");
        } else if (prevState != ALIVE) {
            throw new IllegalStateException("Scope is acquired by " + prevState + " locks");
        }
        boolean success = SCOPED_MEMORY_ACCESS.closeScope(this);
        STATE.setVolatile(this, success ? CLOSED : ALIVE);
        if (!success) {
            throw new IllegalStateException("Cannot close while another thread is accessing the segment");
        }
    }

    @Override
    public boolean isAlive() {
        return (int) STATE.getVolatile(this) != CLOSED;
    }

    /**
     * A shared resource list; this implementation has to handle add vs. add races, as well as add vs. cleanup races.
     */
    static class SharedResourceList extends ResourceList {

        static final VarHandle FST;

        static {
            try {
                FST = MethodHandles.lookup().findVarHandle(ResourceList.class, "fst", ResourceCleanup.class);
            } catch (Throwable ex) {
                throw new ExceptionInInitializerError();
            }
        }

        @Override
        void add(ResourceCleanup cleanup) {
            while (true) {
                ResourceCleanup prev = (ResourceCleanup) FST.getAcquire(this);
                cleanup.next = prev;
                ResourceCleanup newSegment = (ResourceCleanup) FST.compareAndExchangeRelease(this, prev, cleanup);
                if (newSegment == ResourceCleanup.CLOSED_LIST) {
                    // too late
                    throw new IllegalStateException("Already closed");
                } else if (newSegment == prev) {
                    return; //victory
                }
                // keep trying
            }
        }

        void cleanup() {
            // At this point we are only interested about add vs. close races - not close vs. close
            // (because MemoryScope::justClose ensured that this thread won the race to close the scope).
            // So, the only "bad" thing that could happen is that some other thread adds to this list
            // while we're closing it.
            if (FST.getAcquire(this) != ResourceCleanup.CLOSED_LIST) {
                //ok now we're really closing down
                ResourceCleanup prev = null;
                while (true) {
                    prev = (ResourceCleanup) FST.getAcquire(this);
                    // no need to check for DUMMY, since only one thread can get here!
                    if (FST.weakCompareAndSetRelease(this, prev, ResourceCleanup.CLOSED_LIST)) {
                        break;
                    }
                }
                cleanup(prev);
            } else {
                throw new IllegalStateException("Attempt to cleanup an already closed resource list");
            }
        }
    }

    /**
     * A shared resource scope handle; this implementation has to handle close vs. close races.
     */
    class SharedHandle implements HandleImpl {
        final AtomicBoolean released = new AtomicBoolean(false);

        @Override
        public ResourceScopeImpl scope() {
            return SharedScope.this;
        }

        @Override
        public void release() {
            if (released.compareAndSet(false, true)) {
                int value;
                do {
                    value = (int) STATE.getVolatile(jdk.internal.foreign.SharedScope.this);
                    if (value <= ALIVE) {
                        //cannot get here - we can't close segment twice
                        throw new IllegalStateException("Already closed");
                    }
                } while (!STATE.compareAndSet(jdk.internal.foreign.SharedScope.this, value, value - 1));
            }
        }
    }
}
