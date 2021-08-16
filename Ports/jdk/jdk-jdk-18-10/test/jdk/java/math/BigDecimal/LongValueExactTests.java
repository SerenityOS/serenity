/*
 * Copyright (c) 2005, 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6806261 8211936
 * @summary Tests of BigDecimal.longValueExact
 */
import java.math.*;
import java.util.List;
import java.util.Map;
import static java.util.Map.entry;

public class LongValueExactTests {
    public static void main(String... args) {
        int failures = 0;

        failures += longValueExactSuccessful();
        failures += longValueExactExceptional();

        if (failures > 0) {
            throw new RuntimeException("Incurred " + failures +
                                       " failures while testing longValueExact.");
        }
    }

    private static long simpleLongValueExact(BigDecimal bd) {
        return bd.toBigIntegerExact().longValue();
    }

    private static int longValueExactSuccessful() {
        int failures = 0;

        // Strings used to create BigDecimal instances on which invoking
        // longValueExact() will succeed.
        Map<BigDecimal, Long> successCases =
            Map.ofEntries(entry(new BigDecimal("9223372036854775807"),    Long.MAX_VALUE), // 2^63 -1
                          entry(new BigDecimal("9223372036854775807.0"),  Long.MAX_VALUE),
                          entry(new BigDecimal("9223372036854775807.00"), Long.MAX_VALUE),

                          entry(new BigDecimal("-9223372036854775808"),   Long.MIN_VALUE), // -2^63
                          entry(new BigDecimal("-9223372036854775808.0"), Long.MIN_VALUE),
                          entry(new BigDecimal("-9223372036854775808.00"),Long.MIN_VALUE),

                          entry(new BigDecimal("1e0"),    1L),
                          entry(new BigDecimal(BigInteger.ONE, -18),   1_000_000_000_000_000_000L),

                          entry(new BigDecimal("0e13"),   0L), // Fast path zero
                          entry(new BigDecimal("0e64"),   0L),
                          entry(new BigDecimal("0e1024"), 0L),

                          entry(new BigDecimal("10.000000000000000000000000000000000"), 10L));

        for (var testCase : successCases.entrySet()) {
            BigDecimal bd = testCase.getKey();
            long expected = testCase.getValue();
            try {
                long longValueExact = bd.longValueExact();
                if (expected != longValueExact ||
                    longValueExact != simpleLongValueExact(bd)) {
                    failures++;
                    System.err.println("Unexpected longValueExact result " + longValueExact +
                                       " on " + bd);
                }
            } catch (Exception e) {
                failures++;
                System.err.println("Error on " + bd + "\tException message:" + e.getMessage());
            }
        }
        return failures;
    }

    private static int longValueExactExceptional() {
        int failures = 0;
        List<BigDecimal> exceptionalCases =
            List.of(new BigDecimal("9223372036854775808"), // Long.MAX_VALUE + 1
                    new BigDecimal("9223372036854775808.0"),
                    new BigDecimal("9223372036854775808.00"),
                    new BigDecimal("-9223372036854775809"), // Long.MIN_VALUE - 1
                    new BigDecimal("-9223372036854775808.1"),
                    new BigDecimal("-9223372036854775808.01"),

                    new BigDecimal("9999999999999999999"),
                    new BigDecimal("10000000000000000000"),

                    new BigDecimal("0.99"),
                    new BigDecimal("0.999999999999999999999"));

        for (BigDecimal bd : exceptionalCases) {
            try {
                long longValueExact = bd.longValueExact();
                failures++;
                System.err.println("Unexpected non-exceptional longValueExact on " + bd);
            } catch (ArithmeticException e) {
                // Success;
            }
        }
        return failures;
    }
}
