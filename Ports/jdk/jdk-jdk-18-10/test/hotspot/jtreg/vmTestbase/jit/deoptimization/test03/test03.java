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
 * @summary converted from VM Testbase jit/deoptimization/test03.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.deoptimization.test03.test03
 */

package jit.deoptimization.test03;

import nsk.share.TestFailure;

/*
 *      Simple loop causes the optimizer to deoptimize
 *      foo methos when an instance of class B is created.
 *
 *      run with the -XX:TraceDeoptimization to observ the result.
 */

public class test03 {
  public static void main (String[] args) {
    A obj = new A();
    for (int index = 0; index < 100; index++) {
      obj.used_alot();
    }
  }
}

class A {
        protected final int counter = 25000;
        protected int count = 0;
        public void foo(int index) {
                count++;

                if (index == counter - 1) {
                        try {
                                ((B)Class.forName("B").newInstance()).foo(index);
                        }
                        catch(Exception e) {
                        }
                }
        }

        public synchronized void used_alot() {
                for (int index = 0; index < counter - 1; index++) {
                        synchronized (extern_Lock) {
                                foo(index);
                        }
                }
        }

        Object extern_Lock = new Object();
}

class B extends A {
  public void foo(int index) {
    count--;
  }
};
