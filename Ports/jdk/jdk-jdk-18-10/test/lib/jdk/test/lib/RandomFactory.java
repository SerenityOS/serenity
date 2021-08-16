/*
 * Copyright (c) 2015, 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.test.lib;

import java.util.Random;
import java.util.SplittableRandom;

/**
 * Factory class which generates and prints to STDOUT a long-valued seed
 * for use in initializing a PRNG.  An instance of {@code Random} or
 * {@code SplittableRandom} may likewise be obtained.
 */
public class RandomFactory {
    /**
     * Attempt to obtain the seed from the value of the "seed" property.
     * @return The seed or {@code null} if the "seed" property was not set or
     *         could not be parsed.
     */
    private static Long getSystemSeed() {
        Long seed = null;
        try {
            // note that Long.valueOf(null) also throws a
            // NumberFormatException so if the property is undefined this
            // will still work correctly
            seed = Long.valueOf(System.getProperty("seed"));
        } catch (NumberFormatException e) {
            // do nothing: seed is still null
        }

        return seed;
    }

    /**
     * Obtain a seed from an independent PRNG.
     *
     * @return A random seed.
     */
    private static long getRandomSeed() {
        return new Random().nextLong();
    }

    /**
     * Obtain and print to STDOUT a seed appropriate for initializing a PRNG.
     * If the system property "seed" is set and has value which may be correctly
     * parsed it is used, otherwise a seed is generated using an independent
     * PRNG.
     *
     * @return The seed.
     */
    public static long getSeed() {
        Long seed = getSystemSeed();
        if (seed == null) {
            seed = getRandomSeed();
        }
        System.out.println("Seed from RandomFactory = "+seed+"L");
        return seed;
    }

    /**
     * Obtain and print to STDOUT a seed and use it to initialize a new
     * {@code Random} instance which is returned. If the system
     * property "seed" is set and has value which may be correctly parsed it
     * is used, otherwise a seed is generated using an independent PRNG.
     *
     * @return The {@code Random} instance.
     */
    public static Random getRandom() {
        return new Random(getSeed());
    }

    /**
     * Obtain and print to STDOUT a seed and use it to initialize a new
     * {@code SplittableRandom} instance which is returned. If the system
     * property "seed" is set and has value which may be correctly parsed it
     * is used, otherwise a seed is generated using an independent PRNG.
     *
     * @return The {@code SplittableRandom} instance.
     */
    public static SplittableRandom getSplittableRandom() {
        return new SplittableRandom(getSeed());
    }
}
