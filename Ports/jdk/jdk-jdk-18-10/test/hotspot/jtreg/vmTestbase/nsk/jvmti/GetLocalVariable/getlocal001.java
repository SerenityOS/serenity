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

package nsk.jvmti.GetLocalVariable;

import java.io.PrintStream;

public class getlocal001 {

    native static void getMeth(double d, float f);
    native static int getRes();

    static {
        try {
            System.loadLibrary("getlocal001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load getlocal001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    int fld = 17;

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    public static int run(String argv[], PrintStream ref) {
        getlocal001 t = new getlocal001();
        getMeth(0.33, 3.14F);
        t.meth01();
        t.meth02(100);
        meth03(t);
        meth04(1, -100L, (short)2, 0.33, (char)3, 3.14F, (byte)4, true);
        return getRes();
    }

    // dummy method to be breakpointed in agent
    public static void checkPoint() {
    }

    public double meth01() {
        long l = 22;
        float f = 6.0f;
        double d = 7.0;
        checkPoint();
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
            i1 = 1; i2 = 1; i3 = 1; i4 = 1;
            i5 = true;
            checkPoint();
        }
    }

    public static void meth03(getlocal001 ob) {
        getlocal001 ob1 = ob;
        int[] ob2 = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        checkPoint();
    }

    public static void meth04(int i1, long l, short i2, double d,
                              char i3, float f, byte i4, boolean b) {
        checkPoint();
    }
}
