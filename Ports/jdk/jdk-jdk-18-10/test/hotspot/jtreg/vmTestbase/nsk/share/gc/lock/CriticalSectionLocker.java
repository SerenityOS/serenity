/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.share.gc.lock;

import nsk.share.TestBug;
import nsk.share.gc.lock.Locker;

/**
 * CriticalSectionLocker represents a way to lock a resource
 * by entering some critical section.
 */
public abstract class CriticalSectionLocker<T> implements Locker<T> {
        private transient boolean enabled = false;
        private transient boolean locked = false;
        private Object sync = new Object();
        private Thread thread;
        private Throwable exception;

        private final Runnable runnable = new Runnable() {
                public void run() {
                        //System.out.println("Running");
                        try {
                                do {
                                        synchronized (sync) {
                                                while (enabled && !locked) {
                                                        try {
                                                                sync.wait();
                                                        } catch (InterruptedException e) {
                                                        }
                                                }
                                                if (!enabled)
                                                        break;
                                        }
                                        do {
                                                criticalSection();
                                        } while (locked);
                                } while (enabled);
                        //      System.out.println("Exiting");
                        } catch (RuntimeException t) {
                                t.printStackTrace();
                                exception = t;
                                throw t;
                        }
                }
        };

        public CriticalSectionLocker() {
        }

        /**
         * Enter critical section.
         *
         */
        protected abstract void criticalSection();

        public void enable() {
                synchronized (sync) {
                        if (enabled)
                                throw new TestBug("Locker already enabled.");
//                      System.out.println("Enabling " + this);
                        enabled = true;
                        thread = new Thread(runnable, "Locker: " + runnable);
                        thread.setDaemon(true);
                        thread.start();
                }
        }

        public void lock() {
                synchronized (sync) {
                        if (locked)
                                throw new TestBug("Locker already locked.");
//                      System.out.println("Locking " + this);
                        locked = true;
                        sync.notifyAll();
                }
        }

        public void unlock() {
                synchronized (sync) {
                        if (!locked)
                                throw new TestBug("Locker not locked.");
//                      System.out.println("Unocking " + this);
                        locked = false;
                        sync.notifyAll();
                }
        }

        public void disable() {
                synchronized (sync) {
                        if (!enabled)
                                throw new TestBug("Locker not enabled.");
//                      System.out.println("Disabling " + this);
                        enabled = false;
                        sync.notifyAll();
                }
                try {
                        thread.join();
                } catch (InterruptedException e) {
                        throw new TestBug(e);
                }
        }

        public Throwable getException() {
                return exception;
        }
}
