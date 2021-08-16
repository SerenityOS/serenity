/*
 * Copyright (c) 2015, 2020, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.ref;

import java.lang.ref.Cleaner;
import java.lang.ref.Cleaner.Cleanable;
import java.lang.ref.ReferenceQueue;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import jdk.internal.misc.InnocuousThread;

/**
 * CleanerImpl manages a set of object references and corresponding cleaning actions.
 * CleanerImpl provides the functionality of {@link java.lang.ref.Cleaner}.
 */
public final class CleanerImpl implements Runnable {

    /**
     * An object to access the CleanerImpl from a Cleaner; set by Cleaner init.
     */
    private static Function<Cleaner, CleanerImpl> cleanerImplAccess = null;

    /**
     * Heads of a CleanableList for each reference type.
     */
    final PhantomCleanable<?> phantomCleanableList;

    // The ReferenceQueue of pending cleaning actions
    final ReferenceQueue<Object> queue;

    /**
     * Called by Cleaner static initialization to provide the function
     * to map from Cleaner to CleanerImpl.
     * @param access a function to map from Cleaner to CleanerImpl
     */
    public static void setCleanerImplAccess(Function<Cleaner, CleanerImpl> access) {
        if (cleanerImplAccess == null) {
            cleanerImplAccess = access;
        } else {
            throw new InternalError("cleanerImplAccess");
        }
    }

    /**
     * Called to get the CleanerImpl for a Cleaner.
     * @param cleaner the cleaner
     * @return the corresponding CleanerImpl
     */
    static CleanerImpl getCleanerImpl(Cleaner cleaner) {
        return cleanerImplAccess.apply(cleaner);
    }

    /**
     * Constructor for CleanerImpl.
     */
    public CleanerImpl() {
        queue = new ReferenceQueue<>();
        phantomCleanableList = new PhantomCleanableRef();
    }

    /**
     * Starts the Cleaner implementation.
     * Ensure this is the CleanerImpl for the Cleaner.
     * When started waits for Cleanables to be queued.
     * @param cleaner the cleaner
     * @param threadFactory the thread factory
     */
    public void start(Cleaner cleaner, ThreadFactory threadFactory) {
        if (getCleanerImpl(cleaner) != this) {
            throw new AssertionError("wrong cleaner");
        }
        // schedule a nop cleaning action for the cleaner, so the associated thread
        // will continue to run at least until the cleaner is reclaimable.
        new CleanerCleanable(cleaner);

        if (threadFactory == null) {
            threadFactory = CleanerImpl.InnocuousThreadFactory.factory();
        }

        // now that there's at least one cleaning action, for the cleaner,
        // we can start the associated thread, which runs until
        // all cleaning actions have been run.
        Thread thread = threadFactory.newThread(this);
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Process queued Cleanables as long as the cleanable lists are not empty.
     * A Cleanable is in one of the lists for each Object and for the Cleaner
     * itself.
     * Terminates when the Cleaner is no longer reachable and
     * has been cleaned and there are no more Cleanable instances
     * for which the object is reachable.
     * <p>
     * If the thread is a ManagedLocalsThread, the threadlocals
     * are erased before each cleanup
     */
    @Override
    public void run() {
        Thread t = Thread.currentThread();
        InnocuousThread mlThread = (t instanceof InnocuousThread)
                ? (InnocuousThread) t
                : null;
        while (!phantomCleanableList.isListEmpty()) {
            if (mlThread != null) {
                // Clear the thread locals
                mlThread.eraseThreadLocals();
            }
            try {
                // Wait for a Ref, with a timeout to avoid getting hung
                // due to a race with clear/clean
                Cleanable ref = (Cleanable) queue.remove(60 * 1000L);
                if (ref != null) {
                    ref.clean();
                }
            } catch (Throwable e) {
                // ignore exceptions from the cleanup action
                // (including interruption of cleanup thread)
            }
        }
    }

    /**
     * Perform cleaning on an unreachable PhantomReference.
     */
    public static final class PhantomCleanableRef extends PhantomCleanable<Object> {
        private final Runnable action;

        /**
         * Constructor for a phantom cleanable reference.
         * @param obj the object to monitor
         * @param cleaner the cleaner
         * @param action the action Runnable
         */
        public PhantomCleanableRef(Object obj, Cleaner cleaner, Runnable action) {
            super(obj, cleaner);
            this.action = action;
        }

        /**
         * Constructor used only for root of phantom cleanable list.
         */
        PhantomCleanableRef() {
            super();
            this.action = null;
        }

        @Override
        protected void performCleanup() {
            action.run();
        }

        /**
         * Prevent access to referent even when it is still alive.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public Object get() {
            throw new UnsupportedOperationException("get");
        }

        /**
         * Direct clearing of the referent is not supported.
         *
         * @throws UnsupportedOperationException always
         */
        @Override
        public void clear() {
            throw new UnsupportedOperationException("clear");
        }
    }

    /**
     * A ThreadFactory for InnocuousThreads.
     * The factory is a singleton.
     */
    static final class InnocuousThreadFactory implements ThreadFactory {
        static final ThreadFactory factory = new InnocuousThreadFactory();

        static ThreadFactory factory() {
            return factory;
        }

        final AtomicInteger cleanerThreadNumber = new AtomicInteger();

        public Thread newThread(Runnable r) {
            return InnocuousThread.newThread("Cleaner-" + cleanerThreadNumber.getAndIncrement(),
                r, Thread.MIN_PRIORITY - 2);
        }
    }

    /**
     * A PhantomCleanable implementation for tracking the Cleaner itself.
     */
    static final class CleanerCleanable extends PhantomCleanable<Cleaner> {
        CleanerCleanable(Cleaner cleaner) {
            super(cleaner, cleaner);
        }

        @Override
        protected void performCleanup() {
            // no action
        }
    }
}
