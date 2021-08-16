/*
 * Copyright (c) 2011, Oracle and/or its affiliates. All rights reserved.
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

/**
 * @test
 * @bug 7110586
 * @summary C2 generates icorrect results
 *
 * @run main/othervm -Xbatch compiler.c2.Test7110586
 */

package compiler.c2;

public class Test7110586 {
  static int test1() {
    int i = 0;
    for ( ; i < 11; i+=1) {}
    return i;
  }
  static int test2() {
    int i = 0;
    for ( ; i < 11; i+=2) {}
    return i;
  }
  static int test3() {
    int i = 0;
    for ( ; i < 11; i+=3) {}
    return i;
  }
  static int test11() {
    int i = 0;
    for ( ; i < 11; i+=11) {}
    return i;
  }

  static int testm1() {
    int i = 0;
    for ( ; i > -11; i-=1) {}
    return i;
  }
  static int testm2() {
    int i = 0;
    for ( ; i > -11; i-=2) {}
    return i;
  }
  static int testm3() {
    int i = 0;
    for ( ; i > -11; i-=3) {}
    return i;
  }
  static int testm11() {
    int i = 0;
    for ( ; i > -11; i-=11) {}
    return i;
  }

  public static void main(String args[]) {
    int x1  = 0;
    int x2  = 0;
    int x3  = 0;
    int x11 = 0;
    int m1  = 0;
    int m2  = 0;
    int m3  = 0;
    int m11 = 0;
    for (int i=0; i<10000; i++) {
      x1  = test1();
      x2  = test2();
      x3  = test3();
      x11 = test11();
      m1  = testm1();
      m2  = testm2();
      m3  = testm3();
      m11 = testm11();
    }
    boolean failed = false;
    if (x1 != 11) {
      System.out.println("ERROR (incr = +1): " + x1 + " != 11");
      failed = true;
    }
    if (x2 != 12) {
      System.out.println("ERROR (incr = +2): " + x2 + " != 12");
      failed = true;
    }
    if (x3 != 12) {
      System.out.println("ERROR (incr = +3): " + x3 + " != 12");
      failed = true;
    }
    if (x11 != 11) {
      System.out.println("ERROR (incr = +11): " + x11 + " != 11");
      failed = true;
    }
    if (m1 != -11) {
      System.out.println("ERROR (incr = -1): " + m1 + " != -11");
      failed = true;
    }
    if (m2 != -12) {
      System.out.println("ERROR (incr = -2): " + m2 + " != -12");
      failed = true;
    }
    if (m3 != -12) {
      System.out.println("ERROR (incr = -3): " + m3 + " != -12");
      failed = true;
    }
    if (m11 != -11) {
      System.out.println("ERROR (incr = -11): " + m11 + " != -11");
      failed = true;
    }
    if (failed) {
      System.exit(97);
    }
  }
}
