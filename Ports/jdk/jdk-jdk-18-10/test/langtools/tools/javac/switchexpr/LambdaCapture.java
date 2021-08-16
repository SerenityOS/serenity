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
 * @bug 8220041
 * @summary Verify variable capture works inside switch expressions which are
 *          inside variable declarations
 * @compile LambdaCapture.java
 */

import java.util.Objects;

public class LambdaCapture {
    public static void main(String... args) {
        new LambdaCapture().run();
    }

    void run() {
        assertEquals("00", lambdaCapture1(0).t());
        assertEquals("12", lambdaCapture1(1).t());
        assertEquals("D", lambdaCapture1(2).t());
        assertEquals("D", lambdaCapture1(3).t());
        assertEquals("00", lambdaCapture2(0).t());
        assertEquals("12", lambdaCapture2(1).t());
        assertEquals("D", lambdaCapture2(2).t());
        assertEquals("D", lambdaCapture2(3).t());
    }

    I<String> lambdaCapture1(int i) {
        int j = i + 1;
        I<String> r = switch (i) {
            case 0 -> () -> "0" + i; //capture parameter
            case 1 -> () -> "1" + j; //capture local variable
            default -> {
                String k = "D";
                yield () -> k; //capture local from the switch expr.
            }
        };

        return r;
    }

    I<String> lambdaCapture2(int i) {
        int j = i + 1;

        return switch (i) {
            case 0 -> () -> "0" + i; //capture parameter
            case 1 -> () -> "1" + j; //capture local variable
            default -> {
                String k = "D";
                yield () -> k; //capture local from the switch expr.
            }
        };
    }

    {
        int j1 = 1;
        I<String> r1 = switch (j1) {
            case 1 -> () -> "1" + j1; //capture local variable
            default -> {
                String k = "D";
                yield () -> k; //capture local from the switch expr.
            }
        };
        assertEquals("11", r1.t());

        int j2 = 2;
        I<String> r2 = switch (j2) {
            case 1 -> () -> "1" + j2; //capture local variable
            default -> {
                String k = "D";
                yield () -> k; //capture local from the switch expr.
            }
        };
        assertEquals("D", r2.t());
    }

    private void assertEquals(Object expected, Object actual) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError("Unexpected value: " + actual);
        }
    }

    interface I<T> {
        public T t();
    }
}
