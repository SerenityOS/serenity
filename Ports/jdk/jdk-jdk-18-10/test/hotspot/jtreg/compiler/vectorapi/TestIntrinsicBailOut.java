/*
 * Copyright (C) 2021 THL A29 Limited, a Tencent company. All rights reserved.
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

package compiler.vectorapi;

import jdk.incubator.vector.*;
import java.nio.ByteOrder;

/*
 * @test
 * @bug 8262998
 * @summary Vector API intrinsincs should not modify IR when bailing out
 * @modules jdk.incubator.vector
 * @run main/othervm -Xbatch -XX:+IgnoreUnrecognizedVMOptions -XX:UseAVX=1
 *                   -XX:-TieredCompilation compiler.vectorapi.TestIntrinsicBailOut
 */


public class TestIntrinsicBailOut {
  static final VectorSpecies<Double> SPECIES256 = DoubleVector.SPECIES_256;
  static byte[] a = new byte[512];
  static byte[] r = new byte[512];

  static void test() {
    DoubleVector av = DoubleVector.fromByteArray(SPECIES256, a, 0, ByteOrder.BIG_ENDIAN);
    av.intoByteArray(r, 0, ByteOrder.BIG_ENDIAN);

    DoubleVector bv = DoubleVector.fromByteArray(SPECIES256, a, 32, ByteOrder.LITTLE_ENDIAN);
    bv.intoByteArray(r, 32, ByteOrder.LITTLE_ENDIAN);
  }

  public static void main(String[] args) {
    for (int i = 0; i < 15000; i++) {
      test();
    }
    System.out.println(r[0] + r[32]);
  }
}
