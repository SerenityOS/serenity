/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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

package jdk.internal.net.http;

import java.util.Random;

/** Abstract supertype for tests that need random numbers within a given range. */
public class AbstractRandomTest {

    private static Long getSystemSeed() {
        Long seed = null;
        try {
            // note that Long.valueOf(null) also throws a NumberFormatException
            // so if the property is undefined this will still work correctly
            seed = Long.valueOf(System.getProperty("seed"));
        } catch (NumberFormatException e) {
            // do nothing: seed is still null
        }
        return seed;
    }

    private static long getSeed() {
        Long seed = getSystemSeed();
        if (seed == null) {
            seed = (new Random()).nextLong();
        }
        System.out.println("Seed from AbstractRandomTest.getSeed = "+seed+"L");
        return seed;
    }

    private static Random random = new Random(getSeed());

    protected static int randomRange(int lower, int upper) {
        if (lower > upper)
            throw new IllegalArgumentException("lower > upper");
        int diff = upper - lower;
        int r = lower + random.nextInt(diff);
        return r - (r % 8); // round down to multiple of 8 (align for longs)
    }
}
