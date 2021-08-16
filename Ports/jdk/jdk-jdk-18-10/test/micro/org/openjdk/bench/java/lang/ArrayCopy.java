/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * Benchmark measuring System.arraycopy in different ways.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class ArrayCopy {

    private static final byte[] TEST_BYTES = "HTTP/1.0".getBytes();
    private static final char[] TEST_CHARS = new char[46];
    private static final Object[] TEST_OBJECTS = new Object[200];  // Uses a minimum of 160 internal positions for internal copying

    // a length which the compiler cannot prove is a constant
    public static int nonConstCharLength = TEST_CHARS.length;
    public static int nonConstByteLength = TEST_BYTES.length;
    public static int nonConstObjectLength = TEST_OBJECTS.length;

    // Use this array to copy objects in.
    public char[] dummyCharArray = new char[TEST_CHARS.length];
    public byte[] dummyByteArray = new byte[TEST_BYTES.length];
    public Object[] dummyObjectArray = new Object[TEST_OBJECTS.length];

    @Setup
    public void setup() {
        for (int i = 0; i < TEST_OBJECTS.length; i++) {
            TEST_OBJECTS[i] = new Object();
            dummyObjectArray[i] = new Object();
        }
    }

    /**
     * This test case do the same work as testArrayCopy. We should make sure
     * testArrayCopy is equally fast or better. Compare the two and you measure
     * the system call versus explicit copy for-loop.
     */
    @Benchmark
    public void copyLoop() {
        for (int j = 0; j < dummyByteArray.length; j++) {
            dummyByteArray[j] = TEST_BYTES[j];
        }
    }

    /**
     * Test that we can optimize away the code since it should not have any side
     * effects
     */
    @Benchmark
    public void copyLoopLocalArray() {
        byte[] localDummyByteArray = new byte[TEST_BYTES.length];
        for (int j = 0; j < localDummyByteArray.length; j++) {
            localDummyByteArray[j] = TEST_BYTES[j];
        }
    }

    /**
     * This test case do the same work as testArrayCopy. We should make sure
     * testArrayCopy is equally fast or better. Compare the two and you measure
     * the system call versus explicit copy for-loop.
     * <p/>
     * Uses non-provable constant length.
     */
    @Benchmark
    public void copyLoopNonConst() {
        for (int i = 0; i < nonConstByteLength; i++) {
            dummyByteArray[i] = TEST_BYTES[i];
        }
    }

    /**
     * This test case do the same work as testCopyLoop. We should make sure
     * testArrayCopy is equally fast or better. Compare the two and you measure
     * the system call versus explicit copy for-loop.
     */
    @Benchmark
    public void arrayCopy() {
        System.arraycopy(TEST_BYTES, 0, dummyByteArray, 0, dummyByteArray.length);
    }

    /**
     * Test that we can optimize away the code since it should not have any side
     * effects
     */
    @Benchmark
    public void arrayCopyLocalArray() {
        byte[] localDummyByteArray = new byte[TEST_BYTES.length];
        System.arraycopy(TEST_BYTES, 0, localDummyByteArray, 0, localDummyByteArray.length);
    }

    /**
     * This test case do the same work as testCopyLoop. We should make sure
     * testArrayCopy is equally fast or better. Compare the two and you measure
     * the system call versus explicit copy for-loop.
     * <p/>
     * Uses non-provable constant length.
     */
    @Benchmark
    public void arrayCopyNonConst() {
        System.arraycopy(TEST_BYTES, 0, dummyByteArray, 0, nonConstByteLength);
    }

    @Benchmark
    public void arrayCopyChar() {
        System.arraycopy(TEST_CHARS, 0, dummyCharArray, 0, dummyCharArray.length);
    }

    @Benchmark
    public void arrayCopyCharNonConst() {
        System.arraycopy(TEST_CHARS, 0, dummyCharArray, 0, nonConstCharLength);
    }

    @Benchmark
    public void arrayCopyObject() {
        System.arraycopy(TEST_OBJECTS, 0, dummyObjectArray, 0, dummyObjectArray.length);
    }

    @Benchmark
    public void arrayCopyObjectNonConst() {
        System.arraycopy(TEST_OBJECTS, 0, dummyObjectArray, 0, nonConstObjectLength);
    }

    /**
     * This test copies inside a object array, that is same source array as dest
     * array. Copies backwards in the array.
     */
    @Benchmark
    @OperationsPerInvocation(40)
    public void arrayCopyObjectSameArraysBackward() {
        for (int i = 0; i < 40; i++) {
            System.arraycopy(dummyObjectArray, i, dummyObjectArray, i + 40, 80);
        }
    }

    /**
     * This test copies inside a object array, that is same source array as dest
     * array. Copies forward in the array. There is a special version for this
     * in JRockit.
     */
    @Benchmark
    @OperationsPerInvocation(40)
    public void arrayCopyObjectSameArraysForward() {
        for (int i = 0; i < 40; i++) {
            System.arraycopy(dummyObjectArray, i + 40, dummyObjectArray, i, 80);
        }
    }
}
