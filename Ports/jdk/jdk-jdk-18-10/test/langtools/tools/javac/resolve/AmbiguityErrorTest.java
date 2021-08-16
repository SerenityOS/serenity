/*
 * Copyright (c) 2014, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8041663
 */

public class AmbiguityErrorTest {

    public interface A { }

    public interface B extends A { }

    public interface C {
        A m(B strategy);
    }

    public interface D {
        A m(A strategy);
        A m(B strategy);
    }

    public interface T1 extends C, D { }
    public interface T2 extends D, C { }

    int count;

    class T1Impl implements T1, T2 {
        public A m(B strategy) {
            count++;
            return null;
        }
        public A m(A strategy) {
            throw new AssertionError("Should not get here.");
        }
    }

    public static void main(String... args) {
        new AmbiguityErrorTest().test();
    }

    void test() {
        T1 t1 = new T1Impl();
        T2 t2 = new T1Impl();
        final B b = new B() { };
        t1.m(b);
        t2.m(b);

        if (count != 2) {
            throw new IllegalStateException("Did not call the methods properly");
        }
    }

}
