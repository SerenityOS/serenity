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
 * @bug 6726999
 * @summary nsk/stress/jck12a/jck12a010 assert(n != NULL,"Bad immediate dominator info.");
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -Xbatch -XX:+EliminateAutoBox -XX:AutoBoxCacheMax=20000
 *                   -XX:CompileCommand=exclude,compiler.escapeAnalysis.Test6726999::dummy
 *                   compiler.escapeAnalysis.Test6726999
 */

package compiler.escapeAnalysis;

public class Test6726999 {
    static class Point {
        int x;
        int y;
    }

    void dummy() {
        // Empty method to verify correctness of DebugInfo.
        // Use -XX:CompileCommand=exclude,Test.dummy
    }

    int test0_0_0(int y) {
        int x = 3;
        Point p = new Point();
        dummy();
        p.x = x;
        p.y = 3 * x + y;
        return p.x * p.y;
    }

    int test0_0_1(int y) {
        int x = 3;
        Point p = null;
        dummy();
        p = new Point();
        dummy();
        p.x = x;
        p.y = 3 * x + y;
        return p.x * p.y;
    }

    int test0_0_2(int y) {
        int x = 3;
        Point p = new Point();
        dummy();
        p = new Point();
        dummy();
        p.x = x;
        p.y = 3 * x + y;
        return p.x * p.y;
    }

    int test0_0_3(int y) {
        int x = 3;
        Point p[] = new Point[1];
        p[0] = new Point();
        dummy();
        p[0].x = x;
        p[0].y = 3 * x + y;
        return p[0].x * p[0].y;
    }

    int test0_0_4(int y) {
        int x = 3;
        Point p[] = new Point[1];
        dummy();
        p[0] = new Point();
        dummy();
        p[0].x = x;
        p[0].y = 3 * x + y;
        return p[0].x * p[0].y;
    }

    int test0_0_5(int y) {
        int x = 3;
        Point p[] = new Point[1];
        dummy();
        p[0] = null;
        dummy();
        p[0] = new Point();
        dummy();
        p[0].x = x;
        p[0].y = 3 * x + y;
        return p[0].x * p[0].y;
    }

    int test0_0_6(int y) {
        int x = 3;
        Point p[] = new Point[1];
        p[0] = new Point();
        dummy();
        p[0] = new Point();
        dummy();
        p[0].x = x;
        p[0].y = 3 * x + y;
        return p[0].x * p[0].y;
    }

    int test0_1_3(int y) {
        int x = 3;
        Point p1 = new Point();
        dummy();
        Point p[] = new Point[1];
        p[0] = p1;
        dummy();
        p[0].x = x;
        p[0].y = 3 * x + y;
        return p[0].x * p[0].y;
    }

    int test0_1_4(int y) {
        int x = 3;
        Point p1 = new Point();
        dummy();
        Point p[] = new Point[1];
        dummy();
        p[0] = p1;
        dummy();
        p[0].x = x;
        p[0].y = 3 * x + y;
        return p[0].x * p[0].y;
    }

    int test0_1_5(int y) {
        int x = 3;
        Point p1 = new Point();
        dummy();
        Point p[] = new Point[1];
        dummy();
        p[0] = null;
        dummy();
        p[0] = p1;
        dummy();
        p[0].x = x;
        p[0].y = 3 * x + y;
        return p[0].x * p[0].y;
    }

    int test0_1_6(int y) {
        int x = 3;
        Point p1 = new Point();
        dummy();
        Point p2 = new Point();
        dummy();
        Point p[] = new Point[1];
        p[0] = p1;
        dummy();
        p[0] = p2;
        dummy();
        p[0].x = x;
        p[0].y = 3 * x + y;
        return p[0].x * p[0].y;
    }

