/*
 * Copyright (c) 2009, 2010, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6869075
 * @summary regression: javac crashes when compiling compound string assignment with generics
 * @author mcimadamore
 */

public class T6869075 {

    static class Foo<X> {
        X x;
        Foo (X x) {
            this.x = x;
        }
    }

    static void test1(Foo<String> foo) {
        String start = foo.x;
        equals(foo.x += "foo", start + "foo");
    }

    static void test2(Foo<String> foo) {
        String start = foo.x;
        equals((foo.x += "foo"), (start + "foo"));
    }

    static void test3(Foo<String> foo) {
        String start = foo.x;
        equals(((foo.x += "foo")), ((start + "foo")));
    }

    public static void main(String[] args) {
        test1(new Foo<String>("Hello!"));
        test2(new Foo<String>("Hello!"));
        test3(new Foo<String>("Hello!"));
    }

    static void equals(String found, String req) {
        if (!found.equals(req)) {
            throw new AssertionError("Error (expected: "+ req +
                                     " - found: " + found + ")");
        }
    }
}
