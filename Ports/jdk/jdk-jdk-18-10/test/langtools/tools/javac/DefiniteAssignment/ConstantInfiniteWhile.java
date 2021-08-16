/*
 * Copyright (c) 2000, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4053875
 * @summary Verify correct treatment of '|', '&', and '^' in constant expression
 *          controlling loop. No 'return' statements are necessary, as the loops
 *          cannot terminate normally.
 *
 * @author maddox
 *
 * @run compile ConstantInfiniteWhile.java

public class ConstantInfiniteWhile {

  int test1() {
    while ( false | true ) {}
  }

  int test2() {
    while ( true & true ) {}
  }

  int test3() {
    while ( false ^ true ) {}
  }

  // Just for grins...  (included in original bug report)

  int test4() {
    while ( false == false ) {}
  }

  int test5() {
    while ( 1 != 0 ) {}
  }

  int test6() {
    while ( 1 + 2 > 0 ) {}
  }

  int test7() {
    while ( true ? true : false ) {}
  }

}
