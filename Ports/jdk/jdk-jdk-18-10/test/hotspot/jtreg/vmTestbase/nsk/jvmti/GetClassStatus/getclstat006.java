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

package nsk.jvmti.GetClassStatus;

import java.io.PrintStream;

public class getclstat006 {

    final static int JCK_STATUS_BASE = 95;
    final static String PACKAGE_NAME = "nsk.jvmti.GetClassStatus.";

    static {
        try {
            System.loadLibrary("getclstat006");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load getclstat006 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void check(int i, Class cls);
    native static int getRes();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    static boolean flag = true;
    static Class cls;

    public static int run(String args[], PrintStream out) {
        try {
            cls = Class.forName(PACKAGE_NAME + "getclstat006$Positive");
            check(0, cls);
            cls = Class.forName(PACKAGE_NAME + "getclstat006$Negative", false,
                getclstat006.class.getClassLoader());
            try {
                Negative negative = new Negative();
            } catch (Error e) {
                //OK, it's expected
            }
            check(1, cls);
        } catch (ClassNotFoundException ex) {
            ex.printStackTrace(out);
        }
        return getRes();
    }

    static class Positive {
        int i = 0;
    }

    static class Negative {
        static {
            if (flag) {
                throw new Error("emulate bad initialization");
            }
        }
    }
}
