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

package nsk.jvmti.ClearFieldModificationWatch;

import java.io.PrintStream;

public class clrfmodw001 {

    native static void setWatch(int fld_ind);
    native static void clearWatch(int fld_ind);
    native void touchfld0();
    native static void check(int fld_ind, boolean flag);
    native static int getRes();

    static {
        try {
            System.loadLibrary("clrfmodw001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load clrfmodw001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    int fld0 = -1;
    static int fld1 = 1;
    private clrfmodw001a fld2 = new clrfmodw001a();
    static int fld;

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    public static int run(String argv[], PrintStream ref) {
        clrfmodw001 t = new clrfmodw001();
        clrfmodw001a t_a = new clrfmodw001a();
        clrfmodw001b t_b = new clrfmodw001b();
        for (int i = 0; i < 5; i++) {
            setWatch(i);
        }
        t_b.start();
        clearWatch(1);
        fld1 = fld1 + fld;
        check(1, false);
        clearWatch(3);
        t_a.fld3 = new int[10];
        check(3, false);
        t.meth01();
        try {
            t_b.join();
        } catch (InterruptedException e) {}
        return getRes();
    }

    private void meth01() {
        clearWatch(0);
        touchfld0();
        check(0, false);
        clearWatch(2);
        fld2 = new clrfmodw001a();
        check(2, false);
    }
}

class clrfmodw001a {
    int[] fld3;
    int fld = 2;
}

class clrfmodw001b extends Thread {
    float fld4;
    public void run() {
        clrfmodw001.clearWatch(4);
        fld4 = clrfmodw001.fld;
        clrfmodw001.check(4, false);
    }
}
