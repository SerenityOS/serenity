/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8251994
 * @summary Test forced vectorization
 * @requires vm.compiler2.enabled & vm.compMode != "Xint"
 *
 * @run main/othervm -XX:CompileCommand=option,*::test,Vectorize compiler.vectorization.TestOptionVectorize
 */

package compiler.vectorization;

import java.util.stream.IntStream;

public class TestOptionVectorize {
    static final int RANGE = 512;
    static final int ITER  = 100;

    static void init(double[] data) {
       IntStream.range(0, RANGE).parallel().forEach(j -> {
           data[j] = j + 1;
       });
    }

    static void test(double[] data, double A, double B) {
        for (int i = RANGE - 1; i > 0; i--) {
            for (int j = 0; j <= i - 1; j++) {
                data[j] = A * data[j + 1] + B * data[j];
            }
        }
    }

    static void verify(double[] data, double[] gold) {
        for (int i = 0; i < RANGE; i++) {
            if (data[i] != gold[i]) {
                throw new RuntimeException(" Invalid result: data[" + i + "]: " + data[i] + " != " + gold[i]);
            }
        }
    }

    public static void main(String[] args) {
        double[] data = new double[RANGE];
        double[] gold = new double[RANGE];
        System.out.println(" Run test ...");
        init(gold); // reset
        test(gold, 1.0, 2.0);
        for (int i = 0; i < ITER; i++) {
            init(data); // reset
            test(data, 1.0, 2.0);
        }
        verify(data, gold);
        System.out.println(" Finished test.");
    }
}
