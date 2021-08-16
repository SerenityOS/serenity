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

public class setfldw004 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("setfldw004");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load setfldw004 library");
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
        setfldw004 t = new setfldw004();

        // access fields first to force cache load
        t.meth();

        getReady();
        flag = true;

        t.meth();

        return (getRes() | result);
    }

    public void meth() {
        int loc;

        // test getstatic bytecode for initialized field
        loc = fld0;
        checkEvent(0);
        if (loc != 17) {
            out.println("fld0 value is corrupted: expected=17, actual=" + loc);
            result = 2;
        }

        // test getstatic bytecode for uninitialized field
        loc = fld1;
        checkEvent(1);
        if (loc != 0) {
            out.println("fld1 value is corrupted: expected=0, actual=" + loc);
            result = 2;
        }

        // test getfield bytecode for initialized field
        loc = fld2;
        checkEvent(2);
        if (loc != 18) {
            out.println("fld2 value is corrupted: expected=18, actual=" + loc);
            result = 2;
        }

        // test getfield bytecode for uninitialized field
        loc = fld3;
        checkEvent(3);
        if (loc != 0) {
            out.println("fld3 value is corrupted: expected=0, actual=" + loc);
            result = 2;
        }
    }

    static void checkEvent(int ind) {
        if (flag) {
            check(ind);
        }
    }
}
