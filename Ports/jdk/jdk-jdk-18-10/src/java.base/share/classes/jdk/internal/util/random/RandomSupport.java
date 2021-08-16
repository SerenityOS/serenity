/*
 * Copyright (c) 2020, 2021, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.util.random;

import java.lang.annotation.*;
import java.math.BigInteger;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.IntConsumer;
import java.util.function.LongConsumer;
import java.util.random.RandomGenerator;
import java.util.random.RandomGenerator.SplittableGenerator;
import java.util.Spliterator;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Low-level utility methods helpful for implementing pseudorandom number
 * generators.
 *
 * <p> This class is mostly for library writers creating specific
 * implementations of the interface {@link RandomGenerator}. As an
 * internal package it is not intended for general use.
 *
 * @since 17
 */
public class RandomSupport {
    /**
     * Annotation providing RandomGenerator properties.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    public @interface RandomGeneratorProperties {
        /**
         * Name of algorithm.
         */
        String name();

        /**
         * Category of algorithm.
         */
        String group() default "Legacy";

        /**
         * Algorithm period defined as:
         *
         * BigInteger.ONE.shiftLeft(i)
         *               .subtract(j)
         *               .shiftLeft(k)
         */
        int i() default 0;
        int j() default 0;
        int k() default 0;

        /**
         * The equidistribution of the algorithm.
         */
        int equidistribution() default Integer.MAX_VALUE;

        /**
         * Is the algorithm based on entropy (true random.)
         */
        boolean isStochastic() default false;

