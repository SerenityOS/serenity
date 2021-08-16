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
package org.openjdk.bench.jdk.incubator.foreign;

import jdk.incubator.foreign.FunctionDescriptor;
import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.SymbolLookup;
import jdk.incubator.foreign.ResourceScope;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.util.concurrent.TimeUnit;

import static jdk.incubator.foreign.CLinker.C_DOUBLE;
import static jdk.incubator.foreign.CLinker.C_INT;
import static jdk.incubator.foreign.CLinker.C_LONG_LONG;
import static jdk.incubator.foreign.CLinker.C_VA_LIST;
import static jdk.incubator.foreign.CLinker.asVarArg;

@BenchmarkMode(Mode.AverageTime)
@Warmup(iterations = 5, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@Measurement(iterations = 10, time = 500, timeUnit = TimeUnit.MILLISECONDS)
@State(org.openjdk.jmh.annotations.Scope.Thread)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(value = 3, jvmArgsAppend = { "--add-modules=jdk.incubator.foreign", "--enable-native-access=ALL-UNNAMED" })
public class VaList {

    static final CLinker linker = CLinker.getInstance();
    static {
        System.loadLibrary("VaList");
    }

    static final MethodHandle MH_ellipsis;
    static final MethodHandle MH_vaList;

    static {
        SymbolLookup lookup = SymbolLookup.loaderLookup();
        MH_ellipsis = linker.downcallHandle(lookup.lookup("ellipsis").get(),
                MethodType.methodType(void.class, int.class, int.class, double.class, long.class),
                FunctionDescriptor.ofVoid(C_INT, asVarArg(C_INT), asVarArg(C_DOUBLE), asVarArg(C_LONG_LONG)));
        MH_vaList = linker.downcallHandle(lookup.lookup("vaList").get(),
                MethodType.methodType(void.class, int.class, CLinker.VaList.class),
                FunctionDescriptor.ofVoid(C_INT, C_VA_LIST));
    }

    @Benchmark
    public void ellipsis() throws Throwable {
        MH_ellipsis.invokeExact(3,
                                1, 2D, 3L);
    }

    @Benchmark
    public void vaList() throws Throwable {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            CLinker.VaList vaList = CLinker.VaList.make(b ->
                    b.vargFromInt(C_INT, 1)
                            .vargFromDouble(C_DOUBLE, 2D)
                            .vargFromLong(C_LONG_LONG, 3L), scope);
            MH_vaList.invokeExact(3,
                    vaList);
        }
    }
}
