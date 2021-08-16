/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
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

import java.util.Comparator;
import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.*;
import java.util.random.RandomGeneratorFactory;

/**
 * @test
 * @summary test bit sequences produced by clases that implement interface RandomGenerator
 * @bug 8248862
 * @run main RandomCanaryPi
 * @key randomness
 */

public class RandomCanaryPi {
   static double pi(RandomGenerator rng) {
        int N = 10000000;
        int k = 0;

        for (int i = 0; i < N; i++) {
            double x = rng.nextDouble();
            double y = rng.nextDouble();

            if (x * x + y * y <= 1.0) {
                k++;
            }
        }

        return 4.0 * (double)k / (double)N;
    }

    static int failed = 0;

    public static void main(String[] args) {
        RandomGeneratorFactory.all()
                .sorted(Comparator.comparing(RandomGeneratorFactory::name))
                .forEach(factory -> {
                    RandomGenerator rng = factory.create();
                    double pi = pi(rng);
                    double delta = Math.abs(Math.PI - pi);
                    boolean pass = delta < 1E-2;

                    if (!pass) {
                        System.err.println("Algorithm    = " + factory.name() + " failed");
                        System.err.println("Actual       = " + Math.PI);
                        System.err.println("Monte Carlo  = " + pi);
                        System.err.println("Delta        = " + delta);
                        System.err.println();

                        failed++;
                    }
                });
        if (failed != 0) {
            throw new RuntimeException(failed + " tests failed");
        }
    }
}
