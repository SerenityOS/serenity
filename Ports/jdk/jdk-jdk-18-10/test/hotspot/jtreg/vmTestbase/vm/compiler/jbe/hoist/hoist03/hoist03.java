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
 * @summary converted from VM Testbase runtime/jbe/hoist/hoist03.
 * VM Testbase keywords: [quick, runtime]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm vm.compiler.jbe.hoist.hoist03.hoist03
 */

package vm.compiler.jbe.hoist.hoist03;


// hoist03.java

/* -- Loop invariant if-then code where the "then" branch is always executed.
      Example:

      double  foo_static[5000];
      for(i=0 ; i<100 ; i++) {
         if (i < 0) {
                foo_static[k] = -1.;
         }
         else {
                foo_static[k] = +1.;
         }
      }

      Can the optimizer break the code up to avoid re-checking the conditional assignment      within the loop when it is known that i will never be < 0 during that loop.

 */

public class hoist03 {
    int LEN = 60000;
    int a[] = new int[LEN];
    int aopt[] = new int[LEN];

    public static void main(String args[]) {
        hoist03 hst = new hoist03();

        hst.f();
        hst.fopt();
        if (hst.eCheck()) {
            System.out.println("Test hoist03 Passed.");
        } else {
            throw new Error("Test hoist03 Failed.");
        }
    }

    void f() {
        // i is always > 0, hence the conditional statement i < 0 is loop invariant.
        int k = 0;

        for (int j = 0; j < 200; j++) {
            for (int i = 0; i < 300; i++) {
                if (i < 0) {
                    a[k++] = -1;
                }
                else {
                    a[k++] = 1;
                }
            }
        }
    }


    // Code fragment after the invariant expression is hoisted out of the loop.
    void fopt() {
        int k = 0;

        for (int j = 0; j < 200; j++) {
            for (int i = 0; i < 300; i++) {
                aopt[k++] = 1;
            }
        }
    }


    // Check Loop Hoisting results
    boolean eCheck() {
        for (int i = 0; i < a.length; i++)
            if (a[i] != aopt[i]) {
                System.out.println("a["+i+"]="+a[i]+"; aopt["+i+"]="+aopt[i]);
                return false;
            }

        return true;
    }
}
