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

package nsk.jvmti.SetFieldModificationWatch;

import java.io.PrintStream;

public class setfmodw001 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("setfmodw001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load setfmodw001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void setWatch(int fld_ind);
    native void touchfld0();
    native static void check(int fld_ind, boolean flag);
    native static int getRes();

    int fld0 = -1;
    static int fld1 = 1;
    private setfmodw001a fld2 = new setfmodw001a();
    static int fld = 10;

    public static Object lock = new Object();

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        setfmodw001 t = new setfmodw001();
        setfmodw001a t_a = new setfmodw001a();
        setfmodw001b t_b = new setfmodw001b();
        t_b.start();
        synchronized (lock) {
            fld1 = fld1 + 1;
            check(1, false);
        }
        synchronized (lock) {
            t_a.fld3 = new int[10];
            check(3, false);
        }
        setWatch(1);
        synchronized (lock) {
            fld1 = fld1 + fld;
            check(1, true);
        }
        setWatch(3);
        synchronized (lock) {
            t_a.fld3 = new int[10];
            check(3, true);
        }
        t.meth01();
        try {
            t_b.join();
        } catch (InterruptedException e) {}
        return getRes();
    }

    private void meth01() {
        synchronized (lock) {
            touchfld0();
            check(0, true);
        }
        synchronized (lock) {
            fld2 = new setfmodw001a();
            check(2, false);
        }
        setWatch(2);
        synchronized (lock) {
            fld2 = new setfmodw001a();
            check(2, true);
        }
    }
}

class setfmodw001a {
    int[] fld3;
    int fld = 2;
}

class setfmodw001b extends Thread {
    float fld4;
    public void run() {
        synchronized (setfmodw001.lock) {
            fld4 = setfmodw001.fld;
            setfmodw001.check(4, false);
        }
        setfmodw001.setWatch(4);
        synchronized (setfmodw001.lock) {
            fld4 += setfmodw001.fld;
            setfmodw001.check(4, true);
        }
    }
}
