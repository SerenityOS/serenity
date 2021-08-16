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
package org.openjdk.bench.vm.compiler;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class WriteBarrier {

    // For array references
    public static final int NUM_REFERENCES_SMALL = 32;
    public static final int NUM_REFERENCES_LARGE = 2048;

    // For array update tests
    private Object[] theArraySmall;
    private Object[] realReferencesSmall;
    private Object[] nullReferencesSmall;
    private int[] indicesSmall;

    private Object[] theArrayLarge;
    private Object[] realReferencesLarge;
    private Object[] nullReferencesLarge;
    private int[] indicesLarge;

    // For field update tests
    public Referencer head = null;
    public Referencer tail = null;

    // For random number generation
    private int m_w;
    private int m_z;

    // For field references
    public class Referencer {
        Referencer next = null;
        Referencer() {
            this.next = null;
        }
        void append(Referencer r) {
            this.next = r;
        }
        void clear() {
            this.next = null;
        }
    }

    @Setup
    public void setup() {
        theArraySmall = new Object[NUM_REFERENCES_SMALL];
        realReferencesSmall = new Object[NUM_REFERENCES_SMALL];
        nullReferencesSmall = new Object[NUM_REFERENCES_SMALL];
        indicesSmall = new int[NUM_REFERENCES_SMALL];

        theArrayLarge = new Object[NUM_REFERENCES_LARGE];
        realReferencesLarge = new Object[NUM_REFERENCES_LARGE];
        nullReferencesLarge = new Object[NUM_REFERENCES_LARGE];
        indicesLarge = new int[NUM_REFERENCES_LARGE];

        m_w = (int) System.currentTimeMillis();
        Random random = new Random();
        m_z = random.nextInt(10000) + 1;

        for (int i = 0; i < NUM_REFERENCES_SMALL; i++) {
            indicesSmall[i] = get_random() % (NUM_REFERENCES_SMALL - 1);
            realReferencesSmall[i] = new Object();
        }

        for (int i = 0; i < NUM_REFERENCES_LARGE; i++) {
            indicesLarge[i] = get_random() % (NUM_REFERENCES_LARGE - 1);
            realReferencesLarge[i] = new Object();
        }

        // Build a small linked structure
        this.head = new Referencer();
        this.tail = new Referencer();
        this.head.append(this.tail);

        // This will (hopefully) promote objects to old space
        // Run with -XX:+DisableExplicitGC to keep
        // objects in young space
        System.gc();
    }

    private int get_random() {
        m_z = 36969 * (m_z & 65535) + (m_z >> 16);
        m_w = 18000 * (m_w & 65535) + (m_w >> 16);
        return Math.abs((m_z << 16) + m_w);  /* 32-bit result */
    }

    @Benchmark
    public void testArrayWriteBarrierFastPathRealSmall() {
        for (int i = 0; i < NUM_REFERENCES_SMALL; i++) {
            theArraySmall[indicesSmall[NUM_REFERENCES_SMALL - i - 1]] = realReferencesSmall[indicesSmall[i]];
        }
    }

    @Benchmark
    public void testArrayWriteBarrierFastPathNullSmall() {
        for (int i = 0; i < NUM_REFERENCES_SMALL; i++) {
            theArraySmall[indicesSmall[NUM_REFERENCES_SMALL - i - 1]] = nullReferencesSmall[indicesSmall[i]];
        }
    }

    @Benchmark
    public void testArrayWriteBarrierFastPathRealLarge() {
        for (int i = 0; i < NUM_REFERENCES_LARGE; i++) {
            theArrayLarge[indicesLarge[NUM_REFERENCES_LARGE - i - 1]] = realReferencesLarge[indicesLarge[i]];
        }
    }

    @Benchmark
    public void testArrayWriteBarrierFastPathNullLarge() {
        for (int i = 0; i < NUM_REFERENCES_LARGE; i++) {
            theArrayLarge[indicesLarge[NUM_REFERENCES_LARGE - i - 1]] = nullReferencesLarge[indicesLarge[i]];
        }
    }

    @Benchmark()
    public void testFieldWriteBarrierFastPath() {
        // Shuffle everything around
        this.tail.append(this.head);
        this.head.clear();
        this.head.append(this.tail);
        this.tail.clear();
    }
}
