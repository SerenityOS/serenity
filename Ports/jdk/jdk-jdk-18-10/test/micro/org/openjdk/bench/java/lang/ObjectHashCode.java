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

import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;

import java.util.concurrent.TimeUnit;

/**
 * This benchmark assesses different hashCode strategies in HotSpot
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class ObjectHashCode {

    @Benchmark
    @Fork
    public int mode_default() {
        return System.identityHashCode(new Object());
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:+UnlockExperimentalVMOptions", "-XX:hashCode=0"})
    public int mode_0() {
        return System.identityHashCode(new Object());
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:+UnlockExperimentalVMOptions", "-XX:hashCode=1"})
    public int mode_1() {
        return System.identityHashCode(new Object());
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:+UnlockExperimentalVMOptions", "-XX:hashCode=2"})
    public int mode_2() {
        return System.identityHashCode(new Object());
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:+UnlockExperimentalVMOptions", "-XX:hashCode=3"})
    public int mode_3() {
        return System.identityHashCode(new Object());
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:+UnlockExperimentalVMOptions", "-XX:hashCode=4"})
    public int mode_4() {
        return System.identityHashCode(new Object());
    }

    @Benchmark
    @Fork(jvmArgsPrepend = {"-XX:+UnlockExperimentalVMOptions", "-XX:hashCode=5"})
    public int mode_5() {
        return System.identityHashCode(new Object());
    }

}
