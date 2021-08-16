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

package nsk.jvmti.NotifyFramePop;

import java.io.PrintStream;

public class nframepop001 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("nframepop001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load nframepop001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getMethIds();
    native static void setFramePopNotif(Thread thr);
    native static void checkFrame(int point);
    native static int getRes();

    static int fld = 0;
    static Object start_lock = new Object();
    static Object wait_lock = new Object();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        nframepop001 t = new nframepop001();
        nframepop001a thr = new nframepop001a();
        getMethIds();

        t.meth01(2001);
        checkFrame(1);

        try {
            t.meth02(2002);
        } catch (Throwable e) {}
        checkFrame(2);

        synchronized (wait_lock) {
            synchronized (start_lock) {
                thr.start();
                try {
                    start_lock.wait();
                } catch (InterruptedException e) {
                    throw new Error("Unexpected: " + e);
                }
            }
            setFramePopNotif(thr);
        }

        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected: " + e);
        }
        checkFrame(3);

        return getRes();
    }

    public void meth01(int i) {
        try {
            if (i > 0) {
                throw new Throwable();
            }
        } catch (Throwable e) {}
    }

    public void meth02(int i) throws Throwable {
        try {
            if (i > 0) {
                throw new Throwable();
            }
        } catch (Throwable e) {
            throw e;
        }
    }
}

class nframepop001a extends Thread {
    public void run() {
        int local = 2003;
        synchronized (nframepop001.start_lock) {
            nframepop001.start_lock.notify();
        }
        synchronized (nframepop001.wait_lock) {
            nframepop001.fld = local;
        }
    }
}
