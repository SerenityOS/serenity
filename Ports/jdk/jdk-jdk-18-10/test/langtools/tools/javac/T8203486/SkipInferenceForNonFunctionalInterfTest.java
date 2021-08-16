/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8203486
 * @summary skip type inference for non functional interface components of intersection types
 * @compile SkipInferenceForNonFunctionalInterfTest.java
 */

class SkipInferenceForNonFunctionalInterfTest {
    class U1 {}
    class U2 {}
    class U3 {}

    interface SAM<P1 extends U1, P2 extends U2, P3 extends U3> {
        P3 m(P1 p1, P2 p2);
    }

    interface I<T> {}

    class Tester {
        Object method(SAM<U1, U2, U3> sam) {
            return null;
        }

        Object run() {
            return method((SAM<U1, U2, U3> & I<?>) (U1 u1, U2 u2) -> { return new U3(); });
        }
    }
}
