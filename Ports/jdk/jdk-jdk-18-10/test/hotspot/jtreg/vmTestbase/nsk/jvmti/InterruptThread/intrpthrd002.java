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

package nsk.jvmti.InterruptThread;

import java.io.PrintStream;

public class intrpthrd002 {

    final static int THREADS_NUMBER = 32;

    static {
        try {
            System.loadLibrary("intrpthrd002");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load intrpthrd002 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static int check(int ind, Thread thr);
    native static int getResult();

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    public static int run(String argv[], PrintStream ref) {
        intrpthrd002a runn[] = new intrpthrd002a[THREADS_NUMBER];

        for (int i = 0; i < THREADS_NUMBER; i++ ) {
            runn[i] = new intrpthrd002a();
            synchronized (runn[i].syncObject) {
                runn[i].start();
                try {
                    runn[i].syncObject.wait();
                } catch (InterruptedException e) {
                    throw new Error("Unexpected: " + e);
                }
            }
            try {
                runn[i].join();
            } catch (InterruptedException e) {
                throw new Error("Unexpected: " + e);
            }
            if (check(i, runn[i]) == 2) break;
        }

        return getResult();
    }
}

class intrpthrd002a extends Thread {
    public Object syncObject = new Object();

    public void run() {
        synchronized (syncObject) {
            syncObject.notify();
        }
    }
}
