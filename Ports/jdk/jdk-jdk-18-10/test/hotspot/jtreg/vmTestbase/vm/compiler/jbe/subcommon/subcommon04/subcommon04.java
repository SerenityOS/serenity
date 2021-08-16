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
 * @summary converted from VM Testbase runtime/jbe/subcommon/subcommon04.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.subcommon.subcommon04.subcommon04
 */

package vm.compiler.jbe.subcommon.subcommon04;

/* Tests the Global Common Sub-expression Elimination optimization, including duplicate
   calls to math function.
 */

public class subcommon04 {
    int k, m, n;
    double a, b, c, d;
    double arr[] = new double[100];
    double arr_opt[] = new double[100];
    double arr1[][] = new double[10][10];
    double arr1_opt[][] = new double[10][10];

    public static void main(String args[]) {
        subcommon04 sce = new subcommon04();
        sce.init();
        sce.un_optimized();
        sce.hand_optimized();
        sce.mat();

        if (sce.eCheck()) {
            System.out.println("Test subcommon04 Passed.");
        } else {
            throw new Error("Test subcommon04 Failed.");
        }
    }

    void init() {
        for(int i = 0; i < arr.length; i++) {
            arr[i] = 17E-2;
            arr_opt[i] = 17E-2;
        }
        for(m = 0; m < arr1[0].length; m++) {
            arr1[0][m] = arr[m];
            arr1_opt[0][m] = arr_opt[m];
        }
    }

    void mat() {
        for(k = 0; k < arr1[0].length; k++) {
            n = k * arr1[0].length;
            for(m = 0; m < arr1[0].length; m++) {
                arr[n+m] = Math.exp(arr[m]);
                arr_opt[n+m] = Math.exp(arr_opt[m]);
                arr1[k][m] = (arr[m] * 1/Math.PI);
                arr1_opt[k][m] = (arr_opt[m] * 1/Math.PI);
            }
        }
    }

    void un_optimized() {
        c = 1.123456789;
        d = 1.010101012;
        b = 1E-8;
        // example 1
        if (c == d) {
            a = d * c * (b * 10.0);
        }
        else {
            a = d / c * (b * 10.0);
        }

        // example 2
        if ((a * c) > 9.0) {
            b = a * c / 10.0;
            c = 1.0;
        }
        else {
            b = a * c;
            c = 0.1;
        }

        // example 3
        int n = 9;
        for (k = 0; k < arr.length; k++) {
            n = n - 1;
            if (n < 0) n = 9;
            if (arr1[0][n] == arr[k]) break;
        }
        if (arr1[0][n] == b) c += 1.0;
        arr[2] = a;
        arr[3] = arr1[0][n];
        arr[4] = c;
        arr[5] = d;
        arr[8] = a / c;
        arr[9] = c - a;

        // example 4
        b = d * c;
        d = d * c;
        arr[6] = b;
        arr[7] = d;
    }

    void hand_optimized() {
        c = 1.123456789;
        d = 1.010101012;
        b = 1E-8;
        // example 1
        if (c == d) {
            a = d * c;
        }
        else {
            a = d / c;
        }
        a *= (b * 10.0);

        // example 2
        b = a * c;
        if (b > 9.0) {
            b /= 10.0;
            c = 1.0;
        }
        else {
            c = 0.1;
        }

        // example 3
        double t1 = arr1_opt[0][n];
        n = 9;
        for (k = 0; k < arr_opt.length; k++) {
            n--;
            if (n < 0) n = 9;
            if(t1 == arr_opt[k]) break;
        }
        if (t1 == b) c++;
        arr_opt[2] = a;
        arr_opt[3] = t1;
        arr_opt[4] = c;
        arr_opt[5] = d;
        arr_opt[8] = a / c;
        arr_opt[9] = c - a;

        // example 4
        b = d * c;
        d = b;
        arr_opt[6] = b;
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
