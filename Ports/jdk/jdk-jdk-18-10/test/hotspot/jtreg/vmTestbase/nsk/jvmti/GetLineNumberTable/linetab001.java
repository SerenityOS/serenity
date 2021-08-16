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

//    THIS TEST IS LINE NUMBER SENSITIVE

package nsk.jvmti.GetLineNumberTable;

import java.io.PrintStream;

public class linetab001 {

    final static int JCK_STATUS_BASE = 95;

    native static int check();

    static {
        try {
            System.loadLibrary("linetab001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load linetab001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    static int fld;

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String argv[], PrintStream ref) {
        return check();
    }

    /* This part is line number sensitive,
     * must be changed sinchronously with the native part of the test
     */

    public static void meth00() {} // linetab001.c::m0[0]

    public double meth01() {
        long l = 22;      // linetab001.c::m1[0]
        float f = 6.0f;   // linetab001.c::m1[1]
        double d = 7.0;   // linetab001.c::m1[2]
        return d + f + l; // linetab001.c::m1[3]
    }
}