    int test1_0_0(int y) {
        Point p = new Point();
        if ((y & 1) == 1) {
            p = new Point();
        }
        int x = 3;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test1_0_1(int y) {
        Point p = null;
        if ((y & 1) == 1) {
            p = new Point();
        }
        int x = 3;
        if (p == null)
            return (3 * x + y) * x;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test1_0_2(int y) {
        Point p[] = new Point[1];
        if ((y & 1) == 1) {
            p[0] = new Point();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_0_3(int y) {
        Point p[] = new Point[1];
        p[0] = null;
        if ((y & 1) == 1) {
            p[0] = new Point();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_0_4(int y) {
        Point p[] = new Point[1];
        p[0] = new Point();
        if ((y & 1) == 1) {
            p[0] = new Point();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_0_5(int y) {
        Point p[] = new Point[1];
        if ((y & 1) == 1) {
            p[0] = new Point();
        } else {
            p[0] = null;
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_0_6(int y) {
        Point p[] = new Point[1];
        if ((y & 1) == 1) {
            p[0] = new Point();
        } else {
            p[0] = new Point();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_1_0(int y) {
        Point p = new Point();
        if ((y & 1) == 1) {
            dummy();
            p = new Point();
            dummy();
        }
        int x = 3;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test1_1_1(int y) {
        Point p = null;
        if ((y & 1) == 1) {
            dummy();
            p = new Point();
            dummy();
        }
        int x = 3;
        if (p == null)
            return (3 * x + y) * x;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test1_1_2(int y) {
        Point p[] = new Point[1];
        if ((y & 1) == 1) {
            dummy();
            p[0] = new Point();
            dummy();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_1_3(int y) {
        Point p[] = new Point[1];
        dummy();
        p[0] = null;
        if ((y & 1) == 1) {
            dummy();
            p[0] = new Point();
            dummy();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_1_4(int y) {
        Point p[] = new Point[1];
        dummy();
        p[0] = new Point();
        if ((y & 1) == 1) {
            dummy();
            p[0] = new Point();
            dummy();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_1_5(int y) {
        Point p[] = new Point[1];
        if ((y & 1) == 1) {
            dummy();
            p[0] = new Point();
            dummy();
        } else {
            dummy();
            p[0] = null;
            dummy();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_1_6(int y) {
        Point p[] = new Point[1];
        if ((y & 1) == 1) {
            dummy();
            p[0] = new Point();
            dummy();
        } else {
            dummy();
            p[0] = new Point();
            dummy();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_2_0(int y) {
        Point p1 = new Point();
        dummy();
        Point p = new Point();
        if ((y & 1) == 1) {
            dummy();
            p = p1;
            dummy();
        }
        int x = 3;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test1_2_1(int y) {
        Point p1 = new Point();
        dummy();
        Point p = null;
        if ((y & 1) == 1) {
            dummy();
            p = p1;
            dummy();
        }
        int x = 3;
        if (p == null)
            return (3 * x + y) * x;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test1_2_2(int y) {
        Point p1 = new Point();
        dummy();
        Point p[] = new Point[1];
        if ((y & 1) == 1) {
            dummy();
            p[0] = p1;
            dummy();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_2_3(int y) {
        Point p1 = new Point();
        dummy();
        Point p[] = new Point[1];
        dummy();
        p[0] = null;
        if ((y & 1) == 1) {
            dummy();
            p[0] = p1;
            dummy();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_2_4(int y) {
        Point p1 = new Point();
        dummy();
        Point p2 = new Point();
        dummy();
        Point p[] = new Point[1];
        dummy();
        p[0] = p1;
        if ((y & 1) == 1) {
            dummy();
            p[0] = p2;
            dummy();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_2_5(int y) {
        Point p1 = new Point();
        dummy();
        Point p[] = new Point[1];
        if ((y & 1) == 1) {
            dummy();
            p[0] = p1;
            dummy();
        } else {
            dummy();
            p[0] = null;
            dummy();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test1_2_6(int y) {
        Point p1 = new Point();
        dummy();
        Point p2 = new Point();
        dummy();
        Point p[] = new Point[1];
        if ((y & 1) == 1) {
            dummy();
            p[0] = p1;
            dummy();
        } else {
            dummy();
            p[0] = p2;
            dummy();
        }
        int x = 3;
        if (p[0] == null)
            return (3 * x + y) * x;
        p[0].x = x;
        p[0].y = 3 * x + y;
        dummy();
        return p[0].x * p[0].y;
    }

    int test2_0_0(int y) {
        Point p = new Point();
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            p = new Point();
        }
        int x = 3;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test2_0_1(int y) {
        Point p = null;
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            p = new Point();
        }
        int x = 3;
        if (p == null)
            return (3 * x + y) * x;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test2_0_2(int y) {
        Point p[] = new Point[3];
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            p[i] = new Point();
        }
        int x = 3;
        int j = (y & 1);
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_0_3(int y) {
        Point p[] = new Point[3];
        int j = (y & 1);
        p[j] = null;
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            p[i] = new Point();
        }
        int x = 3;
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_0_4(int y) {
        Point p[] = new Point[3];
        int j = (y & 1);
        p[j] = new Point();
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            p[i] = new Point();
        }
        int x = 3;
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_0_5(int y) {
        Point p[] = new Point[3];
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            p[i] = new Point();
        }
        for (int i = 0; i < lim; i++) {
            p[i] = null;
        }
        int x = 3;
        int j = (y & 1);
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_0_6(int y) {
        Point p[] = new Point[3];
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            p[i] = new Point();
        }
        for (int i = 0; i < lim; i++) {
            p[i] = new Point();
        }
        int x = 3;
        int j = (y & 1);
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_1_0(int y) {
        Point p = new Point();
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p = new Point();
            dummy();
        }
        int x = 3;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test2_1_1(int y) {
        Point p = null;
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p = new Point();
            dummy();
        }
        int x = 3;
        if (p == null)
            return (3 * x + y) * x;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test2_1_2(int y) {
        Point p[] = new Point[3];
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = new Point();
            dummy();
        }
        int x = 3;
        int j = (y & 1);
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_1_3(int y) {
        Point p[] = new Point[3];
        dummy();
        int j = (y & 1);
        p[j] = null;
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = new Point();
            dummy();
        }
        int x = 3;
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_1_4(int y) {
        Point p[] = new Point[3];
        dummy();
        int j = (y & 1);
        p[j] = new Point();
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = new Point();
            dummy();
        }
        int x = 3;
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_1_5(int y) {
        Point p[] = new Point[3];
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = new Point();
            dummy();
        }
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = null;
            dummy();
        }
        int x = 3;
        int j = (y & 1);
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_1_6(int y) {
        Point p[] = new Point[3];
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = new Point();
            dummy();
        }
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = new Point();
            dummy();
        }
        int x = 3;
        int j = (y & 1);
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_2_0(int y) {
        Point p1 = new Point();
        dummy();
        Point p = new Point();
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p = p1;
            dummy();
        }
        int x = 3;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test2_2_1(int y) {
        Point p1 = new Point();
        dummy();
        Point p = null;
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p = p1;
            dummy();
        }
        int x = 3;
        if (p == null)
            return (3 * x + y) * x;
        p.x = x;
        p.y = 3 * x + y;
        dummy();
        return p.x * p.y;
    }

    int test2_2_2(int y) {
        Point p1 = new Point();
        dummy();
        Point p[] = new Point[3];
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = p1;
            dummy();
        }
        int x = 3;
        int j = (y & 1);
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_2_3(int y) {
        Point p1 = new Point();
        dummy();
        Point p[] = new Point[3];
        dummy();
        int j = (y & 1);
        p[j] = null;
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = p1;
            dummy();
        }
        int x = 3;
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_2_4(int y) {
        Point p1 = new Point();
        dummy();
        Point p2 = new Point();
        dummy();
        Point p[] = new Point[3];
        dummy();
        int j = (y & 1);
        p[j] = p1;
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = p2;
            dummy();
        }
        int x = 3;
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_2_5(int y) {
        Point p1 = new Point();
        dummy();
        Point p[] = new Point[3];
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = p1;
            dummy();
        }
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = null;
            dummy();
        }
        int x = 3;
        int j = (y & 1);
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    int test2_2_6(int y) {
        Point p1 = new Point();
        dummy();
        Point p2 = new Point();
        dummy();
        Point p[] = new Point[3];
        int lim = (y & 3);
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = p1;
            dummy();
        }
        for (int i = 0; i < lim; i++) {
            dummy();
            p[i] = p2;
            dummy();
        }
        int x = 3;
        int j = (y & 1);
        if (p[j] == null)
            return (3 * x + y) * x;
        p[j].x = x;
        p[j].y = 3 * x + y;
        dummy();
        return p[j].x * p[0].y;
    }

    public static void main(String args[]) {
        Test6726999 tsr = new Test6726999();
        Point p = new Point();
        Point ptmp = p;
        Class cls = Point.class;
        int y = 0;
        for (int i = 0; i < 10000; i++) {
            y = tsr.test0_0_0(y);
            y = tsr.test0_0_0(y);
            y = tsr.test0_0_1(y);
            y = tsr.test0_0_1(y);
            y = tsr.test0_0_2(y);
            y = tsr.test0_0_2(y);
            y = tsr.test0_0_3(y);
            y = tsr.test0_0_3(y);
            y = tsr.test0_0_4(y);
            y = tsr.test0_0_4(y);
            y = tsr.test0_0_5(y);
            y = tsr.test0_0_5(y);
            y = tsr.test0_0_6(y);
            y = tsr.test0_0_6(y);

            y = tsr.test0_1_3(y);
            y = tsr.test0_1_3(y);
            y = tsr.test0_1_4(y);
            y = tsr.test0_1_4(y);
            y = tsr.test0_1_5(y);
            y = tsr.test0_1_5(y);
            y = tsr.test0_1_6(y);
            y = tsr.test0_1_6(y);

            y = tsr.test1_0_0(y & ~1);
            y = tsr.test1_0_1(y & ~1);
            y = tsr.test1_0_2(y & ~1);
            y = tsr.test1_0_3(y & ~1);
            y = tsr.test1_0_4(y & ~1);
            y = tsr.test1_0_5(y & ~1);
            y = tsr.test1_0_6(y & ~1);
            y = tsr.test1_0_0((y & ~1) + 1);
            y = tsr.test1_0_1((y & ~1) + 1);
            y = tsr.test1_0_2((y & ~1) + 1);
            y = tsr.test1_0_3((y & ~1) + 1);
            y = tsr.test1_0_4((y & ~1) + 1);
            y = tsr.test1_0_5((y & ~1) + 1);
            y = tsr.test1_0_6((y & ~1) + 1);

            y = tsr.test1_1_0(y & ~1);
            y = tsr.test1_1_1(y & ~1);
            y = tsr.test1_1_2(y & ~1);
            y = tsr.test1_1_3(y & ~1);
            y = tsr.test1_1_4(y & ~1);
            y = tsr.test1_1_5(y & ~1);
            y = tsr.test1_1_6(y & ~1);
            y = tsr.test1_1_0((y & ~1) + 1);
            y = tsr.test1_1_1((y & ~1) + 1);
            y = tsr.test1_1_2((y & ~1) + 1);
            y = tsr.test1_1_3((y & ~1) + 1);
            y = tsr.test1_1_4((y & ~1) + 1);
            y = tsr.test1_1_5((y & ~1) + 1);
            y = tsr.test1_1_6((y & ~1) + 1);

            y = tsr.test1_2_0(y & ~1);
            y = tsr.test1_2_1(y & ~1);
            y = tsr.test1_2_2(y & ~1);
            y = tsr.test1_2_3(y & ~1);
            y = tsr.test1_2_4(y & ~1);
            y = tsr.test1_2_5(y & ~1);
            y = tsr.test1_2_6(y & ~1);
            y = tsr.test1_2_0((y & ~1) + 1);
            y = tsr.test1_2_1((y & ~1) + 1);
            y = tsr.test1_2_2((y & ~1) + 1);
            y = tsr.test1_2_3((y & ~1) + 1);
            y = tsr.test1_2_4((y & ~1) + 1);
            y = tsr.test1_2_5((y & ~1) + 1);
            y = tsr.test1_2_6((y & ~1) + 1);

            y = tsr.test2_0_0(y & ~3);
            y = tsr.test2_0_1(y & ~3);
            y = tsr.test2_0_2(y & ~3);
            y = tsr.test2_0_3(y & ~3);
            y = tsr.test2_0_4(y & ~3);
            y = tsr.test2_0_5(y & ~3);
            y = tsr.test2_0_6(y & ~3);
            y = tsr.test2_0_0((y & ~3) + 3);
            y = tsr.test2_0_1((y & ~3) + 3);
            y = tsr.test2_0_2((y & ~3) + 3);
            y = tsr.test2_0_3((y & ~3) + 3);
            y = tsr.test2_0_4((y & ~3) + 3);
            y = tsr.test2_0_5((y & ~3) + 3);
            y = tsr.test2_0_6((y & ~3) + 3);

            y = tsr.test2_1_0(y & ~3);
            y = tsr.test2_1_1(y & ~3);
            y = tsr.test2_1_2(y & ~3);
            y = tsr.test2_1_3(y & ~3);
            y = tsr.test2_1_4(y & ~3);
            y = tsr.test2_1_5(y & ~3);
            y = tsr.test2_1_6(y & ~3);
            y = tsr.test2_1_0((y & ~3) + 3);
            y = tsr.test2_1_1((y & ~3) + 3);
            y = tsr.test2_1_2((y & ~3) + 3);
            y = tsr.test2_1_3((y & ~3) + 3);
            y = tsr.test2_1_4((y & ~3) + 3);
            y = tsr.test2_1_5((y & ~3) + 3);
            y = tsr.test2_1_6((y & ~3) + 3);

            y = tsr.test2_2_0(y & ~3);
            y = tsr.test2_2_1(y & ~3);
            y = tsr.test2_2_2(y & ~3);
            y = tsr.test2_2_3(y & ~3);
            y = tsr.test2_2_4(y & ~3);
            y = tsr.test2_2_5(y & ~3);
            y = tsr.test2_2_6(y & ~3);
            y = tsr.test2_2_0((y & ~3) + 3);
            y = tsr.test2_2_1((y & ~3) + 3);
            y = tsr.test2_2_2((y & ~3) + 3);
            y = tsr.test2_2_3((y & ~3) + 3);
            y = tsr.test2_2_4((y & ~3) + 3);
            y = tsr.test2_2_5((y & ~3) + 3);
            y = tsr.test2_2_6((y & ~3) + 3);

        }
        for (int i = 0; i < 10000; i++) {
            y = tsr.test0_0_0(y);
            y = tsr.test0_0_0(y);
            y = tsr.test0_0_1(y);
            y = tsr.test0_0_1(y);
            y = tsr.test0_0_2(y);
            y = tsr.test0_0_2(y);
            y = tsr.test0_0_3(y);
            y = tsr.test0_0_3(y);
            y = tsr.test0_0_4(y);
            y = tsr.test0_0_4(y);
            y = tsr.test0_0_5(y);
            y = tsr.test0_0_5(y);
            y = tsr.test0_0_6(y);
            y = tsr.test0_0_6(y);

            y = tsr.test0_1_3(y);
            y = tsr.test0_1_3(y);
            y = tsr.test0_1_4(y);
            y = tsr.test0_1_4(y);
            y = tsr.test0_1_5(y);
            y = tsr.test0_1_5(y);
            y = tsr.test0_1_6(y);
            y = tsr.test0_1_6(y);

            y = tsr.test1_0_0(y & ~1);
            y = tsr.test1_0_1(y & ~1);
            y = tsr.test1_0_2(y & ~1);
            y = tsr.test1_0_3(y & ~1);
            y = tsr.test1_0_4(y & ~1);
            y = tsr.test1_0_5(y & ~1);
            y = tsr.test1_0_6(y & ~1);
            y = tsr.test1_0_0((y & ~1) + 1);
            y = tsr.test1_0_1((y & ~1) + 1);
            y = tsr.test1_0_2((y & ~1) + 1);
            y = tsr.test1_0_3((y & ~1) + 1);
            y = tsr.test1_0_4((y & ~1) + 1);
            y = tsr.test1_0_5((y & ~1) + 1);
            y = tsr.test1_0_6((y & ~1) + 1);

            y = tsr.test1_1_0(y & ~1);
            y = tsr.test1_1_1(y & ~1);
            y = tsr.test1_1_2(y & ~1);
            y = tsr.test1_1_3(y & ~1);
            y = tsr.test1_1_4(y & ~1);
            y = tsr.test1_1_5(y & ~1);
            y = tsr.test1_1_6(y & ~1);
            y = tsr.test1_1_0((y & ~1) + 1);
            y = tsr.test1_1_1((y & ~1) + 1);
            y = tsr.test1_1_2((y & ~1) + 1);
            y = tsr.test1_1_3((y & ~1) + 1);
            y = tsr.test1_1_4((y & ~1) + 1);
            y = tsr.test1_1_5((y & ~1) + 1);
            y = tsr.test1_1_6((y & ~1) + 1);

            y = tsr.test1_2_0(y & ~1);
            y = tsr.test1_2_1(y & ~1);
            y = tsr.test1_2_2(y & ~1);
            y = tsr.test1_2_3(y & ~1);
            y = tsr.test1_2_4(y & ~1);
            y = tsr.test1_2_5(y & ~1);
            y = tsr.test1_2_6(y & ~1);
            y = tsr.test1_2_0((y & ~1) + 1);
            y = tsr.test1_2_1((y & ~1) + 1);
            y = tsr.test1_2_2((y & ~1) + 1);
            y = tsr.test1_2_3((y & ~1) + 1);
            y = tsr.test1_2_4((y & ~1) + 1);
            y = tsr.test1_2_5((y & ~1) + 1);
            y = tsr.test1_2_6((y & ~1) + 1);

            y = tsr.test2_0_0(y & ~3);
            y = tsr.test2_0_1(y & ~3);
            y = tsr.test2_0_2(y & ~3);
            y = tsr.test2_0_3(y & ~3);
            y = tsr.test2_0_4(y & ~3);
            y = tsr.test2_0_5(y & ~3);
            y = tsr.test2_0_6(y & ~3);
            y = tsr.test2_0_0((y & ~3) + 3);
            y = tsr.test2_0_1((y & ~3) + 3);
            y = tsr.test2_0_2((y & ~3) + 3);
            y = tsr.test2_0_3((y & ~3) + 3);
            y = tsr.test2_0_4((y & ~3) + 3);
            y = tsr.test2_0_5((y & ~3) + 3);
            y = tsr.test2_0_6((y & ~3) + 3);

            y = tsr.test2_1_0(y & ~3);
            y = tsr.test2_1_1(y & ~3);
            y = tsr.test2_1_2(y & ~3);
            y = tsr.test2_1_3(y & ~3);
            y = tsr.test2_1_4(y & ~3);
            y = tsr.test2_1_5(y & ~3);
            y = tsr.test2_1_6(y & ~3);
            y = tsr.test2_1_0((y & ~3) + 3);
            y = tsr.test2_1_1((y & ~3) + 3);
            y = tsr.test2_1_2((y & ~3) + 3);
            y = tsr.test2_1_3((y & ~3) + 3);
            y = tsr.test2_1_4((y & ~3) + 3);
            y = tsr.test2_1_5((y & ~3) + 3);
            y = tsr.test2_1_6((y & ~3) + 3);

            y = tsr.test2_2_0(y & ~3);
            y = tsr.test2_2_1(y & ~3);
            y = tsr.test2_2_2(y & ~3);
            y = tsr.test2_2_3(y & ~3);
            y = tsr.test2_2_4(y & ~3);
            y = tsr.test2_2_5(y & ~3);
            y = tsr.test2_2_6(y & ~3);
            y = tsr.test2_2_0((y & ~3) + 3);
            y = tsr.test2_2_1((y & ~3) + 3);
            y = tsr.test2_2_2((y & ~3) + 3);
            y = tsr.test2_2_3((y & ~3) + 3);
            y = tsr.test2_2_4((y & ~3) + 3);
            y = tsr.test2_2_5((y & ~3) + 3);
            y = tsr.test2_2_6((y & ~3) + 3);

        }
        for (int i = 0; i < 10000; i++) {
            y = tsr.test0_0_0(y);
            y = tsr.test0_0_0(y);
            y = tsr.test0_0_1(y);
            y = tsr.test0_0_1(y);
            y = tsr.test0_0_2(y);
            y = tsr.test0_0_2(y);
            y = tsr.test0_0_3(y);
            y = tsr.test0_0_3(y);
            y = tsr.test0_0_4(y);
            y = tsr.test0_0_4(y);
            y = tsr.test0_0_5(y);
            y = tsr.test0_0_5(y);
            y = tsr.test0_0_6(y);
            y = tsr.test0_0_6(y);

            y = tsr.test0_1_3(y);
            y = tsr.test0_1_3(y);
            y = tsr.test0_1_4(y);
            y = tsr.test0_1_4(y);
            y = tsr.test0_1_5(y);
            y = tsr.test0_1_5(y);
            y = tsr.test0_1_6(y);
            y = tsr.test0_1_6(y);

            y = tsr.test1_0_0(y & ~1);
            y = tsr.test1_0_1(y & ~1);
            y = tsr.test1_0_2(y & ~1);
            y = tsr.test1_0_3(y & ~1);
            y = tsr.test1_0_4(y & ~1);
            y = tsr.test1_0_5(y & ~1);
            y = tsr.test1_0_6(y & ~1);
            y = tsr.test1_0_0((y & ~1) + 1);
            y = tsr.test1_0_1((y & ~1) + 1);
            y = tsr.test1_0_2((y & ~1) + 1);
            y = tsr.test1_0_3((y & ~1) + 1);
            y = tsr.test1_0_4((y & ~1) + 1);
            y = tsr.test1_0_5((y & ~1) + 1);
            y = tsr.test1_0_6((y & ~1) + 1);

            y = tsr.test1_1_0(y & ~1);
            y = tsr.test1_1_1(y & ~1);
            y = tsr.test1_1_2(y & ~1);
            y = tsr.test1_1_3(y & ~1);
            y = tsr.test1_1_4(y & ~1);
            y = tsr.test1_1_5(y & ~1);
            y = tsr.test1_1_6(y & ~1);
            y = tsr.test1_1_0((y & ~1) + 1);
            y = tsr.test1_1_1((y & ~1) + 1);
            y = tsr.test1_1_2((y & ~1) + 1);
            y = tsr.test1_1_3((y & ~1) + 1);
            y = tsr.test1_1_4((y & ~1) + 1);
            y = tsr.test1_1_5((y & ~1) + 1);
            y = tsr.test1_1_6((y & ~1) + 1);

            y = tsr.test1_2_0(y & ~1);
            y = tsr.test1_2_1(y & ~1);
            y = tsr.test1_2_2(y & ~1);
            y = tsr.test1_2_3(y & ~1);
            y = tsr.test1_2_4(y & ~1);
            y = tsr.test1_2_5(y & ~1);
            y = tsr.test1_2_6(y & ~1);
            y = tsr.test1_2_0((y & ~1) + 1);
            y = tsr.test1_2_1((y & ~1) + 1);
            y = tsr.test1_2_2((y & ~1) + 1);
            y = tsr.test1_2_3((y & ~1) + 1);
            y = tsr.test1_2_4((y & ~1) + 1);
            y = tsr.test1_2_5((y & ~1) + 1);
            y = tsr.test1_2_6((y & ~1) + 1);

            y = tsr.test2_0_0(y & ~3);
            y = tsr.test2_0_1(y & ~3);
            y = tsr.test2_0_2(y & ~3);
            y = tsr.test2_0_3(y & ~3);
            y = tsr.test2_0_4(y & ~3);
            y = tsr.test2_0_5(y & ~3);
            y = tsr.test2_0_6(y & ~3);
            y = tsr.test2_0_0((y & ~3) + 3);
            y = tsr.test2_0_1((y & ~3) + 3);
            y = tsr.test2_0_2((y & ~3) + 3);
            y = tsr.test2_0_3((y & ~3) + 3);
            y = tsr.test2_0_4((y & ~3) + 3);
            y = tsr.test2_0_5((y & ~3) + 3);
            y = tsr.test2_0_6((y & ~3) + 3);

            y = tsr.test2_1_0(y & ~3);
            y = tsr.test2_1_1(y & ~3);
            y = tsr.test2_1_2(y & ~3);
            y = tsr.test2_1_3(y & ~3);
            y = tsr.test2_1_4(y & ~3);
            y = tsr.test2_1_5(y & ~3);
            y = tsr.test2_1_6(y & ~3);
            y = tsr.test2_1_0((y & ~3) + 3);
            y = tsr.test2_1_1((y & ~3) + 3);
            y = tsr.test2_1_2((y & ~3) + 3);
            y = tsr.test2_1_3((y & ~3) + 3);
            y = tsr.test2_1_4((y & ~3) + 3);
            y = tsr.test2_1_5((y & ~3) + 3);
            y = tsr.test2_1_6((y & ~3) + 3);

            y = tsr.test2_2_0(y & ~3);
            y = tsr.test2_2_1(y & ~3);
            y = tsr.test2_2_2(y & ~3);
            y = tsr.test2_2_3(y & ~3);
            y = tsr.test2_2_4(y & ~3);
            y = tsr.test2_2_5(y & ~3);
            y = tsr.test2_2_6(y & ~3);
            y = tsr.test2_2_0((y & ~3) + 3);
            y = tsr.test2_2_1((y & ~3) + 3);
            y = tsr.test2_2_2((y & ~3) + 3);
            y = tsr.test2_2_3((y & ~3) + 3);
            y = tsr.test2_2_4((y & ~3) + 3);
            y = tsr.test2_2_5((y & ~3) + 3);
            y = tsr.test2_2_6((y & ~3) + 3);

        }

        int z = 0;
        y = tsr.test0_0_0(0);
        System.out.println("After 'test0_0_0' y=" + y);
        y = tsr.test0_0_1(0);
        System.out.println("After 'test0_0_1' y=" + y);
        y = tsr.test0_0_2(0);
        System.out.println("After 'test0_0_2' y=" + y);
        y = tsr.test0_0_3(0);
        System.out.println("After 'test0_0_3' y=" + y);
        y = tsr.test0_0_4(0);
        System.out.println("After 'test0_0_4' y=" + y);
        y = tsr.test0_0_5(0);
        System.out.println("After 'test0_0_5' y=" + y);
        y = tsr.test0_0_6(0);
        System.out.println("After 'test0_0_6' y=" + y);
        y = tsr.test0_1_3(0);
        System.out.println("After 'test0_1_3' y=" + y);
        y = tsr.test0_1_4(0);
        System.out.println("After 'test0_1_4' y=" + y);
        y = tsr.test0_1_5(0);
        System.out.println("After 'test0_1_5' y=" + y);
        y = tsr.test0_1_6(0);
        System.out.println("After 'test0_1_6' y=" + y);

        y = tsr.test1_0_0(0);
        System.out.println("After 'test1_0_0' y=" + y);
        y = tsr.test1_0_1(0);
        System.out.println("After 'test1_0_1' y=" + y);
        y = tsr.test1_0_2(0);
        System.out.println("After 'test1_0_2' y=" + y);
        y = tsr.test1_0_3(0);
        System.out.println("After 'test1_0_3' y=" + y);
        y = tsr.test1_0_4(0);
        System.out.println("After 'test1_0_4' y=" + y);
        y = tsr.test1_0_5(0);
        System.out.println("After 'test1_0_5' y=" + y);
        y = tsr.test1_0_6(0);
        System.out.println("After 'test1_0_6' y=" + y);

        y = tsr.test1_1_0(0);
        System.out.println("After 'test1_1_0' y=" + y);
        y = tsr.test1_1_1(0);
        System.out.println("After 'test1_1_1' y=" + y);
        y = tsr.test1_1_2(0);
        System.out.println("After 'test1_1_2' y=" + y);
        y = tsr.test1_1_3(0);
        System.out.println("After 'test1_1_3' y=" + y);
        y = tsr.test1_1_4(0);
        System.out.println("After 'test1_1_4' y=" + y);
        y = tsr.test1_1_5(0);
        System.out.println("After 'test1_1_5' y=" + y);
        y = tsr.test1_1_6(0);
        System.out.println("After 'test1_1_6' y=" + y);

        y = tsr.test1_2_0(0);
        System.out.println("After 'test1_2_0' y=" + y);
        y = tsr.test1_2_1(0);
        System.out.println("After 'test1_2_1' y=" + y);
        y = tsr.test1_2_2(0);
        System.out.println("After 'test1_2_2' y=" + y);
        y = tsr.test1_2_3(0);
        System.out.println("After 'test1_2_3' y=" + y);
        y = tsr.test1_2_4(0);
        System.out.println("After 'test1_2_4' y=" + y);
        y = tsr.test1_2_5(0);
        System.out.println("After 'test1_2_5' y=" + y);
        y = tsr.test1_2_6(0);
        System.out.println("After 'test1_2_6' y=" + y);

        y = tsr.test2_0_0(0);
        System.out.println("After 'test2_0_0' y=" + y);
        y = tsr.test2_0_1(0);
        System.out.println("After 'test2_0_1' y=" + y);
        y = tsr.test2_0_2(0);
        System.out.println("After 'test2_0_2' y=" + y);
        y = tsr.test2_0_3(0);
        System.out.println("After 'test2_0_3' y=" + y);
        y = tsr.test2_0_4(0);
        System.out.println("After 'test2_0_4' y=" + y);
        y = tsr.test2_0_5(0);
        System.out.println("After 'test2_0_5' y=" + y);
        y = tsr.test2_0_6(0);
        System.out.println("After 'test2_0_6' y=" + y);

        y = tsr.test2_1_0(0);
        System.out.println("After 'test2_1_0' y=" + y);
        y = tsr.test2_1_1(0);
        System.out.println("After 'test2_1_1' y=" + y);
        y = tsr.test2_1_2(0);
        System.out.println("After 'test2_1_2' y=" + y);
        y = tsr.test2_1_3(0);
        System.out.println("After 'test2_1_3' y=" + y);
        y = tsr.test2_1_4(0);
        System.out.println("After 'test2_1_4' y=" + y);
        y = tsr.test2_1_5(0);
        System.out.println("After 'test2_1_5' y=" + y);
        y = tsr.test2_1_6(0);
        System.out.println("After 'test2_1_6' y=" + y);

        y = tsr.test2_2_0(0);
        System.out.println("After 'test2_2_0' y=" + y);
        y = tsr.test2_2_1(0);
        System.out.println("After 'test2_2_1' y=" + y);
        y = tsr.test2_2_2(0);
        System.out.println("After 'test2_2_2' y=" + y);
        y = tsr.test2_2_3(0);
        System.out.println("After 'test2_2_3' y=" + y);
        y = tsr.test2_2_4(0);
        System.out.println("After 'test2_2_4' y=" + y);
        y = tsr.test2_2_5(0);
        System.out.println("After 'test2_2_5' y=" + y);
        y = tsr.test2_2_6(0);
        System.out.println("After 'test2_2_6' y=" + y);

    }
}
