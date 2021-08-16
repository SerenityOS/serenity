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

package nsk.jvmti.GetMethodDeclaringClass;

import java.io.PrintStream;

public class declcls001 {

    static {
        try {
            System.loadLibrary("declcls001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load declcls001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static int check();

    static int ifld;

    public static void main(String[] args) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        System.exit(run(args, System.out) + 95/*STATUS_TEMP*/);
    }

    public static int run(String argv[], PrintStream ref) {
        return check();
    }

    static void meth(int i) {
        ifld = i;
    }

    class Inn {
        String fld;
        void meth_inn(String s) {
            fld = s;
        }
    }
}

class declcls001a extends declcls001 {
}

class declcls001b extends declcls001a {
    public void meth_b() {
    }
}

class declcls001z {
    int meth_z() {
        return 100;
    }
}

interface declcls001i {
    int meth_i();
}

interface declcls001i1 extends declcls001i {
    int meth_i1();
}

abstract class declcls001i_a extends declcls001z implements declcls001i1 {
    public int meth_i1() {
        return 1;
    }
}
