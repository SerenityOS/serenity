/*
 * Copyright (c) 2003, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetFrameLocation;

import java.io.PrintStream;

public class frameloc001 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("frameloc001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load frameloc001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady(Class cls);
    native static boolean checkFrame01(Thread thr, Class cls, boolean mustPass);
    native static int getRes();

    static int fld = 0;
    static Object lock1 = new Object();
    static Object lock2 = new Object();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        frameloc001a thr = new frameloc001a();

        getReady(frameloc001a.class);
        thr.meth01(2000);

        synchronized (lock2) {
            synchronized (lock1) {
                thr.start();
                try {
                    lock1.wait();
                } catch (InterruptedException e) {
                    throw new Error("Unexpected " + e);
                }
            }
            waitForChildThread(thr);
            checkFrame01(thr, frameloc001a.class, true);
        }

        return getRes();
    }

    private static void waitForChildThread(frameloc001a thr) {
        // Wait for child thread to reach expected position. Wait up to 5 seconds.
        final int MAX_WAIT_MS = 5000;
        int sumWaitMs = 0;
        while (!checkFrame01(thr, frameloc001a.class, false) && sumWaitMs <= MAX_WAIT_MS) {
            try {
                System.out.println("Waited: " + sumWaitMs);
                final int sleepMs = 20;
                sumWaitMs += sleepMs;
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new Error("Unexpected " + e);
            }
        }
    }
}
