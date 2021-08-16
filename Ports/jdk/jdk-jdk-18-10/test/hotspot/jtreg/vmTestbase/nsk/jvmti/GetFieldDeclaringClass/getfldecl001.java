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

public class getfldecl001 {
    final static int exit_delta = 95;

    static {
        try {
            System.loadLibrary("getfldecl001");
        } catch (UnsatisfiedLinkError err) {
            System.err.println("Could not load getfldecl001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw err;
        }
    }

    public static void main(String argv[]) {
        argv = nsk.share.jvmti.JVMTITest.commonInit(argv);

        System.exit(run(argv,System.out) + exit_delta);
    }

    native static void check(int i, Class cls1, Class cls2);
    native static int getResult();

    public static int run(String argv[], PrintStream out) {
        Class cls = getfldecl001c.class;

        check(0, cls, getfldecl001a.class);
        check(1, cls, getfldecl001b.class);
        check(2, cls, getfldecl001c.class);

        return getResult();
    }
}

interface getfldecl001a {
    int x = 0;

    public int x();
}

class getfldecl001b implements getfldecl001a {
    int y = 0;

    public int x() {
        return -1;
    }
}

class getfldecl001c extends getfldecl001b {
    int z = 0;

    public int y() {
        return -1;
    }
}
