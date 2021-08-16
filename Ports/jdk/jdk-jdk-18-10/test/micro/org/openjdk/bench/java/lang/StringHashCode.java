/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.concurrent.TimeUnit;

/**
 * Performance test of String.hashCode() function
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class StringHashCode {

    private String hashcode;
    private String hashcode0;
    private String empty;

    @Setup
    public void setup() {
        hashcode = "abcdefghijkl";
        hashcode0 = new String(new char[]{72, 90, 100, 89, 105, 2, 72, 90, 100, 89, 105, 2});
        empty = new String();
    }

    /**
     * Benchmark testing String.hashCode() with a regular 12 char string with
     * the result possibly cached in String
     */
    @Benchmark
    public int cached() {
        return hashcode.hashCode();
    }

    /**
     * Benchmark testing String.hashCode() with a 12 char string with the
     * hashcode = 0 forcing the value to always be recalculated.
     */
    @Benchmark
    public int notCached() {
        return hashcode0.hashCode();
    }

    /**
     * Benchmark testing String.hashCode() with the empty string. Since the
     * empty String has hashCode = 0, this value is always recalculated.
     */
    @Benchmark
    public int empty() {
        return empty.hashCode();
    }
}
