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
package org.openjdk.bench.java.lang.invoke;


import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark assesses MethodHandle.publicLookup() performance
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class LookupPublicFind {

    /*
        Implementation notes:
            - findSpecial is not tested, unable to do this with public lookup object
     */

    @Benchmark
    public MethodHandle testConstructor() throws Exception {
        return MethodHandles.publicLookup().findConstructor(Victim.class, MethodType.methodType(void.class));
    }

    @Benchmark
    public MethodHandle testGetter() throws Exception {
        return MethodHandles.publicLookup().findGetter(Victim.class, "field", int.class);
    }

    @Benchmark
    public MethodHandle testSetter() throws Exception {
        return MethodHandles.publicLookup().findSetter(Victim.class, "field", int.class);
    }

    @Benchmark
    public MethodHandle testStatic() throws Exception {
        return MethodHandles.publicLookup().findStatic(Victim.class, "staticWork", MethodType.methodType(int.class));
    }

    @Benchmark
    public MethodHandle testStaticGetter() throws Exception {
        return MethodHandles.publicLookup().findStaticGetter(Victim.class, "staticField", int.class);
    }

    @Benchmark
    public MethodHandle testStaticSetter() throws Exception {
        return MethodHandles.publicLookup().findStaticSetter(Victim.class, "staticField", int.class);
    }

    @Benchmark
    public MethodHandle testVirtual() throws Exception {
        return MethodHandles.publicLookup().findVirtual(Victim.class, "virtualWork", MethodType.methodType(int.class));
    }

    public static class Victim {

        public static int staticField;
        public int field;

        public Victim() {
            // do nothing
        }

        public int virtualWork() {
            return 1;
        }

        public static int staticWork() {
            return 1;
        }

    }


}
