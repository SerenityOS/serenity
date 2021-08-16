/*
 * Copyright (c) 2008, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @summary converted from VM Testbase jit/exception.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.exception.exception
 */

package jit.exception;

/*
   This JIT buster test checks to see if a JIT doing register allocation
   on a machine with a callees saves ABI for non-volatile registers can
   get the exception handling correct. Intel and PowerPC are both such
   machines. The problem is restoring the correct values of i and j in
   the catch block. If i and j are never put into registers, then the
   JIT won't have a problem with correctness because the catch block
   will load the correct values from memory. If the JIT puts i and j
   into registers, then restoring their correct values at the catch
   block gets a little bit tougher.
*/

import nsk.share.TestFailure;

public class exception {
    public static void main(String[] args) {
        int i, j;

        for (i=0,j=0; i<1000000; i++) {
           j=j+1;
           j=j+1;
        }
        try {
           int k;
           k = div(0);
        } catch (Exception e) {
           if ((i != 1000000) || (j != 2000000)) {
              System.out.println("i=" + i + "(expected 1000000), j = " + j + "(expected 2000000)");
              throw new TestFailure("Test FAILS");
           } else {
              System.out.println("Test PASSES");
           }
        }
    }

    static int div(int n) {
        int m=15;
        m = m/n;
        return m;
    }
}
