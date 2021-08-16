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
 * @summary Verify behavior of various kinds of breaks.
 * @compile ExpressionSwitchBreaks1.java
 * @run main ExpressionSwitchBreaks1
 */

import java.util.Objects;
import java.util.function.Supplier;

public class ExpressionSwitchBreaks1 {
    public static void main(String... args) {
        new ExpressionSwitchBreaks1().run();
    }

    private void run() {
        check(print1(0, 0), "0-0");
        check(print1(0, 1), "0-1");
        check(print1(0, -1), "0-X");
        check(print1(-1, -1), "X");
        check(print2(0, 0, 0), "0-0-0");
        check(print2(0, 0, 1), "0-0-1");
        check(print2(0, 0, 2), "0-0-2");
        check(print2(0, 0, -1), "0-0-X");
        check(print2(0, 1, -1), "0-1");
        check(print2(0, -1, -1), "0-X");
        check(print2(1, -1, -1), "1");
        check(print2(2, 5, 5), "2-X-5");
        check(print2(-11, -1, -1), "X");
    }

    private String print1(int i, int j) {
        switch (i) {
            case 0:
                return switch (j) {
                    case 0:
                        if (true) yield "0-0";
                    case 1:
                        yield "0-1";
                    default:
                        yield "0-X";
                };
            default: return "X";
        }
    }

    private String print2(int i, int j, int k) {
        return switch (i) {
            case 0:
                String r;
                OUTER: switch (j) {
                    case 0:
                        String res;
                        INNER: switch (k) {
                            case 0: res = "0-0-0"; break;
                            case 1: res = "0-0-1"; break;
                            case 2: res = "0-0-2"; break INNER;
                            default: r = "0-0-X"; break OUTER;
                        }
                        r = res;
                        break;
                    case 1:
                        r = "0-1";
                        break;
                    default:
                        r = "0-X";
                        break;
                }
                yield r;
            case 1:
                yield "1";
            case 2:
                LOP: while (j-- > 0) {
                    if (k == 5) {
                        k--;
                        continue;
                    }
                    break LOP;
                }
                Supplier<String> getter = () -> { return "2-X-5"; };
                yield getter.get();
            default:
                yield "X";
        };
    }

    private void check(String result, String expected) {
        if (!Objects.equals(result, expected)) {
            throw new AssertionError("Unexpected result: " + result);
        }
    }

    enum T {
        A, B;
    }
}
