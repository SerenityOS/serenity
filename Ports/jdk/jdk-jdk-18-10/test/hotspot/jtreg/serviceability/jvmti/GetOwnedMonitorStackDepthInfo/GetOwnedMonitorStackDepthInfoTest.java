/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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


/**
 * @test
 * @bug 8153629
 * @summary Need to cover JVMTI's GetOwnedMonitorStackDepthInfo function
 * @requires vm.jvmti
 * @compile GetOwnedMonitorStackDepthInfoTest.java
 * @run main/othervm/native -agentlib:GetOwnedMonitorStackDepthInfoTest GetOwnedMonitorStackDepthInfoTest
 */


public class GetOwnedMonitorStackDepthInfoTest {

    static {
        try {
            System.loadLibrary("GetOwnedMonitorStackDepthInfoTest");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load GetOwnedMonitorStackDepthInfoTest library");
            System.err.println("java.library.path: "
                    + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    private static native int verifyOwnedMonitors();

    private static volatile int results = -1;


    public static void main(String[] args) throws Exception {

        new GetOwnedMonitorStackDepthInfoTest().runTest();

    }

    public void runTest() throws Exception {
        final Object lock1 = new Lock1();
        Thread t1 = new Thread(() -> {
            synchronized (lock1) {
                System.out.println("Thread in sync section 1: "
                        + Thread.currentThread().getName());
                test1();
            }
        });

        t1.start();
        t1.join();

        if (results != 0) {
            throw new RuntimeException("FAILED status returned from the agent");
        }

    }

    private synchronized void test1() {
        test2();
    }

    private void test2() {
        Object lock2 = new Lock2();
        synchronized (lock2) {
            System.out.println("Thread in sync section 2: "
                    + Thread.currentThread().getName());
            results = verifyOwnedMonitors();
        }

    }

    private static class Lock1 {

    }

    private static class Lock2 {

    }
}

