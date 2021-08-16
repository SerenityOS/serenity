/*
 * Copyright (c) 2011 Hewlett-Packard Company. All rights reserved.
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
 *
 */

/**
 * @test
 * @bug 5091921
 * @summary Sign flip issues in loop optimizer
 *
 * @run main/othervm -Xcomp -XX:+IgnoreUnrecognizedVMOptions -XX:MaxInlineSize=1 -XX:C1MaxInlineSize=1
 *    -XX:CompileCommand=compileonly,compiler.c2.Test5091921::*
 *    compiler.c2.Test5091921
 */

package compiler.c2;

public class Test5091921 {
  private static int result = 0;


  /* Test for the bug of transforming indx >= MININT to indx > MININT-1 */
  public static int test_ge1(int limit) {
    int indx;
    int sum = 0;
    for (indx = 500; indx >= limit; indx -= 2) {
      sum += 2000 / indx;
      result = sum;
    }
    return sum;
  }

  /* Test for the bug of transforming indx <= MAXINT to indx < MAXINT+1 */
  public static int test_le1(int limit) {
    int indx;
    int sum = 0;
    for (indx = -500; indx <= limit; indx += 2)
    {
      sum += 3000 / indx;
      result = sum;
    }
    return sum;
  }

  /* Run with -Xcomp -XX:CompileOnly=wrap1.test1 -XX:MaxInlineSize=1 */
  /* limit reset to ((limit-init+stride-1)/stride)*stride+init */
  /* Calculation may overflow */
  public static volatile int c = 1;
  public static int test_wrap1(int limit)
  {
    int indx;
    int sum = 0;
    for (indx = 0xffffffff; indx < limit; indx += 0x20000000)
    {
      sum += c;
    }
    return sum;
  }

  /* Test for range check elimination with bit flip issue for
     scale*i+offset<limit where offset is not 0 */
  static int[] box5 = {1,2,3,4,5,6,7,8,9};
  public static int test_rce5(int[] b, int limit)
  {
    int indx;
    int sum = b[1];
    result = sum;
    for (indx = 0x80000000; indx < limit; ++indx)
    {
      if (indx > 0x80000000)
      {
        // this test is not issued in pre-loop but issued in main loop
        // trick rce into thinking expression is false when indx >= 0
        // in fact it is false when indx==0x80000001
        if (indx - 9 < -9)
        {
          sum += indx;
          result = sum;
          sum ^= b[indx & 7];
          result = sum;
        }
        else
          break;
      }
      else
      {
        sum += b[indx & 3];
        result = sum;
      }
    }
    return sum;
  }

  /* Test for range check elimination with bit flip issue for
     scale*i<limit where scale > 1 */
  static int[] box6 = {1,2,3,4,5,6,7,8,9};
  public static int test_rce6(int[] b, int limit)
  {
    int indx;
    int sum = b[1];
    result = sum;
    for (indx = 0x80000000; indx < limit; ++indx)
    {
      if (indx > 0x80000000)
      {
        // harmless rce target
        if (indx < 0)
        {
          sum += result;
          result = sum;
        }
        else
          break;
        // this test is not issued in pre-loop but issued in main loop
        // trick rce into thinking expression is false when indx >= 0
        // in fact it is false when indx==0x80000001
        // In compilers that transform mulI to shiftI may mask this issue.
        if (indx * 28 + 1 < 0)
        {
          sum += indx;
          result = sum;
          sum ^= b[indx & 7];
          result = sum;
        }
        else
          break;
      }
      else
      {
        sum += b[indx & 3];
        result = sum;
      }
    }
    return sum;
  }

  /* Test for range check elimination with i <= limit */
  static int[] box7 = {1,2,3,4,5,6,7,8,9,0x7fffffff};
  public static int test_rce7(int[] b)
  {
    int indx;
    int max = b[9];
    int sum = b[7];
    result = sum;
    for (indx = 0; indx < b.length; ++indx)
    {
      if (indx <= max)
      {
        sum += (indx ^ 15) + ((result != 0) ? 0 : sum);
        result = sum;
      }
      else
        throw new RuntimeException();
    }
    for (indx = -7; indx < b.length; ++indx)
    {
      if (indx <= 9)
      {
        sum += (sum ^ 15) + ((result != 0) ? 0 : sum);
        result = sum;
      }
      else
        throw new RuntimeException();
    }
    return sum;
  }

  /* Test for range check elimination with i >= limit */
  static int[] box8 = {-1,0,1,2,3,4,5,6,7,8,0x80000000};
  public static int test_rce8(int[] b)
  {
    int indx;
    int sum = b[5];
    int min = b[10];
    result = sum;
    for (indx = b.length-1; indx >= 0; --indx)
    {
      if (indx >= min)
      {
        sum += (sum ^ 9) + ((result != 0) ? 0 :sum);
        result = sum;
      }
      else
        throw new RuntimeException();
    }
    return sum;
  }

  public static void main(String[] args)
  {
    result=1;
    int r = 0;
    try {
      r = test_ge1(0x80000000);
      System.out.println(result);
      System.out.println("test_ge1 FAILED");
      System.exit(1);
    }
    catch (ArithmeticException e1) {
      System.out.println("test_ge1: Expected exception caught");
      if (result != 5986) {
        System.out.println(result);
        System.out.println("test_ge1 FAILED");
        System.exit(97);
      }
    }
    System.out.println("test_ge1 WORKED");

    result=0;
    try
    {
      r = test_le1(0x7fffffff);
      System.out.println(result);
      System.out.println("test_le1 FAILED");
      System.exit(1);
    }
    catch (ArithmeticException e1)
    {
      System.out.println("test_le1: Expected exception caught");
      if (result != -9039)
      {
        System.out.println(result);
        System.out.println("test_le1 FAILED");
        System.exit(97);
      }
    }
    System.out.println("test_le1 WORKED");

    result=0;
    r = test_wrap1(0x7fffffff);
    if (r != 4)
    {
      System.out.println(result);
      System.out.println("test_wrap1 FAILED");
      System.exit(97);
    }
    else
    {
      System.out.println("test_wrap1 WORKED");
    }

    result=0;
    r = test_rce5(box5,0x80000100);
    if (result != 3)
    {
      System.out.println(result);
      System.out.println("test_rce5 FAILED");
      System.exit(97);
    }
    else
    {
      System.out.println("test_rce5 WORKED");
    }

    result=0;
    r = test_rce6(box6,0x80000100);
    if (result != 6)
    {
      System.out.println(result);
      System.out.println("test_rce6 FAILED");
      System.exit(97);
    }
    else
    {
      System.out.println("test_rce6 WORKED");
    }

    result=0;
    r = test_rce7(box7);
    if (result != 14680079)
    {
      System.out.println(result);
      System.out.println("test_rce7 FAILED");
      System.exit(97);
    }
    else
    {
      System.out.println("test_rce7 WORKED");
    }

    result=0;
    r = test_rce8(box8);
    if (result != 16393)
    {
      System.out.println(result);
      System.out.println("test_rce8 FAILED");
      System.exit(97);
    }
    else
    {
      System.out.println("test_rce8 WORKED");
    }
  }
}
