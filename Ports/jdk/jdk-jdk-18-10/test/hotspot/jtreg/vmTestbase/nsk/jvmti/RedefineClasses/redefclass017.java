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

package nsk.jvmti.RedefineClasses;

import java.io.*;

public class redefclass017 {

    final static int FAILED = 2;
    final static int JCK_STATUS_BASE = 95;

    static String fileDir = ".";

    static {
        try {
            System.loadLibrary("redefclass017");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load redefclass017 library");
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
        if (args.length > 0) {
            fileDir = args[0];
        }

        // Read data from class
        byte[] bytes;
        String fileName = fileDir + File.separator + "newclass" + File.separator +
            redefclass017a.class.getName().replace('.', File.separatorChar) +
            ".class";
        try {
            FileInputStream in = new FileInputStream(fileName);
            bytes = new byte[in.available()];
            in.read(bytes);
            in.close();
        } catch (Exception ex) {
            out.println("# Unexpected exception while reading class file:");
            out.println("# " + ex);
            return FAILED;
        }

        getReady(redefclass017a.class, bytes);

        redefclass017a thr = new redefclass017a();
        thr.sout = out;
        thr.start();
        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }

        return (thr.testFailed ? FAILED : check());
    }
}

class redefclass017a extends Thread {
    public static boolean testFailed = false;
    public static PrintStream sout;

    public void run() {
        try {
            checkPoint();
            sout.println("# checkPoint() does not throw any exception");
            testFailed = true;
        } catch (IOException ex) {
            // OK
        } catch (Exception ex) {
            sout.println("# checkPoint() throws unexpected exception:");
            sout.println("# " + ex);
            testFailed = true;
        }
    }

    // method to be breakpointed in agent
    private void checkPoint() throws IOException {
        throw new IOException("original");
    }
}
