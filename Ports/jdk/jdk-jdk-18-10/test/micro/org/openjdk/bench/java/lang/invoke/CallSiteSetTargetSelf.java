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

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.VolatileCallSite;
import java.util.concurrent.TimeUnit;

/**
 * This benchmark evaluates INDY performance under dynamic target updates.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class CallSiteSetTargetSelf {

    /*
     * Implementation notes:
     *   - This test makes sense for mutable and volatile call sites only
     *   - Multiple threads are calling the same callsite, and each call is swapping the target.
     *   - Additional baseline includes "raw" test, calling callsite's MH directly
     *
     *   - NOTE: invalidating shared target callsite is very bad with multiple threads.
     *     I.e. this test is inherently non-scalable.
     */

    private static CallSite cs;

    private static MethodHandle doCall1;
    private static MethodHandle doCall2;

    static {
        try {
            doCall1 = MethodHandles.lookup().findVirtual(CallSiteSetTargetSelf.class, "call1", MethodType.methodType(int.class));
            doCall2 = MethodHandles.lookup().findVirtual(CallSiteSetTargetSelf.class, "call2", MethodType.methodType(int.class));
            cs = new MutableCallSite(doCall1);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }

    private int i1;
    private int i2;

    public int call1() {
        cs.setTarget(doCall2);
        return i1++;
    }

    public int call2() {
        cs.setTarget(doCall1);
        return i2++;
    }

    @Benchmark
    public int baselineRaw() throws Throwable {
        return (int) cs.getTarget().invokeExact(this);
    }

    @Benchmark
    public int testMutable() throws Throwable {
        return (int) INDY_Mutable().invokeExact(this);
    }

    @Benchmark
    public int testVolatile() throws Throwable {
        return (int) INDY_Volatile().invokeExact(this);
    }

    /* =========================== INDY TRAMPOLINES ============================== */

    private static MethodType MT_bsm() {
        shouldNotCallThis();
        return MethodType.methodType(CallSite.class, MethodHandles.Lookup.class, String.class, MethodType.class);
    }

    private static MethodHandle MH_bsm_Mutable() throws ReflectiveOperationException {
        shouldNotCallThis();
        return MethodHandles.lookup().findStatic(MethodHandles.lookup().lookupClass(), "bsm_Mutable", MT_bsm());
    }

    private static MethodHandle MH_bsm_Volatile() throws ReflectiveOperationException {
        shouldNotCallThis();
        return MethodHandles.lookup().findStatic(MethodHandles.lookup().lookupClass(), "bsm_Volatile", MT_bsm());
    }

    private static MethodHandle INDY_Mutable() throws Throwable {
        shouldNotCallThis();
        return ((CallSite) MH_bsm_Mutable().invoke(MethodHandles.lookup(), "doCall1", MethodType.methodType(int.class, CallSiteSetTargetSelf.class))).dynamicInvoker();
    }

    private static MethodHandle INDY_Volatile() throws Throwable {
        shouldNotCallThis();
        return ((CallSite) MH_bsm_Volatile().invoke(MethodHandles.lookup(), "doCall1", MethodType.methodType(int.class, CallSiteSetTargetSelf.class))).dynamicInvoker();
    }

    public static CallSite bsm_Mutable(MethodHandles.Lookup lookup, String name, MethodType type) {
        synchronized (CallSiteSetTarget.class) {
            if (cs == null)
                cs = new MutableCallSite(doCall1);
            return cs;
        }
    }

    public static CallSite bsm_Volatile(MethodHandles.Lookup lookup, String name, MethodType type) {
        synchronized (CallSiteSetTarget.class) {
            if (cs == null)
                cs = new VolatileCallSite(doCall1);
            return cs;
        }
    }

    private static void shouldNotCallThis() {
        // if this gets called, the transformation has not taken place
        throw new AssertionError("this code should be statically transformed away by Indify");
    }

}
