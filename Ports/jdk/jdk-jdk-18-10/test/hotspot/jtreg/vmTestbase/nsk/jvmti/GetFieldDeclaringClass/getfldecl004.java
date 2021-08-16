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

package nsk.jvmti.GetFieldDeclaringClass;

import java.io.PrintStream;

public class getfldecl004 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("getfldecl004");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load getfldecl004 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void check(int i, Class cls1, Class cls2);
    native static int getRes();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        check(0, InnerClass2.class, InnerInterface1.class);
        check(1, InnerClass2.class, InnerInterface2.class);
        check(2, InnerClass2.class, InnerClass1.class);
        check(3, InnerClass2.class, InnerClass1.class);
        check(4, InnerClass2.class, InnerClass2.class);
        check(5, InnerClass2.class, InnerClass2.class);
        check(6, OuterClass2.class, OuterInterface1.class);
        check(7, OuterClass2.class, OuterInterface2.class);
        check(8, OuterClass2.class, OuterClass1.class);
        check(9, OuterClass2.class, OuterClass1.class);
        check(10, OuterClass2.class, OuterClass2.class);
        check(11, OuterClass2.class, OuterClass2.class);
        return getRes();
    }

    static interface InnerInterface1 {
        int staticField_ii1 = 0;
    }

    static interface InnerInterface2 extends InnerInterface1 {
        int staticField_ii2 = 1;
    }

    static abstract class InnerClass1 implements InnerInterface2 {
        static int staticField_ic1 = 2;
        int instanceField_ic1 = 3;
    }

    static class InnerClass2 extends InnerClass1 {
        static int staticField_ic2 = 4;
        int instanceField_ic2 = 5;
    }
}

interface OuterInterface1 {
    int staticField_oi1 = 6;
}

interface OuterInterface2 extends OuterInterface1 {
    int staticField_oi2 = 7;
}

abstract class OuterClass1 implements OuterInterface2 {
    static int staticField_oc1 = 8;
    int instanceField_oc1 = 9;
}

class OuterClass2 extends OuterClass1 {
    static int staticField_oc2 = 10;
    int instanceField_oc2 = 11;
}
