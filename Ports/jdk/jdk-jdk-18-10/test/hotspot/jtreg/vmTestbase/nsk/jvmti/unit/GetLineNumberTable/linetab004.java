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


package nsk.jvmti.unit.GetLineNumberTable;

import java.io.PrintStream;

interface Interface004 {
    int instanceMeth0();
    int instanceMeth1();
}

abstract class Abstract004 implements Interface004 {
    protected int fld;

    // Constructor
    Abstract004() {
        fld = 1000;
    }

    public abstract int instanceMeth0();

    public int instanceMeth1() {
        fld = 999;
        fld = instanceMeth0();
        return 0;
    }
}

public class linetab004 extends Abstract004 {

    final static int JCK_STATUS_BASE = 95;

    public int instanceMeth0() {
        int i = 0;
        i++;
        return i;
    }

    public int instanceMeth2() {
        return instanceMeth1();
    }

    static native int staticNativeMeth();
    native int instanceNativeMeth();

    static {
        try {
            System.loadLibrary("linetab004");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load linetab004 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static int check();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        return check();
    }
}