        /**
         * Is the algorithm assisted by hardware (fast true random.)
         */
        boolean isHardware() default false;
    }

    /*
     * Implementation Overview.
     *
     * This class provides utility methods and constants frequently
     * useful in the implementation of pseudorandom number generators
     * that satisfy the interface {@link RandomGenerator}.
     *
     * File organization: First some message strings, then the main
     * public methods, followed by a non-public base spliterator class.
     */

    // IllegalArgumentException messages
    public static final String BAD_SIZE = "size must be non-negative";
    public static final String BAD_DISTANCE = "jump distance must be finite, positive, and an exact integer";
    public static final String BAD_BOUND = "bound must be positive";
    public static final String BAD_FLOATING_BOUND = "bound must be finite and positive";
    public static final String BAD_RANGE = "bound must be greater than origin";

    /* ---------------- explicit constructor ---------------- */

    /**
     * Explicit constructor.
     */
    protected RandomSupport() {
    }

    /* ---------------- public methods ---------------- */

    /**
     * Check a {@code long} proposed stream size for validity.
     *
     * @param streamSize the proposed stream size
     *
     * @throws IllegalArgumentException if {@code streamSize} is negative
     */
    public static void checkStreamSize(long streamSize) {
        if (streamSize < 0L) {
            throw new IllegalArgumentException(BAD_SIZE);
        }
    }

    /**
     * Checks a {@code float} upper bound value for validity.
     *
     * @param bound the upper bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code bound} fails to be positive and finite
     */
    public static void checkBound(float bound) {
        if (!(bound > 0.0 && bound < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_FLOATING_BOUND);
        }
    }

    /**
     * Checks a {@code double} upper bound value for validity.
     *
     * @param bound the upper bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code bound} fails to be positive and finite
     */
    public static void checkBound(double bound) {
        if (!(bound > 0.0 && bound < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_FLOATING_BOUND);
        }
    }

    /**
     * Checks an {@code int} upper bound value for validity.
     *
     * @param bound the upper bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    public static void checkBound(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
    }

    /**
     * Checks a {@code long} upper bound value for validity.
     *
     * @param bound the upper bound (exclusive)
     *
     * @throws IllegalArgumentException if {@code bound} is not positive
     */
    public static void checkBound(long bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException(BAD_BOUND);
        }
    }

    /**
     * Checks a {@code float} range for validity.
     *
     * @param origin the least value (inclusive) in the range
     * @param bound  the upper bound (exclusive) of the range
     *
     * @throws IllegalArgumentException if {@code origin} is not finite, {@code bound} is not finite,
     *                                  or {@code bound - origin} is not finite
     */
    public static void checkRange(float origin, float bound) {
        if (!(origin < bound && (bound - origin) < Float.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    /**
     * Checks a {@code double} range for validity.
     *
     * @param origin the least value (inclusive) in the range
     * @param bound  the upper bound (exclusive) of the range
     *
     * @throws IllegalArgumentException if {@code origin} is not finite, {@code bound} is not finite,
     *                                  or {@code bound - origin} is not finite
     */
    public static void checkRange(double origin, double bound) {
        if (!(origin < bound && (bound - origin) < Double.POSITIVE_INFINITY)) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    /**
     * Checks an {@code int} range for validity.
     *
     * @param origin the least value that can be returned
     * @param bound  the upper bound (exclusive) for the returned value
     *
     * @throws IllegalArgumentException if {@code origin} is greater than or equal to {@code bound}
     */
    public static void checkRange(int origin, int bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    /**
     * Checks a {@code long} range for validity.
     *
     * @param origin the least value that can be returned
     * @param bound  the upper bound (exclusive) for the returned value
     *
     * @throws IllegalArgumentException if {@code origin} is greater than or equal to {@code bound}
     */
    public static void checkRange(long origin, long bound) {
        if (origin >= bound) {
            throw new IllegalArgumentException(BAD_RANGE);
        }
    }

    /**
     * Given an array of seed bytes of any length, construct an array of
     * {@code long} seed values of length {@code n}, such that the last
     * {@code z} values are not all zero.
     *
     * @param seed an array of {@code byte} values
     * @param n the length of the result array (should be nonnegative)
     * @param z the number of trailing result elements that are required
     *        to be not all zero (should be nonnegative but not larger
     *        than {@code n})
     *
     * @return an array of length {@code n} containing {@code long} seed values
     */
    public static long[] convertSeedBytesToLongs(byte[] seed, int n, int z) {
        final long[] result = new long[n];
        final int m = Math.min(seed.length, n << 3);
        // Distribute seed bytes into the words to be formed.
        for (int j = 0; j < m; j++) {
            result[j>>3] = (result[j>>3] << 8) | seed[j];
        }
        // If there aren't enough seed bytes for all the words we need,
        // use a SplitMix-style PRNG to fill in the rest.
        long v = result[0];
        for (int j = (m + 7) >> 3; j < n; j++) {
            result[j] = mixMurmur64(v += SILVER_RATIO_64);
        }
        // Finally, we need to make sure the last z words are not all zero.
        search: {
            for (int j = n - z; j < n; j++) {
                if (result[j] != 0) break search;
            }
            // If they are, fill in using a SplitMix-style PRNG.
            // Using "& ~1L" in the next line defends against the case z==1
            // by guaranteeing that the first generated value will be nonzero.
            long w = result[0] & ~1L;
            for (int j = n - z; j < n; j++) {
                result[j] = mixMurmur64(w += SILVER_RATIO_64);
            }
        }

        return result;
    }

    /**
     * Given an array of seed bytes of any length, construct an array of
     * {@code int} seed values of length {@code n}, such that the last {@code z}
     * values are not all zero.
     *
     * @param seed an array of {@code byte} values
     * @param n the length of the result array (should be nonnegative)
     * @param z the number of trailing result elements that are required
     *        to be not all zero (should be nonnegative but not larger
     *        than {@code n})
     *
     * @return an array of length {@code n} containing {@code int} seed values
     */
    public static int[] convertSeedBytesToInts(byte[] seed, int n, int z) {
        final int[] result = new int[n];
        final int m = Math.min(seed.length, n << 2);
        // Distribute seed bytes into the words to be formed.
        for (int j = 0; j < m; j++) {
            result[j>>2] = (result[j>>2] << 8) | seed[j];
        }
        // If there aren't enough seed bytes for all the words we need,
        // use a SplitMix-style PRNG to fill in the rest.
        int v = result[0];
        for (int j = (m + 3) >> 2; j < n; j++) {
            result[j] = mixMurmur32(v += SILVER_RATIO_32);
        }
        // Finally, we need to make sure the last z words are not all zero.
        search: {
            for (int j = n - z; j < n; j++) {
                if (result[j] != 0) break search;
            }
            // If they are, fill in using a SplitMix-style PRNG.
            // Using "& ~1" in the next line defends against the case z==1
            // by guaranteeing that the first generated value will be nonzero.
            int w = result[0] & ~1;
            for (int j = n - z; j < n; j++) {
                result[j] = mixMurmur32(w += SILVER_RATIO_32);
            }
        }
        return result;
    }

    /*
     * Bounded versions of nextX methods used by streams, as well as
     * the public nextX(origin, bound) methods.  These exist mainly to
     * avoid the need for multiple versions of stream spliterators
     * across the different exported forms of streams.
     */

    /**
     * This is the form of {@link RandomGenerator#nextLong() nextLong}() used by
     * a {@link LongStream} {@link Spliterator} and by the public method
     * {@link RandomGenerator#nextLong(long, long) nextLong}(origin, bound). If
     * {@code origin} is greater than {@code bound}, then this method simply
     * calls the unbounded version of
     * {@link RandomGenerator#nextLong() nextLong}(), choosing pseudorandomly
     * from among all 2<sup>64</sup> possible {@code long} values}, and
     * otherwise uses one or more calls to
     * {@link RandomGenerator#nextLong() nextLong}() to choose a value
     * pseudorandomly from the possible values between {@code origin}
     * (inclusive) and {@code bound} (exclusive).
     *
     * @implNote This method first calls {@code nextLong()} to obtain
     * a {@code long} value that is assumed to be pseudorandomly
     * chosen uniformly and independently from the 2<sup>64</sup>
     * possible {@code long} values (that is, each of the 2<sup>64</sup>
     * possible long values is equally likely to be chosen).
     * Under some circumstances (when the specified range is not
     * a power of 2), {@code nextLong()} may be called additional times
     * to ensure that that the values in the specified range are
     * equally likely to be chosen (provided the assumption holds).
     *
     * The implementation considers four cases:
     * <ol>
     *
     * <li> If the {@code} bound} is less than or equal to the {@code origin}
     * (indicated an unbounded form), the 64-bit {@code long} value obtained
     * from {@link RandomGenerator#nextLong() nextLong}() is returned directly.
     * </li>
     *
     * <li> Otherwise, if the length <i>n</i> of the specified range is an
     * exact power of two 2<sup><i>m</i></sup> for some integer
     *      <i>m</i>, then return the sum of {@code origin} and the
     *      <i>m</i> lowest-order bits of the value from {@code nextLong()}.
     * </li>
     *
     * <li> Otherwise, if the length <i>n</i> of the specified range
     * is less than 2<sup>63</sup>, then the basic idea is to use the remainder
     * modulo <i>n</i> of the value from
     * {@link RandomGenerator#nextLong() nextLong}(), but with this approach
     * some values will be over-represented. Therefore a loop is used to avoid
     * potential bias by rejecting candidates that are too large. Assuming that
     * the results from {@link RandomGenerator#nextLong() nextLong}() are truly
     * chosen uniformly and independently, the expected number of iterations
     * will be somewhere between 1 and 2, depending on the precise value of
     * <i>n</i>.</li>
     *
     * <li> Otherwise, the length <i>n</i> of the specified range
     * cannot be represented as a positive {@code long} value. A loop repeatedly
     * calls {@link RandomGenerator#nextLong() nextLong}() until obtaining a
     * suitable candidate, Again, the expected number of iterations is less than
     * 2.</li>
     *
     * </ol>
     *
     * @param rng a random number generator to be used as a
     *        source of pseudorandom {@code long} values
     * @param origin the least value that can be produced,
     *        unless greater than or equal to {@code bound}
     * @param bound the upper bound (exclusive), unless {@code origin}
     *        is greater than or equal to {@code bound}
     *
     * @return a pseudorandomly chosen {@code long} value,
     *         which will be between {@code origin} (inclusive) and
     *         {@code bound} exclusive unless {@code origin}
     *         is greater than or equal to {@code bound}
     */
    public static long boundedNextLong(RandomGenerator rng, long origin, long bound) {
        long r = rng.nextLong();
        if (origin < bound) {
            // It's not case (1).
            final long n = bound - origin;
            final long m = n - 1;
            if ((n & m) == 0L) {
                // It is case (2): length of range is a power of 2.
                r = (r & m) + origin;
            } else if (n > 0L) {
                // It is case (3): need to reject over-represented candidates.
                /* This loop takes an unlovable form (but it works):
                   because the first candidate is already available,
                   we need a break-in-the-middle construction,
                   which is concisely but cryptically performed
                   within the while-condition of a body-less for loop. */
                for (long u = r >>> 1;            // ensure nonnegative
                     u + m - (r = u % n) < 0L;    // rejection check
                     u = rng.nextLong() >>> 1) // retry
                    ;
                r += origin;
            }
            else {
                // It is case (4): length of range not representable as long.
                while (r < origin || r >= bound)
                    r = rng.nextLong();
            }
        }
        return r;
    }

    /**
     * This is the form of {@link RandomGenerator#nextLong() nextLong}() used by
     * the public method {@link RandomGenerator#nextLong(long) nextLong}(bound).
     * This is essentially a version of
     * {@link RandomSupport#boundedNextLong(RandomGenerator, long, long) boundedNextLong}(rng, origin, bound)
     * that has been specialized for the case where the {@code origin} is zero
     * and the {@code bound} is greater than zero. The value returned is chosen
     * pseudorandomly from nonnegative integer values less than {@code bound}.
     *
     * @implNote This method first calls {@code nextLong()} to obtain
     * a {@code long} value that is assumed to be pseudorandomly
     * chosen uniformly and independently from the 2<sup>64</sup>
     * possible {@code long} values (that is, each of the 2<sup>64</sup>
     * possible long values is equally likely to be chosen).
     * Under some circumstances (when the specified range is not
     * a power of 2), {@code nextLong()} may be called additional times
     * to ensure that that the values in the specified range are
     * equally likely to be chosen (provided the assumption holds).
     *
     * The implementation considers two cases:
     * <ol>
     *
     * <li> If {@code bound} is an exact power of two 2<sup><i>m</i></sup>
     * for some integer <i>m</i>, then return the sum of {@code origin} and the
     * <i>m</i> lowest-order bits of the value from
     * {@link RandomGenerator#nextLong() nextLong}().</li>
     *
     * <li> Otherwise, the basic idea is to use the remainder modulo
     *      <i>bound</i> of the value from {@code nextLong()},
     * but with this approach some values will be over-represented. Therefore a
     * loop is used to avoid potential bias by rejecting candidates that vare
     * too large. Assuming that the results from
     * {@link RandomGenerator#nextLong() nextLong}() are truly chosen uniformly
     * and independently, the expected number of iterations will be somewhere
     * between 1 and 2, depending on the precise value of <i>bound</i>.</li>
     *
     * </ol>
     *
     * @param rng a random number generator to be used as a
     *        source of pseudorandom {@code long} values
     * @param bound the upper bound (exclusive); must be greater than zero
     *
     * @return a pseudorandomly chosen {@code long} value
     */
    public static long boundedNextLong(RandomGenerator rng, long bound) {
        // Specialize boundedNextLong for origin == 0, bound > 0
        final long m = bound - 1;
        long r = rng.nextLong();
        if ((bound & m) == 0L) {
            // The bound is a power of 2.
            r &= m;
        } else {
            // Must reject over-represented candidates
            /* This loop takes an unlovable form (but it works):
               because the first candidate is already available,
               we need a break-in-the-middle construction,
               which is concisely but cryptically performed
               within the while-condition of a body-less for loop. */
            for (long u = r >>> 1;
                 u + m - (r = u % bound) < 0L;
                 u = rng.nextLong() >>> 1)
                ;
        }
        return r;
    }

    /**
     * This is the form of {@link RandomGenerator#nextInt() nextInt}() used by
     * an {@link IntStream} {@link Spliterator} and by the public method
     * {@link RandomGenerator#nextInt(int, int) nextInt}(origin, bound). If
     * {@code origin} is greater than {@code bound}, then this method simply
     * calls the unbounded version of
     * {@link RandomGenerator#nextInt() nextInt}(), choosing pseudorandomly
     * from among all 2<sup>64</sup> possible {@code int} values}, and otherwise
     * uses one or more calls to {@link RandomGenerator#nextInt() nextInt}() to
     * choose a value pseudorandomly from the possible values between
     * {@code origin} (inclusive) and {@code bound} (exclusive).
     *
     * @param rng a random number generator to be used as a
     *        source of pseudorandom {@code int} values
     * @param origin the least value that can be produced,
     *        unless greater than or equal to {@code bound}
     * @param bound the upper bound (exclusive), unless {@code origin}
     *        is greater than or equal to {@code bound}
     *
     * @return a pseudorandomly chosen {@code int} value,
     *         which will be between {@code origin} (inclusive) and
     *         {@code bound} exclusive unless {@code origin}
     *         is greater than or equal to {@code bound}
     *
     * @implNote The implementation of this method is identical to
     *           the implementation of {@code nextLong(origin, bound)}
     *           except that {@code int} values and the {@code nextInt()}
     *           method are used rather than {@code long} values and the
     *           {@code nextLong()} method.
     */
    public static int boundedNextInt(RandomGenerator rng, int origin, int bound) {
        int r = rng.nextInt();
        if (origin < bound) {
            // It's not case (1).
            final int n = bound - origin;
            final int m = n - 1;
            if ((n & m) == 0) {
                // It is case (2): length of range is a power of 2.
                r = (r & m) + origin;
            } else if (n > 0) {
                // It is case (3): need to reject over-represented candidates.
                for (int u = r >>> 1;
                     u + m - (r = u % n) < 0;
                     u = rng.nextInt() >>> 1)
                    ;
                r += origin;
            }
            else {
                // It is case (4): length of range not representable as long.
                while (r < origin || r >= bound) {
                    r = rng.nextInt();
                }
            }
        }
        return r;
    }

    /**
     * This is the form of {@link RandomGenerator#nextInt() nextInt}() used by
     * the public method {@link RandomGenerator#nextInt(int) nextInt}(bound).
     * This is essentially a version of
     * {@link RandomSupport#boundedNextInt(RandomGenerator, int, int) boundedNextInt}(rng, origin, bound)
     * that has been specialized for the case where the {@code origin} is zero
     * and the {@code bound} is greater than zero. The value returned is chosen
     * pseudorandomly from nonnegative integer values less than {@code bound}.
     *
     * @param rng a random number generator to be used as a
     *        source of pseudorandom {@code long} values
     * @param bound the upper bound (exclusive); must be greater than zero
     *
     * @return a pseudorandomly chosen {@code long} value
     *
     * @implNote The implementation of this method is identical to
     *           the implementation of {@code nextLong(bound)}
     *           except that {@code int} values and the {@code nextInt()}
     *           method are used rather than {@code long} values and the
     *           {@code nextLong()} method.
     */
    public static int boundedNextInt(RandomGenerator rng, int bound) {
        // Specialize boundedNextInt for origin == 0, bound > 0
        final int m = bound - 1;
        int r = rng.nextInt();
        if ((bound & m) == 0) {
            // The bound is a power of 2.
            r &= m;
        } else {
            // Must reject over-represented candidates
            for (int u = r >>> 1;
                 u + m - (r = u % bound) < 0;
                 u = rng.nextInt() >>> 1)
                ;
        }
        return r;
    }

    /**
     * This is the form of {@link RandomGenerator#nextDouble() nextDouble}()
     * used by a {@link DoubleStream} {@link Spliterator} and by the public
     * method
     * {@link RandomGenerator#nextDouble(double, double) nextDouble}(origin, bound).
     * If {@code origin} is greater than {@code bound}, then this method simply
     * calls the unbounded version of
     * {@link RandomGenerator#nextDouble() nextDouble}(), and otherwise scales
     * and translates the result of a call to
     * {@link RandomGenerator#nextDouble() nextDouble}() so that it lies between
     * {@code origin} (inclusive) and {@code bound} (exclusive).
     *
     * @implNote The implementation considers two cases:
     * <ol>
     *
     * <li> If the {@code bound} is less than or equal to the {@code origin}
     * (indicated an unbounded form), the 64-bit {@code double} value obtained
     * from {@link RandomGenerator#nextDouble() nextDouble}() is returned
     * directly.
     *
     * <li> Otherwise, the result of a call to {@code nextDouble} is
     * multiplied by {@code (bound - origin)}, then {@code origin} is added, and
     * then if this this result is not less than {@code bound} (which can
     * sometimes occur because of rounding), it is replaced with the largest
     * {@code double} value that is less than {@code bound}.
     *
     * </ol>
     *
     * @param rng a random number generator to be used as a
     *        source of pseudorandom {@code double} values
     * @param origin the least value that can be produced,
     *        unless greater than or equal to {@code bound}; must be finite
     * @param bound the upper bound (exclusive), unless {@code origin}
     *        is greater than or equal to {@code bound}; must be finite
     * @return a pseudorandomly chosen {@code double} value,
     *         which will be between {@code origin} (inclusive) and
     *         {@code bound} exclusive unless {@code origin}
     *         is greater than or equal to {@code bound},
     *         in which case it will be between 0.0 (inclusive)
     *         and 1.0 (exclusive)
     */
    public static double boundedNextDouble(RandomGenerator rng, double origin, double bound) {
        double r = rng.nextDouble();
        if (origin < bound) {
            r = r * (bound - origin) + origin;
            if (r >= bound)  // may need to correct a rounding problem
                r = Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
        }
        return r;
    }

    /**
     * This is the form of {@link RandomGenerator#nextDouble() nextDouble}()
     * used by the public method
     * {@link RandomGenerator#nextDouble(double) nextDouble}(bound). This is
     * essentially a version of
     * {@link RandomSupport#boundedNextDouble(RandomGenerator, double, double) boundedNextDouble}(rng, origin, bound)
     * that has been specialized for the case where the {@code origin} is zero
     * and the {@code bound} is greater than zero.
     *
     * @implNote The result of a call to {@code nextDouble} is
     *      multiplied by {@code bound}, and then if this result is
     *      not less than {@code bound} (which can sometimes occur
     *      because of rounding), it is replaced with the largest
     *      {@code double} value that is less than {@code bound}.
     *
     * @param rng a random number generator to be used as a
     *        source of pseudorandom {@code double} values
     * @param bound the upper bound (exclusive); must be finite and
     *        greater than zero
     * @return a pseudorandomly chosen {@code double} value
     *         between zero (inclusive) and {@code bound} (exclusive)
     */
    public static double boundedNextDouble(RandomGenerator rng, double bound) {
        // Specialize boundedNextDouble for origin == 0, bound > 0
        double r = rng.nextDouble();
        r = r * bound;
        if (r >= bound)  // may need to correct a rounding problem
            r = Double.longBitsToDouble(Double.doubleToLongBits(bound) - 1);
        return r;
    }

    /**
     * This is the form of {@link RandomGenerator#nextFloat() nextFloat}() used
     * by a {@link Stream<Float>} {@link Spliterator} (if there were any) and by
     * the public method
     * {@link RandomGenerator#nextFloat(float, float) nextFloat}(origin, bound).
     * If {@code origin} is greater than {@code bound}, then this method simply
     * calls the unbounded version of
     * {@link RandomGenerator#nextFloat() nextFloat}(), and otherwise scales and
     * translates the result of a call to
     * {@link RandomGenerator#nextFloat() nextFloat}() so that it lies between
     * {@code origin} (inclusive) and {@code bound} (exclusive).
     *
     * @implNote The implementation of this method is identical to
     *     the implementation of {@code nextDouble(origin, bound)}
     *     except that {@code float} values and the {@code nextFloat()}
     *     method are used rather than {@code double} values and the
     *     {@code nextDouble()} method.
     *
     * @param rng a random number generator to be used as a
     *        source of pseudorandom {@code float} values
     * @param origin the least value that can be produced,
     *        unless greater than or equal to {@code bound}; must be finite
     * @param bound the upper bound (exclusive), unless {@code origin}
     *        is greater than or equal to {@code bound}; must be finite
     * @return a pseudorandomly chosen {@code float} value,
     *         which will be between {@code origin} (inclusive) and
     *         {@code bound} exclusive unless {@code origin}
     *         is greater than or equal to {@code bound},
     *         in which case it will be between 0.0 (inclusive)
     *         and 1.0 (exclusive)
     */
    public static float boundedNextFloat(RandomGenerator rng, float origin, float bound) {
        float r = rng.nextFloat();
        if (origin < bound) {
            r = r * (bound - origin) + origin;
            if (r >= bound) // may need to correct a rounding problem
                r = Float.intBitsToFloat(Float.floatToIntBits(bound) - 1);
        }
        return r;
    }

    /**
     * This is the form of {@link RandomGenerator#nextFloat() nextFloat}() used
     * by the public method
     * {@link RandomGenerator#nextFloat(float) nextFloat}(bound). This is
     * essentially a version of
     * {@link RandomSupport#boundedNextFloat(RandomGenerator, float, float) boundedNextFloat}(rng, origin, bound)
     * that has been specialized for the case where the {@code origin} is zero
     * and the {@code bound} is greater than zero.
     *
     * @implNote The implementation of this method is identical to
     *     the implementation of {@code nextDouble(bound)}
     *     except that {@code float} values and the {@code nextFloat()}
     *     method are used rather than {@code double} values and the
     *     {@code nextDouble()} method.
     *
     * @param rng a random number generator to be used as a
     *        source of pseudorandom {@code float} values
     * @param bound the upper bound (exclusive); must be finite and
     *        greater than zero
     * @return a pseudorandomly chosen {@code float} value
     *         between zero (inclusive) and {@code bound} (exclusive)
     */
    public static float boundedNextFloat(RandomGenerator rng, float bound) {
        // Specialize boundedNextFloat for origin == 0, bound > 0
        float r = rng.nextFloat();
        r = r * bound;
        if (r >= bound) // may need to correct a rounding problem
            r = Float.intBitsToFloat(Float.floatToIntBits(bound) - 1);
        return r;
    }

    // The following decides which of two strategies initialSeed() will use.
    private static boolean secureRandomSeedRequested() {
        @SuppressWarnings("removal")
        String pp = java.security.AccessController.doPrivileged(
                new sun.security.action.GetPropertyAction(
                        "java.util.secureRandomSeed"));
        return (pp != null && pp.equalsIgnoreCase("true"));
    }

    private static final boolean useSecureRandomSeed = secureRandomSeedRequested();

    /**
     * Returns a {@code long} value (chosen from some machine-dependent entropy
     * source) that may be useful for initializing a source of seed values for
     * instances of {@link RandomGenerator} created by zero-argument
     * constructors. (This method should
     * <i>not</i> be called repeatedly, once per constructed
     * object; at most it should be called once per class.)
     *
     * @return a {@code long} value, randomly chosen using
     *         appropriate environmental entropy
     */
    public static long initialSeed() {
        if (useSecureRandomSeed) {
            byte[] seedBytes = java.security.SecureRandom.getSeed(8);
            long s = (long)(seedBytes[0]) & 0xffL;
            for (int i = 1; i < 8; ++i)
                s = (s << 8) | ((long)(seedBytes[i]) & 0xffL);
            return s;
        }
        return (mixStafford13(System.currentTimeMillis()) ^
                mixStafford13(System.nanoTime()));
    }

    /**
     * The first 32 bits of the golden ratio (1+sqrt(5))/2, forced to be odd.
     * Useful for producing good Weyl sequences or as an arbitrary nonzero odd
     * value.
     */
    public static final int  GOLDEN_RATIO_32 = 0x9e3779b9;

    /**
     * The first 64 bits of the golden ratio (1+sqrt(5))/2, forced to be odd.
     * Useful for producing good Weyl sequences or as an arbitrary nonzero odd
     * value.
     */
    public static final long GOLDEN_RATIO_64 = 0x9e3779b97f4a7c15L;

    /**
     * The first 32 bits of the silver ratio 1+sqrt(2), forced to be odd. Useful
     * for producing good Weyl sequences or as an arbitrary nonzero odd value.
     */
    public static final int  SILVER_RATIO_32 = 0x6A09E667;

    /**
     * The first 64 bits of the silver ratio 1+sqrt(2), forced to be odd. Useful
     * for producing good Weyl sequences or as an arbitrary nonzero odd value.
     */
    public static final long SILVER_RATIO_64 = 0x6A09E667F3BCC909L;

    /**
     * Computes the 64-bit mixing function for MurmurHash3. This is a 64-bit
     * hashing function with excellent avalanche statistics.
     * https://github.com/aappleby/smhasher/wiki/MurmurHash3
     *
     * <p> Note that if the argument {@code z} is 0, the result is 0.
     *
     * @param z any long value
     *
     * @return the result of hashing z
     */
    public static long mixMurmur64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }

    /**
     * Computes Stafford variant 13 of the 64-bit mixing function for
     * MurmurHash3. This is a 64-bit hashing function with excellent avalanche
     * statistics.
     * http://zimbry.blogspot.com/2011/09/better-bit-mixing-improving-on.html
     *
     * <p> Note that if the argument {@code z} is 0, the result is 0.
     *
     * @param z any long value
     *
     * @return the result of hashing z
     */
    public static long mixStafford13(long z) {
        z = (z ^ (z >>> 30)) * 0xbf58476d1ce4e5b9L;
        z = (z ^ (z >>> 27)) * 0x94d049bb133111ebL;
        return z ^ (z >>> 31);
    }

    /**
     * Computes Doug Lea's 64-bit mixing function. This is a 64-bit hashing
     * function with excellent avalanche statistics. It has the advantages of
     * using the same multiplicative constant twice and of using only 32-bit
     * shifts.
     *
     * <p> Note that if the argument {@code z} is 0, the result is 0.
     *
     * @param z any long value
     *
     * @return the result of hashing z
     */
    public static long mixLea64(long z) {
        z = (z ^ (z >>> 32)) * 0xdaba0b6eb09322e3L;
        z = (z ^ (z >>> 32)) * 0xdaba0b6eb09322e3L;
        return z ^ (z >>> 32);
    }

    /**
     * Computes the 32-bit mixing function for MurmurHash3. This is a 32-bit
     * hashing function with excellent avalanche statistics.
     * https://github.com/aappleby/smhasher/wiki/MurmurHash3
     *
     * <p> Note that if the argument {@code z} is 0, the result is 0.
     *
     * @param z any long value
     *
     * @return the result of hashing z
     */
    public static int mixMurmur32(int z) {
        z = (z ^ (z >>> 16)) * 0x85ebca6b;
        z = (z ^ (z >>> 13)) * 0xc2b2ae35;
        return z ^ (z >>> 16);
    }

    /**
     * Computes Doug Lea's 32-bit mixing function. This is a 32-bit hashing
     * function with excellent avalanche statistics. It has the advantages of
     * using the same multiplicative constant twice and of using only 16-bit
     * shifts.
     *
     * <p> Note that if the argument {@code z} is 0, the result is 0.
     *
     * @param z any long value
     *
     * @return the result of hashing z
     */
    public static int mixLea32(int z) {
        z = (z ^ (z >>> 16)) * 0xd36d884b;
        z = (z ^ (z >>> 16)) * 0xd36d884b;
        return z ^ (z >>> 16);
    }

    // Non-public (package only) support for spliterators needed by AbstractSplittableGenerator
    // and AbstractArbitrarilyJumpableGenerator and AbstractSharedGenerator

    /**
     * Base class for making Spliterator classes for streams of randomly chosen
     * values.
     */
    public abstract static class RandomSpliterator {

        /** low range value */
       public long index;

       /** high range value */
       public final long fence;

       /** Constructor
        *
        * @param index  low range value
        * @param fence  high range value
        */
        public RandomSpliterator(long index, long fence) {
            this.index = index; this.fence = fence;
        }

        /**
         * Returns estimated size.
         *
         * @return estimated size
         */
        public long estimateSize() {
            return fence - index;
        }

        /**
         * Returns characteristics.
         *
         * @return characteristics
         */
        public int characteristics() {
            return (Spliterator.SIZED | Spliterator.SUBSIZED |
                    Spliterator.NONNULL | Spliterator.IMMUTABLE);
        }
    }

    /**
     * Spliterators for int streams. These are based on abstract spliterator
     * classes provided by class AbstractSpliteratorGenerator. Each one needs to
     * define only a constructor and two methods.
     */
    public static class RandomIntsSpliterator extends RandomSupport.RandomSpliterator
            implements Spliterator.OfInt {
        final RandomGenerator generatingGenerator;
        final int origin;
        final int bound;

        /**
         * RandomIntsSpliterator constructor.
         *
         * @param generatingGenerator base AbstractSpliteratorGenerator
         * @param index the (inclusive) lower bound on traversal positions
         * @param fence the (exclusive) upper bound on traversal positions
         * @param origin the (inclusive) lower bound on the pseudorandom values to be generated
         * @param bound the (exclusive) upper bound on the pseudorandom values to be generated
         */
        public RandomIntsSpliterator(RandomGenerator generatingGenerator,
                                     long index, long fence, int origin, int bound) {
            super(index, fence);
            this.generatingGenerator = generatingGenerator;
            this.origin = origin; this.bound = bound;
        }

        public Spliterator.OfInt trySplit() {
            long i = index, m = (i + fence) >>> 1;
            if (m <= i) return null;
            index = m;
            // The same generatingGenerator is used, with no splitting or copying.
            return new RandomIntsSpliterator(generatingGenerator, i, m, origin, bound);
        }

        public boolean tryAdvance(IntConsumer consumer) {
            Objects.requireNonNull(consumer);
            long i = index, f = fence;
            if (i < f) {
                consumer.accept(RandomSupport.boundedNextInt(generatingGenerator, origin, bound));
                index = i + 1;
                return true;
            }
            else return false;
        }

        public void forEachRemaining(IntConsumer consumer) {
            Objects.requireNonNull(consumer);
            long i = index, f = fence;
            if (i < f) {
                index = f;
                RandomGenerator r = generatingGenerator;
                int o = origin, b = bound;
                do {
                    consumer.accept(RandomSupport.boundedNextInt(r, o, b));
                } while (++i < f);
            }
        }
    }

    /**
     * Spliterator for long streams.
     */
    public static class RandomLongsSpliterator extends RandomSupport.RandomSpliterator
            implements Spliterator.OfLong {
        final RandomGenerator generatingGenerator;
        final long origin;
        final long bound;

        /**
         * RandomLongsSpliterator constructor.
         *
         * @param generatingGenerator base AbstractSpliteratorGenerator
         * @param index the (inclusive) lower bound on traversal positions
         * @param fence the (exclusive) upper bound on traversal positions
         * @param origin the (inclusive) lower bound on the pseudorandom values to be generated
         * @param bound the (exclusive) upper bound on the pseudorandom values to be generated
         */
        public RandomLongsSpliterator(RandomGenerator generatingGenerator,
                                      long index, long fence, long origin, long bound) {
            super(index, fence);
            this.generatingGenerator = generatingGenerator;
            this.origin = origin; this.bound = bound;
        }

        public Spliterator.OfLong trySplit() {
            long i = index, m = (i + fence) >>> 1;
            if (m <= i) return null;
            index = m;
            // The same generatingGenerator is used, with no splitting or copying.
            return new RandomLongsSpliterator(generatingGenerator, i, m, origin, bound);
        }

        public boolean tryAdvance(LongConsumer consumer) {
            Objects.requireNonNull(consumer);
            long i = index, f = fence;
            if (i < f) {
                consumer.accept(RandomSupport.boundedNextLong(generatingGenerator, origin, bound));
                index = i + 1;
                return true;
            }
            else return false;
        }

        public void forEachRemaining(LongConsumer consumer) {
            Objects.requireNonNull(consumer);
            long i = index, f = fence;
            if (i < f) {
                index = f;
                RandomGenerator r = generatingGenerator;
                long o = origin, b = bound;
                do {
                    consumer.accept(RandomSupport.boundedNextLong(r, o, b));
                } while (++i < f);
            }
        }
    }

    /**
     * Spliterator for double streams.
     */
    public static class RandomDoublesSpliterator extends RandomSupport.RandomSpliterator
            implements Spliterator.OfDouble {
        final RandomGenerator generatingGenerator;
        final double origin;
        final double bound;

        /**
         * RandomDoublesSpliterator constructor.
         *
         * @param generatingGenerator base AbstractSpliteratorGenerator
         * @param index the (inclusive) lower bound on traversal positions
         * @param fence the (exclusive) upper bound on traversal positions
         * @param origin the (inclusive) lower bound on the pseudorandom values to be generated
         * @param bound the (exclusive) upper bound on the pseudorandom values to be generated
         */
        public RandomDoublesSpliterator(RandomGenerator generatingGenerator,
                                        long index, long fence, double origin, double bound) {
            super(index, fence);
            this.generatingGenerator = generatingGenerator;
            this.origin = origin; this.bound = bound;
        }

        public Spliterator.OfDouble trySplit() {
            long i = index, m = (i + fence) >>> 1;
            if (m <= i) return null;
            index = m;
            // The same generatingGenerator is used, with no splitting or copying.
            return new RandomDoublesSpliterator(generatingGenerator, i, m, origin, bound);
        }

        public boolean tryAdvance(DoubleConsumer consumer) {
            Objects.requireNonNull(consumer);
            long i = index, f = fence;
            if (i < f) {
                consumer.accept(RandomSupport.boundedNextDouble(generatingGenerator, origin, bound));
                index = i + 1;
                return true;
            }
            else return false;
        }

        public void forEachRemaining(DoubleConsumer consumer) {
            Objects.requireNonNull(consumer);
            long i = index, f = fence;
            if (i < f) {
                index = f;
                RandomGenerator r = generatingGenerator;
                double o = origin, b = bound;
                do {
                    consumer.accept(RandomSupport.boundedNextDouble(r, o, b));
                } while (++i < f);
            }
        }
    }

    /**
     * Implementation support for the {@code nextExponential} method of
     * {@link java.util.random.RandomGenerator}.
     *
     * Certain details of the algorithm used in this method may depend critically
     * on the quality of the low-order bits delivered by {@code nextLong()}.  This method
     * should not be used with RNG algorithms (such as a simple Linear Congruential
     * Generator) whose low-order output bits do not have good statistical quality.
     *
     * @implNote The reference implementation uses McFarland's fast modified
     * ziggurat algorithm (largely table-driven, with rare cases handled by
     * computation and rejection sampling). Walker's alias method for sampling
     * a discrete distribution also plays a role.
     *
     * @param rng an instance of {@code RandomGenerator}, used to generate uniformly
     *            pseudorandomly chosen {@code long} values
     *
     * @return a nonnegative {@code double} value chosen pseudorandomly
     *         from an exponential distribution whose mean is 1
     */
    public static double computeNextExponential(RandomGenerator rng) {
        /*
         * The tables themselves, as well as a number of associated parameters, are
         * defined in class DoubleZigguratTables, which is automatically
         * generated by the program create_ziggurat_tables.c (which takes only a
         * few seconds to run).
         *
         * For more information about the algorithm, see these articles:
         *
         * Christopher D. McFarland.  2016 (published online 24 Jun 2015).  A modified ziggurat
         * algorithm for generating exponentially and normally distributed pseudorandom numbers.
         * Journal of Statistical Computation and Simulation 86 (7), pages 1281-1294.
         * https://www.tandfonline.com/doi/abs/10.1080/00949655.2015.1060234
         * Also at https://arxiv.org/abs/1403.6870 (26 March 2014).
         *
         * Alastair J. Walker.  1977.  An efficient method for generating discrete random
         * variables with general distributions. ACM Trans. Math. Software 3, 3
         * (September 1977), 253-256. DOI: https://doi.org/10.1145/355744.355749
         *
         */
        long U1 = rng.nextLong();
        // Experimentation on a variety of machines indicates that it is overall much faster
        // to do the following & and < operations on longs rather than first cast U1 to int
        // (but then we need to cast to int before doing the array indexing operation).
        long i = U1 & DoubleZigguratTables.exponentialLayerMask;
        if (i < DoubleZigguratTables.exponentialNumberOfLayers) {
            // This is the fast path (occurring more than 98% of the time).  Make an early exit.
            return DoubleZigguratTables.exponentialX[(int)i] * (U1 >>> 1);
        }
        // We didn't use the upper part of U1 after all.  We'll be able to use it later.

        for (double extra = 0.0; ; ) {
            // Use Walker's alias method to sample an (unsigned) integer j from a discrete
            // probability distribution that includes the tail and all the ziggurat overhangs;
            // j will be less than DoubleZigguratTables.exponentialNumberOfLayers + 1.
            long UA = rng.nextLong();
            int j = (int)UA & DoubleZigguratTables.exponentialAliasMask;
            if (UA >= DoubleZigguratTables.exponentialAliasThreshold[j]) {
                j = DoubleZigguratTables.exponentialAliasMap[j] &
                        DoubleZigguratTables.exponentialSignCorrectionMask;
            }
            if (j > 0) {   // Sample overhang j
                // For the exponential distribution, every overhang is convex.
                final double[] X = DoubleZigguratTables.exponentialX;
                final double[] Y = DoubleZigguratTables.exponentialY;
                for (;; U1 = (rng.nextLong() >>> 1)) {
                    long U2 = (rng.nextLong() >>> 1);
                    // Compute the actual x-coordinate of the randomly chosen point.
                    double x = (X[j] * 0x1.0p63) + ((X[j-1] - X[j]) * (double)U1);
                    // Does the point lie below the curve?
                    long Udiff = U2 - U1;
                    if (Udiff < 0) {
                        // We picked a point in the upper-right triangle.  None of those can be
                        // accepted.  So remap the point into the lower-left triangle and try that.
                        // In effect, we swap U1 and U2, and invert the sign of Udiff.
                        Udiff = -Udiff;
                        U2 = U1;
                        U1 -= Udiff;
                    }
                    if (Udiff >= DoubleZigguratTables.exponentialConvexMargin) {
                        return x + extra;   // The chosen point is way below the curve; accept it.
                    }
                    // Compute the actual y-coordinate of the randomly chosen point.
                    double y = (Y[j] * 0x1.0p63) + ((Y[j] - Y[j-1]) * (double)U2);
                    // Now see how that y-coordinate compares to the curve
                    if (y <= Math.exp(-x)) {
                        return x + extra;   // The chosen point is below the curve; accept it.
                    }
                    // Otherwise, we reject this sample and have to try again.
                }
            }
            // We are now committed to sampling from the tail.  We could do a recursive call
            // and then add X[0], but we save some time and stack space by using an iterative loop.
            extra += DoubleZigguratTables.exponentialX0;
            // This is like the first five lines of this method, but if it returns, it first adds "extra".
            U1 = rng.nextLong();
            i = U1 & DoubleZigguratTables.exponentialLayerMask;
            if (i < DoubleZigguratTables.exponentialNumberOfLayers) {
                return DoubleZigguratTables.exponentialX[(int)i] * (U1 >>> 1) + extra;
            }
        }
    }

    /**
     * Implementation support for the {@code nextGaussian} methods of
     * {@link java.util.random.RandomGenerator}.
     *
     * Certain details of the algorithm used in this method may depend critically
     * on the quality of the low-order bits delivered by {@code nextLong()}.  This method
     * should not be used with RNG algorithms (such as a simple Linear Congruential
     * Generator) whose low-order output bits do not have good statistical quality.
     *
     * @implNote The reference implementation uses McFarland's fast modified
     * ziggurat algorithm (largely table-driven, with rare cases handled by
     * computation and rejection sampling). Walker's alias method for sampling
     * a discrete distribution also plays a role.
     *
     * @param rng an instance of {@code RandomGenerator}, used to generate uniformly
     *            pseudorandomly chosen {@code long} values
     *
     * @return a nonnegative {@code double} value chosen pseudorandomly
     *         from a Gaussian (normal) distribution whose mean is 0 and whose
     *         standard deviation is 1.
     */
    public static double computeNextGaussian(RandomGenerator rng) {
        /*
         * The tables themselves, as well as a number of associated parameters, are
         * defined in class java.util.DoubleZigguratTables, which is automatically
         * generated by the program create_ziggurat_tables.c (which takes only a
         * few seconds to run).
         *
         * For more information about the algorithm, see these articles:
         *
         * Christopher D. McFarland.  2016 (published online 24 Jun 2015).  A modified ziggurat
         * algorithm for generating exponentially and normally distributed pseudorandom numbers.
         * Journal of Statistical Computation and Simulation 86 (7), pages 1281-1294.
         * https://www.tandfonline.com/doi/abs/10.1080/00949655.2015.1060234
         * Also at https://arxiv.org/abs/1403.6870 (26 March 2014).
         *
         * Alastair J. Walker.  1977.  An efficient method for generating discrete random
         * variables with general distributions. ACM Trans. Math. Software 3, 3
         * (September 1977), 253-256. DOI: https://doi.org/10.1145/355744.355749
         *
         */
        long U1 = rng.nextLong();
        // Experimentation on a variety of machines indicates that it is overall much faster
        // to do the following & and < operations on longs rather than first cast U1 to int
        // (but then we need to cast to int before doing the array indexing operation).
        long i = U1 & DoubleZigguratTables.normalLayerMask;

        if (i < DoubleZigguratTables.normalNumberOfLayers) {
            // This is the fast path (occurring more than 98% of the time).  Make an early exit.
            return DoubleZigguratTables.normalX[(int)i] * U1;   // Note that the sign bit of U1 is used here.
        }
        // We didn't use the upper part of U1 after all.
        // Pull U1 apart into a sign bit and a 63-bit value for later use.
        double signBit = (U1 >= 0) ? 1.0 : -1.0;
        U1 = (U1 << 1) >>> 1;

        // Use Walker's alias method to sample an (unsigned) integer j from a discrete
        // probability distribution that includes the tail and all the ziggurat overhangs;
        // j will be less than DoubleZigguratTables.normalNumberOfLayers + 1.
        long UA = rng.nextLong();
        int j = (int)UA & DoubleZigguratTables.normalAliasMask;
        if (UA >= DoubleZigguratTables.normalAliasThreshold[j]) {
            j = DoubleZigguratTables.normalAliasMap[j] & DoubleZigguratTables.normalSignCorrectionMask;
        }

        double x;
        // Now the goal is to choose the result, which will be multiplied by signBit just before return.

        // There are four kinds of overhangs:
        //
        //  j == 0                          :  Sample from tail
        //  0 < j < normalInflectionIndex   :  Overhang is convex; can reject upper-right triangle
        //  j == normalInflectionIndex      :  Overhang includes the inflection point
        //  j > normalInflectionIndex       :  Overhang is concave; can accept point in lower-left triangle
        //
        // Choose one of four loops to compute x, each specialized for a specific kind of overhang.
        // Conditional statements are arranged such that the more likely outcomes are first.

        // In the three cases other than the tail case:
        // U1 represents a fraction (scaled by 2**63) of the width of rectangle measured from the left.
        // U2 represents a fraction (scaled by 2**63) of the height of rectangle measured from the top.
        // Together they indicate a randomly chosen point within the rectangle.

        final double[] X = DoubleZigguratTables.normalX;
        final double[] Y = DoubleZigguratTables.normalY;
        if (j > DoubleZigguratTables.normalInflectionIndex) {   // Concave overhang
            for (;; U1 = (rng.nextLong() >>> 1)) {
                long U2 = (rng.nextLong() >>> 1);
                // Compute the actual x-coordinate of the randomly chosen point.
                x = (X[j] * 0x1.0p63) + ((X[j-1] - X[j]) * (double)U1);
                // Does the point lie below the curve?
                long Udiff = U2 - U1;
                if (Udiff >= 0) {
                    break;   // The chosen point is in the lower-left triangle; accept it.
                }
                if (Udiff <= -DoubleZigguratTables.normalConcaveMargin) {
                    continue;   // The chosen point is way above the curve; reject it.
                }
                // Compute the actual y-coordinate of the randomly chosen point.
                double y = (Y[j] * 0x1.0p63) + ((Y[j] - Y[j-1]) * (double)U2);
                // Now see how that y-coordinate compares to the curve
                if (y <= Math.exp(-0.5*x*x)) {
                    break;   // The chosen point is below the curve; accept it.
                }
                // Otherwise, we reject this sample and have to try again.
            }
        } else if (j == 0) {   // Tail
            // Tail-sampling method of Marsaglia and Tsang.  See any one of:
            // Marsaglia and Tsang. 1984. A fast, easily implemented method for sampling from decreasing
            //    or symmetric unimodal density functions. SIAM J. Sci. Stat. Comput. 5, 349-359.
            // Marsaglia and Tsang. 1998. The Monty Python method for generating random variables.
            //    ACM Trans. Math. Softw. 24, 3 (September 1998), 341-350.  See page 342, step (4).
            //    http://doi.org/10.1145/292395.292453
            // Thomas, Luk, Leong, and Villasenor. 2007. Gaussian random number generators.
            //    ACM Comput. Surv. 39, 4, Article 11 (November 2007).  See Algorithm 16.
            //    http://doi.org/10.1145/1287620.1287622
            // Compute two separate random exponential samples and then compare them in certain way.
            do {
                x = (1.0 / DoubleZigguratTables.normalX0) * computeNextExponential(rng);
            } while (computeNextExponential(rng) < 0.5*x*x);
            x += DoubleZigguratTables.normalX0;
        } else if (j < DoubleZigguratTables.normalInflectionIndex) {   // Convex overhang
            for (;; U1 = (rng.nextLong() >>> 1)) {
                long U2 = (rng.nextLong() >>> 1);
                // Compute the actual x-coordinate of the randomly chosen point.
                x = (X[j] * 0x1.0p63) + ((X[j-1] - X[j]) * (double)U1);
                // Does the point lie below the curve?
                long Udiff = U2 - U1;
                if (Udiff < 0) {
                    // We picked a point in the upper-right triangle.  None of those can be accepted.
                    // So remap the point into the lower-left triangle and try that.
                    // In effect, we swap U1 and U2, and invert the sign of Udiff.
                    Udiff = -Udiff;
                    U2 = U1;
                    U1 -= Udiff;
                }
                if (Udiff >= DoubleZigguratTables.normalConvexMargin) {
                    break;   // The chosen point is way below the curve; accept it.
                }
                // Compute the actual y-coordinate of the randomly chosen point.
                double y = (Y[j] * 0x1.0p63) + ((Y[j] - Y[j-1]) * (double)U2);
                // Now see how that y-coordinate compares to the curve
                if (y <= Math.exp(-0.5*x*x)) break; // The chosen point is below the curve; accept it.
                // Otherwise, we reject this sample and have to try again.
            }
        } else {
            // The overhang includes the inflection point, so the curve is both convex and concave
            for (;; U1 = (rng.nextLong() >>> 1)) {
                long U2 = (rng.nextLong() >>> 1);
                // Compute the actual x-coordinate of the randomly chosen point.
                x = (X[j] * 0x1.0p63) + ((X[j-1] - X[j]) * (double)U1);
                // Does the point lie below the curve?
                long Udiff = U2 - U1;
                if (Udiff >= DoubleZigguratTables.normalConvexMargin) {
                    break;   // The chosen point is way below the curve; accept it.
                }
                if (Udiff <= -DoubleZigguratTables.normalConcaveMargin) {
                    continue;   // The chosen point is way above the curve; reject it.
                }
                // Compute the actual y-coordinate of the randomly chosen point.
                double y = (Y[j] * 0x1.0p63) + ((Y[j] - Y[j-1]) * (double)U2);
                // Now see how that y-coordinate compares to the curve
                if (y <= Math.exp(-0.5*x*x)) {
                    break;   // The chosen point is below the curve; accept it.
                }
                // Otherwise, we reject this sample and have to try again.
            }
        }
        return signBit*x;
    }

    /**
     * This class overrides the stream-producing methods (such as
     * {@link RandomGenerator#ints() ints}()) in class {@link RandomGenerator}
     * to provide {@link Spliterator}-based implmentations that support
     * potentially parallel execution.
     *
     * <p> To implement a pseudorandom number generator, the programmer needs
     * only to extend this class and provide implementations for the methods
     * {@link RandomGenerator#nextInt() nextInt}(),
     * {@link RandomGenerator#nextLong() nextLong}(),
     *
     * <p> This class is not public; it provides shared code to the public
     * classes {@link AbstractSplittableGenerator}, and
     * {@link AbstractArbitrarilyJumpableGenerator}.
     *
     * @since 17
     */
    public abstract static class AbstractSpliteratorGenerator implements RandomGenerator {
        /*
         * Implementation Overview.
         *
         * This class provides most of the "user API" methods needed to
         * satisfy the interface RandomGenerator.  An implementation of this
         * interface need only extend this class and provide implementations
         * of six methods: nextInt, nextLong, and nextDouble (the versions
         * that take no arguments).
         *
         * File organization: First the non-public abstract methods needed
         * to create spliterators, then the main public methods.
         */

        // stream methods, coded in a way intended to better isolate for
        // maintenance purposes the small differences across forms.

        private static IntStream intStream(Spliterator.OfInt srng) {
            return StreamSupport.intStream(srng, false);
        }

        private static LongStream longStream(Spliterator.OfLong srng) {
            return StreamSupport.longStream(srng, false);
        }

        private static DoubleStream doubleStream(Spliterator.OfDouble srng) {
            return StreamSupport.doubleStream(srng, false);
        }

        /* ---------------- public static methods ---------------- */

       public static IntStream ints(RandomGenerator gen, long streamSize) {
            RandomSupport.checkStreamSize(streamSize);
            return intStream(new RandomIntsSpliterator(gen, 0L, streamSize, Integer.MAX_VALUE, 0));
        }

        public static IntStream ints(RandomGenerator gen) {
            return intStream(new RandomIntsSpliterator(gen, 0L, Long.MAX_VALUE, Integer.MAX_VALUE, 0));
        }

        public static IntStream ints(RandomGenerator gen, long streamSize, int randomNumberOrigin, int randomNumberBound) {
            RandomSupport.checkStreamSize(streamSize);
            RandomSupport.checkRange(randomNumberOrigin, randomNumberBound);
            return intStream(new RandomIntsSpliterator(gen, 0L, streamSize, randomNumberOrigin, randomNumberBound));
        }

        public static IntStream ints(RandomGenerator gen, int randomNumberOrigin, int randomNumberBound) {
            RandomSupport.checkRange(randomNumberOrigin, randomNumberBound);
            return intStream(new RandomIntsSpliterator(gen, 0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound));
        }

        public static LongStream longs(RandomGenerator gen, long streamSize) {
            RandomSupport.checkStreamSize(streamSize);
            return longStream(new RandomLongsSpliterator(gen, 0L, streamSize, Long.MAX_VALUE, 0L));
        }

        public static LongStream longs(RandomGenerator gen) {
            return longStream(new RandomLongsSpliterator(gen, 0L, Long.MAX_VALUE, Long.MAX_VALUE, 0L));
        }

        public static LongStream longs(RandomGenerator gen, long streamSize, long randomNumberOrigin, long randomNumberBound) {
            RandomSupport.checkStreamSize(streamSize);
            RandomSupport.checkRange(randomNumberOrigin, randomNumberBound);
            return longStream(new RandomLongsSpliterator(gen, 0L, streamSize, randomNumberOrigin, randomNumberBound));
        }

        public static LongStream longs(RandomGenerator gen, long randomNumberOrigin, long randomNumberBound) {
            RandomSupport.checkRange(randomNumberOrigin, randomNumberBound);
            return longStream(new RandomLongsSpliterator(gen, 0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound));
        }

        public static DoubleStream doubles(RandomGenerator gen, long streamSize) {
            RandomSupport.checkStreamSize(streamSize);
            return doubleStream(new RandomDoublesSpliterator(gen, 0L, streamSize, Double.MAX_VALUE, 0.0));
        }

        public static DoubleStream doubles(RandomGenerator gen) {
            return doubleStream(new RandomDoublesSpliterator(gen, 0L, Long.MAX_VALUE, Double.MAX_VALUE, 0.0));
        }

        public static DoubleStream doubles(RandomGenerator gen, long streamSize, double randomNumberOrigin, double randomNumberBound) {
            RandomSupport.checkStreamSize(streamSize);
            RandomSupport.checkRange(randomNumberOrigin, randomNumberBound);
            return doubleStream(new RandomDoublesSpliterator(gen, 0L, streamSize, randomNumberOrigin, randomNumberBound));
        }

        public static DoubleStream doubles(RandomGenerator gen, double randomNumberOrigin, double randomNumberBound) {
            RandomSupport.checkRange(randomNumberOrigin, randomNumberBound);
            return doubleStream(new RandomDoublesSpliterator(gen, 0L, Long.MAX_VALUE, randomNumberOrigin, randomNumberBound));
        }

        /* ---------------- public instance methods ---------------- */

        public IntStream ints(long streamSize) {
            return ints(this, streamSize);
        }

        public IntStream ints() {
            return ints(this);
        }

        public IntStream ints(long streamSize, int randomNumberOrigin, int randomNumberBound) {
            return ints(this, streamSize, randomNumberOrigin, randomNumberBound);
        }

        public IntStream ints(int randomNumberOrigin, int randomNumberBound) {
            return ints(this, randomNumberOrigin, randomNumberBound);
        }

        public LongStream longs(long streamSize) {
            return longs(this, streamSize);
        }

        public LongStream longs() {
            return longs(this);
        }

        public LongStream longs(long streamSize, long randomNumberOrigin,long randomNumberBound) {
            return longs(this, streamSize, randomNumberOrigin, randomNumberBound);
        }

        public LongStream longs(long randomNumberOrigin, long randomNumberBound) {
            return longs(this, randomNumberOrigin, randomNumberBound);
        }

        public DoubleStream doubles(long streamSize) {
            return doubles(this, streamSize);
        }

        public DoubleStream doubles() {
            return doubles(this);
        }

        public DoubleStream doubles(long streamSize, double randomNumberOrigin, double randomNumberBound) {
            return doubles(this, streamSize, randomNumberOrigin, randomNumberBound);
        }

        public DoubleStream doubles(double randomNumberOrigin, double randomNumberBound) {
            return doubles(this, randomNumberOrigin, randomNumberBound);
        }

    }

    /**
     * This class provides much of the implementation of the
     * {@link ArbitrarilyJumpableGenerator} interface, to minimize the effort
     * required to implement that interface.
     *
     * <p> To implement a pseudorandom number generator, the programmer needs
     * only to extend this class and provide implementations for the methods
     * {@link RandomGenerator#nextInt() nextInt}(),
     * {@link RandomGenerator#nextLong() nextLong}(),
     * {@link ArbitrarilyJumpableGenerator#copy() copy}(),
     * {@link JumpableGenerator#jumps(long) jumps}(long),
     * {@link ArbitrarilyJumpableGenerator#jumpPowerOfTwo(int) jumpPowerOfTwo}(logDistance),
     * {@link JumpableGenerator#jumpDistance() jumpDistance}(),
     * and
     * {@link LeapableGenerator#leapDistance() leapDistance}().
     *
     * <p> (If the pseudorandom number generator also has the ability to
     * split, then the programmer may wish to consider instead extending
     * {@link AbstractSplittableGenerator}.)
     *
     * <p> The programmer should generally provide at least three constructors:
     * one that takes no arguments, one that accepts a {@code long} seed value,
     * and one that accepts an array of seed {@code byte} values. This class
     * provides a public {@link RandomSupport#initialSeed() initialSeed}()
     * method that may be useful in initializing some static state from which to
     * derive defaults seeds for use by the no-argument constructor.
     *
     * <p> For the stream methods (such as {@link RandomGenerator#ints() ints}()
     * and {@link SplittableGenerator#splits() splits}()), this class provides
     * {@link Spliterator}-based implementations that allow parallel execution
     * when appropriate. In this respect {@link ArbitrarilyJumpableGenerator}
     * differs from {@link JumpableGenerator}, which provides very simple
     * implementations that produce sequential streams only.
     *
     * <p> An implementation of the {@link AbstractArbitrarilyJumpableGenerator}
     * class must provide concrete definitions for the methods
     * {@link RandomGenerator#nextInt() nextInt}(),
     * {@link RandomGenerator#nextLong() nextLong}(),
     * {@link AbstractArbitrarilyJumpableGenerator#jumps(double) jumps}(double),
     * {@link JumpableGenerator#jumpDistance() jumpDistance}(),
     * and
     * {@link LeapableGenerator#leapDistance() leapDistance}().
     * Default implementations are provided for all other methods.
     *
     * <p> The documentation for each non-abstract method in this class
     * describes its implementation in detail. Each of these methods may be
     * overridden if the pseudorandom number generator being implemented
     * admits a more efficient implementation.
     *
     * @since 17
     */
    public abstract static class AbstractArbitrarilyJumpableGenerator
            extends AbstractSpliteratorGenerator implements RandomGenerator.ArbitrarilyJumpableGenerator {

        /*
         * Implementation Overview.
         *
         * This class provides most of the "user API" methods needed to satisfy
         * the interface ArbitrarilyJumpableGenerator.  Most of these methods
         * are in turn inherited from AbstractGenerator and the non-public class
         * AbstractSpliteratorGenerator; this file implements four versions of the
         * jumps method and defines the spliterators necessary to support them.
         *
         * File organization: First the non-public methods needed by the class
         * AbstractSpliteratorGenerator, then the main public methods, followed by some
         * custom spliterator classes needed for stream methods.
         */

        /**
         * Explicit constructor.
         */
        protected AbstractArbitrarilyJumpableGenerator() {
        }

        // Similar methods used by this class

        Spliterator<RandomGenerator> makeJumpsSpliterator(long index, long fence, double distance) {
            return new RandomJumpsSpliterator(this, index, fence, distance);
        }

        Spliterator<JumpableGenerator> makeLeapsSpliterator(long index, long fence, double distance) {
            return new RandomLeapsSpliterator(this, index, fence, distance);
        }

        Spliterator<ArbitrarilyJumpableGenerator> makeArbitraryJumpsSpliterator(long index, long fence, double distance) {
            return new RandomArbitraryJumpsSpliterator(this, index, fence, distance);
        }

        /* ---------------- public methods ---------------- */

        /**
         * Returns a new generator whose internal state is an exact copy of this
         * generator (therefore their future behavior should be identical if
         * subjected to the same series of operations).
         *
         * @return a new object that is a copy of this generator
         */
        public abstract AbstractArbitrarilyJumpableGenerator copy();

        // Stream methods for jumping

        private static <T> Stream<T> stream(Spliterator<T> srng) {
            return StreamSupport.stream(srng, false);
        }

        @Override
        public Stream<RandomGenerator> jumps() {
            return stream(makeJumpsSpliterator(0L, Long.MAX_VALUE, jumpDistance()));
        }

        @Override
        public Stream<RandomGenerator> jumps(long streamSize) {
            RandomSupport.checkStreamSize(streamSize);
            return stream(makeJumpsSpliterator(0L, streamSize, jumpDistance()));
        }

        @Override
        public Stream<ArbitrarilyJumpableGenerator> jumps(double distance) {
            return stream(makeArbitraryJumpsSpliterator(0L, Long.MAX_VALUE, distance));
        }

        @Override
        public Stream<ArbitrarilyJumpableGenerator> jumps(long streamSize, double distance) {
            RandomSupport.checkStreamSize(streamSize);
            return stream(makeArbitraryJumpsSpliterator(0L, streamSize, distance));
        }

        @Override
        public void leap() {
            jump(leapDistance());
        }

        // Stream methods for leaping

        @Override
        public Stream<JumpableGenerator> leaps() {
            return stream(makeLeapsSpliterator(0L, Long.MAX_VALUE, leapDistance()));
        }

        @Override
        public Stream<JumpableGenerator> leaps(long streamSize) {
            return stream(makeLeapsSpliterator(0L, streamSize, leapDistance()));
        }


        /**
         * Spliterator for int streams. We multiplex the four int versions into
         * one class by treating a bound less than origin as unbounded, and also
         * by treating "infinite" as equivalent to
         * {@link Long#MAX_VALUE Long.MAX_VALUE}. For splits, we choose to
         * override the method {@code trySplit()} to try to optimize execution
         * speed: instead of dividing a range in half, it breaks off the largest
         * possible chunk whose size is a power of two such that the remaining
         * chunk is not empty. In this way, the necessary jump distances will
         * tend to be powers of two. The long and double versions of this class
         * are identical except for types.
         */
        static class RandomIntsSpliterator extends RandomSupport.RandomSpliterator implements Spliterator.OfInt {
            final ArbitrarilyJumpableGenerator generatingGenerator;
            final int origin;
            final int bound;

            RandomIntsSpliterator(ArbitrarilyJumpableGenerator generatingGenerator, long index, long fence, int origin, int bound) {
                super(index, fence);
                this.origin = origin; this.bound = bound;
                this.generatingGenerator = generatingGenerator;
            }

            public Spliterator.OfInt trySplit() {
                long i = index, delta = Long.highestOneBit((fence - i) - 1), m = i + delta;
                if (m <= i) return null;
                index = m;
                ArbitrarilyJumpableGenerator r = generatingGenerator;
                return new RandomIntsSpliterator(r.copyAndJump((double)delta), i, m, origin, bound);
            }

            public boolean tryAdvance(IntConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(RandomSupport.boundedNextInt(generatingGenerator, origin, bound));
                    index = i + 1;
                    return true;
                }
                else return false;
            }

            public void forEachRemaining(IntConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    ArbitrarilyJumpableGenerator r = generatingGenerator;
                    int o = origin, b = bound;
                    do {
                        consumer.accept(RandomSupport.boundedNextInt(r, o, b));
                    } while (++i < f);
                }
            }
        }

        /**
         * Spliterator for long streams.
         */
        static class RandomLongsSpliterator extends RandomSupport.RandomSpliterator implements Spliterator.OfLong {
            final ArbitrarilyJumpableGenerator generatingGenerator;
            final long origin;
            final long bound;

            RandomLongsSpliterator(ArbitrarilyJumpableGenerator generatingGenerator, long index, long fence, long origin, long bound) {
                super(index, fence);
                this.generatingGenerator = generatingGenerator;
                this.origin = origin; this.bound = bound;
            }

            public Spliterator.OfLong trySplit() {
                long i = index, delta = Long.highestOneBit((fence - i) - 1), m = i + delta;
                if (m <= i) return null;
                index = m;
                ArbitrarilyJumpableGenerator r = generatingGenerator;
                return new RandomLongsSpliterator(r.copyAndJump((double)delta), i, m, origin, bound);
            }

            public boolean tryAdvance(LongConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(RandomSupport.boundedNextLong(generatingGenerator, origin, bound));
                    index = i + 1;
                    return true;
                }
                else return false;
            }

            public void forEachRemaining(LongConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    ArbitrarilyJumpableGenerator r = generatingGenerator;
                    long o = origin, b = bound;
                    do {
                        consumer.accept(RandomSupport.boundedNextLong(r, o, b));
                    } while (++i < f);
                }
            }
        }

        /**
         * Spliterator for double streams.
         */
        static class RandomDoublesSpliterator extends RandomSupport.RandomSpliterator implements Spliterator.OfDouble {
            final ArbitrarilyJumpableGenerator generatingGenerator;
            final double origin;
            final double bound;

            RandomDoublesSpliterator(ArbitrarilyJumpableGenerator generatingGenerator, long index, long fence, double origin, double bound) {
                super(index, fence);
                this.generatingGenerator = generatingGenerator;
                this.origin = origin; this.bound = bound;
            }

            public Spliterator.OfDouble trySplit() {

                long i = index, delta = Long.highestOneBit((fence - i) - 1), m = i + delta;
                if (m <= i) return null;
                index = m;
                ArbitrarilyJumpableGenerator r = generatingGenerator;
                return new RandomDoublesSpliterator(r.copyAndJump((double)delta), i, m, origin, bound);
            }

            public boolean tryAdvance(DoubleConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(RandomSupport.boundedNextDouble(generatingGenerator, origin, bound));
                    index = i + 1;
                    return true;
                }
                else return false;
            }

            public void forEachRemaining(DoubleConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    ArbitrarilyJumpableGenerator r = generatingGenerator;
                    double o = origin, b = bound;
                    do {
                        consumer.accept(RandomSupport.boundedNextDouble(r, o, b));
                    } while (++i < f);
                }
            }
        }

        // Spliterators for producing new generators by jumping or leaping.  The
        // complete implementation of each of these spliterators is right here.
        // In the same manner as for the preceding spliterators, the method trySplit() is
        // coded to optimize execution speed: instead of dividing a range
        // in half, it breaks off the largest possible chunk whose
        // size is a power of two such that the remaining chunk is not
        // empty.  In this way, the necessary jump distances will tend to be
        // powers of two.

        /**
         * Spliterator for stream of generators of type RandomGenerator produced
         * by jumps.
         */
        static class RandomJumpsSpliterator extends RandomSupport.RandomSpliterator implements Spliterator<RandomGenerator> {
            ArbitrarilyJumpableGenerator generatingGenerator;
            final double distance;

            RandomJumpsSpliterator(ArbitrarilyJumpableGenerator generatingGenerator, long index, long fence, double distance) {
                super(index, fence);
                this.generatingGenerator = generatingGenerator; this.distance = distance;
            }

            public Spliterator<RandomGenerator> trySplit() {
                long i = index, delta = Long.highestOneBit((fence - i) - 1), m = i + delta;
                if (m <= i) return null;
                index = m;
                ArbitrarilyJumpableGenerator r = generatingGenerator;
                // Because delta is a power of two, (distance * (double)delta) can always be computed exactly.
                return new RandomJumpsSpliterator(r.copyAndJump(distance * (double)delta), i, m, distance);
            }

            public boolean tryAdvance(Consumer<? super RandomGenerator> consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(generatingGenerator.copyAndJump(distance));
                    index = i + 1;
                    return true;
                }
                return false;
            }

            public void forEachRemaining(Consumer<? super RandomGenerator> consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    ArbitrarilyJumpableGenerator r = generatingGenerator;
                    do {
                        consumer.accept(r.copyAndJump(distance));
                    } while (++i < f);
                }
            }
        }

        /**
         * Spliterator for stream of generators of type RandomGenerator produced
         * by leaps.
         */
        static class RandomLeapsSpliterator extends RandomSupport.RandomSpliterator implements Spliterator<JumpableGenerator> {
            ArbitrarilyJumpableGenerator generatingGenerator;
            final double distance;

            RandomLeapsSpliterator(ArbitrarilyJumpableGenerator generatingGenerator, long index, long fence, double distance) {
                super(index, fence);
                this.generatingGenerator = generatingGenerator; this.distance = distance;
            }

            public Spliterator<JumpableGenerator> trySplit() {
                long i = index, delta = Long.highestOneBit((fence - i) - 1), m = i + delta;
                if (m <= i) return null;
                index = m;
                // Because delta is a power of two, (distance * (double)delta) can always be computed exactly.
                return new RandomLeapsSpliterator(generatingGenerator.copyAndJump(distance * (double)delta), i, m, distance);
            }

            public boolean tryAdvance(Consumer<? super JumpableGenerator> consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(generatingGenerator.copyAndJump(distance));
                    index = i + 1;
                    return true;
                }
                return false;
            }

            public void forEachRemaining(Consumer<? super JumpableGenerator> consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    ArbitrarilyJumpableGenerator r = generatingGenerator;
                    do {
                        consumer.accept(r.copyAndJump(distance));
                    } while (++i < f);
                }
            }
        }

        /**
         * Spliterator for stream of generators of type RandomGenerator produced
         * by arbitrary jumps.
         */
        static class RandomArbitraryJumpsSpliterator extends RandomSupport.RandomSpliterator implements Spliterator<ArbitrarilyJumpableGenerator> {
            ArbitrarilyJumpableGenerator generatingGenerator;
            final double distance;

            RandomArbitraryJumpsSpliterator(ArbitrarilyJumpableGenerator generatingGenerator, long index, long fence, double distance) {
                super(index, fence);
                this.generatingGenerator = generatingGenerator; this.distance = distance;
            }

            public Spliterator<ArbitrarilyJumpableGenerator> trySplit() {
                long i = index, delta = Long.highestOneBit((fence - i) - 1), m = i + delta;
                if (m <= i) return null;
                index = m;
                // Because delta is a power of two, (distance * (double)delta) can always be computed exactly.
                return new RandomArbitraryJumpsSpliterator(generatingGenerator.copyAndJump(distance * (double)delta), i, m, distance);
            }

            public boolean tryAdvance(Consumer<? super ArbitrarilyJumpableGenerator> consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(generatingGenerator.copyAndJump(distance));
                    index = i + 1;
                    return true;
                }
                return false;
            }

            public void forEachRemaining(Consumer<? super ArbitrarilyJumpableGenerator> consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    ArbitrarilyJumpableGenerator r = generatingGenerator;
                    do {
                        consumer.accept(r.copyAndJump(distance));
                    } while (++i < f);
                }
            }
        }

    }

    /**
     * This class provides much of the implementation of the
     * {@link SplittableGenerator} interface, to minimize the effort required to
     * implement this interface.
     *
     * <p> To implement a pseudorandom number generator, the programmer needs
     * only to extend this class and provide implementations for the methods
     * {@link RandomGenerator#nextInt() nextInt}(),
     * {@link RandomGenerator#nextLong() nextLong}(),
     * {@link SplittableGenerator#split(SplittableGenerator) split}(splittable).
     *
     * <p> (If the pseudorandom number generator also has the ability to jump
     * an arbitrary specified distance, then the programmer may wish to consider
     * instead extending the class {@link AbstractArbitrarilyJumpableGenerator}.
     * See also the class {@link AbstractSplittableWithBrineGenerator}.)
     *
     * <p> The programmer should generally provide at least three constructors:
     * one that takes no arguments, one that accepts a {@code long} seed value,
     * and one that accepts an array of seed {@code byte} values. This class
     * provides a public {@link RandomSupport#initialSeed() initialSeed}()
     * method that may be useful in initializing some static state from which to
     * derive defaults seeds for use by the no-argument constructor.
     *
     * <p> For the stream methods (such as {@link RandomGenerator#ints() ints}()
     * and {@link SplittableGenerator#splits() splits}()), this class provides
     * {@link Spliterator}-based implementations that allow parallel execution
     * when appropriate.
     *
     * <p> The documentation for each non-abstract method in this class
     * describes its implementation in detail. Each of these methods may be
     * overridden if the pseudorandom number generator being implemented
     * admits a more efficient implementation.
     *
     * @since 17
     */
    public abstract static class AbstractSplittableGenerator extends AbstractSpliteratorGenerator implements SplittableGenerator {

        /*
         * Implementation Overview.
         *
         * This class provides most of the "user API" methods needed to
         * satisfy the interface SplittableGenerator.  Most of these methods
         * are in turn inherited from AbstractGenerator and the non-public class
         * AbstractSpliteratorGenerator; this class provides two versions of the
         * splits method and defines the spliterators necessary to support
         * them.
         *
         * File organization: First the non-public methods needed by the class
         * AbstractSpliteratorGenerator, then the main public methods, followed by some
         * custom spliterator classes.
         */

        /**
         * Explicit constructor.
         */
        protected AbstractSplittableGenerator() {
        }

        Spliterator<SplittableGenerator> makeSplitsSpliterator(long index, long fence, SplittableGenerator source) {
            return new RandomSplitsSpliterator(source, index, fence, this);
        }

        /* ---------------- public methods ---------------- */

        /**
         * Implements the @code{split()} method as
         * {@link SplittableGenerator#split(SplittableGenerator) split}(this).
         *
         * @return the new {@link SplittableGenerator} instance
         */
        public SplittableGenerator split() {
            return this.split(this);
        }

        // Stream methods for splittings

        @Override
        public Stream<SplittableGenerator> splits() {
            return this.splits(Long.MAX_VALUE, this);
        }

        @Override
        public Stream<SplittableGenerator> splits(long streamSize) {
            return this.splits(streamSize, this);
        }

        @Override
        public Stream<SplittableGenerator> splits(SplittableGenerator source) {
            return this.splits(Long.MAX_VALUE, source);
        }

        @Override
        public Stream<SplittableGenerator> splits(long streamSize, SplittableGenerator source) {
            RandomSupport.checkStreamSize(streamSize);
            Objects.requireNonNull(source, "source should be non-null");

            return StreamSupport.stream(makeSplitsSpliterator(0L, streamSize, source), false);
        }

        /**
         * Spliterator for int streams. We multiplex the four int versions into
         * one class by treating a bound less than origin as unbounded, and also
         * by treating "infinite" as equivalent to
         * {@link Long#MAX_VALUE Long.MAX_VALUE}. For splits, it uses the
         * standard divide-by-two approach. The long and double versions of this
         * class are identical except for types.
         */
        static class RandomIntsSpliterator extends RandomSupport.RandomSpliterator implements Spliterator.OfInt {
            final SplittableGenerator generatingGenerator;
            final int origin;
            final int bound;

            RandomIntsSpliterator(SplittableGenerator generatingGenerator, long index, long fence, int origin, int bound) {
                super(index, fence);
                this.generatingGenerator = generatingGenerator;
                this.origin = origin; this.bound = bound;
            }

            public Spliterator.OfInt trySplit() {
                long i = index, m = (i + fence) >>> 1;
                if (m <= i) return null;
                index = m;
                return new RandomIntsSpliterator(generatingGenerator.split(), i, m, origin, bound);
            }

            public boolean tryAdvance(IntConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(RandomSupport.boundedNextInt(generatingGenerator, origin, bound));
                    index = i + 1;
                    return true;
                }
                else return false;
            }

            public void forEachRemaining(IntConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    RandomGenerator r = generatingGenerator;
                    int o = origin, b = bound;
                    do {
                        consumer.accept(RandomSupport.boundedNextInt(r, o, b));
                    } while (++i < f);
                }
            }
        }

        /**
         * Spliterator for long streams.
         */
        static class RandomLongsSpliterator extends RandomSupport.RandomSpliterator implements Spliterator.OfLong {
            final SplittableGenerator generatingGenerator;
            final long origin;
            final long bound;

            RandomLongsSpliterator(SplittableGenerator generatingGenerator, long index, long fence, long origin, long bound) {
                super(index, fence);
                this.generatingGenerator = generatingGenerator;
                this.origin = origin; this.bound = bound;
            }

            public Spliterator.OfLong trySplit() {
                long i = index, m = (i + fence) >>> 1;
                if (m <= i) return null;
                index = m;
                return new RandomLongsSpliterator(generatingGenerator.split(), i, m, origin, bound);
            }

            public boolean tryAdvance(LongConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(RandomSupport.boundedNextLong(generatingGenerator, origin, bound));
                    index = i + 1;
                    return true;
                }
                else return false;
            }

            public void forEachRemaining(LongConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    RandomGenerator r = generatingGenerator;
                    long o = origin, b = bound;
                    do {
                        consumer.accept(RandomSupport.boundedNextLong(r, o, b));
                    } while (++i < f);
                }
            }
        }

        /**
         * Spliterator for double streams.
         */
        static class RandomDoublesSpliterator extends RandomSupport.RandomSpliterator implements Spliterator.OfDouble {
            final SplittableGenerator generatingGenerator;
            final double origin;
            final double bound;

            RandomDoublesSpliterator(SplittableGenerator generatingGenerator, long index, long fence, double origin, double bound) {
                super(index, fence);
                this.generatingGenerator = generatingGenerator;
                this.origin = origin; this.bound = bound;
            }

            public Spliterator.OfDouble trySplit() {
                long i = index, m = (i + fence) >>> 1;
                if (m <= i) return null;
                index = m;
                return new RandomDoublesSpliterator(generatingGenerator.split(), i, m, origin, bound);
            }

            public boolean tryAdvance(DoubleConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(RandomSupport.boundedNextDouble(generatingGenerator, origin, bound));
                    index = i + 1;
                    return true;
                }
                else return false;
            }

            public void forEachRemaining(DoubleConsumer consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    RandomGenerator r = generatingGenerator;
                    double o = origin, b = bound;
                    do {
                        consumer.accept(RandomSupport.boundedNextDouble(r, o, b));
                    } while (++i < f);
                }
            }
        }

        /**
         * Spliterator for stream of generators of type SplittableGenerator. We
         * multiplex the two versions into one class by treating "infinite" as
         * equivalent to Long.MAX_VALUE. For splits, it uses the standard
         * divide-by-two approach.
         */
        static class RandomSplitsSpliterator extends RandomSpliterator implements Spliterator<SplittableGenerator> {
            final SplittableGenerator generatingGenerator;
            final SplittableGenerator constructingGenerator;

            RandomSplitsSpliterator(SplittableGenerator generatingGenerator,
                                    long index, long fence,
                                    SplittableGenerator constructingGenerator) {
                super(index, fence);
                this.generatingGenerator = generatingGenerator;
                this.constructingGenerator = constructingGenerator;
            }

            public Spliterator<SplittableGenerator> trySplit() {
                long i = index, m = (i + fence) >>> 1;
                if (m <= i) return null;
                index = m;
                return new RandomSplitsSpliterator(generatingGenerator.split(), i, m, constructingGenerator);
            }

            public boolean tryAdvance(Consumer<? super SplittableGenerator> consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(constructingGenerator.split(generatingGenerator));
                    index = i + 1;
                    return true;
                }
                else return false;
            }

            public void forEachRemaining(Consumer<? super SplittableGenerator> consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    SplittableGenerator c = constructingGenerator;
                    SplittableGenerator r = generatingGenerator;
                    do {
                        consumer.accept(c.split(r));
                    } while (++i < f);
                }
            }
        }

    }

    /**
     * This class provides much of the implementation of the
     * {@link SplittableGenerator} interface, to minimize the effort required to
     * implement this interface. It is similar to the class
     * {@link AbstractSplittableGenerator} but makes use of the brine technique
     * for ensuring that distinct generators created by a single call to a
     * {@link SplittableGenerator#splits() splits}() method have distinct state
     * cycles.
     *
     * <p> To implement a pseudorandom number generator, the programmer needs
     * only to extend this class and provide implementations for the methods
     * {@link RandomGenerator#nextInt() nextInt}(),
     * {@link RandomGenerator#nextLong() nextLong}(),
     * {@link RandomSplitsSpliteratorWithSalt#split(SplittableGenerator, long) split}(splittable, brine).
     *
     * <p> The programmer should generally provide at least three constructors:
     * one that takes no arguments, one that accepts a {@code long} seed value,
     * and one that accepts an array of seed {@code byte} values. This class
     * provides a public {@link RandomSupport#initialSeed() initialSeed}()
     * method that may be useful in initializing some static state from which to
     * derive defaults seeds for use by the no-argument constructor.
     *
     * <p> For the stream methods (such as {@link RandomGenerator#ints() ints}()
     * and {@link SplittableGenerator#splits() splits}()), this class provides
     * {@link Spliterator}-based implementations that allow parallel execution
     * when appropriate.
     *
     * <p> The documentation for each non-abstract method in this class
     * describes its implementation in detail. Each of these methods may be
     * overridden if the pseudorandom number generator being implemented
     * admits a more efficient implementation.
     *
     * @since 17
     */
    public abstract static class AbstractSplittableWithBrineGenerator
            extends AbstractSplittableGenerator {

        /*
         * Implementation Overview.
         *
         * This class provides most of the "user API" methods needed to
         * satisfy the interface SplittableGenerator.  Most of these methods
         * are in turn inherited from AbstractSplittableGenerator and the non-public class
         * AbstractSpliteratorGenerator; this class provides four versions of the
         * splits method and defines the spliterators necessary to support
         * them.
         *
         * File organization: First the non-public methods needed by the class
         * AbstractSplittableWithBrineGenerator, then the main public methods,
         * followed by some custom spliterator classes needed for stream methods.
         */

        /**
         * Explicit constructor.
         */
        protected AbstractSplittableWithBrineGenerator() {
        }

        // The salt consists groups of bits each SALT_SHIFT in size, starting from
        // the left-hand (high-order) end of the word.  We can regard them as
        // digits base (1 << SALT_SHIFT).  If SALT_SHIFT does not divide 64
        // evenly, then any leftover bits at the low end of the word are zero.
        // The lowest digit of the salt is set to the largest possible digit
        // (all 1-bits, or ((1 << SALT_SHIFT) - 1)); all other digits are set
        // to a randomly chosen value less than that largest possible digit.
        // The salt may be shifted left by SALT_SHIFT any number of times.
        // If any salt remains in the word, its right-hand end can be identified
        // by searching from left to right for an occurrence of a digit that is
        // all 1-bits (not that we ever do that; this is simply a proof that one
        // can identify the boundary between the salt and the index if any salt
        // remains in the word).  The idea is that before computing the bitwise OR
        // of an index and the salt, one can first check to see whether the
        // bitwise AND is nonzero; if so, one can shift the salt left by
        // SALT_SHIFT and try again.  In this way, when the bitwise OR is
        // computed, if the salt is nonzero then its rightmost 1-bit is to the
        // left of the leftmost 1-bit of the index.

        // We need 2 <= SALT_SHIFT <= 63 (3 through 8 are good values; 4 is probably best)
        static final int SALT_SHIFT = 4;

        // Methods required by class AbstractSpliteratorGenerator (override)
        Spliterator<SplittableGenerator> makeSplitsSpliterator(long index, long fence, SplittableGenerator source) {
            // This little algorithm to generate a new salt value is carefully
            // designed to work even if SALT_SHIFT does not evenly divide 64
            // (the number of bits in a long value).
            long bits = nextLong();
            long multiplier = (1L << SALT_SHIFT) - 1;
            long salt = multiplier << (64 - SALT_SHIFT);
            while ((salt & multiplier) != 0) {
                long digit = Math.multiplyHigh(bits, multiplier);
                salt = (salt >>> SALT_SHIFT) | (digit << (64 - SALT_SHIFT));
                bits *= multiplier;
            }
            // This is the point at which newly generated salt gets injected into
            // the root of a newly created brine-generating splits-spliterator.
            return new RandomSplitsSpliteratorWithSalt(source, index, fence, this, salt);
        }

        /* ---------------- public methods ---------------- */

        // Stream methods for splitting

        /**
         * Constructs and returns a new instance of
         * {@link AbstractSplittableWithBrineGenerator} that shares no mutable
         * state with this instance. However, with very high probability, the
         * set of values collectively generated by the two objects should have
         * the same statistical properties as if the same quantity of values
         * were generated by a single thread using a single may be
         * {@link AbstractSplittableWithBrineGenerator} object. Either or both
         * of the two objects further split using the
         * {@link SplittableGenerator#split() split}() method, and the same
         * expected statistical properties apply to the entire set of generators
         * constructed by such recursive splitting.
         *
         * @param brine a long value, of which the low 63 bits provide a unique id
         * among calls to this method for constructing a single series of Generator objects.
         *
         * @return the new {@code AbstractSplittableWithBrineGenerator} instance
         */
        public SplittableGenerator split(long brine) {
            return this.split(this, brine);
        }

        @Override
        public SplittableGenerator split(SplittableGenerator source) {
            // It's a one-off: supply randomly chosen brine
            return this.split(source, source.nextLong());
        }

        public abstract SplittableGenerator split(SplittableGenerator source, long brine);


        /* ---------------- spliterator ---------------- */
        /**
         * Alternate spliterator for stream of generators of type
         * SplittableGenerator. We multiplex the two versions into one class by
         * treating "infinite" as equivalent to Long.MAX_VALUE. For splits, it
         * uses the standard divide-by-two approach.
         *
         * <p> This differs from
         * {@link AbstractSplittableGenerator.RandomSplitsSpliterator} in that it
         * provides a brine argument (a mixture of salt and an index) when
         * calling the {@link SplittableGenerator#split() split}() method.
         */
        static class RandomSplitsSpliteratorWithSalt
                extends RandomSpliterator implements Spliterator<SplittableGenerator> {

            final SplittableGenerator generatingGenerator;
            final AbstractSplittableWithBrineGenerator constructingGenerator;
            long salt;

            // Important invariant: 0 <= index <= fence

            // Important invariant: if salt and index are both nonzero,
            // the rightmost 1-bit of salt is to the left of the leftmost 1-bit of index.
            // If necessary, the salt can be leftshifted by SALT_SHIFT as many times as
            // necessary to maintain the invariant.

            RandomSplitsSpliteratorWithSalt(SplittableGenerator generatingGenerator, long index, long fence,
                                            AbstractSplittableWithBrineGenerator constructingGenerator, long salt) {
                super(index, fence);
                this.generatingGenerator = generatingGenerator;
                this.constructingGenerator = constructingGenerator;
                while ((salt != 0) && (Long.compareUnsigned(salt & -salt, index) <= 0)) {
                    salt = salt << SALT_SHIFT;
                }
                this.salt = salt;
            }

            public Spliterator<SplittableGenerator> trySplit() {
                long i = index, m = (i + fence) >>> 1;
                if (m <= i) return null;
                RandomSplitsSpliteratorWithSalt result =
                        new RandomSplitsSpliteratorWithSalt(generatingGenerator.split(), i, m, constructingGenerator, salt);
                index = m;
                while ((salt != 0) && (Long.compareUnsigned(salt & -salt, index) <= 0)) {
                    salt = salt << SALT_SHIFT;
                }
                return result;
            }

            public boolean tryAdvance(Consumer<? super SplittableGenerator> consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    consumer.accept(constructingGenerator.split(generatingGenerator, salt | i));
                    ++i;
                    index = i;
                    if ((i & salt) != 0) salt <<= SALT_SHIFT;
                    return true;
                }
                return false;
            }

            public void forEachRemaining(Consumer<? super SplittableGenerator> consumer) {
                Objects.requireNonNull(consumer);
                long i = index, f = fence;
                if (i < f) {
                    index = f;
                    AbstractSplittableWithBrineGenerator c = constructingGenerator;
                    SplittableGenerator r = generatingGenerator;
                    do {
                        consumer.accept(c.split(r, salt | i));
                        ++i;
                        if ((i & salt) != 0) salt <<= SALT_SHIFT;
                    } while (i < f);
                }
            }
        }
    }

    /**
     * Implementation support for modified-ziggurat implementation of
     * nextExponential()
     *
     * <p> This Java class was generated automatically by a program
     * `create_ziggurat_tables.c`.
     *
     * <p> Fraction of the area under the curve that lies outside the layer
     * boxes: 0.0156 Fraction of non-box area that lies in the tail of the
     * distribution: 0.0330
     */
    static final class DoubleZigguratTables {

        static final int exponentialNumberOfLayers = 252;
        static final int exponentialLayerMask = 0xff;
        static final int exponentialAliasMask = 0xff;
        static final int exponentialSignCorrectionMask = 0xff;
        static final double exponentialX0 = 7.56927469414806264;
        static final long exponentialConvexMargin = 853965788476313645L;   // unscaled convex margin = 0.0926

        // exponential_X[i] = length of ziggurat layer i for exponential distribution, scaled by 2**(-63)
        static final double[] exponentialX = {      // 253 entries, which is exponential_number_of_layers+1
                8.2066240675348816e-19,  7.3973732351607284e-19,  6.9133313377915293e-19,  6.5647358820964533e-19,
                6.2912539959818508e-19,  6.0657224129604964e-19,  5.8735276103737269e-19,  5.7058850528536941e-19,
                5.5570945691622390e-19,  5.4232438903743953e-19,  5.3015297696508776e-19,  5.1898739257708062e-19,
                5.0866922617998330e-19,  4.9907492938796469e-19,  4.9010625894449536e-19,  4.8168379010649187e-19,
                4.7374238653644714e-19,  4.6622795807196824e-19,  4.5909509017784048e-19,  4.5230527790658154e-19,
                4.4582558816353960e-19,  4.3962763126368381e-19,  4.3368675967106470e-19,  4.2798143618469714e-19,
                4.2249273027064889e-19,  4.1720391253464110e-19,  4.1210012522465616e-19,  4.0716811225869233e-19,
                4.0239599631006903e-19,  3.9777309342877357e-19,  3.9328975785334499e-19,  3.8893725129310323e-19,
                3.8470763218720385e-19,  3.8059366138180143e-19,  3.7658872138544730e-19,  3.7268674692030177e-19,
                3.6888216492248162e-19,  3.6516984248800068e-19,  3.6154504153287473e-19,  3.5800337915318032e-19,
                3.5454079284533432e-19,  3.5115350988784242e-19,  3.4783802030030962e-19,  3.4459105288907336e-19,
                3.4140955396563316e-19,  3.3829066838741162e-19,  3.3523172262289001e-19,  3.3223020958685874e-19,
                3.2928377502804472e-19,  3.2639020528202049e-19,  3.2354741622810815e-19,  3.2075344331080789e-19,
                3.1800643250478609e-19,  3.1530463211820845e-19,  3.1264638534265134e-19,  3.1003012346934211e-19,
                3.0745435970137301e-19,  3.0491768350005559e-19,  3.0241875541094565e-19,  2.9995630232144550e-19,
                2.9752911310742592e-19,  2.9513603463113224e-19,  2.9277596805684267e-19,  2.9044786545442563e-19,
                2.8815072666416712e-19,  2.8588359639906928e-19,  2.8364556156331615e-19,  2.8143574876779799e-19,
                2.7925332202553125e-19,  2.7709748061152879e-19,  2.7496745707320232e-19,  2.7286251537873397e-19,
                2.7078194919206054e-19,  2.6872508026419050e-19,  2.6669125693153442e-19,  2.6467985271278891e-19,
                2.6269026499668434e-19,  2.6072191381359757e-19,  2.5877424068465143e-19,  2.5684670754248168e-19,
                2.5493879571835479e-19,  2.5305000499077481e-19,  2.5117985269112710e-19,  2.4932787286227806e-19,
                2.4749361546638660e-19,  2.4567664563848669e-19,  2.4387654298267842e-19,  2.4209290090801527e-19,
                2.4032532600140538e-19,  2.3857343743505147e-19,  2.3683686640614648e-19,  2.3511525560671253e-19,
                2.3340825872163284e-19,  2.3171553995306794e-19,  2.3003677356958333e-19,  2.2837164347843482e-19,
                2.2671984281957174e-19,  2.2508107358001938e-19,  2.2345504622739592e-19,  2.2184147936140775e-19,
                2.2024009938224424e-19,  2.1865064017486842e-19,  2.1707284280826716e-19,  2.1550645524878675e-19,
                2.1395123208673778e-19,  2.1240693427550640e-19,  2.1087332888245875e-19,  2.0935018885097035e-19,
                2.0783729277295508e-19,  2.0633442467130712e-19,  2.0484137379170616e-19,  2.0335793440326865e-19,
                2.0188390560756090e-19,  2.0041909115551697e-19,  1.9896329927183254e-19,  1.9751634248643090e-19,
                1.9607803747261946e-19,  1.9464820489157862e-19,  1.9322666924284314e-19,  1.9181325872045647e-19,
                1.9040780507449479e-19,  1.8901014347767504e-19,  1.8762011239677479e-19,  1.8623755346860768e-19,
                1.8486231138030984e-19,  1.8349423375370566e-19,  1.8213317103353295e-19,  1.8077897637931708e-19,
                1.7943150556069476e-19,  1.7809061685599652e-19,  1.7675617095390567e-19,  1.7542803085801941e-19,
                1.7410606179414531e-19,  1.7279013112017240e-19,  1.7148010823836362e-19,  1.7017586450992059e-19,
                1.6887727317167824e-19,  1.6758420925479093e-19,  1.6629654950527621e-19,  1.6501417230628659e-19,
                1.6373695760198277e-19,  1.6246478682288560e-19,  1.6119754281258616e-19,  1.5993510975569615e-19,
                1.5867737310692309e-19,  1.5742421952115544e-19,  1.5617553678444595e-19,  1.5493121374578016e-19,
                1.5369114024951992e-19,  1.5245520706841019e-19,  1.5122330583703858e-19,  1.4999532898563561e-19,
                1.4877116967410352e-19,  1.4755072172615974e-19,  1.4633387956347966e-19,  1.4512053813972103e-19,
                1.4391059287430991e-19,  1.4270393958586506e-19,  1.4150047442513381e-19,  1.4030009380730888e-19,
                1.3910269434359025e-19,  1.3790817277185197e-19,  1.3671642588626657e-19,  1.3552735046573446e-19,
                1.3434084320095729e-19,  1.3315680061998685e-19,  1.3197511901207148e-19,  1.3079569434961214e-19,
                1.2961842220802957e-19,  1.2844319768333099e-19,  1.2726991530715219e-19,  1.2609846895903523e-19,
                1.2492875177568625e-19,  1.2376065605693940e-19,  1.2259407316813331e-19,  1.2142889343858445e-19,
                1.2026500605581765e-19,  1.1910229895518744e-19,  1.1794065870449425e-19,  1.1677997038316715e-19,
                1.1562011745554883e-19,  1.1446098163777869e-19,  1.1330244275772562e-19,  1.1214437860737343e-19,
                1.1098666478700728e-19,  1.0982917454048923e-19,  1.0867177858084351e-19,  1.0751434490529747e-19,
                1.0635673859884002e-19,  1.0519882162526621e-19,  1.0404045260457141e-19,  1.0288148657544097e-19,
                1.0172177474144965e-19,  1.0056116419943559e-19,  9.9399497648346677e-20,  9.8236613076667446e-20,
                9.7072343426320094e-20,  9.5906516230690634e-20,  9.4738953224154196e-20,  9.3569469920159036e-20,
                9.2397875154569468e-20,  9.1223970590556472e-20,  9.0047550180852874e-20,  8.8868399582647627e-20,
                8.7686295519767450e-20,  8.6501005086071005e-20,  8.5312284983141187e-20,  8.4119880684385214e-20,
                8.2923525516513420e-20,  8.1722939648034506e-20,  8.0517828972839211e-20,  7.9307883875099226e-20,
                7.8092777859524425e-20,  7.6872166028429042e-20,  7.5645683383965122e-20,  7.4412942930179128e-20,
                7.3173533545093332e-20,  7.1927017587631075e-20,  7.0672928197666785e-20,  6.9410766239500362e-20,
                6.8139996829256425e-20,  6.6860045374610234e-20,  6.5570293040210081e-20,  6.4270071533368528e-20,
                6.2958657080923559e-20,  6.1635263438143136e-20,  6.0299033732151700e-20,  5.8949030892850181e-20,
                5.7584226359885930e-20,  5.6203486669597397e-20,  5.4805557413499315e-20,  5.3389043909003295e-20,
                5.1952387717989917e-20,  5.0493837866338355e-20,  4.9011415222629489e-20,  4.7502867933366117e-20,
                4.5965615001265455e-20,  4.4396673897997565e-20,  4.2792566302148588e-20,  4.1149193273430015e-20,
                3.9461666762606287e-20,  3.7724077131401685e-20,  3.5929164086204360e-20,  3.4067836691100565e-20,
                3.2128447641564046e-20,  3.0095646916399994e-20,  2.7948469455598328e-20,  2.5656913048718645e-20,
                2.3175209756803909e-20,  2.0426695228251291e-20,  1.7261770330213485e-20,  1.3281889259442578e-20,
                0.0000000000000000e+00 };

        // exponential_Y[i] = value of the exponential distribution function at exponential_X[i], scaled by 2**(-63)
        static final double[] exponentialY = {      // 253 entries, which is exponential_number_of_layers+1
                5.5952054951127360e-23,  1.1802509982703313e-22,  1.8444423386735829e-22,  2.5439030466698309e-22,
                3.2737694311509334e-22,  4.0307732132706715e-22,  4.8125478319495115e-22,  5.6172914896583308e-22,
                6.4435820540443526e-22,  7.2902662343463681e-22,  8.1563888456321941e-22,  9.0411453683482223e-22,
                9.9438488486399206e-22,  1.0863906045969114e-21,  1.1800799775461269e-21,  1.2754075534831208e-21,
                1.3723331176377290e-21,  1.4708208794375214e-21,  1.5708388257440445e-21,  1.6723581984374566e-21,
                1.7753530675030514e-21,  1.8797999785104595e-21,  1.9856776587832504e-21,  2.0929667704053244e-21,
                2.2016497009958240e-21,  2.3117103852306179e-21,  2.4231341516125464e-21,  2.5359075901420891e-21,
                2.6500184374170538e-21,  2.7654554763660391e-21,  2.8822084483468604e-21,  3.0002679757547711e-21,
                3.1196254936130377e-21,  3.2402731888801749e-21,  3.3622039464187092e-21,  3.4854113007409036e-21,
                3.6098893927859475e-21,  3.7356329310971768e-21,  3.8626371568620053e-21,  3.9908978123552837e-21,
                4.1204111123918948e-21,  4.2511737184488913e-21,  4.3831827151633737e-21,  4.5164355889510656e-21,
                4.6509302085234806e-21,  4.7866648071096003e-21,  4.9236379662119969e-21,  5.0618486007478993e-21,
                5.2012959454434732e-21,  5.3419795423648946e-21,  5.4838992294830959e-21,  5.6270551301806347e-21,
                5.7714476436191935e-21,  5.9170774358950678e-21,  6.0639454319177027e-21,  6.2120528079531677e-21,
                6.3614009847804375e-21,  6.5119916214136427e-21,  6.6638266093481696e-21,  6.8169080672926277e-21,
                6.9712383363524377e-21,  7.1268199756340822e-21,  7.2836557582420336e-21,  7.4417486676430174e-21,
                7.6011018943746355e-21,  7.7617188330775411e-21,  7.9236030798322572e-21,  8.0867584297834842e-21,
                8.2511888750363333e-21,  8.4168986028103258e-21,  8.5838919938383098e-21,  8.7521736209986459e-21,
                8.9217482481700712e-21,  9.0926208292996504e-21,  9.2647965076751277e-21,  9.4382806153938292e-21,
                9.6130786730210328e-21,  9.7891963894314161e-21,  9.9666396618278840e-21,  1.0145414575932636e-20,
                1.0325527406345955e-20,  1.0506984617068672e-20,  1.0689792862184811e-20,  1.0873958986701341e-20,
                1.1059490027542400e-20,  1.1246393214695825e-20,  1.1434675972510121e-20,  1.1624345921140471e-20,
                1.1815410878142659e-20,  1.2007878860214202e-20,  1.2201758085082226e-20,  1.2397056973538040e-20,
                1.2593784151618565e-20,  1.2791948452935152e-20,  1.2991558921150600e-20,  1.3192624812605428e-20,
                1.3395155599094805e-20,  1.3599160970797774e-20,  1.3804650839360727e-20,  1.4011635341137284e-20,
                1.4220124840587164e-20,  1.4430129933836705e-20,  1.4641661452404201e-20,  1.4854730467093280e-20,
                1.5069348292058084e-20,  1.5285526489044050e-20,  1.5503276871808626e-20,  1.5722611510726402e-20,
                1.5943542737583543e-20,  1.6166083150566702e-20,  1.6390245619451956e-20,  1.6616043290999594e-20,
                1.6843489594561079e-20,  1.7072598247904713e-20,  1.7303383263267072e-20,  1.7535858953637607e-20,
                1.7770039939284241e-20,  1.8005941154528286e-20,  1.8243577854777398e-20,  1.8482965623825808e-20,
                1.8724120381431627e-20,  1.8967058391181452e-20,  1.9211796268653192e-20,  1.9458350989888484e-20,
                1.9706739900186868e-20,  1.9956980723234356e-20,  2.0209091570579904e-20,  2.0463090951473895e-20,
                2.0718997783083593e-20,  2.0976831401101350e-20,  2.1236611570762130e-20,  2.1498358498287976e-20,
                2.1762092842777868e-20,  2.2027835728562592e-20,  2.2295608758045219e-20,  2.2565434025049041e-20,
                2.2837334128696004e-20,  2.3111332187840010e-20,  2.3387451856080863e-20,  2.3665717337386111e-20,
                2.3946153402349610e-20,  2.4228785405117410e-20,  2.4513639301013211e-20,  2.4800741664897764e-20,
                2.5090119710298442e-20,  2.5381801309347597e-20,  2.5675815013570500e-20,  2.5972190075566336e-20,
                2.6270956471628253e-20,  2.6572144925351523e-20,  2.6875786932281841e-20,  2.7181914785659148e-20,
                2.7490561603315974e-20,  2.7801761355793055e-20,  2.8115548895739172e-20,  2.8431959988666534e-20,
                2.8751031345137833e-20,  2.9072800654466307e-20,  2.9397306620015486e-20,  2.9724588996191657e-20,
                3.0054688627228112e-20,  3.0387647487867642e-20,  3.0723508726057078e-20,  3.1062316707775905e-20,
                3.1404117064129991e-20,  3.1748956740850969e-20,  3.2096884050352357e-20,  3.2447948726504914e-20,
                3.2802201982306013e-20,  3.3159696570631373e-20,  3.3520486848272230e-20,  3.3884628843476888e-20,
                3.4252180327233346e-20,  3.4623200888548644e-20,  3.4997752014001677e-20,  3.5375897171869060e-20,
                3.5757701901149035e-20,  3.6143233905835799e-20,  3.6532563154827400e-20,  3.6925761987883572e-20,
                3.7322905228086981e-20,  3.7724070301302117e-20,  3.8129337363171041e-20,  3.8538789434235234e-20,
                3.8952512543827862e-20,  3.9370595883442399e-20,  3.9793131970351439e-20,  4.0220216822325769e-20,
                4.0651950144388133e-20,  4.1088435528630944e-20,  4.1529780668232712e-20,  4.1976097586926582e-20,
                4.2427502885307452e-20,  4.2884118005513604e-20,  4.3346069515987453e-20,  4.3813489418210257e-20,
                4.4286515477520838e-20,  4.4765291580372353e-20,  4.5249968120658306e-20,  4.5740702418054417e-20,
                4.6237659171683015e-20,  4.6741010952818368e-20,  4.7250938740823415e-20,  4.7767632507051219e-20,
                4.8291291852069895e-20,  4.8822126702292804e-20,  4.9360358072933852e-20,  4.9906218905182021e-20,
                5.0459954986625539e-20,  5.1021825965285324e-20,  5.1592106469178258e-20,  5.2171087345169234e-20,
                5.2759077033045284e-20,  5.3356403093325858e-20,  5.3963413910399511e-20,  5.4580480596259246e-20,
                5.5207999124535584e-20,  5.5846392729873830e-20,  5.6496114614193770e-20,  5.7157651009290713e-20,
                5.7831524654956632e-20,  5.8518298763794323e-20,  5.9218581558791713e-20,  5.9933031488338700e-20,
                6.0662363246796887e-20,  6.1407354758435000e-20,  6.2168855320499763e-20,  6.2947795150103727e-20,
                6.3745196643214394e-20,  6.4562187737537985e-20,  6.5400017881889097e-20,  6.6260077263309343e-20,
                6.7143920145146620e-20,  6.8053293447301698e-20,  6.8990172088133000e-20,  6.9956803158564498e-20,
                7.0955761794878430e-20,  7.1990022788945080e-20,  7.3063053739105458e-20,  7.4178938266266893e-20,
                7.5342542134173124e-20,  7.6559742171142969e-20,  7.7837749863412850e-20,  7.9185582674029512e-20,
                8.0614775537353300e-20,  8.2140502769818073e-20,  8.3783445978280519e-20,  8.5573129249678161e-20,
                8.7554459669590100e-20,  8.9802388057706877e-20,  9.2462471421151086e-20,  9.5919641344951721e-20,
                1.0842021724855044e-19 };

        // alias_threshold[j] is a threshold for the probability mass function that has been
        // scaled by (2**64 - 1), translated by -(2**63), and represented as a long value;
        // in this way it can be directly compared to a randomly chosen long value.
        static final long[] exponentialAliasThreshold = {    // 256 entries
                9223372036854775807L,  1623796909450829958L,  2664290944894281002L,  7387971354164055035L,
                6515064486552722205L,  8840508362680707094L,  6099647593382923818L,  7673130333659514446L,
                6220332867583438718L,  5045979640552814279L,  4075305837223956071L,  3258413672162525964L,
                2560664887087763045L,  1957224924672900129L,  1429800935350578000L,   964606309710808688L,
                551043923599587587L,   180827629096889062L,  -152619738120023316L,  -454588624410291246L,
                -729385126147774679L,  -980551509819444511L, -1211029700667463575L, -1423284293868546830L,
                -1619396356369066372L, -1801135830956194794L, -1970018048575634032L, -2127348289059688469L,
                -2274257249303687482L, -2411729520096654942L, -2540626634159182211L, -2661705860113406183L,
                -2775635634532464842L, -2883008316030448462L, -2984350790383654449L, -3080133339198118132L,
                -3170777096303105047L, -3256660348483802362L, -3338123885075135933L, -3415475560473298752L,
                -3488994201966444258L, -3558932970354456420L, -3625522261068040742L, -3688972217741991689L,
                -3749474917563779627L, -3807206277531072172L, -3862327722496826830L, -3914987649156779312L,
                -3965322714631864882L, -4013458973776911635L, -4059512885612766613L, -4103592206186240662L,
                -4145796782586127736L, -4186219260694346585L, -4224945717447274810L, -4262056226866285147L,
                -4297625367836519229L, -4331722680528536958L, -4364413077437472159L, -4395757214229421760L,
                -4425811824915119137L, -4454630025296932322L, -4482261588141294467L, -4508753193105275908L,
                -4534148654077813412L, -4558489126279965349L, -4581813295192216486L, -4604157549138252679L,
                -4625556137145250151L, -4646041313519109096L, -4665643470413305673L, -4684391259530342697L,
                -4702311703971745066L, -4719430301145102986L, -4735771117539946027L, -4751356876102086987L,
                -4766209036859150188L, -4780347871385996716L, -4793792531638885869L, -4806561113635132333L,
                -4818670716409312334L, -4830137496634465358L, -4840976719260854030L, -4851202804490332239L,
                -4860829371376476047L, -4869869278311650511L, -4878334660640770576L, -4886236965617426832L,
                -4893586984900802224L, -4900394884772702384L, -4906670234238884945L, -4912422031164489009L,
                -4917658726580135697L, -4922388247283531793L, -4926618016851042065L, -4930354975163351025L,
                -4933605596540650674L, -4936375906575303186L, -4938671497741357106L, -4940497543854583186L,
                -4941858813449628882L, -4942759682136114354L, -4943204143989086194L, -4943195822025527282L,
                -4942737977813222130L, -4941833520255011698L, -4940485013586759090L, -4938694684624342322L,
                -4936464429291795314L, -4933795818458824946L, -4930690103114057265L, -4927148218896863345L,
                -4923170790008291569L, -4918758132519196401L, -4913910257091661489L, -4908626871126522161L,
                -4902907380349538608L, -4896750889844272240L, -4890156204540530416L, -4883121829162570096L,
                -4875645967641780528L, -4867726521994909999L, -4859361090668119087L, -4850546966345102383L,
                -4841281133215538414L, -4831560263698491374L, -4821380714613452974L, -4810738522790065581L,
                -4799629400105481389L, -4788048727936296621L, -4775991551010524588L, -4763452570642113772L,
                -4750426137329493931L, -4736906242696388587L, -4722886510751367403L, -4708360188440104938L,
                -4693320135461420394L, -4677758813316098089L, -4661668273553495721L, -4645040145179234152L,
                -4627865621182771687L, -4610135444140936871L, -4591839890849352486L, -4572968755929944934L,
                -4553511334358213029L, -4533456402849109028L, -4512792200036270244L, -4491506405372580067L,
                -4469586116675401954L, -4447017826233099938L, -4423787395382284961L, -4399880027458416864L,
                -4375280239014124063L, -4349971829190464606L, -4323937847117722654L, -4297160557210942813L,
                -4269621402214950684L, -4241300963840750107L, -4212178920821854874L, -4182234004204445017L,
                -4151443949668869272L, -4119785446662323159L, -4087234084103169942L, -4053764292396165205L,
                -4019349281473092435L, -3983960974549686930L, -3947569937258414993L, -3910145301787337104L,
                -3871654685619049615L, -3832064104425389837L, -3791337878631529676L, -3749438533114328651L,
                -3706326689447979465L, -3661960950051859912L, -3616297773528542022L, -3569291340409179909L,
                -3520893408440947267L, -3471053156460649921L, -3419717015797783872L, -3366828488034801534L,
                -3312327947826461820L, -3256152429334023226L, -3198235394669709240L, -3138506482563174262L,
                -3076891235255164340L, -3013310801389715890L, -2947681612411392816L, -2879915029671670702L,
                -2809916959107519276L, -2737587429961855017L, -2662820133571332903L, -2585501917733374884L,
                -2505512231579392929L, -2422722515205190175L, -2336995527534106140L, -2248184604988712345L,
                -2156132842510782614L, -2060672187261016979L, -1961622433929380112L, -1858790108950090508L,
                -1751967229002904073L, -1640929916937134981L, -1525436855617591297L, -1405227557075245821L,
                -1280020420662651897L, -1149510549536605301L, -1013367289578706928L,  -871231448632089708L,
                -722712146453677415L,  -567383236774421729L,  -404779231966956764L,  -234390647591531478L,
                -55658667960121553L,   132030985907831093L,   329355128892817467L,   537061298001091010L,
                755977262693561929L,   987022116608030929L,  1231219266829437401L,  1489711711346524770L,
                1763780090187559275L,  2054864117341782772L,  2364588157623782527L,  2694791916990482441L,
                3047567482883491349L,  3425304305830820514L,  3830744187097285423L,  4267048975685836605L,
                4737884547990014029L,  5247525842199011422L,  5800989391535342064L,  6404202162993303300L,
                7064218894258526746L,  7789505049452354354L,  8590309807749425484L,  7643763810684501605L,
                8891950541491453167L,  5457384281016234818L,  9083704440929285914L,  7976211653914461751L,
                8178631350487124609L,  2821287825726757492L,  6322989683301736617L,  4309503753387630347L,
                4685170734960191673L,  8404845967535252693L,  7330522972447610419L,  1960945799077061994L,
                4742910674644933674L,  -751799822533438695L,  7023456603742021660L,  3843116882594755262L,
                3927231442413889375L, -9223372036854775807L, -9223372036854775807L, -9223372036854775807L };

        static final byte[] exponentialAliasMap = {    // 256 entries
                (byte)  0, (byte)  0, (byte)  1, (byte)235, (byte)  3, (byte)  4, (byte)  5, (byte)  0,
                (byte)  0, (byte)  0, (byte)  0, (byte)  0, (byte)  0, (byte)  0, (byte)  0, (byte)  0,
                (byte)  0, (byte)  0, (byte)  1, (byte)  1, (byte)  1, (byte)  1, (byte)  2, (byte)  2,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)251, (byte)251, (byte)251, (byte)251, (byte)251, (byte)251, (byte)251,
                (byte)251, (byte)251, (byte)251, (byte)251, (byte)251, (byte)251, (byte)250, (byte)250,
                (byte)250, (byte)250, (byte)250, (byte)250, (byte)250, (byte)249, (byte)249, (byte)249,
                (byte)249, (byte)249, (byte)249, (byte)248, (byte)248, (byte)248, (byte)248, (byte)247,
                (byte)247, (byte)247, (byte)247, (byte)246, (byte)246, (byte)246, (byte)245, (byte)245,
                (byte)244, (byte)244, (byte)243, (byte)243, (byte)242, (byte)241, (byte)241, (byte)240,
                (byte)239, (byte)237, (byte)  3, (byte)  3, (byte)  4, (byte)  4, (byte)  6, (byte)  0,
                (byte)  0, (byte)  0, (byte)  0, (byte)236, (byte)237, (byte)238, (byte)239, (byte)240,
                (byte)241, (byte)242, (byte)243, (byte)244, (byte)245, (byte)246, (byte)247, (byte)248,
                (byte)249, (byte)250, (byte)251, (byte)252, (byte)  2, (byte)  0, (byte)  0, (byte)  0 };

        // Implementation support for modified-ziggurat implementation of nextGaussian()

        // Fraction of the area under the curve that lies outside the layer boxes: 0.0117
        // Fraction of non-box area that lies in the tail of the distribution: 0.0236

        static final int normalNumberOfLayers = 253;
        static final int normalLayerMask = 0xff;
        static final int normalAliasMask = 0xff;
        static final int normalSignCorrectionMask = 0xff;
        static final double normalX0 = 3.63600662550094578;
        static final int normalInflectionIndex = 204;
        static final long normalConvexMargin = 760463704284035183L;   // unscaled convex margin = 0.0824
        static final long normalConcaveMargin = 2269182951627976012L;   // unscaled concave margin = 0.2460

        // normal_X[i] = length of ziggurat layer i for normal distribution, scaled by 2**(-63)
        static final double[] normalX = {      // 254 entries, which is normal_number_of_layers+1
                3.9421662825398133e-19,  3.7204945004119012e-19,  3.5827024480628678e-19,  3.4807476236540249e-19,
                3.3990177171882136e-19,  3.3303778360340139e-19,  3.2709438817617550e-19,  3.2183577132495100e-19,
                3.1710758541840432e-19,  3.1280307407034065e-19,  3.0884520655804019e-19,  3.0517650624107352e-19,
                3.0175290292584600e-19,  2.9853983440705320e-19,  2.9550967462801797e-19,  2.9263997988491663e-19,
                2.8991225869977476e-19,  2.8731108780226291e-19,  2.8482346327101335e-19,  2.8243831535194389e-19,
                2.8014613964727031e-19,  2.7793871261807797e-19,  2.7580886921411212e-19,  2.7375032698308758e-19,
                2.7175754543391047e-19,  2.6982561247538484e-19,  2.6795015188771505e-19,  2.6612724730440033e-19,
                2.6435337927976633e-19,  2.6262537282028438e-19,  2.6094035335224142e-19,  2.5929570954331002e-19,
                2.5768906173214726e-19,  2.5611823497719608e-19,  2.5458123593393361e-19,  2.5307623292372459e-19,
                2.5160153867798400e-19,  2.5015559533646191e-19,  2.4873696135403158e-19,  2.4734430003079206e-19,
                2.4597636942892726e-19,  2.4463201347912450e-19,  2.4331015411139206e-19,  2.4200978427132955e-19,
                2.4072996170445879e-19,  2.3946980340903347e-19,  2.3822848067252674e-19,  2.3700521461931801e-19,
                2.3579927220741330e-19,  2.3460996262069972e-19,  2.3343663401054455e-19,  2.3227867054673840e-19,
                2.3113548974303765e-19,  2.3000654002704238e-19,  2.2889129852797606e-19,  2.2778926905921897e-19,
                2.2669998027527321e-19,  2.2562298398527416e-19,  2.2455785360727260e-19,  2.2350418274933911e-19,
                2.2246158390513294e-19,  2.2142968725296249e-19,  2.2040813954857555e-19,  2.1939660310297601e-19,
                2.1839475483749618e-19,  2.1740228540916853e-19,  2.1641889840016519e-19,  2.1544430956570613e-19,
                2.1447824613540345e-19,  2.1352044616350571e-19,  2.1257065792395107e-19,  2.1162863934653125e-19,
                2.1069415749082026e-19,  2.0976698805483467e-19,  2.0884691491567363e-19,  2.0793372969963634e-19,
                2.0702723137954107e-19,  2.0612722589717129e-19,  2.0523352580895635e-19,  2.0434594995315797e-19,
                2.0346432313698148e-19,  2.0258847584216418e-19,  2.0171824394771313e-19,  2.0085346846857531e-19,
                1.9999399530912015e-19,  1.9913967503040585e-19,  1.9829036263028144e-19,  1.9744591733545175e-19,
                1.9660620240469857e-19,  1.9577108494251485e-19,  1.9494043572246307e-19,  1.9411412901962161e-19,
                1.9329204245152935e-19,  1.9247405682708168e-19,  1.9166005600287074e-19,  1.9084992674649826e-19,
                1.9004355860642340e-19,  1.8924084378793725e-19,  1.8844167703488436e-19,  1.8764595551677749e-19,
                1.8685357872097450e-19,  1.8606444834960934e-19,  1.8527846822098793e-19,  1.8449554417517928e-19,
                1.8371558398354868e-19,  1.8293849726199566e-19,  1.8216419538767393e-19,  1.8139259141898448e-19,
                1.8062360001864453e-19,  1.7985713737964743e-19,  1.7909312115393845e-19,  1.7833147038364200e-19,
                1.7757210543468428e-19,  1.7681494793266395e-19,  1.7605992070083140e-19,  1.7530694770004409e-19,
                1.7455595397057217e-19,  1.7380686557563475e-19,  1.7305960954655264e-19,  1.7231411382940904e-19,
                1.7157030723311378e-19,  1.7082811937877138e-19,  1.7008748065025788e-19,  1.6934832214591352e-19,
                1.6861057563126349e-19,  1.6787417349268046e-19,  1.6713904869190636e-19,  1.6640513472135291e-19,
                1.6567236556010242e-19,  1.6494067563053266e-19,  1.6420999975549115e-19,  1.6348027311594532e-19,
                1.6275143120903661e-19,  1.6202340980646725e-19,  1.6129614491314931e-19,  1.6056957272604589e-19,
                1.5984362959313479e-19,  1.5911825197242491e-19,  1.5839337639095554e-19,  1.5766893940370800e-19,
                1.5694487755235889e-19,  1.5622112732380261e-19,  1.5549762510837070e-19,  1.5477430715767271e-19,
                1.5405110954198330e-19,  1.5332796810709688e-19,  1.5260481843056974e-19,  1.5188159577726683e-19,
                1.5115823505412761e-19,  1.5043467076406199e-19,  1.4971083695888395e-19,  1.4898666719118714e-19,
                1.4826209446506113e-19,  1.4753705118554365e-19,  1.4681146910669830e-19,  1.4608527927820112e-19,
                1.4535841199031451e-19,  1.4463079671711862e-19,  1.4390236205786415e-19,  1.4317303567630177e-19,
                1.4244274423783481e-19,  1.4171141334433217e-19,  1.4097896746642792e-19,  1.4024532987312287e-19,
                1.3951042255849034e-19,  1.3877416616527576e-19,  1.3803647990516385e-19,  1.3729728147547174e-19,
                1.3655648697200824e-19,  1.3581401079782068e-19,  1.3506976556752901e-19,  1.3432366200692418e-19,
                1.3357560884748263e-19,  1.3282551271542047e-19,  1.3207327801488087e-19,  1.3131880680481524e-19,
                1.3056199866908076e-19,  1.2980275057923788e-19,  1.2904095674948608e-19,  1.2827650848312727e-19,
                1.2750929400989213e-19,  1.2673919831340482e-19,  1.2596610294799512e-19,  1.2518988584399374e-19,
                1.2441042110056523e-19,  1.2362757876504165e-19,  1.2284122459762072e-19,  1.2205121982017852e-19,
                1.2125742084782245e-19,  1.2045967900166973e-19,  1.1965784020118020e-19,  1.1885174463419555e-19,
                1.1804122640264091e-19,  1.1722611314162064e-19,  1.1640622560939109e-19,  1.1558137724540874e-19,
                1.1475137369333185e-19,  1.1391601228549047e-19,  1.1307508148492592e-19,  1.1222836028063025e-19,
                1.1137561753107903e-19,  1.1051661125053526e-19,  1.0965108783189755e-19,  1.0877878119905372e-19,
                1.0789941188076655e-19,  1.0701268599703640e-19,  1.0611829414763286e-19,  1.0521591019102928e-19,
                1.0430518990027552e-19,  1.0338576948035472e-19,  1.0245726392923699e-19,  1.0151926522209310e-19,
                1.0057134029488235e-19,  9.9613028799672809e-20,  9.8643840599459914e-20,  9.7663252964755816e-20,
                9.6670707427623454e-20,  9.5665606240866670e-20,  9.4647308380433213e-20,  9.3615125017323508e-20,
                9.2568314370887282e-20,  9.1506075837638774e-20,  9.0427543267725716e-20,  8.9331777233763680e-20,
                8.8217756102327883e-20,  8.7084365674892319e-20,  8.5930387109612162e-20,  8.4754482764244349e-20,
                8.3555179508462343e-20,  8.2330848933585364e-20,  8.1079683729129853e-20,  7.9799669284133864e-20,
                7.8488549286072745e-20,  7.7143783700934692e-20,  7.5762496979467566e-20,  7.4341413578485329e-20,
                7.2876776807378431e-20,  7.1364245443525374e-20,  6.9798760240761066e-20,  6.8174368944799054e-20,
                6.6483992986198539e-20,  6.4719110345162767e-20,  6.2869314813103699e-20,  6.0921687548281263e-20,
                5.8859873575576818e-20,  5.6662675116090981e-20,  5.4301813630894571e-20,  5.1738171744494220e-20,
                4.8915031722398545e-20,  4.5744741890755301e-20,  4.2078802568583416e-20,  3.7625986722404761e-20,
                3.1628589805881879e-20,  0.0000000000000000e+00 };

        // normal_Y[i] = value of the normal distribution function at normal_X[i], scaled by 2**(-63)
        static final double[] normalY = {      // 254 entries, which is normal_number_of_layers+1
                1.4598410796619063e-22,  3.0066613427942797e-22,  4.6129728815103466e-22,  6.2663350049234362e-22,
                7.9594524761881544e-22,  9.6874655021705039e-22,  1.1446877002379439e-21,  1.3235036304379167e-21,
                1.5049857692053131e-21,  1.6889653000719298e-21,  1.8753025382711626e-21,  2.0638798423695191e-21,
                2.2545966913644708e-21,  2.4473661518801799e-21,  2.6421122727763533e-21,  2.8387681187879908e-21,
                3.0372742567457284e-21,  3.2375775699986589e-21,  3.4396303157948780e-21,  3.6433893657997798e-21,
                3.8488155868912312e-21,  4.0558733309492775e-21,  4.2645300104283590e-21,  4.4747557422305067e-21,
                4.6865230465355582e-21,  4.8998065902775257e-21,  5.1145829672105489e-21,  5.3308305082046173e-21,
                5.5485291167031758e-21,  5.7676601252690476e-21,  5.9882061699178461e-21,  6.2101510795442221e-21,
                6.4334797782257209e-21,  6.6581781985713897e-21,  6.8842332045893181e-21,  7.1116325227957095e-21,
                7.3403646804903092e-21,  7.5704189502886418e-21,  7.8017853001379744e-21,  8.0344543481570017e-21,
                8.2684173217333118e-21,  8.5036660203915022e-21,  8.7401927820109521e-21,  8.9779904520281901e-21,
                9.2170523553061439e-21,  9.4573722703928820e-21,  9.6989444059269430e-21,  9.9417633789758424e-21,
                1.0185824195119818e-20,  1.0431122230114770e-20,  1.0677653212987396e-20,  1.0925413210432004e-20,
                1.1174398612392891e-20,  1.1424606118728715e-20,  1.1676032726866302e-20,  1.1928675720361027e-20,
                1.2182532658289373e-20,  1.2437601365406785e-20,  1.2693879923010674e-20,  1.2951366660454145e-20,
                1.3210060147261461e-20,  1.3469959185800733e-20,  1.3731062804473644e-20,  1.3993370251385596e-20,
                1.4256880988463136e-20,  1.4521594685988369e-20,  1.4787511217522902e-20,  1.5054630655196170e-20,
                1.5322953265335218e-20,  1.5592479504415048e-20,  1.5863210015310328e-20,  1.6135145623830982e-20,
                1.6408287335525592e-20,  1.6682636332737932e-20,  1.6958193971903124e-20,  1.7234961781071113e-20,
                1.7512941457646084e-20,  1.7792134866331487e-20,  1.8072544037271070e-20,  1.8354171164377277e-20,
                1.8637018603838945e-20,  1.8921088872801004e-20,  1.9206384648209468e-20,  1.9492908765815636e-20,
                1.9780664219333857e-20,  2.0069654159747839e-20,  2.0359881894760859e-20,  2.0651350888385696e-20,
                2.0944064760670539e-20,  2.1238027287557466e-20,  2.1533242400870487e-20,  2.1829714188430474e-20,
                2.2127446894294597e-20,  2.2426444919118270e-20,  2.2726712820637798e-20,  2.3028255314272276e-20,
                2.3331077273843558e-20,  2.3635183732413286e-20,  2.3940579883236352e-20,  2.4247271080830277e-20,
                2.4555262842160330e-20,  2.4864560847940368e-20,  2.5175170944049622e-20,  2.5487099143065929e-20,
                2.5800351625915997e-20,  2.6114934743643687e-20,  2.6430855019297323e-20,  2.6748119149937411e-20,
                2.7066734008766247e-20,  2.7386706647381193e-20,  2.7708044298153558e-20,  2.8030754376735269e-20,
                2.8354844484695747e-20,  2.8680322412291631e-20,  2.9007196141372126e-20,  2.9335473848423219e-20,
                2.9665163907753988e-20,  2.9996274894828624e-20,  3.0328815589748056e-20,  3.0662794980885287e-20,
                3.0998222268678760e-20,  3.1335106869588609e-20,  3.1673458420220558e-20,  3.2013286781622988e-20,
                3.2354602043762612e-20,  3.2697414530184806e-20,  3.3041734802864950e-20,  3.3387573667257349e-20,
                3.3734942177548938e-20,  3.4083851642125208e-20,  3.4434313629256243e-20,  3.4786339973011376e-20,
                3.5139942779411164e-20,  3.5495134432826171e-20,  3.5851927602632460e-20,  3.6210335250134172e-20,
                3.6570370635764384e-20,  3.6932047326575882e-20,  3.7295379204034252e-20,  3.7660380472126401e-20,
                3.8027065665798284e-20,  3.8395449659736649e-20,  3.8765547677510167e-20,  3.9137375301086406e-20,
                3.9510948480742172e-20,  3.9886283545385430e-20,  4.0263397213308566e-20,  4.0642306603393541e-20,
                4.1023029246790967e-20,  4.1405583099096438e-20,  4.1789986553048817e-20,  4.2176258451776819e-20,
                4.2564418102621759e-20,  4.2954485291566197e-20,  4.3346480298300118e-20,  4.3740423911958146e-20,
                4.4136337447563716e-20,  4.4534242763218286e-20,  4.4934162278076256e-20,  4.5336118991149025e-20,
                4.5740136500984466e-20,  4.6146239026271279e-20,  4.6554451427421133e-20,  4.6964799229185088e-20,
                4.7377308644364938e-20,  4.7792006598684169e-20,  4.8208920756888113e-20,  4.8628079550147814e-20,
                4.9049512204847653e-20,  4.9473248772842596e-20,  4.9899320163277674e-20,  5.0327758176068971e-20,
                5.0758595537153414e-20,  5.1191865935622696e-20,  5.1627604062866059e-20,  5.2065845653856416e-20,
                5.2506627530725194e-20,  5.2949987648783448e-20,  5.3395965145159426e-20,  5.3844600390237576e-20,
                5.4295935042099358e-20,  5.4750012104183868e-20,  5.5206875986405073e-20,  5.5666572569983821e-20,
                5.6129149276275792e-20,  5.6594655139902476e-20,  5.7063140886520563e-20,  5.7534659015596918e-20,
                5.8009263888591218e-20,  5.8487011822987583e-20,  5.8967961192659803e-20,  5.9452172535103471e-20,
                5.9939708666122605e-20,  6.0430634802618929e-20,  6.0925018694200531e-20,  6.1422930764402860e-20,
                6.1924444262401531e-20,  6.2429635426193939e-20,  6.2938583658336214e-20,  6.3451371715447563e-20,
                6.3968085912834963e-20,  6.4488816345752736e-20,  6.5013657128995346e-20,  6.5542706656731714e-20,
                6.6076067884730717e-20,  6.6613848637404196e-20,  6.7156161942412980e-20,  6.7703126395950580e-20,
                6.8254866562246408e-20,  6.8811513411327825e-20,  6.9373204799659681e-20,  6.9940085998959109e-20,
                7.0512310279279503e-20,  7.1090039553397167e-20,  7.1673445090644796e-20,  7.2262708309655784e-20,
                7.2858021661057338e-20,  7.3459589613035800e-20,  7.4067629754967553e-20,  7.4682374037052817e-20,
                7.5304070167226666e-20,  7.5932983190698547e-20,  7.6569397282483754e-20,  7.7213617789487678e-20,
                7.7865973566417016e-20,  7.8526819659456755e-20,  7.9196540403850560e-20,  7.9875553017037968e-20,
                8.0564311788901630e-20,  8.1263312996426176e-20,  8.1973100703706304e-20,  8.2694273652634034e-20,
                8.3427493508836792e-20,  8.4173494807453416e-20,  8.4933097052832066e-20,  8.5707219578230905e-20,
                8.6496899985930695e-20,  8.7303317295655327e-20,  8.8127821378859504e-20,  8.8971970928196666e-20,
                8.9837583239314064e-20,  9.0726800697869543e-20,  9.1642181484063544e-20,  9.2586826406702765e-20,
                9.3564561480278864e-20,  9.4580210012636175e-20,  9.5640015550850358e-20,  9.6752334770503130e-20,
                9.7928851697808831e-20,  9.9186905857531331e-20,  1.0055456271343397e-19,  1.0208407377305566e-19,
                1.0390360993240711e-19,  1.0842021724855044e-19 };

        // alias_threshold[j] is a threshold for the probability mass function that has been
        // scaled by (2**64 - 1), translated by -(2**63), and represented as a long value;
        // in this way it can be directly compared to a randomly chosen long value.
        static final long[] normalAliasThreshold = {    // 256 entries
                9223372036854775732L,  1100243796470199922L,  7866600928967318259L,  6788754710669718688L,
                9022865200207136940L,  6522434035182564354L,  4723064097388367094L,  3360495653202227820L,
                2289663232347306830L,  1423968905585875379L,   708364817795238883L,   106102487338962592L,
                -408333464668584328L,  -853239722790494085L, -1242095211827090004L, -1585059631108655444L,
                -1889943050267333598L, -2162852901996526266L, -2408637386596951353L, -2631196530256993348L,
                -2833704942542501760L, -3018774289008775598L, -3188573753501888049L, -3344920681670389334L,
                -3489349705095933019L, -3623166100045386711L, -3747487436861293578L, -3863276422709141026L,
                -3971367044055496571L, -4072485557008423504L, -4167267476835653997L, -4256271432259158584L,
                -4339990541931699221L, -4418861817116128356L, -4493273980399812066L, -4563574004455583972L,
                -4630072609765608272L, -4693048910437239656L, -4752754358851355990L, -4809416110064308151L,
                -4863239903553549801L, -4914412541525462120L, -4963104028438393907L, -5009469424783376781L,
                -5053650458852410933L, -5095776932714599237L, -5135967952538787362L, -5174333008440005397L,
                -5210972924976812191L, -5245980700089102084L, -5279442247516610920L, -5311437055455710870L,
                -5342038772315685218L, -5371315728848281940L, -5399331404596850615L, -5426144845492958401L,
                -5451811038482575296L, -5476381248268660540L, -5499903320574200237L, -5522421955754019296L,
                -5543978956088644891L, -5564613449670076120L, -5584362093426489951L, -5603259257517942559L,
                -5621337193067953247L, -5638626184957155131L, -5655154691206501482L, -5670949470299055313L,
                -5686035697633988263L, -5700437072176015065L, -5714175914241450413L, -5727273255262198220L,
                -5739748920276454057L, -5751621603817308582L, -5762908939796390234L, -5773627565922293024L,
                -5783793183134813122L, -5793420610488485693L, -5802523835876777512L, -5811116062947540603L,
                -5819209754528321254L, -5826816672847738703L, -5833947916812588598L, -5840613956576464230L,
                -5846824665611918318L, -5852589350480860931L, -5857916778478181241L, -5862815203308620040L,
                -5867292388942958035L, -5871355631785040459L, -5875011781271709877L, -5878267259014830525L,
                -5881128076587168793L, -5883599852042383670L, -5885687825255517495L, -5887396872158140520L,
                -5888731517940791413L, -5889695949285098191L, -5890294025685452079L, -5890529289913339019L,
                -5890404977673728891L, -5889924026498433105L, -5889089083917111413L, -5887902514943630556L,
                -5886366408911444323L, -5884482585689698188L, -5882252601307215732L, -5879677753010810505L,
                -5876759083779777633L, -5873497386319005871L, -5869893206546653493L, -5865946846595933526L,
                -5861658367342436656L, -5857027590471882377L, -5852054100098427498L, -5846737243942430862L,
                -5841076134076202917L, -5835069647242632620L, -5828716424752710909L, -5822014871963881822L,
                -5814963157341321336L, -5807559211102860368L, -5799800723445392235L, -5791685142351319976L,
                -5783209670970726741L, -5774371264573181466L, -5765166627063894671L, -5755592207054728713L,
                -5745644193480823967L, -5735318510752045177L, -5724610813425415465L, -5713516480385581414L,
                -5702030608515423737L, -5690148005840583288L, -5677863184127162093L, -5665170350911168791L,
                -5652063400935782694L, -5638535906971010691L, -5624581109986711207L, -5610191908648783765L,
                -5595360848105231304L, -5580080108024969737L, -5564341489852042876L, -5548136403231016978L,
                -5531455851558564459L, -5514290416611714856L, -5496630242199355791L, -5478465016777918644L,
                -5459783954970839371L, -5440575777921757436L, -5420828692410297267L, -5400530368650229789L,
                -5379667916685479525L, -5358227861290596404L, -5336196115276119372L, -5313557951090901350L,
                -5290297970603367798L, -5266400072934326313L, -5241847420204395031L, -5216622401044877639L,
                -5190706591710560934L, -5164080714616987256L, -5136724594109421094L, -5108617109256031912L,
                -5079736143434386281L, -5050058530465123570L, -5019559997019987907L, -4988215101007960589L,
                -4955997165616088151L, -4922878208649305943L, -4888828866781574127L, -4853818314291958392L,
                -4817814175818125756L, -4780782432613346925L, -4742687321741700014L, -4703491227589533028L,
                -4663154565006030194L, -4621635653315226847L, -4578890580363657638L, -4534873055674290590L,
                -4489534251682380820L, -4442822631912146606L, -4394683764829968681L, -4345060121963632469L,
                -4293890858720706245L, -4241111576152819891L, -4186654061709945180L, -4130446006793453666L,
                -4072410698652140640L, -4012466683862855933L, -3950527400292573339L, -3886500774045756804L,
                -3820288777448438119L, -3751786943603804843L, -3680883832458819395L, -3607460442634330728L,
                -3531389562479403081L, -3452535052892669800L, -3370751053387208615L, -3285881101636362572L,
                -3197757155290696249L, -3106198503163967069L, -3011010550898974052L, -2911983463889090176L,
                -2808890647471134035L, -2701487041141521265L, -2589507199668960785L, -2472663129352313038L,
                -2350641842148622058L, -2223102583752258356L, -2089673683718520949L, -1949948966041670625L,
                -1803483646850545328L, -1649789631543398131L, -1488330106106063370L, -1318513295716695859L,
                -1139685236949889721L,  -951121376566993538L,  -752016768187462359L,  -541474585679321485L,
                -318492605702529265L,   -81947227237782935L,   169425512586600501L,   437052607251310002L,
                722551297576808029L,  1027761939321803391L,  1354787941562529921L,  1706044619231670700L,
                2084319374410687061L,  2492846399585974279L,  2935400169364870493L,  3416413484632185639L,
                3941127949845221101L,  4515787798750242711L,  5147892401460631081L,  5846529325404347588L,
                6622819682189677227L,  7490522659877439279L,  8466869998300400224L,  8216968526327386835L,
                4550693915429835301L,  7628019504075715697L,  6605080500885794707L,  7121156327618549405L,
                2484871780310660533L,  7179104797025802172L,  7066086283790288107L,  1516500120772178463L,
                216305945406470492L,  6295963418490399062L,  2889316805640753770L, -2712587580563247199L,
                6562498853480442900L,  7975754821117214681L, -9223372036854775807L, -9223372036854775807L };

        static final byte[] normalAliasMap = {    // 256 entries
                (byte)  0, (byte)  0, (byte)239, (byte)  2, (byte)  0, (byte)  0, (byte)  0, (byte)  0,
                (byte)  0, (byte)  0, (byte)  0, (byte)  0, (byte)  1, (byte)  1, (byte)  1, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253, (byte)253,
                (byte)253, (byte)253, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252, (byte)252,
                (byte)252, (byte)252, (byte)252, (byte)252, (byte)251, (byte)251, (byte)251, (byte)251,
                (byte)251, (byte)251, (byte)251, (byte)250, (byte)250, (byte)250, (byte)250, (byte)250,
                (byte)249, (byte)249, (byte)249, (byte)248, (byte)248, (byte)248, (byte)247, (byte)247,
                (byte)247, (byte)246, (byte)246, (byte)245, (byte)244, (byte)244, (byte)243, (byte)242,
                (byte)240, (byte)  2, (byte)  2, (byte)  3, (byte)  3, (byte)  0, (byte)  0, (byte)240,
                (byte)241, (byte)242, (byte)243, (byte)244, (byte)245, (byte)246, (byte)247, (byte)248,
                (byte)249, (byte)250, (byte)251, (byte)252, (byte)253, (byte)  1, (byte)  0, (byte)  0 };

    }

}
