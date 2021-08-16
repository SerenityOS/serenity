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

package nsk.jvmti.SetFieldAccessWatch;

import java.io.PrintStream;

public class setfldw001 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("setfldw001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load setfldw001 library");
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
    private setfldw001a fld2 = new setfldw001a();
    static int fld;

    public static Object lock = new Object();

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        setfldw001 t = new setfldw001();
        setfldw001a t_a = new setfldw001a();
        setfldw001b t_b = new setfldw001b();
        t_b.start();
        synchronized (lock) {
            fld = fld1 + 1;
            check(1, false);
        }
        synchronized (lock) {
            fld += t_a.fld3[1];
            check(3, false);
        }
        setWatch(1);
        synchronized (lock) {
            fld = fld1 + fld;
            check(1, true);
        }
        setWatch(3);
        synchronized (lock) {
            fld -= t_a.fld3[2];
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
            fld += fld2.fld;
            check(2, false);
        }
        setWatch(2);
        synchronized (lock) {
            fld += fld2.fld;
            check(2, true);
        }
    }
}

class setfldw001a {
    int[] fld3 = {10, 9, 8, 7, 6};
    int fld = 2;
}

class setfldw001b extends Thread {
    float fld4 = 6.0f;
    public void run() {
        synchronized (setfldw001.lock) {
            setfldw001.fld += fld4;
            setfldw001.check(4, false);
        }
        setfldw001.setWatch(4);
        synchronized (setfldw001.lock) {
            setfldw001.fld += fld4;
            setfldw001.check(4, true);
        }
    }
}
