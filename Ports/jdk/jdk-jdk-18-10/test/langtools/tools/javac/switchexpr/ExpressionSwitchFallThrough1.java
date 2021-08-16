/*
 * Copyright (c) 2017, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8206986
 * @summary Check fall through in switch expressions.
 * @compile ExpressionSwitchFallThrough1.java
 * @run main ExpressionSwitchFallThrough1
 */

import java.util.Objects;

public class ExpressionSwitchFallThrough1 {
    public static void main(String... args) {
        new ExpressionSwitchFallThrough1().test();
    }

    private void test() {
        assertEquals("01", printExprFallThrough(0));
        assertEquals("1", printExprFallThrough(1));
        assertEquals("other", printExprFallThrough(3));
        assertEquals("01", printStatementFallThrough(0));
        assertEquals("1", printStatementFallThrough(1));
        assertEquals("other", printStatementFallThrough(3));
    }

    private String printExprFallThrough(Integer p) {
        String result = "";
        return switch (p) {
            case 0: result += "0";
            case 1: result += "1";
                yield result;
            default: yield "other";
        };
    }

    private String printStatementFallThrough(Integer p) {
        String result = "";
        switch (p) {
            case 0: result += "0";
            case 1: result += "1";
                break;
            default: result = "other";
                break;
        }
        return result;
    }

    private static void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(actual, expected)) {
            throw new AssertionError("Unexpected result: " + actual + ", expected: " + expected);
        }
    }
}
