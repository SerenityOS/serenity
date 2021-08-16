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

package nsk.jvmti.RedefineClasses;

import java.io.*;

public class redefclass027 {

    final static int FAILED = 2;
    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("redefclass027");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load redefclass027 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady(Class cls, byte bytes[]);
    native static int check();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        // Read data from class
        byte[] bytes;
        String fileName =
            redefclass027a.class.getName().replace('.', File.separatorChar) +
            ".class";
        try {
            ClassLoader cl = redefclass027.class.getClassLoader();
            InputStream in = cl.getSystemResourceAsStream(fileName);
            if (in == null) {
                out.println("# Class file \"" + fileName + "\" not found");
                return FAILED;
            }
            bytes = new byte[in.available()];
            in.read(bytes);
            in.close();
        } catch (Exception ex) {
            out.println("# Unexpected exception while reading class file:");
            out.println("# " + ex);
            return FAILED;
        }

        redefclass027a thr = new redefclass027a();
        getReady(redefclass027a.class, bytes);

        thr.start();
        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }

        return check();
    }
}

class redefclass027a extends Thread {
    static int staticInt;
    int instanceInt;

    public void run() {
        int localInt1 = 1; // redefclass027.c::frames[0] // redefclass027.c::frames[1]
        instanceInt = 100;
        staticInt = 200; // redefclass027.c::frames[2]
        method1(10);
    }

    void method1(int argInt1) {
        int localInt2 = 2;
        if (staticInt > 0) {
            try {
                method2(20);
            } catch (Exception ex) { // // redefclass027.c::frames[5]
                int localInt3 = 3;
            }
        }
    } // redefclass027.c::frames[6]

    void method2(int argInt2) throws Exception {
        int localInt4 = 4;
        if (instanceInt > 0) { // redefclass027.c::frames[3]
            throw new Exception("test"); // redefclass027.c::frames[4]
        }
    }
}
