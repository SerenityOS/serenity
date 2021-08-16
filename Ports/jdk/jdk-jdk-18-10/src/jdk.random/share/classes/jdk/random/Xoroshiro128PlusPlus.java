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
 * is roughly 2<sup>128</sup>.  Class {@link Xoroshiro128PlusPlus} implements
 * interfaces {@link RandomGenerator} and {@link LeapableGenerator},
 * and therefore supports methods for producing pseudorandomly chosen
 * numbers of type {@code int}, {@code long}, {@code float}, and {@code double}
 * as well as creating new {@link Xoroshiro128PlusPlus} objects
 * by "jumping" or "leaping".
 * <p>
 * The class {@link Xoroshiro128PlusPlus} uses the {@code xoroshiro128} algorithm
 * (parameters 49, 21, 28) with the "++" scrambler that computes
 * {@code Long.rotateLeft(s0 + s1, 17) + s0}.
 * (See David Blackman and Sebastiano Vigna, "Scrambled Linear Pseudorandom
 * Number Generators," ACM Transactions on Mathematical Software, 2021.)
 * Its state consists of two {@code long} fields {@code x0} and {@code x1},
 * which can take on any values provided that they are not both zero.
 * The period of this generator is 2<sup>128</sup>-1.
 * <p>
 * The 64-bit values produced by the {@code nextLong()} method are equidistributed.
 * To be precise, over the course of the cycle of length 2<sup>128</sup>-1,
 * each nonzero {@code long} value is generated 2<sup>64</sup> times,
 * but the value 0 is generated only 2<sup>64</sup>-1 times.
 * The values produced by the {@code nextInt()}, {@code nextFloat()}, and {@code nextDouble()}
 * methods are likewise equidistributed.
 * <p>
 * Instances {@link Xoroshiro128PlusPlus} are <em>not</em> thread-safe.
 * They are designed to be used so that each thread as its own instance.
 * The methods {@link #jump} and {@link #leap} and {@link #jumps} and {@link #leaps}
 * can be used to construct new instances of {@link Xoroshiro128PlusPlus} that traverse
 * other parts of the state cycle.
 * <p>
 * Instances of {@link Xoroshiro128PlusPlus} are not cryptographically
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
        name = "Xoroshiro128PlusPlus",
        group = "Xoroshiro",
        i = 128, j = 1, k = 0,
        equidistribution = 1
)
public final class Xoroshiro128PlusPlus implements LeapableGenerator {

    /*
     * Implementation Overview.
     *
     * This is an implementation of the xoroshiro128++ algorithm version 1.0,
     * written in 2019 by David Blackman and Sebastiano Vigna (vigna@acm.org).
     *
     * The jump operation moves the current generator forward by 2*64
     * steps; this has the same effect as calling nextLong() 2**64
     * times, but is much faster.  Similarly, the leap operation moves
     * the current generator forward by 2*96 steps; this has the same
     * effect as calling nextLong() 2**96 times, but is much faster.
     * The copy method may be used to make a copy of the current
     * generator.  Thus one may repeatedly and cumulatively copy and
     * jump to produce a sequence of generators whose states are well
     * spaced apart along the overall state cycle (indeed, the jumps()
     * and leaps() methods each produce a stream of such generators).
     * The generators can then be parceled out to other threads.
     *
     * File organization: First the non-public methods that constitute the
     * main algorithm, then the public methods.  Note that many methods are
     * defined by classes {@link AbstractJumpableGenerator} and {@link AbstractGenerator}.
     */

    /* ---------------- static fields ---------------- */

    /**
     * Group name.
     */
    private static final String GROUP = "Xoroshiro";

    /**
     * The seed generator for default constructors.
     */
    private static final AtomicLong defaultGen = new AtomicLong(RandomSupport.initialSeed());

    /* ---------------- instance fields ---------------- */

    /**
     * The per-instance state.
     * At least one of the two fields x0 and x1 must be nonzero.
     */
    private long x0, x1;

    /* ---------------- constructors ---------------- */

    /**
     * Basic constructor that initializes all fields from parameters.
     * It then adjusts the field values if necessary to ensure that
     * all constraints on the values of fields are met.
     *
     * @param x0 first word of the initial state
     * @param x1 second word of the initial state
     */
    public Xoroshiro128PlusPlus(long x0, long x1) {
        this.x0 = x0;
        this.x1 = x1;
        // If x0 and x1 are both zero, we must choose nonzero values.
        if ((x0 | x1) == 0) {
            this.x0 = RandomSupport.GOLDEN_RATIO_64;
            this.x1 = RandomSupport.SILVER_RATIO_64;
        }
    }

    /**
     * Creates a new instance of {@link Xoroshiro128PlusPlus} using the
     * specified {@code long} value as the initial seed. Instances of
     * {@link Xoroshiro128PlusPlus} created with the same seed in the same
     * program generate identical sequences of values.
     *
     * @param seed the initial seed
     */
    public Xoroshiro128PlusPlus(long seed) {
        // Using a value with irregularly spaced 1-bits to xor the seed
        // argument tends to improve "pedestrian" seeds such as 0 or
        // other small integers.  We may as well use SILVER_RATIO_64.
        //
        // The x values are then filled in as if by a SplitMix PRNG with
        // GOLDEN_RATIO_64 as the gamma value and Stafford13 as the mixer.
        this(RandomSupport.mixStafford13(seed ^= RandomSupport.SILVER_RATIO_64),
             RandomSupport.mixStafford13(seed + RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Creates a new instance of {@link Xoroshiro128PlusPlus} that is likely to
     * generate sequences of values that are statistically independent
     * of those of any other instances in the current program execution,
     * but may, and typically does, vary across program invocations.
     */
    public Xoroshiro128PlusPlus() {
        // Using GOLDEN_RATIO_64 here gives us a good Weyl sequence of values.
        this(defaultGen.getAndAdd(RandomSupport.GOLDEN_RATIO_64));
    }

    /**
     * Creates a new instance of {@link Xoroshiro128PlusPlus} using the specified array of
     * initial seed bytes. Instances of {@link Xoroshiro128PlusPlus} created with the same
     * seed array in the same program execution generate identical sequences of values.
     *
     * @param seed the initial seed
     */
    public Xoroshiro128PlusPlus(byte[] seed) {
        // Convert the seed to 2 long values, which are not both zero.
        long[] data = RandomSupport.convertSeedBytesToLongs(seed, 2, 2);
        long x0 = data[0], x1 = data[1];
        this.x0 = x0;
        this.x1 = x1;
    }

    /* ---------------- public methods ---------------- */

    public Xoroshiro128PlusPlus copy() {
        return new Xoroshiro128PlusPlus(x0, x1);
    }

    /*
     * The following two comments are quoted from http://prng.di.unimi.it/xoroshiro128plusplus.c
     */

    /*
     * To the extent possible under law, the author has dedicated all copyright
     * and related and neighboring rights to this software to the public domain
     * worldwide. This software is distributed without any warranty.
     * <p>
     * See http://creativecommons.org/publicdomain/zero/1.0/.
     */

    /*
     * This is xoroshiro128++ 1.0, one of our all-purpose, rock-solid,
     * small-state generators. It is extremely (sub-ns) fast and it passes all
     * tests we are aware of, but its state space is large enough only for
     * mild parallelism.
     * <p>
     * For generating just floating-point numbers, xoroshiro128+ is even
     * faster (but it has a very mild bias, see notes in the comments).
     * <p>
     * The state must be seeded so that it is not everywhere zero. If you have
     * a 64-bit seed, we suggest to seed a splitmix64 generator and use its
     * output to fill s.
     */

    @Override
    public long nextLong() {
        final long s0 = x0;
        long s1 = x1;
   // Compute the result based on current state information
   // (this allows the computation to be overlapped with state update).
   final long result = Long.rotateLeft(s0 + s1, 17) + s0;  // "plusplus" scrambler

        s1 ^= s0;
        x0 = Long.rotateLeft(s0, 49) ^ s1 ^ (s1 << 21); // a, b
        x1 = Long.rotateLeft(s1, 28); // c

        return result;
    }

    @Override
    public double jumpDistance() {
        return 0x1.0p64;
    }

    @Override
    public double leapDistance() {
        return 0x1.0p96;
    }

    private static final long[] JUMP_TABLE = { 0x2bd7a6a6e99c2ddcL, 0x0992ccaf6a6fca05L };

    private static final long[] LEAP_TABLE = { 0x360fd5f2cf8d5d99L, 0x9c6e6877736c46e3L };

    @Override
    public void jump() {
        jumpAlgorithm(JUMP_TABLE);
    }

    @Override
    public void leap() {
        jumpAlgorithm(LEAP_TABLE);
    }

    private void jumpAlgorithm(long[] table) {
        long s0 = 0, s1 = 0;
        for (int i = 0; i < table.length; i++) {
            for (int b = 0; b < 64; b++) {
                if ((table[i] & (1L << b)) != 0) {
                    s0 ^= x0;
                    s1 ^= x1;
                }
                nextLong();
            }
        }
       x0 = s0;
       x1 = s1;
    }
}
