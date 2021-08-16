/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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

package gc.arguments;

import java.util.concurrent.Callable;

/**
 * Helper class which allocates memory.
 *
 * Typical usage:
 * <pre>
 * {@code
 *           AllocationHelper allocator = new AllocationHelper(MAX_ITERATIONS, ARRAY_LENGTH, CHUNK_SIZE,
 *                   () -> (verifier()));
 *           // Allocate byte[CHUNK_SIZE] ARRAY_LENGTH times. Total allocated bytes will be CHUNK_SIZE * ARRAY_LENGTH + refs length.
 *           // Then invoke verifier and iterate MAX_ITERATIONS times.
 *           allocator.allocateMemoryAndVerify();
 * }
 * </pre>
 */
public final class AllocationHelper {

    private final int arrayLength;
    private final int maxIterations;
    private final int chunkSize;

    // garbageStorage is used to store link to garbage to prevent optimization.
    private static Object garbageStorage;
    private byte garbage[][];
    private final Callable<?> verifierInstance;

    /**
     * Create an AllocationHelper with specified iteration count, array length, chunk size and verifier.
     *
     * @param maxIterations
     * @param arrayLength
     * @param chunkSize
     * @param verifier - Callable instance which will be invoked after all allocation cycle. Can be null;
     */
    public AllocationHelper(int maxIterations, int arrayLength, int chunkSize, Callable<?> verifier) {
        if ((arrayLength <= 0) || (maxIterations <= 0) || (chunkSize <= 0)) {
            throw new IllegalArgumentException("maxIterations, arrayLength and chunkSize should be greater then 0.");
        }
        this.arrayLength = arrayLength;
        this.maxIterations = maxIterations;
        this.chunkSize = chunkSize;
        verifierInstance = verifier;
        garbage = new byte[this.arrayLength][];
        garbageStorage = garbage;
    }

    private void allocateMemoryOneIteration() {
        for (int j = 0; j < arrayLength; j++) {
            garbage[j] = new byte[chunkSize];
        }
    }

    /**
     * Allocate memory and invoke Verifier during all iteration.
     *
     * @throws java.lang.Exception
     */
    public void allocateMemoryAndVerify() throws Exception {
        for (int i = 0; i < maxIterations; i++) {
            allocateMemoryOneIteration();
            if (verifierInstance != null) {
                verifierInstance.call();
            }
        }
    }

    /**
     * The same as allocateMemoryAndVerify() but hides OOME
     *
     * @throws Exception
     */
    public void allocateMemoryAndVerifyNoOOME() throws Exception {
        try {
            allocateMemoryAndVerify();
        } catch (OutOfMemoryError e) {
            // exit on OOME
        }
    }

    /**
     * Release link to allocated garbage to make it available for further GC
     */
    public void release() {
        if (garbage != null) {
            garbage = null;
            garbageStorage = null;
        }
    }
}
