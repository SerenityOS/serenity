/*
 * Copyright (c) 2011, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @key randomness
 * @bug 7100757
 * @summary The BitSet.nextSetBit() produces incorrect result in 32bit VM on Sparc
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 *
 * @run main/timeout=300 compiler.codegen.Test7100757
 */

package compiler.codegen;

import jdk.test.lib.Utils;

import java.util.BitSet;
import java.util.Random;

public class Test7100757 {

  public static final int NBITS = 256;

  public static void main(String[] args) {

    BitSet bs = new BitSet(NBITS);
    Random rnd = Utils.getRandomInstance();
    long[] ra = new long[(NBITS+63)/64];

    for(int l=0; l < 5000000; l++) {

      for(int r = 0; r < ra.length; r++) {
        ra[r] = rnd.nextLong();
      }
      test(ra, bs);
    }
  }

  static void test(long[] ra, BitSet bs) {
      bs.clear();
      int bits_set = 0;
      for(int i = 0, t = 0, b = 0; i < NBITS; i++) {
        long bit = 1L << b++;
        if((ra[t]&bit) != 0) {
          bs.set(i);
          bits_set++;
        }
        if(b == 64) {
          t++;
          b = 0;
        }
      }
      // Test Long.bitCount()
      int check_bits = bs.cardinality();
      if (check_bits != bits_set) {
        String bs_str = bs.toString();
        System.err.printf("cardinality bits: %d != %d  bs: %s\n", check_bits, bits_set, bs_str);
        System.exit(97);
      }
      // Test Long.numberOfTrailingZeros()
      check_bits = 0;
      for (int i = bs.nextSetBit(0); i >= 0; i = bs.nextSetBit(i+1)) {
        check_bits++;
      }
      if (check_bits != bits_set) {
        String bs_str = bs.toString();
        System.err.printf("nextSetBit bits: %d != %d  bs: %s\n", check_bits, bits_set, bs_str);
        System.exit(97);
      }
      // Test Long.numberOfLeadingZeros()
      for(int i = bs.length(); i > 0; i = bs.length()) {
        bs.clear(i-1);
      }
      // Test Long.bitCount()
      check_bits = bs.cardinality();
      if (check_bits != 0) {
        String bs_str = bs.toString();
        System.err.printf("after clear bits: %d != 0  bs: %s\n", check_bits, bs_str);
        System.exit(97);
      }
  }

};
