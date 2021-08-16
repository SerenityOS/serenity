/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

package jdk.random;

import java.util.concurrent.atomic.AtomicLong;
import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.LeapableGenerator;
import jdk.internal.util.random.RandomSupport;
import jdk.internal.util.random.RandomSupport.RandomGeneratorProperties;

/**
 * A "jumpable and leapable" pseudorandom number generator (PRNG) whose period
 * is roughly 2<sup>256</sup>.  Class {@link Xoshiro256PlusPlus} implements
 * interfaces {@link RandomGenerator} and {@link LeapableGenerator},
 * and therefore supports methods for producing pseudorandomly chosen
 * values of type {@code int}, {@code long}, {@code float}, {@code double},
 * and {@code boolean} (and for producing streams of pseudorandomly chosen
 * numbers of type {@code int}, {@code long}, and {@code double}),
 * as well as methods for creating new {@link Xoshiro256PlusPlus} objects
 * by moving forward either a large distance (2<sup>128</sup>) or a very large
 * distance (2<sup>192</sup>) around the state cycle.
 * <p>
 * Series of generated values pass the TestU01 BigCrush and PractRand test suites
 * that measure independence and uniformity properties of random number generators.
 * (Most recently validated with
 * <a href="http://simul.iro.umontreal.ca/testu01/tu01.html">version 1.2.3 of TestU01</a>
 * and <a href="http://pracrand.sourceforge.net">version 0.90 of PractRand</a>.
 * Note that TestU01 BigCrush was used to test not only values produced by the {@code nextLong()}
 * method but also the result of bit-reversing each value produced by {@code nextLong()}.)
 * These tests validate only the methods for certain
 * types and ranges, but similar properties are expected to hold, at
 * least approximately, for others as well.
 * <p>
 * The class {@link Xoshiro256PlusPlus} uses the {@code xoshiro256} algorithm,
 * version 1.0 (parameters 17, 45), with the "++" scrambler that computes
 * {@code Long.rotateLeft(s0 + s3, 23) + s0}.
 * (See David Blackman and Sebastiano Vigna, "Scrambled Linear Pseudorandom
 * Number Generators," ACM Transactions on Mathematical Software, 2021.)
 * Its state consists of four {@code long} fields {@code x0}, {@code x1}, {@code x2},
 * and {@code x3}, which can take on any values provided that they are not all zero.
 * The period of this generator is 2<sup>256</sup>-1.
 * <p>
 * The 64-bit values produced by the {@code nextLong()} method are equidistributed.
 * To be precise, over the course of the cycle of length 2<sup>256</sup>-1,
 * each nonzero {@code long} value is generated 2<sup>192</sup> times,
 * but the value 0 is generated only 2<sup>192</sup>-1 times.
 * The values produced by the {@code nextInt()}, {@code nextFloat()}, and {@code nextDouble()}
 * methods are likewise equidistributed.
 * Moreover, the 64-bit values produced by the {@code nextLong()} method are 3-equidistributed.
 * <p>
 * Instances {@link Xoshiro256PlusPlus} are <em>not</em> thread-safe.
 * They are designed to be used so that each thread as its own instance.
 * The methods {@link #jump} and {@link #leap} and {@link #jumps} and {@link #leaps}
 * can be used to construct new instances of {@link Xoshiro256PlusPlus} that traverse
 * other parts of the state cycle.
 * <p>
 * Instances of {@link Xoshiro256PlusPlus} are not cryptographically
 * secure.  Consider instead using {@link java.security.SecureRandom}
 * in security-sensitive applications. Additionally,
 * default-constructed instances do not use a cryptographically random
 * seed unless the {@linkplain System#getProperty system property}
 * {@code java.util.secureRandomSeed} is set to {@code true}.
 *
 * @since   17
 *
 */
@RandomGeneratorProperties(
        name = "Xoshiro256PlusPlus",
        group = "Xoshiro",
        i = 256, j = 1, k = 0,
        equidistribution = 3
)
public final class Xoshiro256PlusPlus implements LeapableGenerator {

    /*
     * Implementation Overview.
     *
     * This is an implementation of the xoshiro256++ algorithm version 1.0,
     * written in 2019 by David Blackman and Sebastiano Vigna (vigna@acm.org).
     *
     * The jump operation moves the current generator forward by 2*128
     * steps; this has the same effect as calling nextLong() 2**128
     * times, but is much faster.  Similarly, the leap operation moves
     * the current generator forward by 2*192 steps; this has the same
     * effect as calling nextLong() 2**192 times, but is much faster.
     * The copy method may be used to make a copy of the current
     * generator.  Thus one may repeatedly and cumulatively copy and
     * jump to produce a sequence of generators whose states are well
     * spaced apart along the overall state cycle (indeed, the jumps()
     * and leaps() methods each produce a stream of such generators).
     * The generators can then be parceled out to other threads.
     *
     * File organization: First static fields, then instance
     * fields, then constructors, then instance methods.
     */

    /* ---------------- static fields ---------------- */

    /**
     * The seed generator for default constructors.
     */
    private static final AtomicLong DEFAULT_GEN = new AtomicLong(RandomSupport.initialSeed());

    /* ---------------- instance fields ---------------- */

    /**
     * The per-instance state.
     * At least one of the four fields x0, x1, x2, and x3 must be nonzero.
     */
    private long x0, x1, x2, x3;

    /* ---------------- constructors ---------------- */

