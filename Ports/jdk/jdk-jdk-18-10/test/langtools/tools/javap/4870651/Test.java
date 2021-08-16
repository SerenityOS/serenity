/*
 * Copyright (c) 2008, Oracle and/or its affiliates. All rights reserved.
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

import java.util.*;

abstract class Test<T,E extends Exception & Comparable<T>,U extends Comparable> {
    T t;

    Test(T t) { }
    <G> Test(G g, int i) { }

    Test(String... args) { }
    Test(int i, Object[]... args) { }
    abstract void v1(String... args);
    abstract void v2(int i, String[]... args);

    abstract void a1(int x);
    abstract void a2(int[] x);
    abstract void a3(T x);
    abstract void a4(T[] x);

    abstract int r1();
    abstract int[] r2();
    abstract T r3();
    abstract T[] r4();

    abstract <G> void ga1(int x);
    abstract <G> void ga2(int[] x);
    abstract <G> void ga3(G x);
    abstract <G> void ga4(G[] x);

    abstract <G> int gr1();
    abstract <G> int[] gr2();
    abstract <G> G gr3();
    abstract <G> G[] gr4();

    abstract <G extends Exception> void ge() throws G;

    abstract void w(List<?> l);
    abstract void we(List<? extends T> l);
    abstract void ws(List<? super T> l);

    abstract void t1() throws Error;
    abstract void t2() throws E;
    abstract void t3() throws E,Error;

    abstract void i1(Test<T, E, Comparable> x);
    abstract void i3(Test<T, E, Comparable>.Inner<String> x);

    class Inner<Q> { }
    class Inner2<Q> extends Inner<Q> { }

    class Simple { }

    enum Enum { e1, e2, e3 }
}
