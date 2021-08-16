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
// srm 961012
// Test if array stores and reads are correct for
// integral types and floating points


/*
 * @test
 *
 * @summary converted from VM Testbase jit/Arrays/ArrayTests.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.Arrays.ArrayTests.ArrayTests
 */

package jit.Arrays.ArrayTests;

import nsk.share.TestFailure;

public class ArrayTests  {
  int base_array[];
  static int the_int_res = 200;
  static int the_char_res = 13041864;
  static int the_byte_res = -312;
  static int n = 400;

  ArrayTests() {
    base_array = new int [n];
    int start_value = n/2;
    for (int i=0; i<n; i++) {
      base_array[i]= start_value;
      start_value--;
    }
  };

  void print() {
    for (int i=0; i<base_array.length; i++)
      System.out.print(" "+base_array[i]);
    // System.out.println("Result is " + the_res);
  }

  boolean with_chars () {
    char char_array[] = new char[n];
    int res = 0;
    for (int i=0; i<n; i++) {
      char_array[i] = (char)base_array[i];
      // System.out.print (" " + (int) char_array[i]);
    }
    for (int i=0; i<n; i++) {
      res += (int) char_array[i];
    }
    System.out.println("chars " + res + " == " + the_char_res);
    return (res==the_char_res);
  }

  boolean with_bytes () {
    byte byte_array[] = new byte[n];
    int res = 0;
    for (int i=0; i<n; i++) {
      byte_array[i] = (byte)base_array[i];
    }
    for (int i=0; i<n; i++) {
      res += (int) byte_array[i];
    }
    System.out.println("bytes " + res + " == " + the_byte_res);
    return res==the_byte_res;
  }

  boolean with_shorts () {
    short short_array[] = new short[n];
    int res = 0;
    for (int i=0; i<n; i++) {
      short_array[i] = (short)base_array[i];
    }
    for (int i=0; i<n; i++) {
      res += (int) short_array[i];
    }
    System.out.println("shorts " + res + " == " + the_int_res);
    return res==the_int_res;
  }

  boolean with_ints () {
    int res = 0;
    for (int i=0; i<n; i++) {
      res +=   base_array[i];
    }
    // base_array is integer
    return (res==the_int_res);
  }

  boolean with_longs() {
    long long_array[] = new long[n];
    int res = 0;
    for (int i=0; i<n; i++) {
      long_array[i] = (long)base_array[i];
    }
    for (int i=0; i<n; i++) {
      res += (int) long_array[i];
    }
    System.out.println("longs " + res + " == " + the_int_res);
    return res==the_int_res;
  }

  boolean with_floats () {
    float float_array[] = new float[n];
    int res = 0;
    for (int i=0; i<n; i++) {
      float_array[i] = (float)base_array[i];
    }
    for (int i=0; i<n; i++) {
      res += (int) float_array[i];
    }
    System.out.println("floats " + res + " == " + the_int_res);
    return res==the_int_res;
  }

  boolean with_doubles () {
    double double_array[] = new double[n];
    int res = 0;
    for (int i=0; i<n; i++) {
      double_array[i] = (double)base_array[i];
    }
    for (int i=0; i<n; i++) {
      res += (int) double_array[i];
    }
    System.out.println("doubles " + res + " == " + the_int_res);
    return res==the_int_res;
  }

  void check(String msg, boolean flag) {
    if (!flag) {
      System.out.println("ERROR in " + msg);
    }
  }

  boolean execute() {
    // print();
    boolean res = true;
    res = res & with_chars();   check("chars",res);
    res = res & with_shorts();  check("shorts",res);
    res = res & with_bytes();   check("bytes",res);
    res = res & with_ints();    check("ints",res);
    res = res & with_longs();   check("longs",res);
    res = res & with_floats();  check("floats",res);
    res = res & with_doubles(); check("doubles",res);

    return res;
  }


  public static void main (String s[]) {
    boolean res = true;
    ArrayTests at = new ArrayTests();
    res  = res  & at.execute();

    if (res) System.out.println("Array read/write testsOK (srm 10/22/96)");
    else throw new TestFailure("Error in read/write array tests!");
  }

}
