/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
import java.util.ArrayList;
import java.util.List;

import java.util.random.*;

import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.DoubleSupplier;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

/**
 * @test
 * @summary test bit sequences produced by clases that implement interface RandomGenerator
 * @bug 8248862
 * @run main RandomTestMoments
 * @author Guy Steele
 * @key randomness
 */

public class RandomTestMoments {

    static String currentRNG = "";
    static int failCount = 0;

    static void exceptionOnFail() {
        if (failCount != 0) {
            throw new RuntimeException(failCount + " fails detected");
        }
    }

    static void setRNG(String rng) {
        currentRNG = rng;
    }

    static void fail(String format, Object... args) {
        if (currentRNG.length() != 0) {
            System.err.println(currentRNG);
            currentRNG = "";
        }

        System.err.format("  " + format, args);
        failCount++;
    }

    static final int SEQUENCE_SIZE = 50000;

    // Moment k of uniform distribution over [0.0,1.0) is 1.0/(1+k).

    static double[][] momentsUniform = {
       { 1.00000, 1.00000, 0.01000 },
       { 0.500000, 0.500000, 0.0266265 },
       { 0.333333, 0.333333, 0.0391128 },
       { 0.250000, 0.250000, 0.0477151 },
       { 0.200000, 0.200000, 0.0540496 },
       { 0.166667, 0.166667, 0.0589355 },
       { 0.142857, 0.142857, 0.0628462 },
       { 0.125000, 0.125000, 0.0660693 },
       { 0.111111, 0.111111, 0.0688036 },
       { 0.100000, 0.100000, 0.0712002 },
       { 0.0909091, 0.0909091, 0.0733755 },
       { 0.0833333, 0.0833333, 0.0754172 },
       { 0.0769231, 0.0769231, 0.0773868 },
       { 0.0714286, 0.0714286, 0.0793244 },
       { 0.0666667, 0.0666667, 0.0812526 },
       { 0.0625000, 0.0625000, 0.0831806 },
    };

    // Moment k of exponential distribution with mean 1 is k!.

    static double[][] momentsExponential = {
       { 1.00000, 1.00000, 0.01000 },
       { 1.00000, 1.00000, 0.0718997 },
       { 2.00000, 2.00000, 0.153241 },
       { 6.00000, 6.00000, 0.282813 },
       { 24.0000, 24.0000, 0.503707 },
    };


    // Absolute moment k of Gaussian distribution with mean 0 and stddev 1 is
    //    pow(2.0,k/2.0)*gamma((k+1)/2.0)/sqrt(PI)
    // https://arxiv.org/pdf/1209.4340.pdf, equation (18)

    static double[][] absoluteMomentsGaussian = {
       { 1.00000, 1.00000, 0.01 },
       { 0.797885, 0.797885, 0.1 },
       { 1.00000, 1.00000, 0.1 },
       { 1.59577, 1.59577, 0.2 },
       { 3.00000, 3.00000, 0.2 },
       { 6.38308, 6.38308, 0.2 },
       { 15.0000, 15.0000, 0.2 },
       { 38.2985, 38.2985, 0.2 },
       { 105.000, 105.000, 0.4 },
    };

    static void checkMoments(String test, double[] measurements, double[][] standard) {
       for (int j = 0; j < measurements.length; j++) {
           double actual = measurements[j];
           double expected = standard[j][0];
           double basis = standard[j][1];
           double tolerance = standard[j][2];
           if (!(Math.abs(actual - expected)/basis < tolerance)) {
               fail("%s fail: actual=%f, expected=%f, basis=%f, tolerance=%f\n",
                     test, actual, expected, basis, tolerance);
           }
       }
    }

    static void testMomentsGaussian(DoubleSupplier theSupplier) {
       int m = absoluteMomentsGaussian.length;
       double[] absoluteMoments = new double[m];
       for (int j = 0; j < SEQUENCE_SIZE; j++) {
           double v = theSupplier.getAsDouble();
           double z = 1.0;
           for (int k = 0; k < m; k++) {
               absoluteMoments[k] += Math.abs(z);
               z *= v;
           }
       }
       for (int k = 0; k < m; k++) {
           absoluteMoments[k] /= SEQUENCE_SIZE;
       }
       checkMoments("Gaussian", absoluteMoments, absoluteMomentsGaussian);
    }

    static void testMomentsExponential(DoubleSupplier theSupplier) {
       int m = momentsExponential.length;
       double[] moments = new double[m];
       for (int j = 0; j < SEQUENCE_SIZE; j++) {
           double v = theSupplier.getAsDouble();
           double z = 1.0;
           for (int k = 0; k < m; k++) {
               moments[k] += z;
               z *= v;
           }
       }
       for (int k = 0; k < m; k++) {
           moments[k] /= SEQUENCE_SIZE;
       }
       checkMoments("Exponential", moments, momentsExponential);
    }

    static void testMomentsUniform(DoubleSupplier theSupplier) {
       int m = momentsUniform.length;
       double[] moments = new double[m];
       for (int j = 0; j < SEQUENCE_SIZE; j++) {
           double v = theSupplier.getAsDouble();
           double z = 1.0;
           for (int k = 0; k < m; k++) {
               moments[k] += z;
               z *= v;
           }
       }
       for (int k = 0; k < m; k++) {
           moments[k] /= SEQUENCE_SIZE;
       }
       checkMoments("Uniform", moments, momentsUniform);
    }

    static void testOneRng(RandomGenerator rng) {
        testMomentsGaussian(rng::nextGaussian);
        testMomentsExponential(rng::nextExponential);
        testMomentsUniform(rng::nextDouble);
        testMomentsUniform(rng.doubles().iterator()::next);
    }

    public static void main(String[] args) {
        RandomGeneratorFactory.all()
             .forEach(factory -> {
                setRNG(factory.name());
                testOneRng(factory.create(325) );
            });

        exceptionOnFail();
    }

}
