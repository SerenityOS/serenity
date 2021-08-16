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
 * @bug 8206986
 * @summary Check fall through in switch expressions.
 * @compile ExpressionSwitchFallThrough.java
 * @run main ExpressionSwitchFallThrough
 */

import java.util.Objects;
import java.util.function.Function;

public class ExpressionSwitchFallThrough {
    public static void main(String... args) {
        new ExpressionSwitchFallThrough().run();
    }

    private void run() {
        runTest(this::expression1);
        runTest(this::expression2);
    }

    private void runTest(Function<T, String> print) {
        check(T.A,  print, "ab");
        check(T.B,  print, "b");
        check(T.C,  print, "");
    }

    private String expression1(T t) {
        String help = "";
        return switch (t) {
            case A: help = "a";
            case B: help += "b";
            default: yield help;
        };
    }

    private String expression2(T t) {
        String help = "";
        return switch (t) {
            case A: help = "a";
            case B: help += "b";
            default: yield help;
        };
    }

    private void check(T t, Function<T, String> print, String expected) {
        String result = print.apply(t);
        if (!Objects.equals(result, expected)) {
            throw new AssertionError("Unexpected result: " + result);
        }
    }

    enum T {
        A, B, C;
    }
}
