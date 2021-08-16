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
 * @summary converted from VM Testbase runtime/jbe/dead/dead16.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.dead.dead16.dead16
 */

package vm.compiler.jbe.dead.dead16;

// dead16.java

/* -- Test the elimination of redundant code.
      Example:

      double x;
      x = x + 0;
      x = x + 1;
      x = x - 1;
      return x;
 */
class doit {
    static final double pause1(double x) {
        for(int k = 1; k <= 10000; k++) {
            x = x + 1.0;
        }
        return x;
    }

    static final double pause2(double x) {
        for(int k = 1; k <= 10000; k++) {
            x = x - 1.0;
        }
        return x;
    }
}

public class dead16 {
    static double c = 1;
    static double z = 0;
    public static void main(String args[]) {

        System.out.println("un_optimized()="+un_optimized()+"; hand_optimized()="+hand_optimized());
        if (un_optimized() == hand_optimized()) {
            System.out.println("Test dead16 Passed.");
        } else {
            throw new Error("Test dead16 Failed: un_optimized()=" + un_optimized() + " != hand_optimized()=" + hand_optimized());
        }
    }

    static double un_optimized() {
        double x = 1;
        // example 1
        x = x + 0;
        x = x - 0;
        x = x * 1;
        x = x / 1;

        // example 2
        x = x + c;
        x = x - c;
        x = x * c;
        x = x / c;

        // example 3
        x = x + z;
        x = x - z;
        x = x * (z + c);

        // example 4
        x = doit.pause1(x);
        x = doit.pause2(x);
        x = doit.pause2(x);
        x = doit.pause1(x);
        x = doit.pause1(x);
        x = doit.pause1(x);
        x = doit.pause2(x);
        x = doit.pause1(x);
        x = doit.pause2(x);
        x = doit.pause2(x);
        x = doit.pause2(x);
        x = doit.pause1(x);

        return x;
    }

    // Code fragment after redundent code elimination
    static double hand_optimized() {
        int k;
        double x = 1;
        // example 1

        // example 2

        // example 3

        // example 4
        k = 10001;

        return x;
    }
}
