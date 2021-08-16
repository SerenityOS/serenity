/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8007294
 * @bug 8146999
 * @summary ReduceFieldZeroing doesn't check for dependent load and can lead to incorrect execution
 *
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:+AlwaysIncrementalInline
 *      -XX:-UseOnStackReplacement -XX:-BackgroundCompilation
 *      compiler.c2.Test8007294
 */

package compiler.c2;

public class Test8007294 {

    int i1;
    int i2;

    Test8007294(int i1, int i2) {
        this.i1 = i1;
        this.i2 = i2;
    }

    static int m(int v) {
        return v;
    }

    static Test8007294 test1() {
        Test8007294 obj = new Test8007294(10, 100);
        int v1 = obj.i1;

        int v3 = m(v1);
        int v2 = obj.i2;
        obj.i2 = v3;
        obj.i1 = v2;

        return obj;
    }

    static int test2(int i) {
        int j = 0;
        if (i > 0) {
            j = 1;
        }

        int[] arr = new int[10];
        arr[0] = 1;
        arr[1] = 2;
        int v1 = arr[j];
        arr[0] = 3;
        arr[1] = 4;

        return v1;
    }

    static public void main(String[] args) {
        boolean failed = false;
        for (int i = 0; i < 20000; i++) {
            Test8007294 obj = test1();
            if (obj.i1 != 100 || obj.i2 != 10) {
                System.out.println("FAILED test1 obj.i1 = " + obj.i1 +", obj.i2 = " + obj.i2);
                failed = true;
                break;
            }
        }
        for (int i = 0; i < 20000; i++) {
            test2(0);  // pollute profile
            int res = test2(1);
            if (res != 2) {
                System.out.println("FAILED test2 = " + res);
                failed = true;
                break;
            }
        }
        if (failed) {
            System.exit(97);
        } else {
            System.out.println("PASSED");
        }
    }
}
