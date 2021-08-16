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

package nsk.jvmti.ClassPrepare;

import java.io.PrintStream;

public class classprep001 {

    final static int JCK_STATUS_BASE = 95;

    static {
        try {
            System.loadLibrary("classprep001");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load classprep001 library");
            System.err.println("java.library.path:"
                + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static void getReady(Thread thread);
    native static int check(Thread thread);

    public static void main(String args[]) {
        args = nsk.share.jvmti.JVMTITest.commonInit(args);

        // produce JCK-like exit status.
        System.exit(run(args, System.out) + JCK_STATUS_BASE);
    }

    public static int run(String args[], PrintStream out) {
        Thread otherThread = new Thread(() -> {
            new TestClass2().run();
        });

        getReady(Thread.currentThread());

        // should generate the events
        new TestClass().run();

        // loading classes on other thread should not generate the events
        otherThread.start();
        try {
            otherThread.join();
        } catch (InterruptedException e) {
        }

        return check(Thread.currentThread());
    }

    interface TestInterface {
        int constant = Integer.parseInt("10");
        void run();
    }

    static class TestClass implements TestInterface {
        static int i = 0;
        int count = 0;
        static {
            i++;
        }
        public void run() {
            count++;
        }
    }

    interface TestInterface2 {
        void run();
    }

    static class TestClass2 implements TestInterface2 {
        public void run() {
        }
    }
}
