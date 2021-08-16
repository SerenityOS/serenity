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
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.lang.invoke.SwitchPoint;
import java.util.concurrent.TimeUnit;

/**
 * Benchmark assessing SwitchPoint performance.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class SwitchPointAdhoc {

    /*
     * Implementation notes:
     *   - this test asserts SwitchPoint performance in both valid and invalid cases
     *   - this test does not assert invalidation performance (hard to do with irreversible SwitchPoint)
     *   - raw baseline gives the idea for MethodHandle invocation cost
     *   - CS baseline gives the idea for additional dereference cost
     */

    private MethodHandle body1, body2;
    private int i;
    private java.lang.invoke.SwitchPoint sw1, sw2;
    private CallSite cs;

    @Setup
    public void setup() throws NoSuchMethodException, IllegalAccessException {
        sw1 = new java.lang.invoke.SwitchPoint();
        sw2 = new java.lang.invoke.SwitchPoint();
        SwitchPoint.invalidateAll(new SwitchPoint[]{sw2});
        body1 = MethodHandles.lookup().findVirtual(SwitchPointAdhoc.class, "body1", MethodType.methodType(int.class, int.class));
        body2 = MethodHandles.lookup().findVirtual(SwitchPointAdhoc.class, "body2", MethodType.methodType(int.class, int.class));
        cs = new MutableCallSite(body1);
    }

    @Benchmark
    public void baselineRaw() throws Throwable {
        i = (int) body1.invoke(this, i);
    }

    @Benchmark
    public void baselineCS() throws Throwable {
        i = (int) cs.getTarget().invoke(this, i);
    }

    @Benchmark
    public void testValid() throws Throwable {
        MethodHandle handle = sw1.guardWithTest(body1, body2);
        i = (int) handle.invoke(this, i);
    }

    @Benchmark
    public void testInvalid() throws Throwable {
        MethodHandle handle = sw2.guardWithTest(body1, body2);
        i = (int) handle.invoke(this, i);
    }

    public int body1(int i) {
        return i + 1;
    }

    public int body2(int i) {
        return i + 1;
    }


}
