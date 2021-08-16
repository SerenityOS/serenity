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

package nsk.jvmti.GetImplementedInterfaces;

import java.io.PrintStream;

public class getintrf007 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("getintrf007");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load getintrf007 library");
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

    public static int run(String args[], PrintStream out) {
        try {
            check(0, Class.forName(InnerClass1.class.getName()));
            check(1, Class.forName(InnerInterface1.class.getName()));
            check(2, Class.forName(InnerInterface2.class.getName()));
            check(3, Class.forName(InnerClass2.class.getName()));
            check(4, Class.forName(OuterClass1.class.getName()));
            check(5, Class.forName(OuterClass2.class.getName()));
            check(6, Class.forName(OuterInterface1.class.getName()));
            check(7, Class.forName(OuterClass3.class.getName()));
            check(8, Class.forName(OuterInterface2.class.getName()));
            check(9, Class.forName(OuterClass4.class.getName()));
            check(10,Class.forName(OuterClass5.class.getName()));
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return getRes();
    }

    class InnerClass1 {
    }

    interface InnerInterface1 {
    }

    static interface InnerInterface2 extends InnerInterface1 {
    }

    static class InnerClass2 implements InnerInterface2 {
    }
}

class OuterClass1 {
}

class OuterClass2 extends OuterClass1 {
}

interface OuterInterface1 {
}

class OuterClass3 implements OuterInterface1 {
}

interface OuterInterface2 extends OuterInterface1 {
}

abstract class OuterClass4 extends OuterClass3 implements OuterInterface2 {
}

class OuterClass5 extends OuterClass4 {
}
