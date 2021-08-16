/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8234692
 * @summary Add C2 x86 intrinsic for BigInteger::shiftLeft() and BigInteger::shiftRight() method
 * @library /test/lib
 * @requires vm.compiler2.enabled
 *
 * @run main/othervm/timeout=600 -XX:-TieredCompilation -Xbatch
 *      -XX:CompileCommand=exclude,compiler.intrinsics.bigInteger.TestShift::main
 *      -XX:CompileCommand=option,compiler.intrinsics.bigInteger.TestShift::base_left_shift,ccstr,DisableIntrinsic,_bigIntegerLeftShiftWorker
 *      -XX:CompileCommand=option,compiler.intrinsics.bigInteger.TestShift::base_right_shift,ccstr,DisableIntrinsic,_bigIntegerRightShiftWorker
 *      -XX:CompileCommand=inline,java.math.BigInteger::shiftLeft
 *      -XX:CompileCommand=inline,java.math.BigInteger::shiftRight
 *      compiler.intrinsics.bigInteger.TestShift
 *
 * @run main/othervm/timeout=600
 *      -XX:CompileCommand=exclude,compiler.intrinsics.bigInteger.TestShift::main
 *      -XX:CompileCommand=option,compiler.intrinsics.bigInteger.TestShift::base_left_shift,ccstr,DisableIntrinsic,_bigIntegerLeftShiftWorker
 *      -XX:CompileCommand=option,compiler.intrinsics.bigInteger.TestShift::base_right_shift,ccstr,DisableIntrinsic,_bigIntegerRightShiftWorker
 *      -XX:CompileCommand=inline,java.math.BigInteger::shiftLeft
 *      -XX:CompileCommand=inline,java.math.BigInteger::shiftRight
 *      compiler.intrinsics.bigInteger.TestShift
 *
 */

package compiler.intrinsics.bigInteger;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Random;
import jdk.test.lib.Utils;

public class TestShift {

    public static BigInteger base_left_shift(BigInteger op1, int shift) {
      return op1.shiftLeft(shift);
    }

    public static BigInteger new_left_shift(BigInteger op1, int shift) {
      return op1.shiftLeft(shift);
    }

    public static BigInteger base_right_shift(BigInteger op1, int shift) {
      return op1.shiftRight(shift);
    }

    public static BigInteger new_right_shift(BigInteger op1, int shift) {
      return op1.shiftRight(shift);
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
      BigInteger [] inputbuffer = new BigInteger[10];
      BigInteger [] oldLeftShiftResult = new BigInteger[10];
      BigInteger [] newLeftShiftResult = new BigInteger[10];
      BigInteger [] oldRightShiftResult = new BigInteger[10];
      BigInteger [] newRightShiftResult = new BigInteger[10];

      Random rand = Utils.getRandomInstance();
      int shiftCount = rand.nextInt(30) + 1;

      for(int i = 0; i < inputbuffer.length; i++) {
        int numbits = rand.nextInt(4096)+32;
        inputbuffer[i] = new BigInteger(numbits, rand);
      }

      for (int j = 0; j < 100000; j++) {
        for(int i = 0; i < inputbuffer.length; i++) {
           oldLeftShiftResult[i] = base_left_shift(inputbuffer[i], shiftCount);
           newLeftShiftResult[i] = new_left_shift(inputbuffer[i], shiftCount);
           if (!bytecompare(oldLeftShiftResult[i], newLeftShiftResult[i])) {
            System.out.println("mismatch for input:" + stringify(inputbuffer[i]) + "\n" + "expected left shift result:" + stringify(oldLeftShiftResult[i]) + "\n" +
                               "calculated left shift result:" + stringify(newLeftShiftResult[i]));
            throw new Exception("Failed");
          }

          oldRightShiftResult[i] = base_right_shift(inputbuffer[i], shiftCount);
          newRightShiftResult[i] = new_right_shift(inputbuffer[i], shiftCount);
          if (!bytecompare(oldRightShiftResult[i], newRightShiftResult[i])) {
            System.out.println("mismatch for input:" + stringify(inputbuffer[i]) + "\n" + "expected right shift result:" + stringify(oldRightShiftResult[i]) + "\n" +
                               "calculated right shift result:" + stringify(newRightShiftResult[i]));
            throw new Exception("Failed");
          }
        }
      }
    }
}
