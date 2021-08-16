/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

package compiler.exceptions;

/**
 * @test
 * @bug 8263227
 * @summary Tests that users of return values from exception-throwing method
 *          calls are not duplicated in the call's exception path. The second
 *          run with a variable seed is added for test robustness.
 * @library /test/lib /
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -Xbatch -XX:+StressGCM -XX:StressSeed=0
 *                   -XX:+VerifyRegisterAllocator
 *                   -XX:CompileCommand=dontinline,java.lang.Integer::*
 *                   compiler.exceptions.TestSpilling
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions
 *                   -XX:+UnlockDiagnosticVMOptions
 *                   -Xbatch -XX:+StressGCM
 *                   -XX:+VerifyRegisterAllocator
 *                   -XX:CompileCommand=dontinline,java.lang.Integer::*
 *                   compiler.exceptions.TestSpilling
 */

public class TestSpilling {

    public static void test() {
        int a = Integer.valueOf(42).intValue();
        // After global code motion, the logic below should only be placed in
        // the fall-through path of java.lang.Integer::intValue(). Otherwise,
        // live range splitting might create uses without reaching definitions
        // if 'a' is spilled.
        int b = (((a & 0x0000F000)) + 1);
        int c = a / b + ((a % b > 0) ? 1 : 0);
    }

    public static void main(String[] args) {
        for (int i = 0; i < 10_000; i++) {
            test();
        }
    }

}
