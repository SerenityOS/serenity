/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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
package org.openjdk.bench.java.util;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

@State(Scope.Thread)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Fork(value = 3)
@Warmup(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
public class UUIDBench {

    @Param("20000")
    private int size;

    private byte[][] uuidBytes;

    private UUID[] uuids;

    private String[] uuidStrings;

    private int index = 0;

    @Setup
    public void setup() {
        uuidBytes = new byte[size][];
        uuids = new UUID[size];
        uuidStrings = new String[size];
        java.util.Random r = new java.util.Random(0);
        for (int i = 0; i < this.uuidStrings.length; i++) {
            final UUID uuid = UUID.randomUUID();
            this.uuidBytes[i] = new byte[16];
            r.nextBytes(this.uuidBytes[i]);
            this.uuids[i] = uuid;
            this.uuidStrings[i] = uuid.toString();
        }
    }

    @Setup(Level.Iteration)
    public void setupIteration() {
        index++;
        if (index >= size) {
            index = 0;
        }
    }

    @Benchmark
    public UUID fromString() {
        return UUID.fromString(uuidStrings[index]);
    }

    @Benchmark
    public String toString() {
        return uuids[index].toString();
    }

    @Benchmark
    public UUID fromType3Bytes() {
        return UUID.nameUUIDFromBytes(uuidBytes[index]);
    }
}
