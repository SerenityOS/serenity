/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8078093 8055894
 * @summary Exponential performance regression Java 8 compiler compared to Java 7 compiler
 * @compile T8055984.java
 */

class T8055984 {
    static class C<U> {
        U fu;

        C() { }

        C(C<U> other) {
            this.fu = other.fu;
        }

        C(U fu) {
            this.fu = fu;
        }
    }

    static <U> C<U> m(C<U> src) { return new C<U>(src); }

    static void test() {
        C<String> c2 = m(new C<>(m(new C<>() )) );
        C<String> c3 = m(new C<>(m(new C<>(m(new C<>() )) )) );
        C<String> c4 = m(new C<>(m(new C<>(m(new C<>(m(new C<>() )) )) )) );
        C<String> c5 = m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>() )) )) )) )) );
        C<String> c6 = m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>() )) )) )) )) )) );
        C<String> c7 = m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>() )) )) )) )) )) )) );
        C<String> c8 = m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>() )) )) )) )) )) )) )) );
        C<String> c9 = m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>() )) )) )) )) )) )) )) )) );
        C<String> c10 = m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>(m(new C<>())))))))))))))))))));
    }
}

