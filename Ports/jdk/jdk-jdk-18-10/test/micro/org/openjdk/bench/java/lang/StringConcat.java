/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.util.concurrent.TimeUnit;

/**
 * Trivial String concatenation benchmark.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 5, time = 1000, timeUnit = TimeUnit.MILLISECONDS)
@Fork(3)
public class StringConcat {

    @Param("4711")
    public int intValue;

    public String stringValue = String.valueOf(intValue);

    public Object objectValue = Long.valueOf(intValue);

    public boolean boolValue = true;

    public byte byteValue = (byte)-128;

    public String emptyString = "";

    @Benchmark
    public String concatConstInt() {
        return "string" + intValue;
    }

    @Benchmark
    public String concatConstString() {
        return "string" + stringValue;
    }

    @Benchmark
    public String concatEmptyRight() {
        return stringValue + emptyString;
    }

    @Benchmark
    public String concatEmptyLeft() {
        return emptyString + stringValue;
    }

    @Benchmark
    public String concatEmptyConstInt() {
        return "" + intValue;
    }

    @Benchmark
    public String concatEmptyConstString() {
        return "" + stringValue;
    }

    @Benchmark
    public String concatMethodConstString() {
        return "string".concat(stringValue);
    }

    @Benchmark
    public String concatConstIntConstInt() {
        return "string" + intValue + "string" + intValue;
    }

    @Benchmark
    public String concatConstStringConstInt() {
        return "string" + stringValue + "string" + intValue;
    }

    @Benchmark
    public String concatMix4String() {
        // Investigate "profile pollution" between shared LFs that might eliminate some JIT optimizations
        String s1 = "string" + stringValue + stringValue + stringValue + stringValue;
        String s2 = "string" + stringValue + "string" + stringValue + stringValue + stringValue;
        String s3 = stringValue + stringValue + "string" + stringValue + "string" + stringValue + "string";
        String s4 = "string" + stringValue + "string" + stringValue + "string" + stringValue + "string" + stringValue + "string";
        return s1 + s2 + s3 + s4;
    }

    @Benchmark
    public String concatConst4String() {
        return "string" + stringValue + stringValue + stringValue + stringValue;
    }

    @Benchmark
    public String concat4String() {
        return stringValue + stringValue + stringValue + stringValue;
    }

    @Benchmark
    public String concatConst2String() {
        return "string" + stringValue + stringValue;
    }

    @Benchmark
    public String concatConstBoolByte() {
        return "string" + boolValue + byteValue;
    }

    @Benchmark
    public String concatConst6String() {
        return "string" + stringValue + stringValue + stringValue + stringValue + stringValue + stringValue;
    }

    @Benchmark
    public String concat6String() {
        return stringValue + stringValue + stringValue + stringValue + stringValue + stringValue;
    }

    @Benchmark
    public String concatConst6Object() {
        return "string" + objectValue + objectValue + objectValue + objectValue + objectValue + objectValue;
    }

}
