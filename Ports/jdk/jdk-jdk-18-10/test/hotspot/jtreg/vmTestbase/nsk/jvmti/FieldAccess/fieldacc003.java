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

package nsk.jvmti.FieldAccess;

import java.io.PrintStream;

public class fieldacc003 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("fieldacc003");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load fieldacc003 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady();
    native static int check();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        getReady();
        fieldacc003a t = new fieldacc003a();
        t.run();
        return check();
    }
}

class fieldacc003e {
    boolean extendsBoolean = false;
    byte extendsByte = 10;
    short extendsShort = 20;
    int extendsInt = 30;
    long extendsLong = 40;
    float extendsFloat = 0.05F;
    double extendsDouble = 0.06;
    char extendsChar = 'D';
    Object extendsObject = new Object();
    int extendsArrInt[] = {70, 80};
}

class fieldacc003a extends fieldacc003e {
    public int run() {
        int i = 0;
        if (extendsBoolean == true) i++;
        if (extendsByte == 1) i++;
        if (extendsShort == 2) i++;
        if (extendsInt == 3) i++;
        if (extendsLong == 4) i++;
        if (extendsFloat == 0.5F) i++;
        if (extendsDouble == 0.6) i++;
        if (extendsChar == 'C') i++;
        if (extendsObject == this) i++;
        if (extendsArrInt[1] == 7) i++;
        return i;
    }
}
