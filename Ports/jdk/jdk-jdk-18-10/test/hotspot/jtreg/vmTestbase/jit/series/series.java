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
 * @summary converted from VM Testbase jit/series.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.series.series
 */

package jit.series;

import nsk.share.TestFailure;
import nsk.share.GoldChecker;

public class series {

  public static final GoldChecker goldChecker = new GoldChecker( "series" );

  private double arithmeticSeries(double i)
  {
    if (i == 0.0D)
      return 0.0D;
    else
      return i + arithmeticSeries(i - 1.0D);
  }

  private float arithmeticSeries(float i)
  {
    if (i == 0.0F)
      return 0.0F;
    else
      return i + arithmeticSeries(i - 1.0F);
  }

  private long arithmeticSeries(long i)
  {
    if (i == 0L)
      return 0L;
    else
      return i + arithmeticSeries(i - 1L);
  }

  private int arithmeticSeries(int i)
  {
    if (i == 0)
      return 0;
    else
      return i + arithmeticSeries(i - 1);
  }

  private double fake(double i)
  {
    if (i == 0.0D)
      return 0.0D;
    else
      return i + mul100(i - 1.0D);
  }
  private double mul100(double i)
  {
    return i * 100.0D;
  }

  private float fake(float i)
  {
    if (i == 0.0F)
      return 0.0F;
    else
      return i + mul100(i - 1.0F);
  }
  private float mul100(float i)
  {
    return i * 100F;
  }

  private long fake(long i)
  {
    if (i == 0L)
      return 0L;
    else
      return i + mul100(i - 1L);
  }
  private long mul100(long i)
  {
    return i * 100L;
  }

  private int fake(int i)
  {
    if (i == 0)
      return 0;
    else
      return i + mul100(i - 1);
  }
  private int mul100(int i)
  {
    return i * 100;
  }

  int runit()
  {
     double dres;
     float fres;
     long lres;
     int res;
     int failures = 0;

     res = arithmeticSeries(50);
     series.goldChecker.println("1: "+Math.abs(res - 1275));
     if (res != 1275) {
      series.goldChecker.println(" *** Fail test 1: expected = 1275, computed = " +
res);
      failures++;
     }
     res = fake(50);
     series.goldChecker.println("2: "+Math.abs(res - 4950));
     if (res != 4950) {
      series.goldChecker.println(" *** Fail test 2: expected = 4950, computed = " +
res);
      failures++;
     }
     lres = arithmeticSeries(50L);
     series.goldChecker.println("3: "+Math.abs(lres - 1275L));
     if (lres != 1275L) {
      series.goldChecker.println(" *** Fail test 3: expected = 1275, computed = " +
res);
      failures++;
     }
     lres = fake(50L);
     series.goldChecker.println("4: "+Math.abs(lres - 4950L));
     if (lres != 4950L) {
      series.goldChecker.println(" *** Fail test 4: expected = 4950, computed = " +
res);
      failures++;
     }
     dres = arithmeticSeries(50.0D);
     series.goldChecker.println("5: "+Math.abs(dres - 1275.0D));
     if (Math.abs(dres - 1275.0D) > 1.0E-09D) {
      series.goldChecker.println(" *** Fail test 5: expected = 1275, computed = " +
dres);
      failures++;
     }
     dres = fake(50.0D);
     series.goldChecker.println("6: "+Math.abs(dres - 4950.0D));
     if (Math.abs(dres - 4950.0D) > 5.0E-09D) {
      series.goldChecker.println(" *** Fail test 6: expected = 4950, computed = " +
dres);
      failures++;
     }
     fres = arithmeticSeries(50.0F);
     series.goldChecker.println("7: "+Math.abs(fres - 1275.0F));
     if (Math.abs(fres - 1275.0F) > 1.0E-04F) {
      series.goldChecker.println(" *** Fail test 7: expected = 1275, computed = " +
fres);
      failures++;
     }
     fres = fake(50.0F);
     series.goldChecker.println("8: "+Math.abs(fres - 4950.0F));
     if (Math.abs(fres - 4950.0F) > 5.0E-04F) {
      series.goldChecker.println(" *** Fail test 8: expected = 4950, computed = " +
fres);
      failures++;
     }
     return(failures);
}

  static public void main(String args[])
  {
      series patObj = new series();
      if (patObj.runit()!=0)
         throw new TestFailure("Test failed.");;
                                               series.goldChecker.check();
  }
}
