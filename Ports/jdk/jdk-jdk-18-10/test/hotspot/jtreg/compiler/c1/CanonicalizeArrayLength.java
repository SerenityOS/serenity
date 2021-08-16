/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 8150102 8150514 8150534 8171435
 * @summary C1 crashes in Canonicalizer::do_ArrayLength() after fix for JDK-8150102
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:CompileThreshold=100 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                   -XX:-BackgroundCompilation
 *                   compiler.c1.CanonicalizeArrayLength
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:CompileThreshold=100 -XX:+TieredCompilation -XX:TieredStopAtLevel=3
 *                   -XX:-BackgroundCompilation
 *                   -XX:+PatchALot
 *                   compiler.c1.CanonicalizeArrayLength
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:CompileThreshold=100 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                   -XX:-BackgroundCompilation
 *                   -XX:ScavengeRootsInCode=0
 *                   compiler.c1.CanonicalizeArrayLength
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:CompileThreshold=100 -XX:+TieredCompilation -XX:TieredStopAtLevel=1
 *                   -XX:-BackgroundCompilation -XX:ScavengeRootsInCode=1
 *                   compiler.c1.CanonicalizeArrayLength
 */

package compiler.c1;

public class CanonicalizeArrayLength {
    int[] arr = new int[42];
    int[] arrNull = null;

    final int[] finalArr = new int[42];
    final int[] finalArrNull = null;

    static int[] staticArr = new int[42];
    static int[] staticArrNull = null;

    static final int[] staticFinalArr = new int[42];
    static final int[] staticFinalArrNull = null;

    public static void main(String... args) {
        CanonicalizeArrayLength t = new CanonicalizeArrayLength();
        for (int i = 0; i < 20000; i++) {
            if (t.testLocal() != 42)
                throw new IllegalStateException();
            if (t.testLocalNull() != 42)
                throw new IllegalStateException();
            if (t.testField() != 42)
                throw new IllegalStateException();
            if (t.testFieldNull() != 42)
                throw new IllegalStateException();
            if (t.testFinalField() != 42)
                throw new IllegalStateException();
            if (t.testFinalFieldNull() != 42)
                throw new IllegalStateException();
            if (t.testStaticField() != 42)
                throw new IllegalStateException();
            if (t.testStaticFieldNull() != 42)
                throw new IllegalStateException();
            if (t.testStaticFinalField() != 42)
                throw new IllegalStateException();
            if (t.testStaticFinalFieldNull() != 42)
                throw new IllegalStateException();
        }
    }

    int testField() {
        try {
            return arr.length;
        } catch (Throwable t) {
            return -1;
        }
    }

    int testFieldNull() {
        try {
            return arrNull.length;
        } catch (Throwable t) {
            return 42;
        }
    }

    int testFinalField() {
        try {
            return finalArr.length;
        } catch (Throwable t) {
            return -1;
        }
    }

    int testFinalFieldNull() {
        try {
            return finalArrNull.length;
        } catch (Throwable t) {
            return 42;
        }
    }

    int testStaticField() {
        try {
            return staticArr.length;
        } catch (Throwable t) {
            return -1;
        }
    }

    int testStaticFieldNull() {
        try {
            return staticArrNull.length;
        } catch (Throwable t) {
            return 42;
        }
    }

    int testStaticFinalField() {
        try {
            return staticFinalArr.length;
        } catch (Throwable t) {
            return -1;
        }
    }

    int testStaticFinalFieldNull() {
        try {
            return staticFinalArrNull.length;
        } catch (Throwable t) {
            return 42;
        }
    }

    int testLocal() {
        int[] arr = new int[42];
        try {
            return arr.length;
        } catch (Throwable t) {
            return -1;
        }
    }

    int testLocalNull() {
        int[] arrNull = null;
        try {
            return arrNull.length;
        } catch (Throwable t) {
            return 42;
        }
    }


}
