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
package nsk.jvmti.AttachOnDemand.attach008;

import nsk.share.aod.TargetApplicationWaitingAgents;
import nsk.share.locks.MonitorLockingThread;

public class attach008Target extends TargetApplicationWaitingAgents {

    /*
     * Thread generates MonitorContentedEnter/MonitorContentedEntered events
     */
    static class ThreadGeneratingEvents extends Thread {

        ThreadGeneratingEvents() {
            super("ThreadGeneratingEvents");
        }

        public void run() {
            try {
                Object lock = new Object();
                MonitorLockingThread monitorLockingThread = new MonitorLockingThread(lock);

                MonitorLockingThread.LockFreeThread lockFreeThread =
                    new MonitorLockingThread.LockFreeThread(Thread.currentThread(), monitorLockingThread);

                monitorLockingThread.acquireLock();

                lockFreeThread.start();

                log.display(Thread.currentThread() + ": trying to acquire lock");

                // try to acquire lock which is already held by MonitorLockingThread
                synchronized (lock) {
                    log.display(Thread.currentThread() + ": lock is acquired");
                }

                lockFreeThread.join();
                monitorLockingThread.join();
            } catch (Throwable t) {
                setStatusFailed("Unexpected exception: " + t);
                t.printStackTrace(log.getOutStream());
            }
        }
    }

    protected void targetApplicationActions() throws InterruptedException {
        ThreadGeneratingEvents threadGeneratingEvents = new ThreadGeneratingEvents();
        threadGeneratingEvents.start();
        threadGeneratingEvents.join();
    }

    public static void main(String[] args) {
        new attach008Target().runTargetApplication(args);
    }
}
