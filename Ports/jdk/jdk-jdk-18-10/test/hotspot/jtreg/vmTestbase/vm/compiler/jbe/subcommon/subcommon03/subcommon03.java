/*
 * Copyright (c) 2002, 2020, Oracle and/or its affiliates. All rights reserved.
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
 *
 * @summary converted from VM Testbase runtime/jbe/subcommon/subcommon03.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.subcommon.subcommon03.subcommon03
 */

package vm.compiler.jbe.subcommon.subcommon03;

/* Tests the Local Common Sub-expression Elimination optimization, including duplicate
   calls to math function.
 */

public class subcommon03 {
    int k, m, n;
    float a, b, c, d, x;
    float arr[] = new float[100];
    float arr_opt[] = new float[100];
    float arr1[][] = new float[10][10];
    float arr1_opt[][] = new float[10][10];

    public static void main(String args[]) {
        subcommon03 sce = new subcommon03();
        sce.init();
        sce.un_optimized();
        sce.hand_optimized();
        sce.mat();

        if (sce.eCheck()) {
            System.out.println("Test subcommon03 Passed.");
        } else {
            throw new Error("Test subcommon03 Failed.");
        }
    }

    void init() {
        for(int i = 0; i < 10; i++) {
            arr[i] = (float)1E-2;
            arr_opt[i] = (float)1E-2;
        }
        for(m = 0; m < 10; m++) {
            arr1[0][m] = arr[m];
            arr1_opt[0][m] = arr_opt[m];
        }
    }

    void mat() {
        for(k = 1; k < 10; k++) {
            n = k*10;
            for(m = 0; m < 10; m++) {
                arr[n+m] = (float)Math.exp((double)arr[m]);
                arr1[k][m] = (float)(arr[m] * 1/Math.PI);
                arr_opt[n+m] = (float)Math.exp((double)arr_opt[m]);
                arr1_opt[k][m] = (float)(arr_opt[m] * 1/Math.PI);
            }
        }
    }

    void un_optimized() {
        c = (float)1.123456789;
        d = (float)1.010101012;
        // example 1
        a = (float)((c * Math.sqrt(d * 2.0)) / (2.0 * d));
        b = (float)((c / Math.sqrt(d * 2.0)) / (2.0 * d));
        System.out.print("a="+a+";  b="+b);

        // example 2
        c = arr[0] / (arr[0] * arr[0] + arr[1] * arr[1]);
        d = arr[1] * (arr[0] * arr[0] + arr[1] * arr[1]);
        System.out.println(";  c="+c+";  d="+d);

        // example 3
        k = 0;
        float t1 = arr[k];
        float t2 = arr[k] * 2;
        arr[2] = a;
        arr[3] = b;
        arr[4] = c;
        arr[5] = d;
        arr[8] = b / c;
        arr[9] = c - a;

        // Example -4
        c = t2 / t1 * b / a;
        x = (float)(d * 2.0);
        d = t2 / t1 * b / a;
        arr[6] = c;
        arr[7] = d;
    }


    void hand_optimized() {
        c = (float)1.123456789;
        d = (float)1.010101012;
        // example 1
        x = d * (float)2.0;
        double t1 = Math.sqrt((double)x);
        double t2 = 1.0 / (double)x;
        a = (float)(c * t1 * t2);
        b = (float)(c / t1 * t2);
        System.out.print("a_opt="+a+";  b_opt="+b);

        // example 2
        t1 = (double)arr_opt[0];
        t2 = (double)arr_opt[1];
        double t3 = 1.0 / (t1*t1 + t2*t2);
        c = (float)t1 * (float)t3;
        d = (float)t2 / (float)t3;
        System.out.println(";  c_opt="+c+";  d_opt="+d);

        // example 3
        t2 = t1 * 2;
        arr_opt[2] = a;
        arr_opt[3] = b;
        arr_opt[4] = c;
        arr_opt[5] = d;
        arr_opt[8] = b / c;
        arr_opt[9] = c - a;

        // example 4
        c = (float)t2 / (float)t1 * b / a;
        d = c;
        arr_opt[6] = c;
        arr_opt[7] = d;
    }


    boolean eCheck() {
        boolean st = true;

        for (int i = 0; i < arr.length; i++) {
            if (arr[i] != arr_opt[i]) {
                System.out.println(">>Bad output: arr["+i+"]="+arr[i]+"; arr_opt["+i+"]="+arr_opt[i]);
                st = false;
            }
        }

        for (int i = 0; i < arr1.length; i++)
            for (int j = 0; j < arr1[i].length; j++) {
                if (arr1[i][j] != arr1_opt[i][j]) {
                    System.out.println(">>Bad output: arr["+i+"]["+j+"]="+arr1[i][j]+"; arr_opt["+i+"]["+j+"]="+arr1_opt[i][j]);
                    st = false;
                }
            }
        return st;
    }
}
