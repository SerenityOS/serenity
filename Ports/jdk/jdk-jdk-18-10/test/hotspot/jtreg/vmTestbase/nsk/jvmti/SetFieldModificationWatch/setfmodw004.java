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

public class setfmodw004 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("setfmodw004");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load setfmodw004 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady();
    native static void check(int ind);
    native static int getRes();

    static PrintStream out;
    static int result = 0;
    static boolean flag = false;

    static int fld0 = 17;
    static int fld1;
    int fld2 = 18;
    int fld3;

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        out = ref;
        setfmodw004 t = new setfmodw004();

        // access fields first to force cache load
        t.meth(42, 43, 44, 45);

        getReady();
        flag = true;

        t.meth(96, 97, 98, 99);

        return (getRes() | result);
    }

    public void meth(int val0, int val1, int val2, int val3) {

        // test getstatic bytecode for initialized field
        fld0 = val0;
        checkEvent(0);
        if (fld0 != val0) {
            out.println("fld0 value is corrupted: expected=" + val0
                + ", actual=" + fld0);
            result = 2;
        }

        // test getstatic bytecode for uninitialized field
        fld1 = val1;
        checkEvent(1);
        if (fld1 != val1) {
            out.println("fld1 value is corrupted: expected=" + val1
                + ", actual=" + fld1);
            result = 2;
        }

        // test getfield bytecode for initialized field
        fld2 = val2;
        checkEvent(2);
        if (fld2 != val2) {
            out.println("fld2 value is corrupted: expected=" + val2
                + ", actual=" + fld2);
            result = 2;
        }

        // test getfield bytecode for uninitialized field
        fld3 = val3;
        checkEvent(3);
        if (fld3 != val3) {
            out.println("fld3 value is corrupted: expected=" + val3
                + ", actual=" + fld3);
            result = 2;
        }
    }

    static void checkEvent(int ind) {
        if (flag) {
            check(ind);
        }
    }
}
