/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214031
 * @summary Verify various corner cases with nested switch expressions.
 * @compile ExpressionSwitchBugsInGen.java
 * @run main ExpressionSwitchBugsInGen
 */

public class ExpressionSwitchBugsInGen {
    public static void main(String... args) {
        new ExpressionSwitchBugsInGen().test(0, 0, 0, false);
        new ExpressionSwitchBugsInGen().test(0, 0, 1, true);
        new ExpressionSwitchBugsInGen().test(0, 1, 1, false);
        new ExpressionSwitchBugsInGen().test(1, 1, -1, true);
        new ExpressionSwitchBugsInGen().test(1, 12, -1, false);
        new ExpressionSwitchBugsInGen().testCommonSuperType(0, "a", new StringBuilder(), "a");
        new ExpressionSwitchBugsInGen().testCommonSuperType(1, "", new StringBuilder("a"), "a");
        new ExpressionSwitchBugsInGen().testSwitchExpressionInConditional(0, null, -1);
        new ExpressionSwitchBugsInGen().testSwitchExpressionInConditional(1, "", 0);
        new ExpressionSwitchBugsInGen().testSwitchExpressionInConditional(1, 1, 1);
        new ExpressionSwitchBugsInGen().testIntBoxing(0, 10, 10);
        new ExpressionSwitchBugsInGen().testIntBoxing(1, 10, -1);
    }

    private void test(int a, int b, int c, boolean expected) {
        if ( !(switch (a) {
                case 0 -> b == (switch (c) { case 0 -> 0; default -> 1; });
                default -> b == 12;
            })) {
            if (!expected) {
                throw new IllegalStateException();
            }
        } else {
            if (expected) {
                throw new IllegalStateException();
            }
        }
    }

    private void testCommonSuperType(int a, String s1, StringBuilder s2, String expected) {
        String r = (switch (a) {
            case 0 -> s1;
            default -> s2;
        }).toString();
        if (!expected.equals(r)) {
            throw new IllegalStateException();
        }
    }

    private void testSwitchExpressionInConditional(int a, Object o, int expected) {
        int v = a == 0 ? -1
                       : switch (o instanceof String ? 0 : 1) {
                             case 0 -> 0;
                             default -> 1;
                       };
        if (v != expected) {
            throw new IllegalStateException();
        }
    }

    private void testIntBoxing(int a, Integer res, int expected) {
        int r = switch (a) {
            case 0 -> res;
            default -> -1;
        };
        if (r != expected) {
            throw new IllegalStateException();
        }
    }

}
