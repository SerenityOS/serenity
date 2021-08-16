/*
 * Copyright (c) 2017, 2020, Red Hat, Inc. All rights reserved.
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

/**
 * @test id=aggressive
 * @summary Tests JVMTI heap dumps
 * @requires vm.gc.Shenandoah
 * @requires vm.jvmti
 * @compile TestHeapDump.java
 * @run main/othervm/native/timeout=300 -agentlib:TestHeapDump
 *      -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -Xmx128m
 *      -XX:ShenandoahGCHeuristics=aggressive
 *      TestHeapDump
 *
 */

/**
 * @test id=no-coops-aggressive
 * @summary Tests JVMTI heap dumps
 * @requires vm.gc.Shenandoah
 * @requires vm.jvmti
 * @requires vm.bits == "64"
 * @compile TestHeapDump.java
 * @run main/othervm/native/timeout=300 -agentlib:TestHeapDump
 *      -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -Xmx128m
 *      -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:-UseCompressedOops TestHeapDump
 */

/**
 * @test id=aggressive-strdedup
 * @summary Tests JVMTI heap dumps
 * @requires vm.gc.Shenandoah
 * @requires vm.jvmti
 * @compile TestHeapDump.java
 * @run main/othervm/native/timeout=300 -agentlib:TestHeapDump
 *      -XX:+UnlockDiagnosticVMOptions -XX:+UnlockExperimentalVMOptions
 *      -XX:+UseShenandoahGC -Xmx128m
 *      -XX:ShenandoahGCHeuristics=aggressive
 *      -XX:+UseStringDeduplication TestHeapDump
 */

import java.lang.ref.Reference;

public class TestHeapDump {

    private static final int NUM_ITER = 10000;

    private static final int ARRAY_SIZE = 1000;

    private static final int EXPECTED_OBJECTS =
            ARRAY_SIZE +   // array reachable from instance field
                    1 +            // static field root
                    1;             // local field root

    static {
        try {
            System.loadLibrary("TestHeapDump");
        } catch (UnsatisfiedLinkError ule) {
            System.err.println("Could not load TestHeapDump library");
            System.err.println("java.library.path: "
                    + System.getProperty("java.library.path"));
            throw ule;
        }
    }

    native static int heapdump(Class<?> filterClass);

    public static void main(String args[]) {
        new TestHeapDump().run();
    }

    // This root needs to be discovered
    static Object root = new TestObject();

    // This field needs to be discovered
    TestObject[] array;

    public void run() {
        array = new TestObject[ARRAY_SIZE];
        for (int i = 0; i < ARRAY_SIZE; i++) {
            array[i] = new TestObject();
        }
        TestObject localRoot = new TestObject();
        for (int i = 0; i < NUM_ITER; i++) {
            int numObjs = heapdump(TestObject.class);
            if (numObjs != EXPECTED_OBJECTS) {
                throw new RuntimeException("Expected " + EXPECTED_OBJECTS + " objects, but got " + numObjs);
            }
        }
        Reference.reachabilityFence(array);
        Reference.reachabilityFence(localRoot);
    }

    // We look for the instances of this class during the heap scan
    public static class TestObject {}
}
