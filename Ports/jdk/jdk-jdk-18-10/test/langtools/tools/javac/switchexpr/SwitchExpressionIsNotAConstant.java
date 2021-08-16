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

/**
 * @test
 * @bug 8214113
 * @summary Verify the switch expression's type does not have a constant attached,
 *          and so the switch expression is not elided.
 * @compile SwitchExpressionIsNotAConstant.java
 * @run main SwitchExpressionIsNotAConstant
 */
public class SwitchExpressionIsNotAConstant {

    public static void main(String[] args) {
        int i = 0;
        {
            i = 0;
            int dummy = 1 + switch (i) {
                default -> {
                    i++;
                    yield 1;
                }
            };
            if (i != 1) {
                throw new IllegalStateException("Side effects missing.");
            }
        }
        {
            i = 0;
            int dummy = 1 + switch (i) {
                case -1 -> 1;
                default -> {
                    i++;
                    yield 1;
                }
            };
            if (i != 1) {
                throw new IllegalStateException("Side effects missing.");
            }
        }
        {
            i = 0;
            int dummy = 1 + switch (i) {
                 default :
                    i++;
                    yield 1;
            };
            if (i != 1) {
                throw new IllegalStateException("Side effects missing.");
            }
        }
        {
            i = 0;
            int dummy = 1 + switch (i) {
                case -1: yield 1;
                default:
                    i++;
                    yield 1;
            };
            if (i != 1) {
                throw new IllegalStateException("Side effects missing.");
            }
        }
    }

}