    /**
     * Basic constructor that initializes all fields from parameters.
     * It then adjusts the field values if necessary to ensure that
     * all constraints on the values of fields are met.
     *
     * @param x0 first word of the initial state
     * @param x1 second word of the initial state
     * @param x2 third word of the initial state
     * @param x3 fourth word of the initial state
     */
    public Xoshiro256PlusPlus(long x0, long x1, long x2, long x3) {
        this.x0 = x0;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
        // If x0, x1, x2, and x3 are all zero, we must choose nonzero values.
        if ((x0 | x1 | x2 | x3) == 0) {
            // At least three of the four values generated here will be nonzero.
            this.x0 = RandomSupport.mixStafford13(x0 += RandomSupport.GOLDEN_RATIO_64);
            this.x1 = (x0 += RandomSupport.GOLDEN_RATIO_64);
            this.x2 = (x0 += RandomSupport.GOLDEN_RATIO_64);
            this.x3 = (x0 += RandomSupport.GOLDEN_RATIO_64);
        }
    }

    /**
     * Creates a new instance of {@link Xoshiro256PlusPlus} using the
     * specified {@code long} value as the initial seed. Instances of
     * {@link Xoshiro256PlusPlus} created with the same seed in the same
     * program generate identical sequences of values.
     *
     * @param seed the initial seed
     */
    public Xoshiro256PlusPlus(long seed) {
        // Using a value with irregularly spaced 1-bits to xor the seed
        // argument tends to improve "pedestrian" seeds such as 0 or
        // other small integers.  We may as well use SILVER_RATIO_64.
        //
        // The x values are then filled in as if by a SplitMix PRNG with
        // GOLDEN_RATIO_64 as the gamma value and Stafford13 as the mixer.
        this(RandomSupport.mixStafford13(seed ^= RandomSupport.SILVER_RATIO_64),
             RandomSupport.mixStafford13(seed += RandomSupport.GOLDEN_RATIO_64),
             RandomSupport.mixStafford13(seed += RandomSupport.GOLDEN_RATIO_64),
             RandomSupport.mixStafford13(seed + RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Creates a new instance of {@link Xoshiro256PlusPlus} that is likely to
     * generate sequences of values that are statistically independent
     * of those of any other instances in the current program execution,
     * but may, and typically does, vary across program invocations.
     */
    public Xoshiro256PlusPlus() {
        // Using GOLDEN_RATIO_64 here gives us a good Weyl sequence of values.
        this(DEFAULT_GEN.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Creates a new instance of {@link Xoshiro256PlusPlus} using the specified array of
     * initial seed bytes. Instances of {@link Xoshiro256PlusPlus} created with the same
     * seed array in the same program execution generate identical sequences of values.
     *
     * @param seed the initial seed
     */
    public Xoshiro256PlusPlus(byte[] seed) {
        // Convert the seed to 4 long values, which are not all zero.
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 4, 4);
        long x0 = data[0], x1 = data[1], x2 = data[2], x3 = data[3];
        this.x0 = x0;
        this.x1 = x1;
        this.x2 = x2;
        this.x3 = x3;
    }

    /* ---------------- public methods ---------------- */

    public Xoshiro256PlusPlus copy() {
        return new Xoshiro256PlusPlus(x0, x1, x2, x3);
    }

    /*
     * The following two comments are quoted from http://prng.di.unimi.it/xoshiro256plusplus.c
     */

    /*
     * To the extent possible under law, the author has dedicated all copyright
     * and related and neighboring rights to this software to the public domain
     * worldwide. This software is distributed without any warranty.
     * <p>
     * See http://creativecommons.org/publicdomain/zero/1.0/.
     */

    /*
     * This is xoshiro256++ 1.0, one of our all-purpose, rock-solid generators.
     * It has excellent (sub-ns) speed, a state (256 bits) that is large
     * enough for any parallel application, and it passes all tests we are
     * aware of.
     *
     * For generating just floating-point numbers, xoshiro256+ is even faster.
     *
     * The state must be seeded so that it is not everywhere zero. If you have
     * a 64-bit seed, we suggest to seed a splitmix64 generator and use its
     * output to fill s.
     */

    @Override
    public long nextLong() {
        // Compute the result based on current state information
        // (this allows the computation to be overlapped with state update).
        final long result = Long.rotateLeft(x0 + x3, 23) + x0;  // "plusplus" scrambler

        long q0 = x0, q1 = x1, q2 = x2, q3 = x3;
        {   // xoshiro256 1.0
            long t = q1 << 17;
            q2 ^= q0;
            q3 ^= q1;
            q1 ^= q2;
            q0 ^= q3;
            q2 ^= t;
            q3 = Long.rotateLeft(q3, 45);
        }
        x0 = q0; x1 = q1; x2 = q2; x3 = q3;
        return result;
    }

    @Override
    public double jumpDistance() {
        return 0x1.0p128;
    }

    @Override
    public double leapDistance() {
        return 0x1.0p192;
    }

    private static final long[] JUMP_TABLE = {
        0x180ec6d33cfd0abaL, 0xd5a61266f0c9392cL, 0xa9582618e03fc9aaL, 0x39abdc4529b1661cL };

    private static final long[] LEAP_TABLE = {
        0x76e15d3efefdcbbfL, 0xc5004e441c522fb3L, 0x77710069854ee241L, 0x39109bb02acbe635L };

    @Override
    public void jump() {
        jumpAlgorithm(JUMP_TABLE);
    }

    @Override
    public void leap() {
        jumpAlgorithm(LEAP_TABLE);
    }

    private void jumpAlgorithm(long[] table) {
        long s0 = 0, s1 = 0, s2 = 0, s3 = 0;
        for (int i = 0; i < table.length; i++) {
            for (int b = 0; b < 64; b++) {
                if ((table[i] & (1L << b)) != 0) {
                    s0 ^= x0;
                    s1 ^= x1;
                    s2 ^= x2;
                    s3 ^= x3;
                }
                nextLong();
            }
        }
   x0 = s0;
   x1 = s1;
   x2 = s2;
   x3 = s3;
    }

}
