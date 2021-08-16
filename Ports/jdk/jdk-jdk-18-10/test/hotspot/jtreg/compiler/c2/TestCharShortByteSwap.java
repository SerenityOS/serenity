/*
 * Copyright 2010 Google, Inc.  All Rights Reserved.
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

/*
 * @test
 * @bug 6946040
 * @summary Tests Character/Short.reverseBytes and their intrinsics implementation in the server compiler
 *
 * @run main/othervm -Xbatch
 *      -XX:CompileCommand=compileonly,compiler.c2.TestCharShortByteSwap::testChar
 *      -XX:CompileCommand=compileonly,compiler.c2.TestCharShortByteSwap::testShort
 *      compiler.c2.TestCharShortByteSwap
 */

package compiler.c2;

// This test must run without any command line arguments.

public class TestCharShortByteSwap {

  private static short initShort(String[] args, short v) {
    if (args.length > 0) {
      try {
        return (short) Integer.valueOf(args[0]).intValue();
      } catch (NumberFormatException e) { }
    }
    return v;
  }

  private static char initChar(String[] args, char v) {
    if (args.length > 0) {
      try {
        return (char) Integer.valueOf(args[0]).intValue();
      } catch (NumberFormatException e) { }
    }
    return v;
  }

  private static void testChar(char a, char b) {
    if (a != Character.reverseBytes(b)) {
      throw new RuntimeException("FAIL: " + (int)a + " != Character.reverseBytes(" + (int)b + ")");
    }
    if (b != Character.reverseBytes(a)) {
      throw new RuntimeException("FAIL: " + (int)b + " != Character.reverseBytes(" + (int)a + ")");
    }
  }

  private static void testShort(short a, short b) {
    if (a != Short.reverseBytes(b)) {
      throw new RuntimeException("FAIL: " + (int)a + " != Short.reverseBytes(" + (int)b + ")");
    }
    if (b != Short.reverseBytes(a)) {
      throw new RuntimeException("FAIL: " + (int)b + " != Short.reverseBytes(" + (int)a + ")");
    }
  }

  public static void main(String[] args) {
    for (int i = 0; i < 100000; ++i) { // Trigger compilation
      char c1 = initChar(args, (char) 0x0123);
      char c2 = initChar(args, (char) 0x2301);
      char c3 = initChar(args, (char) 0xaabb);
      char c4 = initChar(args, (char) 0xbbaa);
      short s1 = initShort(args, (short) 0x0123);
      short s2 = initShort(args, (short) 0x2301);
      short s3 = initShort(args, (short) 0xaabb);
      short s4 = initShort(args, (short) 0xbbaa);
      testChar(c1, c2);
      testChar(c3, c4);
      testShort(s1, s2);
      testShort(s3, s4);
    }
  }
}
