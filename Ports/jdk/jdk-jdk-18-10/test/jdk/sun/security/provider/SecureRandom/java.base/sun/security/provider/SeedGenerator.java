/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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

// This is not the real sun.security.provider.SeedGenerator class. Used by
// ../../../../CommonSeeder.java only.
package sun.security.provider;

public class SeedGenerator {

    static int count = 100;
    static int lastCount = 100;

    public static void generateSeed(byte[] result) {
        count--;
    }

    /**
     * Confirms genEntropy() has been called {@code less} times
     * since last check.
     */
    public static void checkUsage(int less) throws Exception {
        if (lastCount != count + less) {
            throw new Exception(String.format(
                    "lastCount = %d, count = %d, less = %d",
                    lastCount, count, less));
        }
        lastCount = count;
    }

    // Needed by AbstractDrbg.java
    static byte[] getSystemEntropy() {
        return new byte[20];
    }
}
