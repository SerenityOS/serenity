/*
 * Copyright (c) 2004, 2013, Oracle and/or its affiliates. All rights reserved.
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

import static java.lang.Thread.State.*;

/*
 * @test
 * @bug     5014783 8022208
 * @summary Basic unit test of thread states returned by
 *          Thread.getState().
 *
 * @author  Mandy Chung
 * @library /test/lib
 * @build jdk.test.lib.LockFreeLogger
 * @build ThreadStateTest ThreadStateController
 * @run main/othervm -Xmixed ThreadStateTest
 */

public class ThreadStateTest {
    private static boolean testFailed = false;

    // used to achieve waiting states
    private static final Object globalLock = new Object();

    public static void main(String[] argv) throws Exception {
        // Call Thread.getState to force all initialization done
        // before test verification begins.
        Thread.currentThread().getState();
        ThreadStateController thread = new ThreadStateController("StateChanger", globalLock);
        thread.setDaemon(true);

        // before myThread starts
        thread.checkThreadState(NEW);

        thread.start();
        thread.transitionTo(RUNNABLE);
        thread.checkThreadState(RUNNABLE);

        synchronized (globalLock) {
            thread.transitionTo(BLOCKED);
            thread.checkThreadState(BLOCKED);
        }

        thread.transitionTo(WAITING);
        thread.checkThreadState(WAITING);

        thread.transitionTo(TIMED_WAITING);
        thread.checkThreadState(TIMED_WAITING);

        thread.transitionToPark(true /* timed park*/);
        thread.checkThreadState(TIMED_WAITING);

        thread.transitionToPark(false /* indefinite park */);
        thread.checkThreadState(WAITING);

        thread.transitionToSleep();
        thread.checkThreadState(TIMED_WAITING);

        thread.transitionTo(TERMINATED);
        thread.checkThreadState(TERMINATED);

        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.out.println("Unexpected exception.");
            testFailed = true;
        }

        if (testFailed)
            throw new RuntimeException("TEST FAILED.");
        System.out.println("Test passed.");
    }
}
