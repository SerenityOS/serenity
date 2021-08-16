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

/**
 * @test
 * @bug 8038420
 * @summary Lambda returning post-increment generates wrong code
 * @run main LambdaIncrement
 */

public class LambdaIncrement {

    interface IntegerOp { Integer apply(Integer arg); }

    private static void assertNotIncremented(IntegerOp lmb, String label) {
        int result = lmb.apply(3);
        if (result != 3) {
            throw new AssertionError("Post-increment failure. Expected 3, got: " +
                                     result + " " + label);
        }
    }

    public static void main(String... args) throws Exception {
        assertNotIncremented(x -> x++, "PostIncExpr");
        assertNotIncremented(x -> { return x++; }, "PostIncReturn");
        assertNotIncremented(x -> { int y = x; return y++; }, "PostIncLocal");
        assertNotIncremented(x -> { Integer y = x; return y++; }, "PostIncLocalBox");
        assertNotIncremented(x -> { int y = x; return y; }, "HASINIT");
    }
}
