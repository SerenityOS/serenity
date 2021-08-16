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
package org.openjdk.bench.vm.lang;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OperationsPerInvocation;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Tests various usages of instanceof.
 */
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Thread)
public class InstanceOf {

    private static final int NOOFOBJECTS = 100;
    private static final int NULLRATIO = 3;

    public Date[] dateArray;
    public Object[] objectArray;

    @Setup
    public void setup() {
        dateArray = new Date[NOOFOBJECTS * NULLRATIO];
        for (int i = 0; i < NOOFOBJECTS * NULLRATIO; i += NULLRATIO) {
            dateArray[i] = new Date();
        }
        objectArray = dateArray;
    }

    /**
     * Performs "instanceof Cloneable" on objects that definitely are of that interface. It is not clear however whether
     * the objects are null or not, therefore a simple nullcheck is all that should be left of here.
     */
    @Benchmark
    @OperationsPerInvocation((NOOFOBJECTS * NULLRATIO))
    public int instanceOfInterfacePartialRemove() {
        int dummy = 0;
        Date[] localArray = dateArray;
        for (int i = 0; i < NOOFOBJECTS * NULLRATIO; i++) {
            if (localArray[i] instanceof Cloneable) {
                dummy++;
            }
        }
        return dummy;
    }

    /**
     * Performs three serial instanceof statements on the same object for three different interfaces. The objects are
     * 50% null, and all non-null are instanceof the last interface.
     */
    @Benchmark
    @OperationsPerInvocation((NOOFOBJECTS * NULLRATIO))
    public int instanceOfInterfaceSerial() {
        int dummy = 0;
        Object[] localArray = objectArray;
        for (int i = 0; i < NOOFOBJECTS * NULLRATIO; i++) {
            if (localArray[i] instanceof Runnable) {
                dummy += 1000;
            } else if (localArray[i] instanceof CharSequence) {
                dummy += 2000;
            } else if (localArray[i] instanceof Serializable) {
                dummy++;
            }
        }
        return dummy;
    }
}
