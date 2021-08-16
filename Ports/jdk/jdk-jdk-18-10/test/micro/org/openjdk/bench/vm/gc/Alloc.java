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
package org.openjdk.bench.vm.gc;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.TimeUnit;

@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class Alloc {

    public static final int LENGTH = 400;
    public static final int ARR_LEN = 100;
    public int largeLen = 100;
    public int smalllen = 6;

    @Benchmark
    public void testLargeConstArray(Blackhole bh) throws Exception {
        int localArrlen = ARR_LEN;
        for (int i = 0; i < LENGTH; i++) {
            Object[] tmp = new Object[localArrlen];
            bh.consume(tmp);
        }
    }

    @Benchmark
    public void testLargeVariableArray(Blackhole bh) throws Exception {
        int localArrlen = largeLen;
        for (int i = 0; i < LENGTH; i++) {
            Object[] tmp = new Object[localArrlen];
            bh.consume(tmp);
        }
    }

    @Benchmark
    public void testSmallConstArray(Blackhole bh) throws Exception {
        int localArrlen = largeLen;
        for (int i = 0; i < LENGTH; i++) {
            Object[] tmp = new Object[localArrlen];
            bh.consume(tmp);
        }
    }

    @Benchmark
    public void testSmallObject(Blackhole bh) throws Exception {
        FortyBytes localDummy = null;
        for (int i = 0; i < LENGTH; i++) {
            FortyBytes tmp = new FortyBytes();
            tmp.next = localDummy;
            localDummy = tmp;
            bh.consume(tmp);
        }
    }

    @Benchmark
    public void testSmallVariableArray(Blackhole bh) throws Exception {
        int localArrlen = smalllen;
        for (int i = 0; i < LENGTH; i++) {
            Object[] tmp = new Object[localArrlen];
            bh.consume(tmp);
        }
    }

    final class FortyBytes {
        Object next;
        int y, z, k, f, g, e, t;
    }

}
