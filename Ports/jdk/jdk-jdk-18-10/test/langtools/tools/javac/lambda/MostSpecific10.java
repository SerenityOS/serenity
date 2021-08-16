/*
 * Copyright (c) 2014, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8034223
 * @summary Structural most-specific logic for lambdas, method refs, parens, and conditionals
 */

public class MostSpecific10 {

    public static void main(String[] args) {
        new MostSpecific10().test(true);
    }

    interface GetInt {
        int get();
    }

    interface GetInteger {
        Integer get();
    }

    void m(GetInt getter) {}
    void m(GetInteger getter) {
        throw new AssertionError("Less-specific method invocation: " + getter.getClass());
    }

    void test(boolean cond) {
        m(() -> 23);
        m("abc"::length);
        m(( () -> 23 ));
        m(( "abc"::length ));
        m(cond ? () -> 23 : "abc"::length);
        m(( cond ? () -> 23 : "abc"::length ));
        m(cond ? (() -> 23) : ("abc"::length) );
        m(( cond ? () -> 23 : cond ? ("abc"::length) : (() -> 23) ));
    }

}
