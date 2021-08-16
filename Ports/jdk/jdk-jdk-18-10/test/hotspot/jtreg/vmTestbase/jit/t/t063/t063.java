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
 * @summary converted from VM Testbase jit/t/t063.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.t.t063.t063
 */

package jit.t.t063;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class t063
{
  public static final GoldChecker goldChecker = new GoldChecker( "t063" );

  t063() { }

  byte b = 0;
  short s = 0;
  int i = 0;
  long l = 0;

  static void chkCond(String s, boolean b)
  {
      if(!b)
          t063.goldChecker.println(s);
  }

  static void chkIntVal(String s, String t, int x, int y)
  {
      if(x != y)
          t063.goldChecker.println(s + " " + t + " is " + x + " sb " + y);
  }

  public static void chkBounds()
  {
       t063 ox = new t063();

       ox.b = -128;
       chkCond("err_1", ox.b == -128);

       ox.b = 127;
       chkCond("err_2", ox.b == 127);

       ox.s = -32768;
       chkCond("err_3", ox.s == -32768);

       ox.s = 32767;
       chkCond("err_4", ox.s == 32767);

       ox.i = -2147483647 -1;
       chkCond("err_5", ox.i == 0x80000000);

       ox.i = 2147483647;
       chkCond("err_6", ox.i == 017777777777);

       ox.l = -9223372036854775807L -1;
       chkCond("err_7", ox.l == 0x8000000000000000l);

       ox.l = 9223372036854775807L;
       chkCond("err_8", ox.l == 0x7fffffffffffffffl);

       ox.i = -2147483647 - 2;
       chkCond("err_9", ox.i <= 2147483647);
       chkCond("err_10", ox.i >= 0x80000000);

       ox.i = 2147483647 + 1;
       chkCond("err_11", ox.i <= 2147483647);
       chkCond("err_12", ox.i >= 0x80000000);

       ox.l = -9223372036854775807l - 2;
       chkCond("err_13", ox.l <= 9223372036854775807l);
       chkCond("err_14", ox.l >= 0x8000000000000000l);

       ox.l = 9223372036854775807l + 1;
       chkCond("err_15", ox.l <= 9223372036854775807l);
       chkCond("err_16", ox.l >= 0x8000000000000000l);

       ox.b = 100;
       chkIntVal("err_17", "ox.b * ox.b", ox.b * ox.b, 10000);
       for (int i = 0; i < 10000; i++)
            ox.b++;
       chkCond("err_18", ox.b <= 127);
       chkCond("err_19", ox.b >= -128);

       ox.s = 32750;
       chkIntVal("err_20", "ox.s * ox.s", ox.s * ox.s,  1072562500);
       for (int i = 0; i < 1000000; i++)
            ox.s++;
       chkCond("err_21", ox.s <= 32767);
       chkCond("err_22", ox.s >= -32768);
  }

  public static void main(String argv[])
  {
      t063.goldChecker.println("Hi, t063.java here.");
      chkBounds();
      t063.goldChecker.println("... and that's all the news that's fit to print.");
      t063.goldChecker.check();
  }
}
