/*
 * Copyright (c) 2015, 2018, Oracle and/or its affiliates. All rights reserved.
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

package java.lang.ref;

import jdk.internal.ref.CleanerImpl;

import java.util.Objects;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

/**
 * {@code Cleaner} manages a set of object references and corresponding cleaning actions.
 * <p>
 * Cleaning actions are {@link #register(Object object, Runnable action) registered}
 * to run after the cleaner is notified that the object has become
 * phantom reachable.
 * The cleaner uses {@link PhantomReference} and {@link ReferenceQueue} to be
 * notified when the <a href="package-summary.html#reachability">reachability</a>
 * changes.
 * <p>
 * Each cleaner operates independently, managing the pending cleaning actions
 * and handling threading and termination when the cleaner is no longer in use.
 * Registering an object reference and corresponding cleaning action returns
 * a {@link Cleanable Cleanable}. The most efficient use is to explicitly invoke
 * the {@link Cleanable#clean clean} method when the object is closed or
 * no longer needed.
 * The cleaning action is a {@link Runnable} to be invoked at most once when
 * the object has become phantom reachable unless it has already been explicitly cleaned.
 * Note that the cleaning action must not refer to the object being registered.
 * If so, the object will not become phantom reachable and the cleaning action
 * will not be invoked automatically.
 * <p>
 * The execution of the cleaning action is performed
 * by a thread associated with the cleaner.
 * All exceptions thrown by the cleaning action are ignored.
 * The cleaner and other cleaning actions are not affected by
 * exceptions in a cleaning action.
 * The thread runs until all registered cleaning actions have
 * completed and the cleaner itself is reclaimed by the garbage collector.
 * <p>
 * The behavior of cleaners during {@link System#exit(int) System.exit}
 * is implementation specific. No guarantees are made relating
 * to whether cleaning actions are invoked or not.
 * <p>
 * Unless otherwise noted, passing a {@code null} argument to a constructor or
 * method in this class will cause a
 * {@link java.lang.NullPointerException NullPointerException} to be thrown.
 *
 * @apiNote
 * The cleaning action is invoked only after the associated object becomes
 * phantom reachable, so it is important that the object implementing the
 * cleaning action does not hold references to the object.
 * In this example, a static class encapsulates the cleaning state and action.
 * An "inner" class, anonymous or not,  must not be used because it implicitly
 * contains a reference to the outer instance, preventing it from becoming
 * phantom reachable.
 * The choice of a new cleaner or sharing an existing cleaner is determined
 * by the use case.
 * <p>
 * If the CleaningExample is used in a try-finally block then the
 * {@code close} method calls the cleaning action.
 * If the {@code close} method is not called, the cleaning action is called
 * by the Cleaner when the CleaningExample instance has become phantom reachable.
 * <pre>{@code
 * public class CleaningExample implements AutoCloseable {
 *        // A cleaner, preferably one shared within a library
 *        private static final Cleaner cleaner = <cleaner>;
 *
 *        static class State implements Runnable {
 *
 *            State(...) {
 *                // initialize State needed for cleaning action
 *            }
 *
 *            public void run() {
 *                // cleanup action accessing State, executed at most once
 *            }
 *        }
 *
 *        private final State state;
 *        private final Cleaner.Cleanable cleanable;
 *
 *        public CleaningExample() {
 *            this.state = new State(...);
 *            this.cleanable = cleaner.register(this, state);
 *        }
 *
 *        public void close() {
 *            cleanable.clean();
 *        }
 *    }
 * }</pre>
 * The cleaning action could be a lambda but all too easily will capture
 * the object reference, by referring to fields of the object being cleaned,
 * preventing the object from becoming phantom reachable.
 * Using a static nested class, as above, will avoid accidentally retaining the
 * object reference.
 * <p>
 * <a id="compatible-cleaners"></a>
 * Cleaning actions should be prepared to be invoked concurrently with
 * other cleaning actions.
 * Typically the cleaning actions should be very quick to execute
 * and not block. If the cleaning action blocks, it may delay processing
 * other cleaning actions registered to the same cleaner.
 * All cleaning actions registered to a cleaner should be mutually compatible.
 * @since 9
 */
