/*
 * Copyright (c) 2015, 2016 SAP SE. All rights reserved.
 * Copyright (c) 2018 Red Hat, Inc. All rights reserved.
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

/* @test
 * @bug 8207838
 * @summary Regression test for passing float args to a synchronized jni function.
 *
 *
 * @run main/othervm/native compiler.floatingpoint.TestFloatSyncJNIArgs
 */

package compiler.floatingpoint;

public class TestFloatSyncJNIArgs {
    static {
        try {
            System.loadLibrary("TestFloatSyncJNIArgs");
        } catch (UnsatisfiedLinkError e) {
            System.out.println("could not load native lib: " + e);
        }
    }

    private static final int numberOfThreads = 8;

    static volatile Error testFailed = null;

    public synchronized static native float combine15floats(
        float f1, float f2, float f3, float f4,
        float f5, float f6, float f7, float f8,
        float f9, float f10, float f11, float f12,
        float f13, float f14, float f15);

    public synchronized static native double combine15doubles(
        double d1, double d2, double d3, double d4,
        double d5, double d6, double d7, double d8,
        double d9, double d10, double d11, double d12,
        double d13, double d14, double d15);

    static void test() throws Exception {
        Thread[] threads = new Thread[numberOfThreads];

        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    float f = combine15floats(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f,
                                              9, 10, 11, 12, 13, 14, 15);
                    if (f != 81720.0f) {
                        testFailed = new Error("jni function didn't combine 15 float args properly: " + f);
                        throw testFailed;
                    }
                }
            });
        }
        for (int i = 0; i < numberOfThreads; i++) {
            threads[i].start();
        }
        for (int i = 0; i < numberOfThreads; i++) {
            threads[i].join();
        }
        if (testFailed != null) {
            throw testFailed;
        }

        for (int i = 0; i < numberOfThreads; i++) {
            threads[i] = new Thread(() -> {
                for (int j = 0; j < 10000; j++) {
                    double d = combine15doubles(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0,
                                                9, 10, 11, 12, 13, 14, 15);
                    if (d != 81720.0) {
                        testFailed = new Error("jni function didn't combine 15 double args properly: " + d);
                        throw testFailed;
                    }
                }
            });
        }
        for (int i = 0; i < numberOfThreads; i++) {
            threads[i].start();
        }
        for (int i = 0; i < numberOfThreads; i++) {
            threads[i].join();
        }
        if (testFailed != null) {
            throw testFailed;
        }
    }

    public static void main(String[] args) throws Exception {
        for (int i = 0; i < 200; ++i) {
            test();
        }
    }
}
