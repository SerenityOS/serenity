/*
 * Copyright (c) 2004, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 5003207
 * @summary new "for" statement  fails to cast to second upper bound
 * @author gafter
 */

import java.util.*;

interface A {}
interface B<T> extends Iterable<T> {}
interface X {}

class C extends ArrayList<String> implements A, B<String> {
    C() {
        super(Arrays.<String>asList(new String[] {"Hello", "world"}));
    }
}

class D implements A, X {
    final String s;
    D(String s) {
        this.s = s;
    }
    public String toString() {
        return s;
    }
}

public class IntersectIterator {

    static
    <T extends A & B<String>>
    void f(T t) {
        for (String s : t) System.out.println(s);
    }
    static
    <T extends A & X>
    void f(Iterable<T> t) {
        for (A a : t) System.out.println(a);
        for (X x : t) System.out.println(x);
    }

    public static void main(String[] args) {
        f(new C());
        f(Arrays.<D>asList(new D[] {new D("Hello"), new D("world")}));
    }
}
