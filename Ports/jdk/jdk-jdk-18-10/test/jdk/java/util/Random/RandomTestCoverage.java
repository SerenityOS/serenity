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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.concurrent.ThreadLocalRandom;
import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.ArbitrarilyJumpableGenerator;
import java.util.random.RandomGenerator.JumpableGenerator;
import java.util.random.RandomGenerator.LeapableGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.random.RandomGenerator.StreamableGenerator;
import java.util.random.RandomGeneratorFactory;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

/**
 * @test
 * @summary Ensure that all implementations of RandomGenerator supply required methods.
 * @bug 8248862
 * @run main RandomTestCoverage
 * @key randomness
 */


public class RandomTestCoverage {
    static void coverRandomGenerator(RandomGenerator rng) {
        boolean bool = rng.nextBoolean();
        byte[] bytes = new byte[8];
        rng.nextBytes(bytes);

        int i1 = rng.nextInt();
        int i2 = rng.nextInt(10);
        int i3 = rng.nextInt(5, 10);

        long l1 = rng.nextLong();
        long l2 = rng.nextLong(10L);
        long l3 = rng.nextLong(5L, 10L);

        float f1 = rng.nextFloat();
        float f2 = rng.nextFloat(1.0f);
        float f3 = rng.nextFloat(0.5f, 1.0f);

        double d1 = rng.nextDouble();
        double d2 = rng.nextDouble(1.0);
        double d3 = rng.nextDouble(0.5, 1.0);

        double exp = rng.nextExponential();
        double gauss1 = rng.nextGaussian();
        double gauss2 = rng.nextGaussian(0.5, 2.0);

        IntStream intStream1 = rng.ints();
        IntStream intStream2 = rng.ints(5, 10);
        IntStream intStream3 = rng.ints(5L);
        IntStream intStream4 = rng.ints(5L, 5, 10);

        LongStream longStream1 = rng.longs();
        LongStream longStream2 = rng.longs(5L, 10L);
        LongStream longStream3 = rng.longs(5L);
        LongStream longStream4 = rng.longs(5L, 5L, 10L);

        DoubleStream doubleStream1 = rng.doubles();
        DoubleStream doubleStream2 = rng.doubles(0.5, 1.0);
        DoubleStream doubleStream3 = rng.doubles(5);
        DoubleStream doubleStream4 = rng.doubles(5, 0.5, 1.0);
    }

    static void checkPredicates(RandomGeneratorFactory factory) {
        RandomGenerator rng = factory.create();
        if (rng instanceof ArbitrarilyJumpableGenerator != factory.isArbitrarilyJumpable()) {
            throw new RuntimeException("isArbitrarilyJumpable failing");
        }
        if (rng instanceof JumpableGenerator != factory.isJumpable()) {
            throw new RuntimeException("isJumpable failing");
        }
        if (rng instanceof LeapableGenerator != factory.isLeapable()) {
            throw new RuntimeException("isLeapable failing");
        }
        if (rng instanceof SplittableGenerator != factory.isSplittable()) {
            throw new RuntimeException("isArbitrarilyJumpable failing");
        }
        if (rng instanceof StreamableGenerator != factory.isStreamable()) {
            throw new RuntimeException("isArbitrarilyJumpable failing");
        }
    }

    static void coverStreamable(StreamableGenerator rng) {
        Stream<RandomGenerator> rngs1 = rng.rngs();
        Stream<RandomGenerator> rngs2 = rng.rngs(5L);
    }

    static void coverSplittable(SplittableGenerator rng) {
        SplittableGenerator s1 = rng.split();
        SplittableGenerator s2 = rng.split(rng);
        Stream<SplittableGenerator> s3 = rng.splits();
        Stream<SplittableGenerator> s4 = rng.splits(5L);
        Stream<SplittableGenerator> s5 = rng.splits(rng);
        Stream<SplittableGenerator> s6 = rng.splits(5L, rng);
    }

