/*
 * Copyright (c) 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6943289
 * @summary Project Coin: Improved Exception Handling for Java (aka 'multicatch')
 */

import java.lang.annotation.*;

public class Pos04 {

    enum ExceptionKind {
        A(1),
        B(2),
        C(1);

        int expectedValue;

        ExceptionKind(int expectedValue) {
            this.expectedValue = expectedValue;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface CatchNumber {
       int value();
    }

    @CatchNumber(1)
    static class A extends Exception { }

    @CatchNumber(2)
    static class B extends Exception { }

    @CatchNumber(1)
    static class C extends Exception { }

    static int sum = 0;

    public static void main(String[] args) {
        for (ExceptionKind ekind : ExceptionKind.values()) {
            test(ekind);
        }
        if (sum != 4) {
            throw new Error("bad checksum - expected:4, found:" + sum);
        }
    }

    public static void test(ExceptionKind ekind) {
        try {
            switch(ekind) {
                case A: throw new A();
                case B: throw new B();
                case C: throw new C();
            }
        } catch(final A | C ex) {// Catch number 1
            CatchNumber catchNumber = ex.getClass().getAnnotation(CatchNumber.class);
            if (catchNumber == null || catchNumber.value() != ekind.expectedValue) {
                throw new Error("was expecting 1 - got " + catchNumber);
            }
            sum += catchNumber.value();
        } catch (final B ex) { // Catch number 2
            CatchNumber catchNumber = ex.getClass().getAnnotation(CatchNumber.class);
            if (catchNumber == null || catchNumber.value() != ekind.expectedValue) {
                throw new Error("was expecting 2 - got " + catchNumber);
            }
            sum += catchNumber.value();
        }
    }
}
