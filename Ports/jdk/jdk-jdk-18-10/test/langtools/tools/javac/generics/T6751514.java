/*
 * Copyright (c) 2008, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6751514
 * @summary Unary post-increment with type variables crash javac during lowering
 * @author Maurizio Cimadamore
 */

public class T6751514 {

    static class Foo<X> {
        X x;
        Foo (X x) {
            this.x = x;
        }
    }

    static void test1(Foo<Integer> foo) {
        int start = foo.x;
        equals(foo.x += 1, start + 1);
        equals(foo.x++, start + 1);
        equals(++foo.x, start + 3);
        equals(foo.x--, start + 3);
        equals(foo.x -= 1, start + 1);
        equals(--foo.x, start);
    }

    static void test2(Foo<Integer> foo) {
        int start = foo.x;
        equals((foo.x) += 1, start + 1);
        equals((foo.x)++, start + 1);
        equals(++(foo.x), start + 3);
        equals((foo.x)--, start + 3);
        equals((foo.x) -= 1, start + 1);
        equals(--(foo.x), start);
    }

    static void test3(Foo<Integer> foo) {
        int start = foo.x;
        equals(((foo.x)) += 1, start + 1);
        equals(((foo.x))++, start + 1);
        equals(++((foo.x)), start + 3);
        equals(((foo.x))--, start + 3);
        equals(((foo.x)) -= 1, start + 1);
        equals(--((foo.x)), start);
    }

    public static void main(String[] args) {
        test1(new Foo<Integer>(1));
        test2(new Foo<Integer>(1));
        test3(new Foo<Integer>(1));
    }

    static void equals(int found, int req) {
        if (found != req) {
            throw new AssertionError("Error (expected: "+ req +
                                     " - found: " + found + ")");
        }
    }
}
