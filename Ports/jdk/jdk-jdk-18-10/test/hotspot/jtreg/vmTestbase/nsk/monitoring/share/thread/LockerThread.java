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

package nsk.monitoring.share.thread;

/**
 * Locker thread is used to lock monitor on which BlockedThread's
 * waits.
 */
public class LockerThread extends Thread {
        private String lock = new String("a lock");
        private Object ready = new Object();
        private Object lockit = new Object();
        private volatile boolean locked = false;

        public Object getLock() {
                return lock;
        }

        public boolean isLocked() {
                return locked;
        }

        public void waitState() {
                synchronized (ready) {
                        while (!locked) {
                                try {
                                        ready.wait();
                                } catch (InterruptedException e) {
                                }
                        }
                }

                ThreadUtils.waitThreadState(this, Thread.State.WAITING);

                System.out.println("Locker thread reached WAITING state");
        }

        public void finish() {
                locked = false;
                synchronized (lockit) {
                        lockit.notifyAll();
                }
        }

        public void run() {
                synchronized (lock) {
                        synchronized (lockit) {
                                synchronized (ready) {
                                        locked = true;
                                        ready.notifyAll();
                                }
                                while (locked) {
                                        try {
                                                lockit.wait();
                                        } catch (InterruptedException e) {
                                        }
                                }
                        }
                }

                System.out.println("Locker thread leaved WAITING state");
        }
}
