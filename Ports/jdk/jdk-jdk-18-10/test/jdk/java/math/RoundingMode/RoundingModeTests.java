/*
 * Copyright (c) 2003, Oracle and/or its affiliates. All rights reserved.
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

/*
 * @test
 * @bug 4851776 4891522 4905335
 * @summary Basic tests for the RoundingMode class.
 * @author Joseph D. Darcy
 */

import java.math.RoundingMode;
import java.math.BigDecimal;

public class RoundingModeTests {
    public static void main(String [] argv) {

        // For each member of the family, make sure
        // rm == valueOf(rm.toString())

        for(RoundingMode rm: RoundingMode.values()) {
            if (rm != RoundingMode.valueOf(rm.toString())) {
                throw new RuntimeException("Bad roundtrip conversion of " +
                                           rm.toString());
            }
        }

        // Test that mapping of old integers to new values is correct
        if (RoundingMode.valueOf(BigDecimal.ROUND_CEILING) !=
                RoundingMode.CEILING) {
                throw new RuntimeException("Bad mapping for ROUND_CEILING");
        }

        if (RoundingMode.valueOf(BigDecimal.ROUND_DOWN) !=
                RoundingMode.DOWN) {
                throw new RuntimeException("Bad mapping for ROUND_DOWN");
        }

        if (RoundingMode.valueOf(BigDecimal.ROUND_FLOOR) !=
                RoundingMode.FLOOR) {
                throw new RuntimeException("Bad mapping for ROUND_FLOOR");
        }

        if (RoundingMode.valueOf(BigDecimal.ROUND_HALF_DOWN) !=
                RoundingMode.HALF_DOWN) {
                throw new RuntimeException("Bad mapping for ROUND_HALF_DOWN");
        }

        if (RoundingMode.valueOf(BigDecimal.ROUND_HALF_EVEN) !=
                RoundingMode.HALF_EVEN) {
                throw new RuntimeException("Bad mapping for ROUND_HALF_EVEN");
        }

        if (RoundingMode.valueOf(BigDecimal.ROUND_HALF_UP) !=
                RoundingMode.HALF_UP) {
                throw new RuntimeException("Bad mapping for ROUND_HALF_UP");
        }

        if (RoundingMode.valueOf(BigDecimal.ROUND_UNNECESSARY) !=
                RoundingMode.UNNECESSARY) {
                throw new RuntimeException("Bad mapping for ROUND_UNNECESARY");
        }
    }
}