public final class Cleaner {

    /**
     * The Cleaner implementation.
     */
    final CleanerImpl impl;

    static {
        CleanerImpl.setCleanerImplAccess(new Function<Cleaner, CleanerImpl>() {
            @Override
            public CleanerImpl apply(Cleaner cleaner) {
                return cleaner.impl;
            }
        });
    }

    /**
     * Construct a Cleaner implementation and start it.
     */
    private Cleaner() {
        impl = new CleanerImpl();
    }

    /**
     * Returns a new {@code Cleaner}.
     * <p>
     * The cleaner creates a {@link Thread#setDaemon(boolean) daemon thread}
     * to process the phantom reachable objects and to invoke cleaning actions.
     * The {@linkplain java.lang.Thread#getContextClassLoader context class loader}
     * of the thread is set to the
     * {@link ClassLoader#getSystemClassLoader() system class loader}.
     * The thread has no permissions, enforced only if a
     * {@link java.lang.System#setSecurityManager(SecurityManager) SecurityManager is set}.
     * <p>
     * The cleaner terminates when it is phantom reachable and all of the
     * registered cleaning actions are complete.
     *
     * @return a new {@code Cleaner}
     *
     * @throws  SecurityException  if the current thread is not allowed to
     *               create or start the thread.
     */
    public static Cleaner create() {
        Cleaner cleaner = new Cleaner();
        cleaner.impl.start(cleaner, null);
        return cleaner;
    }

    /**
     * Returns a new {@code Cleaner} using a {@code Thread} from the {@code ThreadFactory}.
     * <p>
     * A thread from the thread factory's {@link ThreadFactory#newThread(Runnable) newThread}
     * method is set to be a {@link Thread#setDaemon(boolean) daemon thread}
     * and started to process phantom reachable objects and invoke cleaning actions.
     * On each call the {@link ThreadFactory#newThread(Runnable) thread factory}
     * must provide a Thread that is suitable for performing the cleaning actions.
     * <p>
     * The cleaner terminates when it is phantom reachable and all of the
     * registered cleaning actions are complete.
     *
     * @param threadFactory a {@code ThreadFactory} to return a new {@code Thread}
     *                      to process cleaning actions
     * @return a new {@code Cleaner}
     *
     * @throws  IllegalThreadStateException  if the thread from the thread
     *               factory was {@link Thread.State#NEW not a new thread}.
     * @throws  SecurityException  if the current thread is not allowed to
     *               create or start the thread.
     */
    public static Cleaner create(ThreadFactory threadFactory) {
        Objects.requireNonNull(threadFactory, "threadFactory");
        Cleaner cleaner = new Cleaner();
        cleaner.impl.start(cleaner, threadFactory);
        return cleaner;
    }

    /**
     * Registers an object and a cleaning action to run when the object
     * becomes phantom reachable.
     * Refer to the <a href="#compatible-cleaners">API Note</a> above for
     * cautions about the behavior of cleaning actions.
     *
     * @param obj   the object to monitor
     * @param action a {@code Runnable} to invoke when the object becomes phantom reachable
     * @return a {@code Cleanable} instance
     */
    public Cleanable register(Object obj, Runnable action) {
        Objects.requireNonNull(obj, "obj");
        Objects.requireNonNull(action, "action");
        return new CleanerImpl.PhantomCleanableRef(obj, this, action);
    }

    /**
     * {@code Cleanable} represents an object and a
     * cleaning action registered in a {@code Cleaner}.
     * @since 9
     */
    public interface Cleanable {
        /**
         * Unregisters the cleanable and invokes the cleaning action.
         * The cleanable's cleaning action is invoked at most once
         * regardless of the number of calls to {@code clean}.
         */
        void clean();
    }

}
