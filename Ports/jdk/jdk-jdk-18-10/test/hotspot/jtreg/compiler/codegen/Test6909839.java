/*
 * Copyright (c) 2010, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6909839
 * @summary missing unsigned compare cases for some cmoves in sparc.ad
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+EliminateAutoBox -XX:AutoBoxCacheMax=20000 -Xbatch compiler.codegen.Test6909839
 */

package compiler.codegen;

public class Test6909839 {
    public static void main(String[] args) {
        testi();
        testi();
        testi();
        testui();
        testui();
        testui();
        testdi();
        testdi();
        testdi();
        testfi();
        testfi();
        testfi();

        testl();
        testl();
        testl();
        testul();
        testul();
        testul();
        testdl();
        testdl();
        testdl();
        testfl();
        testfl();
        testfl();

        testf();
        testf();
        testf();
        testuf();
        testuf();
        testuf();
        testdf();
        testdf();
        testdf();
        testff();
        testff();
        testff();

        testd();
        testd();
        testd();
        testud();
        testud();
        testud();
        testdd();
        testdd();
        testdd();
        testfd();
        testfd();
        testfd();

        testp();
        testp();
        testp();
        testup();
        testup();
        testup();
        testdp();
        testdp();
        testdp();
        testfp();
        testfp();
        testfp();
    }

    static void testui() {
        int total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += ((v >= 1 && v < 3) ? 1 : 2);
        }
        System.out.println(total);
    }

    static void testdi() {
        int total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += (v > 1.0) ? 1 : 2;
        }
        System.out.println(total);
    }

    static void testfi() {
        int total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += (v > 1.0f) ? 1 : 2;
        }
        System.out.println(total);
    }

    static void testi() {
        int total = 0;
        for (int i = 0 ; i < 10000; i++) {
            total += (i % 4 != 0) ? 1 : 2;
        }
        System.out.println(total);
    }

    static void testul() {
        long total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += ((v >= 1 && v < 3) ? 1L : 2L);
        }
        System.out.println(total);
    }

    static void testdl() {
        long total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += (v > 1.0) ? 1L : 2L;
        }
        System.out.println(total);
    }

    static void testfl() {
        long total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += (v > 1.0f) ? 1L : 2L;
        }
        System.out.println(total);
    }

    static void testl() {
        long total = 0;
        for (int i = 0 ; i < 10000; i++) {
            total += (i % 4 != 0) ? 1L : 2L;
        }
        System.out.println(total);
    }

    static void testuf() {
        float total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += ((v >= 1 && v < 3) ? 1.0f : 2.0f);
        }
        System.out.println(total);
    }

    static void testdf() {
        float total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += (v > 0.0) ? 1.0f : 2.0f;
        }
        System.out.println(total);
    }

    static void testff() {
        float total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += (v > 0.0f) ? 1.0f : 2.0f;
        }
        System.out.println(total);
    }

    static void testf() {
        float total = 0;
        for (int i = 0 ; i < 10000; i++) {
            total += (i % 4 != 0) ? 1.0f : 2.0f;
        }
        System.out.println(total);
    }

    static void testud() {
        double total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += ((v >= 1 && v < 3) ? 1.0d : 2.0d);
        }
        System.out.println(total);
    }

    static void testdd() {
        double total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += (v > 1.0) ? 1.0d : 2.0d;
        }
        System.out.println(total);
    }

    static void testfd() {
        double total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += (v > 1.0f) ? 1.0d : 2.0d;
        }
        System.out.println(total);
    }

    static void testd() {
        double total = 0;
        for (int i = 0 ; i < 10000; i++) {
            total += (i % 4 != 0) ? 1.0d : 2.0d;
        }
        System.out.println(total);
    }

    static void testp() {
        Object a = new Object();
        Object b = new Object();;
        int total = 0;
        for (int i = 0 ; i < 10000; i++) {
            total += ((i % 4 != 0) ? a : b).hashCode();
        }
        System.out.println(total);
    }

    static void testup() {
        Object a = new Object();
        Object b = new Object();;
        int total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += ((v >= 1 && v < 3) ? a : b).hashCode();
        }
        System.out.println(total);
    }

    static void testdp() {
        Object a = new Object();
        Object b = new Object();;
        int total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += ((v > 1.0) ? a : b).hashCode();
        }
        System.out.println(total);
    }
    static void testfp() {
        Object a = new Object();
        Object b = new Object();;
        int total = 0;
        for (int i = 0 ; i < 10000; i++) {
            int v = i % 4;
            total += ((v > 1.0f) ? a : b).hashCode();
        }
        System.out.println(total);
    }
}
