/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214239
 * @summary Missing x86_64.ad patterns for clearing and setting long vector bits
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+UnlockDiagnosticVMOptions
 *                   -XX:-TieredCompilation -XX:CompileThreshold=1000
 *                   -XX:CompileCommand=print,compiler/c2/TestBitSetAndReset.test*
 *                   -XX:CompileCommand=compileonly,compiler/c2/TestBitSetAndReset.test*
 *                   -XX:CompileCommand=dontinline,compiler/c2/TestBitSetAndReset.test*
 *                   compiler.c2.TestBitSetAndReset
 */

package compiler.c2;

public class TestBitSetAndReset {
    private static final int COUNT = 10_000;

    private static final long MASK63 = 0x8000_0000_0000_0000L;
    private static final long MASK32 = 0x0000_0001_0000_0000L;
    private static final long MASK31 = 0x0000_0000_8000_0000L;
    private static final long MASK15 = 0x0000_0000_0000_8000L;
    private static final long MASK00 = 0x0000_0000_0000_0001L;

    private static long andq, orq;

    public static void main(String... args) {
        boolean success = true;

        for (int i=0; i<COUNT; i++) {
            andq = MASK63 | MASK31 | MASK15 | MASK00;
            orq = 0;
            test63();
            test32();
            test31();
            test15();
            test00();
            success &= andq == 0 && orq == (MASK63 | MASK32 | MASK31 | MASK15 | MASK00);
        }
        if (!success)
            throw new AssertionError("Failure while setting or clearing long vector bits!");
    }

    private static void test63() {
        andq &= ~MASK63;
        orq |= MASK63;
    }
    private static void test32() {
        andq &= ~MASK32;
        orq |= MASK32;
    }
    private static void test31() {
        andq &= ~MASK31;
        orq |= MASK31;
    }
    private static void test15() {
        andq &= ~MASK15;
        orq |= MASK15;
    }
    private static void test00() {
        andq &= ~MASK00;
        orq |= MASK00;
    }
}
