/*
 * Copyright (c) 1999, 2018, Oracle and/or its affiliates. All rights reserved.
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


import jdk.internal.misc.VM;

/**
 * Package-private utility class containing data structures and logic
 * governing the virtual-machine shutdown sequence.
 *
 * @author   Mark Reinhold
 * @since    1.3
 *
 * @see java.io.Console
 * @see ApplicationShutdownHooks
 * @see java.io.DeleteOnExitHook
 */

class Shutdown {

    // The system shutdown hooks are registered with a predefined slot.
    // The list of shutdown hooks is as follows:
    // (0) Console restore hook
    // (1) ApplicationShutdownHooks that invokes all registered application
    //     shutdown hooks and waits until they finish
    // (2) DeleteOnExit hook
    private static final int MAX_SYSTEM_HOOKS = 10;
    private static final Runnable[] hooks = new Runnable[MAX_SYSTEM_HOOKS];

    // the index of the currently running shutdown hook to the hooks array
    private static int currentRunningHook = -1;

    /* The preceding static fields are protected by this lock */
    private static class Lock { };
    private static Object lock = new Lock();

    /* Lock object for the native halt method */
    private static Object haltLock = new Lock();

    /**
     * Add a new system shutdown hook.  Checks the shutdown state and
     * the hook itself, but does not do any security checks.
     *
     * The registerShutdownInProgress parameter should be false except
     * registering the DeleteOnExitHook since the first file may
     * be added to the delete on exit list by the application shutdown
     * hooks.
     *
     * @params slot  the slot in the shutdown hook array, whose element
     *               will be invoked in order during shutdown
     * @params registerShutdownInProgress true to allow the hook
     *               to be registered even if the shutdown is in progress.
     * @params hook  the hook to be registered
     *
     * @throws IllegalStateException
     *         if registerShutdownInProgress is false and shutdown is in progress; or
     *         if registerShutdownInProgress is true and the shutdown process
     *         already passes the given slot
     */
    static void add(int slot, boolean registerShutdownInProgress, Runnable hook) {
        if (slot < 0 || slot >= MAX_SYSTEM_HOOKS) {
            throw new IllegalArgumentException("Invalid slot: " + slot);
        }
        synchronized (lock) {
            if (hooks[slot] != null)
                throw new InternalError("Shutdown hook at slot " + slot + " already registered");

            if (!registerShutdownInProgress) {
                if (currentRunningHook >= 0)
                    throw new IllegalStateException("Shutdown in progress");
            } else {
                if (VM.isShutdown() || slot <= currentRunningHook)
                    throw new IllegalStateException("Shutdown in progress");
            }

            hooks[slot] = hook;
        }
    }

    /* Run all system shutdown hooks.
     *
     * The system shutdown hooks are run in the thread synchronized on
     * Shutdown.class.  Other threads calling Runtime::exit, Runtime::halt
     * or JNI DestroyJavaVM will block indefinitely.
     *
     * ApplicationShutdownHooks is registered as one single hook that starts
     * all application shutdown hooks and waits until they finish.
     */
    private static void runHooks() {
        synchronized (lock) {
            /* Guard against the possibility of a daemon thread invoking exit
             * after DestroyJavaVM initiates the shutdown sequence
             */
            if (VM.isShutdown()) return;
        }

        for (int i=0; i < MAX_SYSTEM_HOOKS; i++) {
            try {
                Runnable hook;
                synchronized (lock) {
                    // acquire the lock to make sure the hook registered during
                    // shutdown is visible here.
                    currentRunningHook = i;
                    hook = hooks[i];
                }
                if (hook != null) hook.run();
            } catch (Throwable t) {
                if (t instanceof ThreadDeath td) {
                    throw td;
                }
            }
        }

        // set shutdown state
        VM.shutdown();
    }

    /* Notify the VM that it's time to halt. */
    static native void beforeHalt();

    /* The halt method is synchronized on the halt lock
     * to avoid corruption of the delete-on-shutdown file list.
     * It invokes the true native halt method.
     */
    static void halt(int status) {
        synchronized (haltLock) {
            halt0(status);
        }
    }

    static native void halt0(int status);

    /* Invoked by Runtime.exit, which does all the security checks.
     * Also invoked by handlers for system-provided termination events,
     * which should pass a nonzero status code.
     */
    static void exit(int status) {
        synchronized (lock) {
            if (status != 0 && VM.isShutdown()) {
                /* Halt immediately on nonzero status */
                halt(status);
            }
        }
        synchronized (Shutdown.class) {
            /* Synchronize on the class object, causing any other thread
             * that attempts to initiate shutdown to stall indefinitely
             */
            beforeHalt();
            runHooks();
            halt(status);
        }
    }


    /* Invoked by the JNI DestroyJavaVM procedure when the last non-daemon
     * thread has finished.  Unlike the exit method, this method does not
     * actually halt the VM.
     */
    static void shutdown() {
        synchronized (Shutdown.class) {
            runHooks();
        }
    }

}
