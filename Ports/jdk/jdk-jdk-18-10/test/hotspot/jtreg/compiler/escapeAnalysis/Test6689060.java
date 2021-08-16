/*
 * Copyright (c) 2008, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6689060
 * @summary Escape Analysis does not work with Compressed Oops
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -Xbatch -XX:+EliminateAutoBox -XX:AutoBoxCacheMax=20000
 *                   -XX:CompileCommand=exclude,compiler.escapeAnalysis.Test6689060::dummy
 *                   compiler.escapeAnalysis.Test6689060
 */

package compiler.escapeAnalysis;

import java.lang.reflect.Array;

public class Test6689060 {
    static class Point {
        int x;
        int y;
        Point next;
        int ax[];
        int ay[];
        Point pax[];
        Point pay[];

        public Point getNext() {
            return next;
        }
    }

    void dummy() {
        // Empty method to verify correctness of DebugInfo.
        // Use -XX:CompileCommand=exclude,Test.dummy
    }

    int ival(int i) {
        return i * 2;
    }

    int test80(int y, int l, int i) {
        Point p = new Point();
        p.ax = new int[2];
        p.ay = new int[2];
        int x = 3;
        p.ax[0] = x;
        p.ay[1] = 3 * x + y;
        dummy();
        return p.ax[0] * p.ay[1];
    }

    int test81(int y, int l, int i) {
        Point p = new Point();
        p.ax = new int[2];
        p.ay = new int[2];
        int x = 3;
        p.ax[0] = x;
        p.ay[1] = 3 * x + y;
        dummy();
        return p.ax[0] * p.ay[1];
    }


    int test44(int y) {
        Point p1 = new Point();
        p1.x = ival(3);
        dummy();
        p1.y = 3 * p1.x + y;
        return p1.y;
    }

    int test43(int y) {
        Point p1 = new Point();
        if ((y & 1) == 1) {
            p1.x = ival(3);
        } else {
            p1.x = ival(5);
        }
        dummy();
        p1.y = 3 * p1.x + y;
        return p1.y;
    }

    int test42(int y) {
        Point p1 = new Point();
        p1.x = 3;
        for (int i = 0; i < y; i++) {
            if ((i & 1) == 1) {
                p1.x += 4;
            }
        }
        p1.y = 3 * y + p1.x;
        return p1.y;
    }

    int test40(int y) {
        Point p1 = new Point();
        if ((y & 1) == 1) {
            p1.x = 3;
        } else {
            p1.x = 5;
        }
        p1.y = 3 * p1.x + y;
        return p1.y;
    }

    int test41(int y) {
        Point p1 = new Point();
        if ((y & 1) == 1) {
            p1.x += 4;
        } else {
            p1.x += 5;
        }
        p1.y = 3 * p1.x + y;
        return p1.y;
    }

    Point test00(int y) {
        int x = 3;
        Point p = new Point();
        p.x = x;
        p.y = 3 * x + y;
        return p;
    }

    Point test01(int y) {
        int x = 3;
        Point p = new Point();
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p;
    }

    Point test02(int y) {
        int x = 3;
        Point p1 = null;
        for (int i = 0; i < y; i++) {
            Point p2 = new Point();
            p2.x = x;
            p2.y = 3 * y + x;
            p2.next = p1;
            p1 = p2;
        }
        return p1;
    }

    Point test03(int y) {
        int x = 3;
        Point p1 = null;
        for (int i = 0; i < y; i++) {
            Point p2 = new Point();
            p2.x = x;
            p2.y = 3 * y + x;
            p2.next = p1;
            p1 = p2;
        }
        dummy();
        return p1;
    }

    Point test04(int y) {
        int x = 3;
        Point p1 = null;
        for (int i = 0; i < y; i++) {
            Point p2 = new Point();
            p2.x = x;
            p2.y = 3 * y + x;
            p2.next = p1;
            dummy();
            p1 = p2;
        }
        return p1;
    }

    int test05(int y) {
        int x = 3;
        Point p1 = new Point();
        for (int i = 0; i < y; i++) {
            Point p2 = new Point();
            p2.x = x;
            p2.y = 3 * y + x;
            p1.next = p2;
            p1 = p2;
        }
        return p1.y;
    }

