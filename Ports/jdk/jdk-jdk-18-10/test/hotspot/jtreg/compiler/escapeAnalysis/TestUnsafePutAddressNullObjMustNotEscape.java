/*
 * Copyright (c) 2014 SAP SE. All rights reserved.
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

/**
 * @test
 * @bug 8038048
 * @summary assert(null_obj->escape_state() == PointsToNode::NoEscape,etc)
 * @modules java.base/jdk.internal.misc:+open
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+DoEscapeAnalysis
 *      -XX:-TieredCompilation -Xbatch
 *      compiler.escapeAnalysis.TestUnsafePutAddressNullObjMustNotEscape
 *
 * @author Richard Reingruber richard DOT reingruber AT sap DOT com
 */

package compiler.escapeAnalysis;

import jdk.internal.misc.Unsafe;

import java.lang.reflect.Field;

public class TestUnsafePutAddressNullObjMustNotEscape {

    public static Unsafe usafe;
    public static long mem;
    public static long checksum;

    public static void main(String[] args) throws Exception {
        System.out.println("EXECUTING test.");

        {
            System.out.println("Acquiring jdk.internal.misc.Unsafe.theUnsafe using reflection.");
            getUnsafe();
            System.out.println("Allocating raw memory.");
            mem = (usafe.allocateMemory(1024) + 8L) & ~7L;
            System.out.println("Triggering JIT compilation of the test method");
            triggerJitCompilationOfTestMethod();
        }

        System.out.println("SUCCESSFULLY passed test.");
    }

    public static void triggerJitCompilationOfTestMethod() {
        long sum = 0;
        for (int ii = 50000; ii >= 0; ii--) {
            sum = testMethod();
        }
        checksum = sum;
    }

    public static class IDGen {
        private static long id;
        public long nextId() {
            return id++;
        }
    }

    public static long testMethod() {
        // dummy alloc to trigger escape analysis
        IDGen gen = new IDGen();
        // StoreP of null_obj to raw mem triggers assertion in escape analysis
        usafe.putAddress(mem, 0L);
        return gen.nextId();
    }

    private static void getUnsafe() throws Exception {
        Field field = jdk.internal.misc.Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        usafe = (jdk.internal.misc.Unsafe) field.get(null);
    }
}