    static void coverJumpable(JumpableGenerator rng) {
        JumpableGenerator j1 = rng.copy();
        rng.jump();
        RandomGenerator j2 = rng.copyAndJump();
        double d = rng.jumpDistance();
        Stream<RandomGenerator> j3 = rng.jumps();
        Stream<RandomGenerator> j4 = rng.jumps(5L);
    }

    static void coverLeapable(LeapableGenerator rng) {
        LeapableGenerator l1 = rng.copy();
        rng.leap();
        JumpableGenerator l2 = rng.copyAndLeap();
        double d = rng.leapDistance();
        Stream<JumpableGenerator> l3 = rng.leaps();
        Stream<JumpableGenerator> l4 = rng.leaps(5L);
    }

    static void coverArbitrarilyJumpable(ArbitrarilyJumpableGenerator rng) {
        ArbitrarilyJumpableGenerator a1 = rng.copy();
        rng.jump();
        rng.leap();
        rng.jump(1.2345);
        rng.jumpPowerOfTwo(4);
        RandomGenerator a2 = rng.copyAndJump();
        RandomGenerator a3 = rng.copyAndJump(1.2345);
        Stream<ArbitrarilyJumpableGenerator> a4 = rng.jumps(1.2345);
        Stream<ArbitrarilyJumpableGenerator> a5 = rng.jumps(5L, 1.2345);

    }

    static void coverOf(String name) {
        coverRandomGenerator(RandomGenerator.of(name));
        coverFactory(RandomGeneratorFactory.of(name));
    }

    static void coverFactory(RandomGeneratorFactory factory) {
        String name = factory.name();
        String group = factory.group();
        int stateBits = factory.stateBits();
        int equidistribution = factory.equidistribution();
        BigInteger period = factory.period();
        boolean isStatistical = factory.isStatistical();
        boolean isStochastic = factory.isStochastic();
        boolean isHardware = factory.isHardware();
        boolean isArbitrarilyJumpable = factory.isArbitrarilyJumpable();
        boolean isJumpable = factory.isJumpable();
        boolean isLeapable = factory.isLeapable();
        boolean isSplittable = factory.isSplittable();

        coverRandomGenerator(factory.create());
        coverRandomGenerator(factory.create(12345L));
        coverRandomGenerator(factory.create(new byte[] {1, 2, 3, 4, 5, 6, 7, 8}));
    }

    static void coverDefaults() {
        RandomGeneratorFactory<RandomGenerator> factory =
            RandomGeneratorFactory.getDefault();
        RandomGenerator rng = RandomGenerator.getDefault();
    }

    public static void main(String[] args) throws Throwable {
        RandomGeneratorFactory.all()
                .forEach(factory -> {
                    coverFactory(factory);
                    coverOf(factory.name());
                 });
        RandomGeneratorFactory.all()
                .filter(f -> f.isStreamable())
                .forEach(factory -> {
                    coverStreamable((StreamableGenerator)factory.create());
                });
        RandomGeneratorFactory.all()
                .filter(f -> f.isSplittable())
                .forEach(factory -> {
                    coverSplittable((SplittableGenerator)factory.create());
                });
        RandomGeneratorFactory.all()
                .filter(f -> f.isJumpable())
                .forEach(factory -> {
                    coverJumpable((JumpableGenerator)factory.create());
                });
        RandomGeneratorFactory.all()
                .filter(f -> f.isLeapable())
                .forEach(factory -> {
                    coverLeapable((LeapableGenerator)factory.create());
                });
        RandomGeneratorFactory.all()
                .filter(f -> f.isArbitrarilyJumpable())
                .forEach(factory -> {
                    coverArbitrarilyJumpable((ArbitrarilyJumpableGenerator)factory.create());
                });

        coverRandomGenerator(new SecureRandom());
        coverRandomGenerator(ThreadLocalRandom.current());
    }
}
