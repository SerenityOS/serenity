/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

/*

Sample runs on Ryzen 3700X:

before 8247532:

Benchmark                                 (length)  Mode  Cnt     Score    Error  Units
RecordDeserialization.deserializeClasses        10  avgt   10     8.382 :  0.013  us/op
RecordDeserialization.deserializeClasses       100  avgt   10    33.736 :  0.171  us/op
RecordDeserialization.deserializeClasses      1000  avgt   10   271.224 :  0.953  us/op
RecordDeserialization.deserializeRecords        10  avgt   10    58.606 :  0.446  us/op
RecordDeserialization.deserializeRecords       100  avgt   10   530.044 :  1.752  us/op
RecordDeserialization.deserializeRecords      1000  avgt   10  5335.624 : 44.942  us/op

after 8247532:

Benchmark                                 (length)  Mode  Cnt    Score   Error  Units
RecordDeserialization.deserializeClasses        10  avgt   10    8.681 : 0.155  us/op
RecordDeserialization.deserializeClasses       100  avgt   10   32.496 : 0.087  us/op
RecordDeserialization.deserializeClasses      1000  avgt   10  279.014 : 1.189  us/op
RecordDeserialization.deserializeRecords        10  avgt   10    8.537 : 0.032  us/op
RecordDeserialization.deserializeRecords       100  avgt   10   31.451 : 0.083  us/op
RecordDeserialization.deserializeRecords      1000  avgt   10  250.854 : 2.772  us/op

*/

package org.openjdk.bench.java.io;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

/**
 * A micro benchmark used to measure/compare the performance of
 * de-serializing record(s) vs. classical class(es)
 */
@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 10, time = 1)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Fork(value = 1, warmups = 0)
public class RecordDeserialization {

    public record PointR(int x, int y) implements Serializable {}

    public record LineR(PointR p1, PointR p2) implements Serializable {}

    public static class PointC implements Serializable {
        private final int x, y;

        public PointC(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static class LineC implements Serializable {
        private final PointC p1, p2;

        public LineC(PointC p1, PointC p2) {
            this.p1 = p1;
            this.p2 = p2;
        }
    }

    private byte[] lineRsBytes, lineCsBytes;

    private static LineR newLineR() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        return new LineR(new PointR(rnd.nextInt(), rnd.nextInt()),
                         new PointR(rnd.nextInt(), rnd.nextInt()));
    }

    private static LineC newLineC() {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        return new LineC(new PointC(rnd.nextInt(), rnd.nextInt()),
                         new PointC(rnd.nextInt(), rnd.nextInt()));
    }

    private static byte[] serialize(Object o) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(o);
            oos.close();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static Object deserialize(byte[] bytes) {
        try {
            return new ObjectInputStream(new ByteArrayInputStream(bytes))
                .readObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    @Param({"10", "100", "1000"})
    public int length;

    @Setup(Level.Trial)
    public void setup() {
        LineR[] lineRs = IntStream
            .range(0, length)
            .mapToObj(i -> newLineR())
            .toArray(LineR[]::new);
        lineRsBytes = serialize(lineRs);

        LineC[] lineCs = IntStream
            .range(0, length)
            .mapToObj(i -> newLineC())
            .toArray(LineC[]::new);
        lineCsBytes = serialize(lineCs);
    }

    @Benchmark
    public Object deserializeRecords() {
        return deserialize(lineRsBytes);
    }

    @Benchmark
    public Object deserializeClasses() {
        return deserialize(lineCsBytes);
    }
}
