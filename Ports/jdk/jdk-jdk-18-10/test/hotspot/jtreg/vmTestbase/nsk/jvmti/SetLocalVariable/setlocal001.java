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

package nsk.jvmti.SetLocalVariable;

import java.io.PrintStream;

public class setlocal001 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("setlocal001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load setlocal001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getMethReady(float f, double d, Object o, int[] a);
    native static int getRes();

    static PrintStream out;
    static int result = 0;
    static Thread currThread;
    static float floatVal = 6.0f;
    static double doubleVal = 7.0f;
    static setlocal001 objVal = new setlocal001();
    static int[] arrVal = {10, 9, 8, 7, 6};
    int val = 3;

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        out = ref;
        currThread = Thread.currentThread();
        setlocal001 t = new setlocal001();
        getMethReady(floatVal, doubleVal, objVal, arrVal);
        t.meth01();
        t.meth02(100);
        meth03();
        meth04(0, 0L, (short)0, 0.0, (char)0, 0f, (byte)0, false);
        return (getRes() == 0 && result == 0 ? 0 : 2);
    }

    // dummy method to be breakpointed in agent
    public static void checkPoint() {
    }

    public double meth01() {
        long l = 0;
        float f = 0f;
        double d = 0;
        checkPoint();
        if (l != 22L || f != floatVal || d != doubleVal) {
            out.println("meth01: l =" + l + " f = " + f + " d = " + d);
            result = 2;
        }
        return d + f + l;
    }

    public void meth02(int step) {
        int i1 = 0;
        short i2 = 0;
        char i3 = 0;
        byte i4 = 0;
        boolean i5 = false;
        if (step > 0) {
            meth02(step - 1);
        } else {
            checkPoint();
            if (i1 != 1 || i2 != 1 || i3 != 1 || i4 != 1 || !i5) {
                out.println("meth02: i1 =" + i1 + " i2 = " + i2 +
                    " i3 = " + i3 + " i4 = " + i4 + " i5 = " + i5);
                result = 2;
            }
        }
    }

    public static void meth03() {
        setlocal001 ob1 = null;
        int[] ob2 = null;
        checkPoint();
        if (ob1.val != 3 || ob2[2] != 8) {
            out.println("meth03: ob1.val =" + ob1.val + " ob2[2] = " + ob2[2]);
            result = 2;
        }
    }

    public static void meth04(int i1, long l, short i2, double d,
                              char i3, float f, byte i4, boolean b) {
        checkPoint();
        if (i1 != 1 || i2 != 2 || i3 != 3 || i4 != 4 ||
                l != 22L || f != floatVal || d != doubleVal || !b) {
            out.println("meth04: i1 =" + i1 + " i2 = " + i2 +
                " i3 = " + i3 + " i4 = " + i4);
            out.println("        l =" + l + " f = " + f +
                " d = " + d + " b = " + b);
            result = 2;
        }
    }
}
