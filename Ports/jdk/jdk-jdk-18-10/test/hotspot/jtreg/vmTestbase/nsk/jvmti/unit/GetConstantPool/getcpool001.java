/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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

package nsk.jvmti.unit.GetConstantPool;

import java.io.PrintStream;
import nsk.share.Consts;

public class getcpool001 {

    final static int NESTING_DEPTH = 0;

    static {
        try {
            System.loadLibrary("getcpool001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load getcpool001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady(Class cls);
    native static void getCP(int id, Class cls);
    native static int check();

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        int errCode = run(args, System.out);
        System.exit(errCode + Consts.JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        TestThread thr = new TestThread();
        Long[] larr = new Long[3];

        thr.start();
        getReady(TestThread.class);
        getCP(1, TestThread.class);
        getCP(2, getcpool001.class);
        getCP(3, String.class);
        getCP(4, TestThread.InnerStat.class);
        getCP(5, Long.class);
        getCP(6, Integer.class);

        try {
            thr.join();
        } catch (InterruptedException e) {
            throw new Error("Unexpected " + e);
        }
        return check();
    }

    static class TestThread extends Thread {
        // Some constants to fill in the class constant pool
        boolean    bl = true;
        byte       bt = 127;
        char       cc = 256;
        short      ss = 1024;
        int        ii = 1000000;
        long       ll = 1000000000L;
        float      ff = 0.134f;
        double     dd = 0.999;
        String     str = "Unused String";
        InnerStat  me  = new InnerStat();

        public void run() {
           checkPoint();
        }

        // dummy method to be breakpointed in agent
        void checkPoint() {
        }

        static class InnerStat {
           static InnerStat me = new InnerStat();
        }
    }
}
