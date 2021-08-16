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
// testing correct initialization order


/*
 * @test
 *
 * @summary converted from VM Testbase jit/init/init01.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.init.init01.init01
 */

package jit.init.init01;

import nsk.share.TestFailure;

class InitTest1 {
  static int ix1 = 0;
  int i_ix;
  InitTest1 () {
    i_ix = ix1;
  }
}

class InitTest2 {
  static int ix2;
  static InitTest1 oop = new InitTest1();
}

public class init01 {


  public static void main (String s[]) {
        InitTest1.ix1 = 5445;
        InitTest2.ix2 = 1;
        if (InitTest2.oop.i_ix == 5445)
           System.out.println ("Correct order of initialization");
        else
           throw new TestFailure("Incorrect order of initialization");
  }

}
