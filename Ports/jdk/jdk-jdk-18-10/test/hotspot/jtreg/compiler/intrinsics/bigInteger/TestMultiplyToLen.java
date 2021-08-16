/*
 * Copyright (c) 2014, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8055494
 * @summary Add C2 x86 intrinsic for BigInteger::multiplyToLen() method
 *
 * @library /test/lib
 * @run main/othervm/timeout=600 -XX:-TieredCompilation -Xbatch
 *      -XX:CompileCommand=exclude,compiler.intrinsics.bigInteger.TestMultiplyToLen::main
 *      -XX:CompileCommand=option,compiler.intrinsics.bigInteger.TestMultiplyToLen::base_multiply,ccstr,DisableIntrinsic,_multiplyToLen
 *      -XX:CompileCommand=option,java.math.BigInteger::multiply,ccstr,DisableIntrinsic,_multiplyToLen
 *      -XX:CompileCommand=inline,java.math.BigInteger::multiply
 *      compiler.intrinsics.bigInteger.TestMultiplyToLen
 */

package compiler.intrinsics.bigInteger;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import jdk.test.lib.Utils;

public class TestMultiplyToLen {

    // Avoid intrinsic by preventing inlining multiply() and multiplyToLen().
    public static BigInteger base_multiply(BigInteger op1, BigInteger op2) {
      return op1.multiply(op2);
    }

    // Generate multiplyToLen() intrinsic by inlining multiply().
    public static BigInteger new_multiply(BigInteger op1, BigInteger op2) {
      return op1.multiply(op2);
    }

    public static boolean bytecompare(BigInteger b1, BigInteger b2) {
      byte[] data1 = b1.toByteArray();
      byte[] data2 = b2.toByteArray();
      if (data1.length != data2.length)
        return false;
      for (int i = 0; i < data1.length; i++) {
        if (data1[i] != data2[i])
          return false;
      }
      return true;
    }

    public static String stringify(BigInteger b) {
      String strout= "";
      byte [] data = b.toByteArray();
      for (int i = 0; i < data.length; i++) {
        strout += (String.format("%02x",data[i]) + " ");
      }
      return strout;
    }

    public static void main(String args[]) throws Exception {

      BigInteger oldsum = new BigInteger("0");
      BigInteger newsum = new BigInteger("0");

      BigInteger b1, b2, oldres, newres;

      Random rand = new Random(Utils.getRandomInstance().nextLong());
      Random rand1 = new Random(Utils.getRandomInstance().nextLong());

      for (int j = 0; j < 1000000; j++) {
        int rand_int = rand1.nextInt(3136)+32;
        int rand_int1 = rand1.nextInt(3136)+32;
        b1 = new BigInteger(rand_int, rand);
        b2 = new BigInteger(rand_int1, rand);

        oldres = base_multiply(b1,b2);
        newres = new_multiply(b1,b2);

        oldsum = oldsum.add(oldres);
        newsum = newsum.add(newres);

        if (!bytecompare(oldres,newres)) {
          System.out.println(b1);
          System.out.println(b2);
          System.out.print("mismatch for:b1:" + stringify(b1) + " :b2:" + stringify(b2) + " :oldres:" + stringify(oldres) + " :newres:" + stringify(newres));
          throw new Exception("Failed");
        }
      }

      // Test carry propagation.  Multiple carries during bignum
      // multiplication are rare (especially when using 64-bit
      // arithmetic) so we have to provoke them deliberately.
      for (int j = 4; j <= 396; j += 4) {
        byte[] bytes = new byte[j];
        Arrays.fill(bytes, (byte)255);
        b1 = new BigInteger(bytes);
        b2 = new BigInteger(bytes);

        oldres = base_multiply(b1,b2);
        newres = new_multiply(b1,b2);

        oldsum = oldsum.add(oldres);
        newsum = newsum.add(newres);

        if (!bytecompare(oldres,newres)) {
          System.out.print("mismatch for:b1:" + stringify(b1) + " :b2:" + stringify(b2) + " :oldres:" + stringify(oldres) + " :newres:" + stringify(newres));
          System.out.println(b1);
          System.out.println(b2);
          throw new Exception("Failed");
        }
      }

      if (!bytecompare(oldsum,newsum))  {
        System.out.println("Failure: oldsum:" + stringify(oldsum) + " newsum:" + stringify(newsum));
        throw new Exception("Failed");
      } else {
        System.out.println("Success");
      }
   }
}
