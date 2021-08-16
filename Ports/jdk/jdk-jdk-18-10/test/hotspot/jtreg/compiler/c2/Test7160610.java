/*
 * Copyright (c) 2012, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 7160610
 * @summary Unknown Native Code compilation issue.
 * @library /test/lib
 * @run main/othervm -XX:+IgnoreUnrecognizedVMOptions -XX:-OptimizeFill compiler.c2.Test7160610
 */

package compiler.c2;

import jdk.test.lib.Utils;

import java.util.Random;

public class Test7160610 {
  private static final byte[] BYTE_ARRAY = new byte[7];
  private static int[] anIntArray1190 = new int[32768];
  private static int[] anIntArray1191 = new int[32768];

  public static void main(String arg[]) {
    Random rng = Utils.getRandomInstance();
    int i = 256;
    for(int j = BYTE_ARRAY[2]; j < anIntArray1190.length; j++) {
      anIntArray1190[j] = BYTE_ARRAY[2];
    }

    for(int k = BYTE_ARRAY[2]; (k ^ BYTE_ARRAY[1]) > -5001; k++) {
      int i1 = (int)(rng.nextDouble() * 128D * (double)i);
      anIntArray1190[i1] = (int)(rng.nextDouble() * 256D);
    }

    for(int l = BYTE_ARRAY[2]; (l ^ BYTE_ARRAY[1]) > -21; l++) {
      for(int j1 = BYTE_ARRAY[0]; j1 < i + -BYTE_ARRAY[0]; j1++) {
        for(int k1 = BYTE_ARRAY[0]; (k1 ^ BYTE_ARRAY[1]) > -128; k1++) {
          int l1 = k1 - -(j1 << 0x26cb6487);
          anIntArray1191[l1] = (anIntArray1190[l1 + -BYTE_ARRAY[0]] - -anIntArray1190[l1 - -BYTE_ARRAY[0]] - -anIntArray1190[-128 + l1] - -anIntArray1190[128 + l1]) / BYTE_ARRAY[6];
        }
      }
      int ai[] = anIntArray1190;
      anIntArray1190 = anIntArray1191;
      anIntArray1191 = ai;
    }
  }

  static {
    BYTE_ARRAY[6] = 4;
    BYTE_ARRAY[5] = 5;
    BYTE_ARRAY[4] = 3;
    BYTE_ARRAY[3] = 2;
    BYTE_ARRAY[2] = 0;
    BYTE_ARRAY[1] = -1;
    BYTE_ARRAY[0] = 1;
  }
}
