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

package nsk.jvmti.GetObjectMonitorUsage;

import java.io.PrintStream;

public class objmonusage001 {
    final static int JCK_STATUS_BASE = 95;
    final static int NUMBER_OF_THREADS = 32;

    static {
        try {
            System.loadLibrary("objmonusage001");
        } catch (UnsatisfiedLinkError err) {
            System.err.println("Could not load objmonusage001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw err;
        }
    }

    native static int getResult();
    native static void check(int i, Object o, Thread owner, int ec, int wc);

    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream out) {
        Object syncObject[] = new Object[NUMBER_OF_THREADS];
        objmonusage001a runn[] = new objmonusage001a[NUMBER_OF_THREADS];

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            syncObject[i] = new Object();
            runn[i] = new objmonusage001a(i, syncObject[i]);
        }

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            synchronized (syncObject[i]) {
                runn[i].start();
                try {
                    syncObject[i].wait();
                } catch (Throwable e) {
                    out.println(e);
                    return 2;
                }
            }
            check(NUMBER_OF_THREADS + i, syncObject[i], null, 0, 1);
        }

        for (int i = 0; i < NUMBER_OF_THREADS; i++) {
            synchronized (syncObject[i]) {
                syncObject[i].notify();
            }
            try {
                runn[i].join();
            } catch (InterruptedException e) {
                throw new Error("Unexpected " + e);
            }
        }

        return getResult();
    }
}

class objmonusage001a extends Thread {
    Object syncObject;
    int ind;

    public objmonusage001a(int i, Object s) {
        ind = i;
        syncObject = s;
    }

    public void run() {
        synchronized (syncObject) {
            objmonusage001.check(ind, syncObject, this, 1, 1);
            syncObject.notify();
            try {
                syncObject.wait();
            } catch (InterruptedException e) {
                throw new Error("Unexpected " + e);
            }
        }
    }
}
