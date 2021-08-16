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

package nsk.jvmti.GetFrameCount;

import java.io.PrintStream;

public class framecnt001 {

    final static int JCK_STATUS_BASE = 95;

    native static void checkFrames(Thread thr, int thr_num, int ans);
    native static int getRes();

    static {
        try {
            System.loadLibrary("framecnt001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load framecnt001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    static Object flag1 = new Object();
    static Object flag2 = new Object();
    static Object check_flag = new Object();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        Thread currThread = Thread.currentThread();
        framecnt001a thr1 = new framecnt001a("thr1", 0, flag1);
        framecnt001a thr2 = new framecnt001a("thr2", 500, flag2);
        synchronized(check_flag) {
            synchronized(flag1) {
                synchronized(flag2) {
                    thr1.start();
                    thr2.start();
                    checkFrames(currThread, 0, 3);
                    try {
                        flag1.wait();
                        flag2.wait();
                    } catch(InterruptedException e) {}
                }
            }
            checkFrames(thr1, 1, 1);
            checkFrames(thr2, 2, 501);
        }
        return getRes();
    }
}

class framecnt001a extends Thread {
    int steps;
    Object flag;

    framecnt001a(String name, int steps, Object flag) {
        super(name);
        this.steps = steps;
        this.flag = flag;
    }

    public void run() {
        if (steps > 0) {
            steps--;
            run();
        }
        synchronized(flag) {
            flag.notify();  // let main thread know that all frames are in place
        }
        synchronized(framecnt001.check_flag) {  // wait for the check done
        }
    }
}
