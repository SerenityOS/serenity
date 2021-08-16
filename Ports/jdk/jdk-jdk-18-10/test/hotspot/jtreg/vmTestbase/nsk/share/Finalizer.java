/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share;

import java.util.Stack;

/**
 * Finalizer performs object finalization when virtual mashine shuts down.
 * Finalizer is a thread that acts as a VM shutdown hook.
 * This thread will be activated as VM shuts down because of
 * invocation of <code>exit()</code> or termination. After activation
 * Finalizer just calls <code>finalizeAtExit()</code> method of the specified object.
 * The finalizable object should implement interface <code>Finalizable</code>.
 *
 * @see Finalizable
 */
public class Finalizer {

    /** Finalizer thread to register as a VM shutdown hook. */
    private static FinalizerThread finalizerThread = null;

    /** An object to finalize. */
    private Finalizable object;

    /**
     * Create finalizer for the specified object.
     */
    public Finalizer(Finalizable object) {
        this.object = object;
    }

    /**
     * Register finalizer for finalization at VM shutdown.
     */
    public void activate() {
        if (finalizerThread == null) {
            finalizerThread = new FinalizerThread("FinalizerThread for Finalizable objects");
            finalizerThread.activate();
        }
        finalizerThread.add(object);
    }

    /**
     * Unregister finalizer for finalization at VM shutdown.
     */
    public void deactivate() {
        if (finalizerThread == null)
            return;
        finalizerThread.remove(object);
    }

    /**
     * Static inner thread that is registered as a VM shutdown hook
     * and performs finalization of all registered finalizable objects.
     */
    private static class FinalizerThread extends Thread {

        /** Stack of objects registered for finalization. */
        private Stack<Object> objects = new Stack<Object>();

        /** Make new instance of FinalizerThread with given thread name. */
        public FinalizerThread(String threadName) {
            super(threadName);
        }

        /**
         * Push an object to the stack of registered objects.
         */
        public void add(Finalizable object) {
            objects.push(object);
        }

        /**
         * Remove an object from the stack of registered objects.
         */
        public void remove(Finalizable object) {
            objects.remove(object);
        }

        /**
         * Register finalizer thread as a VM shutdown hook.
         */
        public void activate() {
            Runtime.getRuntime().addShutdownHook( this );
        }

        /**
         * Unregister finalizer thread as a VM shutdown hook.
         */
        public void deactivate() {
            Runtime.getRuntime().removeShutdownHook( this );
        }

        /**
         * Pop all registered objects from the stack and finalize them.
         */
        public void run() {
            while (!objects.empty()) {
                Finalizable object = (Finalizable)objects.pop();
                try {
                    object.finalizeAtExit();
                } catch (ThreadDeath e) {
                    throw e;
                } catch (Throwable ex) {
                    ex.printStackTrace();
                }
            }
        }

    } // end of FinalizerThread

} // end of Finalizer
