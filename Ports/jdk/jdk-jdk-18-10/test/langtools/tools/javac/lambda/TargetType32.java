/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8003280
 * @summary Add lambda tests
 *  target-typing and conditional operator
 */

public class TargetType32 {

    interface A<X> {
        X m();
    }

    interface B<X> extends A<X> {}

    void m(A<Integer> a) { }
    void m(B<String> b) { }

    <Z extends Integer> void m2(A<Z> a) { }
    <Z extends String> void m2(B<Z> b) { }

    void m3(A<TargetType32> a) { }
    void m3(B<String> b) { }

    <Z> void m4(A<Z> a) { }
    <Z extends String> void m4(B<Z> b) { }

    int intRes() { return 42; }

    void testLambda(boolean flag) {
        A<Integer> c = flag ? (() -> 23) : (() -> 42);
        m(flag ? (() -> 23) : (() -> 42));
        m2(flag ? (() -> 23) : (() -> 23));
    }

    void testMethodRef(boolean flag) {
        A<Integer> c = flag ? this::intRes : this::intRes;
        m(flag ? this::intRes : this::intRes);
        m2(flag ? this::intRes : this::intRes);
    }

    void testConstrRef(boolean flag) {
        A<TargetType32> c = flag ? TargetType32::new : TargetType32::new;
        m3(flag ? TargetType32::new : TargetType32::new);
        m4(flag ? TargetType32::new : TargetType32::new);
    }

    public static void main(String[] args) {
        TargetType32 test = new TargetType32();
        test.testLambda(true);
        test.testMethodRef(true);
        test.testConstrRef(true);
    }
}