    int test0(int y) {
        int x = 3;
        Point p = new Point();
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test1(int y) {
        Point p = new Point();
        if ((y & 1) == 1) {
            p = new Point(); // Kill previous
        }
        int x = 3;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test2(int y) {
        Point p1 = new Point();
        Point p2 = new Point();
        p1.x = 3;
        p2.x = 4;
        p1.y = 3 * p2.x + y;
        p2.y = 3 * p1.x + y;
        dummy();
        return p1.y * p2.y;
    }

    int test3(int y, Point p1) {
        Point p2 = new Point();
        p1.x = 3;
        p2.x = 4;
        p1.y = 3 * p2.x + y;
        p2.y = 3 * p1.x + y;
        dummy();
        return p1.y * p2.y;
    }

    int test4(int y) {
        Point p1 = new Point();
        Point p2 = new Point();
        if ((y & 1) == 1) {
            p1.x = 3;
            p2.x = 4;
        } else {
            p1.x = 5;
            p2.x = 6;
        }
        p1.y = 3 * p2.x + y;
        p2.y = 3 * p1.x + y;
        dummy();
        return p1.y * p2.y;
    }

    int test5(int y, Point p1) {
        Point p2 = new Point();
        if ((y & 1) == 1) {
            p1.x = 3;
            p2.x = 4;
        } else {
            p1.x = 5;
            p2.x = 6;
        }
        p1.y = 3 * p2.x + y;
        p2.y = 3 * p1.x + y;
        dummy();
        return p1.y * p2.y;
    }

    int test6(int y) {
        Point p1 = new Point();
        Point p2 = new Point();
        p1.next = p2;
        if ((y & 1) == 1) {
            p1.x = 3;
            p1.getNext().x = 4;
        } else {
            p1.x = 5;
            p1.getNext().x = 6;
        }
        p1.y = 3 * p2.x + y;
        p2.y = 3 * p1.x + y;
        dummy();
        return p1.y * p2.y;
    }

    int test7(int y, Point p1) {
        Point p2 = new Point();
        p1.next = p2;
        if ((y & 1) == 1) {
            p1.x = 3;
            p1.getNext().x = 4;
        } else {
            p1.x = 5;
            p1.getNext().x = 6;
        }
        p1.y = 3 * p2.x + y;
        p2.y = 3 * p1.x + y;
        dummy();
        return p1.y * p2.y;
    }

    int test8(int y, int l, int i) {
        Point p = new Point();
        p.ax = new int[l];
        p.ay = new int[l];
        int x = 3;
        p.ax[i] = x;
        p.ay[i] = 3 * x + y;
        dummy();
        return p.ax[i] * p.ay[i];
    }

    int test9(int y, int l, int i) {
        Point p = new Point();
        p.pax = new Point[l];
        p.pay = new Point[l];
        p.pax[i] = new Point();
        p.pay[i] = new Point();
        p.pax[i].x = 3;
        p.pay[i].x = 4;
        p.pax[i].y = 3 * p.pay[i].x + y;
        p.pay[i].y = 3 * p.pax[i].x + y;
        dummy();
        return p.pax[i].y * p.pay[i].y;
    }

    int test10(int y, int l, int i, Class cls) {
        Point p = new Point();
        try {
            p.pax = (Point[]) Array.newInstance(cls, l);
            p.pax[i] = (Point) cls.newInstance();
        } catch (java.lang.InstantiationException ex) {
            return 0;
        } catch (java.lang.IllegalAccessException ex) {
            return 0;
        }
        p.pax[i].x = 3;
        p.pax[i].y = 3 * p.pax[i].x + y;
        dummy();
        return p.pax[i].x * p.pax[i].y;
    }

    int test11(int y) {
        Point p1 = new Point();
        Point p2 = new Point();
        p1.next = p2;
        if ((y & 1) == 1) {
            p1.x = 3;
            p1.next.x = 4;
        } else {
            p1.x = 5;
            p1.next.x = 6;
        }
        p1.y = 3 * p1.next.x + y;
        p1.next.y = 3 * p1.x + y;
        dummy();
        return p1.y * p1.next.y;
    }

    int test12(int y) {
        Point p1 = new Point();
        p1.next = p1;
        if ((y & 1) == 1) {
            p1.x = 3;
            p1.next.x = 4;
        } else {
            p1.x = 5;
            p1.next.x = 6;
        }
        p1.y = 3 * p1.next.x + y;
        p1.next.y = 3 * p1.x + y;
        dummy();
        return p1.y * p1.next.y;
    }


    public static void main(String args[]) {
        Test6689060 tsr = new Test6689060();
        Point p = new Point();
        Point ptmp = p;
        Class cls = Point.class;
        int y = 0;
        for (int i = 0; i < 10000; i++) {
            ptmp.next = tsr.test00(1);
            ptmp.next = tsr.test01(1);
            ptmp.next = tsr.test02(1);
            ptmp.next = tsr.test03(1);
            ptmp.next = tsr.test04(1);

            y = tsr.test05(1);

            y = tsr.test80(y, 1, 0);
            y = tsr.test81(y, 1, 0);

            y = tsr.test44(y);
            y = tsr.test43(y);
            y = tsr.test42(y);
            y = tsr.test40(y);
            y = tsr.test41(y);

            y = tsr.test0(y);
            y = tsr.test1(y);
            y = tsr.test2(y);
            y = tsr.test3(y, p);
            y = tsr.test4(y);
            y = tsr.test5(y, p);
            y = tsr.test6(y);
            y = tsr.test7(y, p);
            y = tsr.test8(y, 1, 0);
            y = tsr.test9(y, 1, 0);
            y = tsr.test10(y, 1, 0, cls);
            y = tsr.test11(y);
            y = tsr.test12(y);
        }
        for (int i = 0; i < 10000; i++) {
            ptmp.next = tsr.test00(1);
            ptmp.next = tsr.test01(1);
            ptmp.next = tsr.test02(1);
            ptmp.next = tsr.test03(1);
            ptmp.next = tsr.test04(1);

            y = tsr.test05(1);

            y = tsr.test80(y, 1, 0);
            y = tsr.test81(y, 1, 0);

            y = tsr.test44(y);
            y = tsr.test43(y);
            y = tsr.test42(y);
            y = tsr.test40(y);
            y = tsr.test41(y);

            y = tsr.test0(y);
            y = tsr.test1(y);
            y = tsr.test2(y);
            y = tsr.test3(y, p);
            y = tsr.test4(y);
            y = tsr.test5(y, p);
            y = tsr.test6(y);
            y = tsr.test7(y, p);
            y = tsr.test8(y, 1, 0);
            y = tsr.test9(y, 1, 0);
            y = tsr.test10(y, 1, 0, cls);
            y = tsr.test11(y);
            y = tsr.test12(y);
        }
        for (int i = 0; i < 10000; i++) {
            ptmp.next = tsr.test00(1);
            ptmp.next = tsr.test01(1);
            ptmp.next = tsr.test02(1);
            ptmp.next = tsr.test03(1);
            ptmp.next = tsr.test04(1);

            y = tsr.test05(1);

            y = tsr.test80(y, 1, 0);
            y = tsr.test81(y, 1, 0);

            y = tsr.test44(y);
            y = tsr.test43(y);
            y = tsr.test42(y);
            y = tsr.test40(y);
            y = tsr.test41(y);

            y = tsr.test0(y);
            y = tsr.test1(y);
            y = tsr.test2(y);
            y = tsr.test3(y, p);
            y = tsr.test4(y);
            y = tsr.test5(y, p);
            y = tsr.test6(y);
            y = tsr.test7(y, p);
            y = tsr.test8(y, 1, 0);
            y = tsr.test9(y, 1, 0);
            y = tsr.test10(y, 1, 0, cls);
            y = tsr.test11(y);
            y = tsr.test12(y);
        }

        int z = 0;
        y = tsr.test80(0, 1, 0);
        z += y;
        System.out.println("After 'test80' y=" + y);
        y = tsr.test81(0, 1, 0);
        z += y;
        System.out.println("After 'test81' y=" + y);

        y = tsr.test44(0);
        z += y;
        System.out.println("After 'test44' y=" + y);
        y = tsr.test43(0);
        z += y;
        System.out.println("After 'test43' y=" + y);
        y = tsr.test42(0);
        z += y;
        System.out.println("After 'test42' y=" + y);
        y = tsr.test40(0);
        z += y;
        System.out.println("After 'test40' y=" + y);
        y = tsr.test41(0);
        z += y;
        System.out.println("After 'test41' y=" + y);

        ptmp.next = tsr.test00(1);
        z += y;
        System.out.println("After 'test00' p.y=" + ptmp.next.y);
        ptmp.next = tsr.test01(1);
        z += y;
        System.out.println("After 'test01' p.y=" + ptmp.next.y);
        ptmp.next = tsr.test02(1);
        z += y;
        System.out.println("After 'test02' p.y=" + ptmp.next.y);
        ptmp.next = tsr.test03(1);
        z += y;
        System.out.println("After 'test03' p.y=" + ptmp.next.y);
        ptmp.next = tsr.test04(1);
        z += y;
        System.out.println("After 'test04' p.y=" + ptmp.next.y);

        y = tsr.test05(1);
        z += y;
        System.out.println("After 'test05' y=" + y);

        y = tsr.test0(0);
        z += y;
        System.out.println("After 'test0' y=" + y);
        y = tsr.test1(0);
        z += y;
        System.out.println("After 'test1' y=" + y);
        y = tsr.test2(0);
        z += y;
        System.out.println("After 'test2' y=" + y);
        y = tsr.test3(0, new Point());
        z += y;
        System.out.println("After 'test3' y=" + y);
        y = tsr.test4(0);
        z += y;
        System.out.println("After 'test4' y=" + y);
        y = tsr.test5(0, new Point());
        z += y;
        System.out.println("After 'test5' y=" + y);
        y = tsr.test6(0);
        z += y;
        System.out.println("After 'test6' y=" + y);
        y = tsr.test7(0, new Point());
        z += y;
        System.out.println("After 'test7' y=" + y);
        y = tsr.test8(0, 1, 0);
        z += y;
        System.out.println("After 'test8' y=" + y);
        y = tsr.test9(0, 1, 0);
        z += y;
        System.out.println("After 'test9' y=" + y);
        y = tsr.test10(0, 1, 0, cls);
        z += y;
        System.out.println("After 'test10' y=" + y);
        y = tsr.test11(0);
        z += y;
        System.out.println("After 'test11' y=" + y);
        y = tsr.test12(0);
        z += y;
        System.out.println("After 'test12' y=" + y);
        System.out.println("Sum of y =" + z);
    }
}
