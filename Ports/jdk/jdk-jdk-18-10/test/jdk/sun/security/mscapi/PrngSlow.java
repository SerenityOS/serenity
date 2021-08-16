/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6449335 8210476
 * @requires os.family == "windows"
 * @summary MSCAPI's PRNG is too slow
 * @key randomness
 */

import java.security.SecureRandom;

public class PrngSlow {

    public static void main(String[] args) throws Exception {
        double t = 0.0;
        SecureRandom sr = null;
        sr = SecureRandom.getInstance("Windows-PRNG", "SunMSCAPI");
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            if (i % 100 == 0) System.err.print(".");
            sr.nextBoolean();
        };
        t = (System.nanoTime() - start) / 1000000000.0;
        System.err.println("\nSpend " + t + " seconds");
        if (t > 0.5)
            throw new RuntimeException("Still too slow");
    }
}
