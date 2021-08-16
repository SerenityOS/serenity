/*
 * Copyright (c) 2003, 2021, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.GetMethodModifiers;

import java.io.PrintStream;

public class methmod001 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("methmod001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load methmod001 library");
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

    public static int run(String argv[], PrintStream ref) {
        return check();
    }

    protected static final float[] meth_stat(int i, String s) {
        return new float[i];
    }

    private char meth_1(char c1) {
        return c1;
    }

    class Inn {
        String fld;
        public synchronized final void meth_inn(String s) {
            fld = s;
        }
    }
}

abstract class methmod001a {
    synchronized methmod001 meth_new() {
        return new methmod001();
    }

    abstract void meth_abs();
}
