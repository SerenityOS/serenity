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

/**
 * @test
 * @bug 8002069
 * @summary Assert failed in C2: assert(field->edge_count() > 0) failed: sanity
 *
 * @run main/othervm -Xmx128m -XX:+IgnoreUnrecognizedVMOptions -Xbatch
 *      -XX:CompileCommand=exclude,compiler.c2.Test8002069::dummy
 *      compiler.c2.Test8002069
 */

package compiler.c2;

public class Test8002069 {
    static abstract class O {
        int f;

        public O() {
            f = 5;
        }

        abstract void put(int i);

        public int foo(int i) {
            put(i);
            return i;
        }
    }

    static class A extends O {
        int[] a;

        public A(int s) {
            a = new int[s];
        }

        public void put(int i) {
            a[i % a.length] = i;
        }
    }

    static class B extends O {
        int sz;
        int[] a;

        public B(int s) {
            sz = s;
            a = new int[s];
        }

        public void put(int i) {
            a[i % sz] = i;
        }
    }

    public static void main(String args[]) {
        int sum = 0;
        for (int i = 0; i < 8000; i++) {
            sum += test1(i);
        }
        for (int i = 0; i < 100000; i++) {
            sum += test2(i);
        }
        System.out.println("PASSED. sum = " + sum);
    }

    private O o;

    private int foo(int i) {
        return o.foo(i);
    }

    static int test1(int i) {
        Test8002069 t = new Test8002069();
        t.o = new A(5);
        return t.foo(i);
    }

    static int test2(int i) {
        Test8002069 t = new Test8002069();
        t.o = new B(5);
        dummy(i);
        return t.foo(i);
    }

    static int dummy(int i) {
        return i * 2;
    }
}
