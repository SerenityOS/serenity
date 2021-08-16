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
 * @summary converted from VM Testbase jit/CEETest.
 * VM Testbase keywords: [jit, quick]
 *
 * @library /vmTestbase
 *          /test/lib
 * @run main/othervm jit.CEETest.CEETest
 */

package jit.CEETest;

import java.io.PrintStream;
import nsk.share.TestFailure;

public class CEETest {

  public static final long WarmUp = 1500;
  public static final long Iterations = 100000;

  public static void main(String args[]) {
    boolean pass = true;
    for (int i=0; ( i < WarmUp ) & pass; i++) {
      pass = pass & doInt();
      pass = pass & doBoolean();
      pass = pass & doByte();
      pass = pass & doChar();
      pass = pass & doShort();
      pass = pass & doLong();
      pass = pass & doFloat();
      pass = pass & doDouble();
      pass = pass & doObject();
      pass = pass & doBitOps();
    }

    long start = System.currentTimeMillis() ;
    for (int i=0; i<Iterations & pass; i++) {
      pass = pass & doInt();
      pass = pass & doBoolean();
      pass = pass & doByte();
      pass = pass & doChar();
      pass = pass & doShort();
      pass = pass & doLong();
      pass = pass & doFloat();
      pass = pass & doDouble();
      pass = pass & doObject();
      pass = pass & doBitOps();
    }

    long duration = System.currentTimeMillis() - start;

    if (true == pass) {
      System.out.println ("CEETest PASSed in " + duration + " ms.");
    }
    else {
      throw new TestFailure("CEETest FAILed in " + duration + " ms.");
    }
  }

  public static boolean doInt () {

    int x = 0;
    int y = 1;

    int a = (x == y) ? x : y;
    int b = (x != y) ? y : x;
    int c = (x <  y) ? y : x;
    int d = (x >  y) ? x : y;
    int e = (x <= y) ? y : x;
    int f = (x >= y) ? x : y;

    if ( (a != y) ||
         (b != y) ||
         (c != y) ||
         (d != y) ||
         (e != y) ||
         (f != y) ) {
      System.err.println ("doInt() failed");
      return false;
    }
    else {
      return true;
    }
  }

  public static boolean doBoolean () {

    boolean x = false;
    boolean y = !x;
    boolean a = (x == y) ? x : y;
    boolean b = (x != y) ? y : x;

    if ( (a == y) &&
         (b == y) ) {
      return true;
    }
    else {
      System.err.println ("doBoolean() failed");
      return false;
    }
  }

  public static boolean doByte () {

    byte x = 0;
    byte y = 1;

    byte a = (x == y) ? x : y;
    byte b = (x != y) ? y : x;
    byte c = (x <  y) ? y : x;
    byte d = (x >  y) ? x : y;
    byte e = (x <= y) ? y : x;
    byte f = (x >= y) ? x : y;

    if ( (a != y) ||
         (b != y) ||
         (c != y) ||
         (d != y) ||
         (e != y) ||
         (f != y) ) {
      System.err.println ("doByte() failed");
      return false;
    }
    else {
      return true;
    }
  }

  public static boolean doChar () {

    char x = 0;
    char y = 1;

    char a = (x == y) ? x : y;
    char b = (x != y) ? y : x;
    char c = (x <  y) ? y : x;
    char d = (x >  y) ? x : y;
    char e = (x <= y) ? y : x;
    char f = (x >= y) ? x : y;

    if ( (a == y) &&
         (b == y) &&
         (c == y) &&
         (d == y) &&
         (e == y) &&
         (f == y) ) {
      return true;
    }
    else {
      System.err.println ("doChar() failed");
      return false;
    }
  }

  public static boolean doShort () {

    short x = 0;
    short y = 1;

    short a = (x == y) ? x : y;
    short b = (x != y) ? y : x;
    short c = (x <  y) ? y : x;
    short d = (x >  y) ? x : y;
    short e = (x <= y) ? y : x;
    short f = (x >= y) ? x : y;

    if ( (a != y) ||
         (b != y) ||
         (c != y) ||
         (d != y) ||
         (e != y) ||
         (f != y) ) {
      System.err.println ("doShort() failed");
      return false;
    }
    else {
      return true;
    }
  }

  public static boolean doLong () {

    long x = 0;
    long y = 1;
    long a = (x == y) ? x : y;
    long b = (x != y) ? y : x;
    long c = (x <  y) ? y : x;
    long d = (x >  y) ? x : y;
    long e = (x <= y) ? y : x;
    long f = (x >= y) ? x : y;

    if ( (a == y) &&
         (b == y) &&
         (c == y) &&
         (d == y) &&
         (e == y) &&
         (f == y) ) {
      return true;
    }
    else {
      System.err.println ("doLong() failed");
      return false;
    }
  }

  public static boolean doFloat () {

    float x = 0.0f;
    float y = 1.0f;
    float a = (x == y) ? x : y;
    float b = (x != y) ? y : x;
    float c = (x <  y) ? y : x;
    float d = (x >  y) ? x : y;
    float e = (x <= y) ? y : x;
    float f = (x >= y) ? x : y;

    if ( (a != y) ||
         (b != y) ||
         (c != y) ||
         (d != y) ||
         (e != y) ||
         (f != y) ) {
      System.err.println ("doFloat() failed");
      return false;
    }
    else {
      return true;
    }
  }

  public static boolean doDouble () {

    double x = 0.0;
    double y = 1.0;
    double a = (x == y) ? x : y;
    double b = (x != y) ? y : x;
    double c = (x <  y) ? y : x;
    double d = (x <= y) ? y : x;
    double e = (x >  y) ? x : y;
    double f = (x >= y) ? x : y;

    if ( (a == y) &&
         (b == y) &&
         (c == y) &&
         (d == y) &&
         (e == y) &&
         (f == y) ) {
      return true;
    }
    else {
      System.err.println ("doDouble() failed");
      return false;
    }
  }

  public static boolean doObject () {

    String x = new String("x");
    String y = new String("y");
    String a = (x == y) ? x : y;
    String b = (x != y) ? y : x;
    String c = (x instanceof String) ? y : x;

    if ( (a != y) ||
         (b != y) ||
         (c != y) ) {
      System.err.println ("doBoolean() failed");
      return false;
    }
    else {
      return true;
    }
  }

  public static boolean doBitOps () {
    int x = 0;
    int y = 1;

    int a = x; a += y;
    int b = (x == y) ? x : (x | y);
    int c = (x == y) ? x : (x ^ y);
    int d = (x == y) ? x : (y & y);
    int e = (x == y) ? x : (y % 2);
    int f = (x == y) ? x : (2 >> y);
    int g = (x == y) ? x : (~-2);

    if ( (a == y) &&
         (b == y) &&
         (c == y) &&
         (d == y) &&
         (e == y) &&
         (f == y) &&
         (g == y) ) {
      return true;
    } else {
      System.err.println ("doBoolean() failed");
      return false;
    }
  }

}
