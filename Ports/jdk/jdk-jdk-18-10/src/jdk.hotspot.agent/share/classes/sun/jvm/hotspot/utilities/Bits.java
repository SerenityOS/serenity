/*
 * Copyright (c) 2001, 2004, Oracle and/or its affiliates. All rights reserved.
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

package sun.jvm.hotspot.utilities;

/** Bit manipulation routines */

public class Bits {
  public static final int AllBits = ~0;
  public static final int NoBits  = 0;
  public static final int OneBit  = 1;

  public static final int BitsPerByte =  8;
  public static final int BitsPerInt  = 32;

  public static final int LogBytesPerInt  = 2;
  public static final int LogBytesPerLong = 3;


  public static int setBits(int x, int m) {
    return x | m;
  }

  public static int clearBits(int x, int m) {
    return x & ~m;
  }

  public static int nthBit(int n) {
    return (n > 32) ? 0 : (OneBit << n);
  }

  public static int setNthBit(int x, int n) {
    return setBits(x, nthBit(n));
  }

  public static int clearNthBit(int x, int n) {
    return clearBits(x, nthBit(n));
  }

  public static boolean isSetNthBit(int word, int n) {
    return maskBits(word, nthBit(n)) != NoBits;
  }

  public static int rightNBits(int n) {
    return (nthBit(n) - 1);
  }

  public static int maskBits(int x, int m) {
    return x & m;
  }

  public static long maskBitsLong(long x, long m) {
    return x & m;
  }

  /** Returns integer round-up to the nearest multiple of s (s must be
      a power of two) */
  public static int roundTo(int x, int s) {
    int m = s - 1;
    return maskBits(x + m, ~m);
  }
}
