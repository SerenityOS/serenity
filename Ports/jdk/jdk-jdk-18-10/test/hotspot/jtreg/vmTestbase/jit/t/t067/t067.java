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
 * @summary converted from VM Testbase jit/t/t067.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t067.t067
 */

package jit.t.t067;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t067
{
  public static final GoldChecker goldChecker = new GoldChecker( "t067" );

  static int i = -1;

  static void chkCond(String s, boolean b)
  {
      t067.goldChecker.print(s);
      if(b)
          t067.goldChecker.println(" ok");
      else
          t067.goldChecker.println(" fubar");
  }

  private static void check_sign()
  {
   // dividend is positive
   //
     chkCond("err_1", (Integer.MAX_VALUE%Integer.MIN_VALUE)>=0);
     chkCond("err_2", (Integer.MAX_VALUE%i)>=0);
     chkCond("err_3", (1%Integer.MIN_VALUE)>=0);
     chkCond("err_4", (1%(-1))>=0);
     chkCond("err_5", (3%4)>=0);

   // dividend is negative
   //
     chkCond("err_6", (Integer.MIN_VALUE%Integer.MAX_VALUE)<=0);
     chkCond("err_7", (Integer.MIN_VALUE%1)<=0);

     // This one fails because the division raises some sort of
     // hardware arithmetic exception.  I guess the hardware notices
     // that it can't represent the result of 0x80000000 / -1 or
     // something.  Anyhow, the behavior is the same, jit or no jit.
     // If they ever get the VM fixed, I'll have to make the
     // corresponding fix to the jit.
     // chkCond("err_8", (Integer.MIN_VALUE%(i))<=0);

     chkCond("err_9", ((-1)%Integer.MAX_VALUE)<=0);
     chkCond("err_10", ((-1)%Integer.MIN_VALUE)<=0);
     chkCond("err_11", (-2)%(-1)<=0);
     chkCond("err_12", (-2)%(-3)<=0);

   // dividend is zero
   //
     chkCond("err_13", (0%Integer.MIN_VALUE)==0);
  }

  public static void main(String argv[])
  {
      t067.goldChecker.println("Integer.MIN_VALUE == " + Integer.MIN_VALUE);
      check_sign();
      t067.goldChecker.println("Miller time.");
      t067.goldChecker.check();
  }
}
