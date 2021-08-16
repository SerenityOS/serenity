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

public class setfmodw006 {

    final static int JCK_STATUS_BASE = 95;
    final static int NUM_TRIES = 1000;

    static {
        try {
            System.loadLibrary("setfmodw006");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load setfmodw006 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady(int n);
    native static int check(boolean flag);

    static boolean staticBoolean;
    static byte staticByte;
    static short staticShort;
    static int staticInt;
    static long staticLong;
    static float staticFloat;
    static double staticDouble;
    static char staticChar;
    static Object staticObject;
    static int staticArrInt[];

    boolean instanceBoolean;
    byte instanceByte;
    short instanceShort;
    int instanceInt;
    long instanceLong;
    float instanceFloat;
    double instanceDouble;
    char instanceChar;
    Object instanceObject;
    int instanceArrInt[];

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        setfmodw006 t = new setfmodw006();
        getReady(NUM_TRIES);
        t.tryModification();
        check(true);
        t.tryAccess();
        return check(false);
    }

    public void tryModification() {
        for (int i = 0; i < NUM_TRIES; i++) {
            staticBoolean = true;
            staticByte = 1;
            staticShort = 2;
            staticInt = 3;
            staticLong = 4L;
            staticFloat = 0.5F;
            staticDouble = 0.6;
            staticChar = '\u0007';
            staticObject = null;
            staticArrInt = null;
            this.instanceBoolean = false;
            this.instanceByte = 10;
            this.instanceShort = 20;
            this.instanceInt = 30;
            this.instanceLong = 40L;
            this.instanceFloat = 0.05F;
            this.instanceDouble = 0.06;
            this.instanceChar = '\u0070';
            this.instanceObject = null;
            this.instanceArrInt = null;
        }
    }

    public void tryAccess() {
        int count = 0;
        for (int i = 0; i < NUM_TRIES; i++) {
            if (staticBoolean == true) count++;
            if (staticByte == 1) count++;
            if (staticShort == 2) count++;
            if (staticInt == 3) count++;
            if (staticLong == 4L) count++;
            if (staticFloat == 0.5F) count++;
            if (staticDouble == 0.6) count++;
            if (staticChar == '\u0007') count++;
            if (staticObject == null) count++;
            if (staticArrInt == null) count++;
            if (this.instanceBoolean == false) count++;
            if (this.instanceByte == 10) count++;
            if (this.instanceShort == 20) count++;
            if (this.instanceInt == 30) count++;
            if (this.instanceLong == 40L) count++;
            if (this.instanceFloat == 0.05F) count++;
            if (this.instanceDouble == 0.06) count++;
            if (this.instanceChar == '\u0070') count++;
            if (this.instanceObject == null) count++;
            if (this.instanceArrInt == null) count++;
        }
    }
}
