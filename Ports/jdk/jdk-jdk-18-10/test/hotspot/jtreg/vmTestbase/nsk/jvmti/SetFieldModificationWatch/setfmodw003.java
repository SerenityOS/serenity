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

public class setfmodw003 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("setfmodw003");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load setfmodw003 library");
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
        setfmodw003 t = new setfmodw003();
        getReady();

        stat_meth();
        t.meth();

        return (getRes() | result);
    }

    public static void stat_meth() {

        // test putstatic bytecode for initialized field
        fld0 = 42;
        check(0);
        if (fld0 != 42) {
            out.println("fld0 value is corrupted: expected=42, actual=" + fld0);
            result = 2;
        }

        // test putstatic bytecode for uninitialized field
        fld1 = 43;
        check(1);
        if (fld1 != 43) {
            out.println("fld1 value is corrupted: expected=43, actual=" + fld1);
            result = 2;
        }
    }

    public void meth() {
        int loc;

        // test putfield bytecode for initialized field
        fld2 = 44;
        check(2);
        if (fld2 != 44) {
            out.println("fld2 value is corrupted: expected=44, actual=" + fld2);
            result = 2;
        }

        // test putfield bytecode for uninitialized field
        fld3 = 45;
        check(3);
        if (fld3 != 45) {
            out.println("fld3 value is corrupted: expected=45, actual=" + fld3);
            result = 2;
        }
    }
}
