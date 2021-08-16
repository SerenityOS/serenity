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


package nsk.jvmti.unit.GetLocalVariable;

import java.io.PrintStream;

public class getlocal004 {

    static Thread currThread;
    int fld = 17;

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    public static int run(String argv[], PrintStream ref) {
        currThread = Thread.currentThread();
        getlocal004 t = new getlocal004();
        getMeth();
        t.meth01();
        staticMeth(0);
        return getRes();
    }

    public static synchronized int staticMeth(int intArg) {
        System.out.println(" JAVA: staticMeth: Started  " + intArg);
        float slot1 = 3.1415926f;
        checkLoc(currThread, 1);          // 1-st call to checkLoc
        {
             int slot_2 = 22;
             int slot_3 = 33;
             int slot_4 = 44;
             {
                 int slot_5 = 55;
                 checkLoc(currThread, 2); // 2-nd call to checkLoc
             }
        }
        int slot2  = 2;
        int slot3  = 3;
        int slot4  = 4;
        int slot5  = 5;
        checkLoc(currThread, 3);          // 3-rd call to checkLoc

        System.out.println(" JAVA: staticMeth: Finished " + intArg);
        return intArg;
    }

    public double meth01() {
        float f = 6.0f;
        double d = 7.0;
        return d + f;
    }

    native static void getMeth();
    native static void checkLoc(Thread thr, int scope_no);
    native static int getRes();

    static {
        try {
            System.loadLibrary("getlocal004");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println(" JAVA: Could not load getlocal004 library");
            System.err.println(" JAVA: java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }
}
