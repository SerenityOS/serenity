/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8214751
 * @summary Test operations in C2 MulAddS2I and MulAddVS2VI nodes.
 * @library /test/lib
 * @requires vm.compiler2.enabled
 *
 * @run main/othervm -XX:LoopUnrollLimit=250
 *      -XX:CompileThresholdScaling=0.1
 *      -XX:+UseSuperWord
 *      -XX:LoopMaxUnroll=2
 *      compiler.loopopts.superword.Vec_MulAddS2I
 * @run main/othervm -XX:LoopUnrollLimit=250
 *      -XX:CompileThresholdScaling=0.1
 *      -XX:-UseSuperWord
 *      -XX:LoopMaxUnroll=2
 *      compiler.loopopts.superword.Vec_MulAddS2I
 *
 * @run main/othervm -XX:LoopUnrollLimit=250
 *      -XX:CompileThresholdScaling=0.1
 *      -XX:+UseSuperWord
 *      -XX:LoopMaxUnroll=4
 *      compiler.loopopts.superword.Vec_MulAddS2I
 * @run main/othervm -XX:LoopUnrollLimit=250
 *      -XX:CompileThresholdScaling=0.1
 *      -XX:-UseSuperWord
 *      -XX:LoopMaxUnroll=4
 *      compiler.loopopts.superword.Vec_MulAddS2I
 *
 * @run main/othervm -XX:LoopUnrollLimit=250
 *      -XX:CompileThresholdScaling=0.1
 *      -XX:+UseSuperWord
 *      -XX:LoopMaxUnroll=8
 *      compiler.loopopts.superword.Vec_MulAddS2I
 * @run main/othervm -XX:LoopUnrollLimit=250
 *      -XX:CompileThresholdScaling=0.1
 *      -XX:-UseSuperWord
 *      -XX:LoopMaxUnroll=8
 *      compiler.loopopts.superword.Vec_MulAddS2I
 *
 * @run main/othervm -XX:LoopUnrollLimit=250
 *      -XX:CompileThresholdScaling=0.1
 *      -XX:+UseSuperWord
 *      -XX:LoopMaxUnroll=16
 *      compiler.loopopts.superword.Vec_MulAddS2I
 * @run main/othervm -XX:LoopUnrollLimit=250
 *      -XX:CompileThresholdScaling=0.1
 *      -XX:-UseSuperWord
 *      -XX:LoopMaxUnroll=16
 *      compiler.loopopts.superword.Vec_MulAddS2I
 */

package compiler.loopopts.superword;
import java.util.Random;
import jdk.test.lib.Utils;

public class Vec_MulAddS2I {
        static final int NUM = 1024;
        static int[] out =  new int[NUM];
        static short[] in1 = new short[2*NUM];
        static short[] in2 = new short[2*NUM];
    public static void main(String[] args) throws Exception {
        Vec_MulAddS2IInit(in1, in2);
        int result = 0;
        int valid = 204800000;
        for (int j = 0; j < 10000*512; j++) {
            result = Vec_MulAddS2IImplement(in1, in2, out);
        }
        if (result == valid) {
            System.out.println("Success");
        } else {
            System.out.println("Invalid calculation of element variables in the out array: " + result);
            System.out.println("Expected value for each element of out array = " + valid);
            throw new Exception("Failed");
        }
    }

    public static void Vec_MulAddS2IInit(
            short[] in1,
            short[] in2) {
        for (int i=0; i<2*NUM; i++) {
            in1[i] = (short)4;
            in2[i] = (short)5;
        }
    }

    public static int Vec_MulAddS2IImplement(
            short[] in1,
            short[] in2,
            int[] out) {
        for (int i = 0; i < NUM; i++) {
            out[i] += ((in1[2*i] * in2[2*i]) + (in1[2*i+1] * in2[2*i+1]));
        }
        Random rand = Utils.getRandomInstance();
        int n = rand.nextInt(NUM-1);
        return out[n];
    }

}
