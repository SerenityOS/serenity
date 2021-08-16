/*
 * Copyright (c) 2018, Red Hat, Inc. All rights reserved.
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
 *
 */

/* @test id=aggressive-verify
 * @summary Test JNI Global Refs with Shenandoah
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm/native -Xmx1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+ShenandoahVerify
 *      TestJNIGlobalRefs
 */

/* @test id=aggressive
 * @summary Test JNI Global Refs with Shenandoah
 * @requires vm.gc.Shenandoah
 *
 * @run main/othervm/native -Xmx1g -Xlog:gc -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -XX:ShenandoahGCHeuristics=aggressive
 *      TestJNIGlobalRefs
 */

import java.util.Arrays;
import java.util.Random;

public class TestJNIGlobalRefs {
    static {
        System.loadLibrary("TestJNIGlobalRefs");
    }

    private static final int TIME_MSEC = 120000;
    private static final int ARRAY_SIZE = 10000;

    private static native void makeGlobalRef(Object o);
    private static native void makeWeakGlobalRef(Object o);
    private static native Object readGlobalRef();
    private static native Object readWeakGlobalRef();

    public static void main(String[] args) throws Throwable {
        seedGlobalRef();
        seedWeakGlobalRef();
        long start = System.currentTimeMillis();
        long current = start;
        while (current - start < TIME_MSEC) {
            testGlobal();
            testWeakGlobal();
            Thread.sleep(1);
            current = System.currentTimeMillis();
        }
    }

    private static void seedGlobalRef() {
        int[] a = new int[ARRAY_SIZE];
        fillArray(a, 1337);
        makeGlobalRef(a);
    }

    private static void seedWeakGlobalRef() {
        int[] a = new int[ARRAY_SIZE];
        fillArray(a, 8080);
        makeWeakGlobalRef(a);
    }

    private static void testGlobal() {
        int[] a = (int[]) readGlobalRef();
        checkArray(a, 1337);
    }

    private static void testWeakGlobal() {
        int[] a = (int[]) readWeakGlobalRef();
        if (a != null) {
            checkArray(a, 8080);
        } else {
            // weak reference is cleaned, recreate:
            seedWeakGlobalRef();
        }
    }

    private static void fillArray(int[] array, int seed) {
        Random r = new Random(seed);
        for (int i = 0; i < ARRAY_SIZE; i++) {
            array[i] = r.nextInt();
        }
    }

    private static void checkArray(int[] array, int seed) {
        Random r = new Random(seed);
        if (array.length != ARRAY_SIZE) {
            throw new IllegalStateException("Illegal array length: " + array.length + ", but expected " + ARRAY_SIZE);
        }
        for (int i = 0; i < ARRAY_SIZE; i++) {
            int actual = array[i];
            int expected = r.nextInt();
            if (actual != expected) {
                throw new IllegalStateException("Incorrect array data: " + actual + ", but expected " + expected);
            }
        }
    }
}
