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

import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.VolatileCallSite;
import java.util.concurrent.TimeUnit;

/**
 * This benchmark evaluates INDY performance when call sites are not changed.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class CallSiteStable {

    /*
     * Implementation notes:
     *   - Test is calling simple method via INDY
     *   - Additional baseline includes "raw" test, calling target method directly in virtual and static modes
     */

    private static java.lang.invoke.CallSite cs;

    private static MethodHandle doCallMH;

    static {
        try {
            doCallMH = MethodHandles.lookup().findVirtual(CallSiteStable.class, "doCall", MethodType.methodType(int.class, int.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private int i;

    public int doCall(int value) {
        return value + 1;
    }

    public static int doCallStatic(int value) {
        return value + 1;
    }

    @Benchmark
    public void baselineVirtual() {
        i = doCall(i);
    }

    @Benchmark
    public void baselineStatic() {
        i = doCallStatic(i);
    }

    @Benchmark
    public void testConstant() throws Throwable {
        i = (int) INDY_Constant().invokeExact(this, i);
    }

    @Benchmark
    public void testMutable() throws Throwable {
        i = (int) INDY_Mutable().invokeExact(this, i);
    }

    @Benchmark
    public void testVolatile() throws Throwable {
        i = (int) INDY_Volatile().invokeExact(this, i);
    }

    /* =========================== INDY TRAMPOLINES ============================== */

    private static MethodType MT_bsm() {
        shouldNotCallThis();
        return MethodType.methodType(java.lang.invoke.CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    }

    private static MethodHandle MH_bsm_Constant() throws ReflectiveOperationException {
        shouldNotCallThis();
        return MethodHandles.lookup().findStatic(MethodHandles.lookup().lookupClass(), "bsm_Constant", MT_bsm());
    }

    private static MethodHandle MH_bsm_Mutable() throws ReflectiveOperationException {
        shouldNotCallThis();
        return MethodHandles.lookup().findStatic(MethodHandles.lookup().lookupClass(), "bsm_Mutable", MT_bsm());
    }

    private static MethodHandle MH_bsm_Volatile() throws ReflectiveOperationException {
        shouldNotCallThis();
        return MethodHandles.lookup().findStatic(MethodHandles.lookup().lookupClass(), "bsm_Volatile", MT_bsm());
    }

    private static MethodHandle INDY_Constant() throws Throwable {
        shouldNotCallThis();
        return ((java.lang.invoke.CallSite) MH_bsm_Constant().invoke(MethodHandles.lookup(), "doCall", MethodType.methodType(int.class, CallSiteStable.class, int.class))).dynamicInvoker();
    }
    private static MethodHandle INDY_Mutable() throws Throwable {
        shouldNotCallThis();
        return ((java.lang.invoke.CallSite) MH_bsm_Mutable().invoke(MethodHandles.lookup(), "doCall", MethodType.methodType(int.class, CallSiteStable.class, int.class))).dynamicInvoker();
    }
    private static MethodHandle INDY_Volatile() throws Throwable {
        shouldNotCallThis();
        return ((java.lang.invoke.CallSite) MH_bsm_Volatile().invoke(MethodHandles.lookup(), "doCall", MethodType.methodType(int.class, CallSiteStable.class, int.class))).dynamicInvoker();
    }

    public static java.lang.invoke.CallSite bsm_Constant(MethodHandles.Lookup lookup, String name, MethodType type) {
        synchronized (CallSiteStable.class) {
            if (cs == null)
                cs = new ConstantCallSite(doCallMH);
            return cs;
        }
    }

    public static java.lang.invoke.CallSite bsm_Mutable(MethodHandles.Lookup lookup, String name, MethodType type) {
        synchronized (CallSiteStable.class) {
            if (cs == null)
                cs = new MutableCallSite(doCallMH);
            return cs;
        }
    }

    public static java.lang.invoke.CallSite bsm_Volatile(MethodHandles.Lookup lookup, String name, MethodType type) {
        synchronized (CallSiteStable.class) {
            if (cs == null)
                cs = new VolatileCallSite(doCallMH);
            return cs;
        }
    }

    private static void shouldNotCallThis() {
        // if this gets called, the transformation has not taken place
        throw new AssertionError("this code should be statically transformed away by Indify");
    }

}
