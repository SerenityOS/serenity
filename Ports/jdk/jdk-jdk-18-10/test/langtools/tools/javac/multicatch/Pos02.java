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
 *
 */

public class Pos02 {

    static class A extends Exception {}
    static class B extends Exception {}
    static class C extends Exception {}
    static class C1 extends C {}
    static class C2 extends C {}

    enum ExceptionKind {
        A,
        B,
        C1,
        C2
    }

    static int caughtExceptions = 0;
    static int caughtRethrownExceptions = 0;

    static void test(ExceptionKind ekind) throws A, C1 {
        try {
            switch (ekind) {
                case A : throw new A();
                case B : throw new B();
                case C1: throw new C1();
                case C2 : throw new C2();
            }
        }
        catch (final C2 | B ex) {
            caughtExceptions++;
        }
        catch (final C | A ex) {
            caughtExceptions++;
            throw ex;
        }
    }

    public static void main(String[] args) {
        for (ExceptionKind ekind : ExceptionKind.values()) {
            try {
                test(ekind);
            }
            catch (final C1 | A ex) {
                caughtRethrownExceptions++;
            }
        }
        if (caughtExceptions != 4 && caughtRethrownExceptions == 2) {
            throw new AssertionError("Exception handler called " + caughtExceptions + "times" +
                                     " rethrown handler called " + caughtRethrownExceptions + "times");
        }
    }
}
