/*
 * Copyright (c) 2004, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetThreadState;

import java.io.PrintStream;

public class thrstat003 {

    final static int JCK_STATUS_BASE = 95;
    static final int NOT_STARTED = 0;
    static final int SLEEPING = 1;
    static final int ZOMBIE = 2;

    native static void init(int waitTime);
    native static int check(Thread thread, int status);

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    static int contendCount = 0;
    public static Object lock = new Object();
    public static int waitTime = 2;

    public static int run(String args[], PrintStream out) {
        if (args.length > 0) {
            try {
                int i  = Integer.parseInt(args[0]);
                waitTime = i;
            } catch (NumberFormatException ex) {
                out.println("# Wrong argument \"" + args[0]
                    + "\", the default value is used");
            }
        }
        out.println("# Waiting time = " + waitTime + " mins");

        init(waitTime);

        TestThread t = new TestThread();

        check(t, NOT_STARTED);

        synchronized (lock) {
            t.start();
            try {
                lock.wait();
            } catch (InterruptedException e) {
                throw new Error("Unexpected: " + e);
            }

        }

        check(t, SLEEPING);

        t.interrupt();
        try {
            t.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected: " + e);
        }

        return check(t, ZOMBIE);
    }


    static class TestThread extends Thread {
        public void run() {
            synchronized (lock) {
                lock.notify();
            }
            try {
                sleep(waitTime*60000);
            } catch (InterruptedException e) {
                // OK, it's expected
            }
        }
    }
}
