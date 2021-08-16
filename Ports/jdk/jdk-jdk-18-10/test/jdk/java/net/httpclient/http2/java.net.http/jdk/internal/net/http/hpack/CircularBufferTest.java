/*
 * Copyright (c) 2016, 2018, Oracle and/or its affiliates. All rights reserved.
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
package jdk.internal.net.http.hpack;

import org.testng.annotations.Test;
import jdk.internal.net.http.hpack.SimpleHeaderTable.CircularBuffer;

import java.util.Arrays;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;

import static jdk.internal.net.http.common.Utils.pow2Size;
import static org.testng.Assert.assertEquals;
import static jdk.internal.net.http.hpack.TestHelper.assertVoidThrows;
import static jdk.internal.net.http.hpack.TestHelper.newRandom;
import static org.testng.Assert.assertTrue;

public final class CircularBufferTest {

    private final Random random = newRandom();

    @Test
    public void queue() {
        for (int capacity = 1; capacity <= 2048; capacity++) {
            queueOnce(capacity, 32);
        }
    }

    @Test
    public void resize() {
        for (int capacity = 1; capacity <= 4096; capacity++) {
            resizeOnce(capacity);
        }
    }

    @Test
    public void downSizeEmptyBuffer() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(16);
        buffer.resize(15);
    }

    @Test
    public void newCapacityLessThanCurrentSize1() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(0);
        buffer.resize(5);
        buffer.add(1);
        buffer.add(1);
        buffer.add(1);
        assertVoidThrows(IllegalStateException.class, () -> buffer.resize(2));
        assertVoidThrows(IllegalStateException.class, () -> buffer.resize(1));
    }

    @Test
    public void newCapacityLessThanCurrentSize2() {
        CircularBuffer<Integer> buffer = new CircularBuffer<>(5);
        buffer.add(1);
        buffer.add(1);
        buffer.add(1);
        assertVoidThrows(IllegalStateException.class, () -> buffer.resize(2));
        assertVoidThrows(IllegalStateException.class, () -> buffer.resize(1));
    }

    private void resizeOnce(int capacity) {

        capacity = pow2Size(capacity);

        int nextNumberToPut = 0;

        Queue<Integer> referenceQueue = new ArrayBlockingQueue<>(capacity);
        CircularBuffer<Integer> buffer = new CircularBuffer<>(capacity);

        // Fill full, so the next add will wrap
        for (int i = 0; i < capacity; i++, nextNumberToPut++) {
            buffer.add(nextNumberToPut);
            referenceQueue.add(nextNumberToPut);
        }
        int gets = random.nextInt(capacity); // [0, capacity)
        for (int i = 0; i < gets; i++) {
            referenceQueue.poll();
            buffer.remove();
        }
        int puts = random.nextInt(gets + 1); // [0, gets]
        for (int i = 0; i < puts; i++, nextNumberToPut++) {
            buffer.add(nextNumberToPut);
            referenceQueue.add(nextNumberToPut);
        }

        Integer[] expected = referenceQueue.toArray(new Integer[0]);
        buffer.resize(expected.length);

        boolean equals = Arrays.equals(buffer.elements, 0, buffer.size,
                                       expected, 0, expected.length);
        assertTrue(equals);
    }

    private void queueOnce(int capacity, int numWraps) {

        capacity = pow2Size(capacity);

        Queue<Integer> referenceQueue = new ArrayBlockingQueue<>(capacity);
        CircularBuffer<Integer> buffer = new CircularBuffer<>(capacity);

        int nextNumberToPut = 0;
        int totalPuts = 0;
        int putsLimit = capacity * numWraps;
        int remainingCapacity = capacity;
        int size = 0;

        while (totalPuts < putsLimit) {
            assert remainingCapacity + size == capacity;
            int puts = random.nextInt(remainingCapacity + 1); // [0, remainingCapacity]
            remainingCapacity -= puts;
            size += puts;
            for (int i = 0; i < puts; i++, nextNumberToPut++) {
                referenceQueue.add(nextNumberToPut);
                buffer.add(nextNumberToPut);
            }
            totalPuts += puts;
            int gets = random.nextInt(size + 1); // [0, size]
            size -= gets;
            remainingCapacity += gets;
            for (int i = 0; i < gets; i++) {
                Integer expected = referenceQueue.poll();
                Integer actual = buffer.remove();
                assertEquals(actual, expected);
            }
        }
    }
}
