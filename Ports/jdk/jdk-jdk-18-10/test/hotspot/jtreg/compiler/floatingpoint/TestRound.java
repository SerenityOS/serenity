/*
 * Copyright (c) 2002, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4755500
 * @summary calling Math.round(NaN) can break subsequent calls to Math.round()
 * @run main compiler.floatingpoint.TestRound
 */

package compiler.floatingpoint;

public class TestRound {
    public static void main(String[] args) {
        // Note: it's really only necessary to run this loop 8 times to
        // reproduce the bug, but the 10000-length loop causes compilation
        // of Math.round() without any other command-line flags.
        // A bug in the d2l NaN case was causing overflow of the FPU
        // stack, yielding subsequent wrong results for flds.
        for (int i = 0; i < 10_000; i++) {
            Math.round(Double.NaN);
        }
        if (Math.round(1d) != 1) {
            throw new AssertionError("TEST FAILED");
        }
        System.out.println("Test passed.");
    }
}

