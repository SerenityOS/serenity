/*
 * Copyright (c) 2003, 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8136874
 * @summary Tests for StrictMath.pow
 * @author Joseph D. Darcy
 */

/**
 * The tests in ../Math/PowTests.java test properties that should
 * hold for any pow implementation, including the FDLIBM-based one
 * required for StrictMath.pow.  Therefore, the test cases in
 * ../Math/PowTests.java are run against both the Math and
 * StrictMath versions of pow.  The role of this test is to verify
 * that the FDLIBM pow algorithm is being used by running golden
 * file tests on values that may vary from one conforming pow
 * implementation to another.
 */

public class PowTests {
    private PowTests(){}

    private static final double INFINITY = Double.POSITIVE_INFINITY;

    public static void main(String... args) {
        int failures = 0;

        failures += testPow();

        if (failures > 0) {
            System.err.println("Testing pow incurred "
                               + failures + " failures.");
            throw new RuntimeException();
        }
    }

    private static int testPow() {
        int failures = 0;

        double [][] testCases = {
            // Probe near decision points of the fdlibm algorithm

            {0x1.00000_0000_0001p1,  // |x| > 1.0
             INFINITY,               // infinity
             INFINITY                // 0
            },


            {0x1.fffffp-1,           // |x| = 0.9999995231628418
             0x1.0p31,               // 2^31
             0.0                     // 0
            },

            {0x1.ffffe_ffffffffp-1,  // |x| < 0.9999995231628418
             0x1.0p31,               // 2^31
             0.0                     // 0
            },

            {-0x1.ffffe_ffffffffp-1, // |x| < 0.9999995231628418
             0x1.0p31,               // 2^31
             0.0                     // 0
            },

            {0x1.fffffp-1,           // |x| = 0.9999995231628418
             0x1.0000000000001p31,   // nextUp(2^31)
             0.0                     // 0
            },

            {0x1.fffffp-1,           // |x| = 0.9999995231628418
             0x1.0p31 + 1.0,         // 2^31 + 1, odd integer
             0.0                     // 0
            },

            {0x1.fffffp-1,           // |x| = 0.9999995231628418
             0x1.0p31 + 2.0,         // 2^31 + 2, even integer
             0.0                     // 0
            },

            {0x1.ffffe_ffffffffp-1,  // |x| < 0.9999995231628418
             0x1.0000000000001p31,   // nextUp(2^31)
             0.0                     // 0
            },

            {-0x1.ffffe_ffffffffp-1, // |x| < 0.9999995231628418
             0x1.0000000000001p31,   // nextUp(2^31)
             Double.NaN              // 0
            },

            {-0x1.ffffe_ffffffffp-1, // |x| < 0.9999995231628418
             0x1.0p31 + 1.0,         // 2^31 + 1, odd integer
             -0.0                    // 0
            },

            {-0x1.ffffe_ffffffffp-1, // |x| < 0.9999995231628418
             0x1.0p31 + 2.0,         // 2^31 + 2, even integer
             0.0                     // 0
            },

            {0x1.0000000000001p0,    // nextUp(1)
             0x1.0000000000001p31,   // nextUp(2^31)
             0x1.00000800002p0
            },

            {0x1.0000000000001p0,    // nextUp(1)
             -0x1.0000000000001p31,  // -nextUp(2^31)
             0x1.fffff000004p-1
            },

            {-0x1.0000000000001p0,   // -nextUp(1)
             -0x1.0000000000001p31,  // -nextUp(2^31)
             Double.NaN
            },

            {-0x1.0000000000001p0,   // -nextUp(1)
             0x1.0p31 + 1.0,         // 2^31 + 1, odd integer
             -0x1.0000080000201p0
            },

            {-0x1.0000000000001p0,   // -nextUp(1)
             0x1.0p31 + 2.0,         // 2^31 + 2, even integer
             0x1.0000080000202p0
            },

            {0x1.00000_ffff_ffffp0,
             0x1.00001_0000_0000p31,
             INFINITY
            },

            // Huge y, |y| > 0x1.00000_ffff_ffffp31 ~2**31 is a decision point

            // First y = 0x1.00001_0000_0000p31
            {0x1.fffff_ffff_ffffp-1,
             0x1.00001_0000_0000p31,
             0x1.fffff7ffff9p-1
            },

            {0x1.fffff_ffff_fffep-1,
             0x1.00001_0000_0000p31,
             0x1.ffffefffff4p-1
            },

            {0x1.fffff_0000_0000p-1,
             0x1.00001_0000_0000p31,
             0.0
            },

            //  Cycle through decision points on x values

            {0x1.fffff_0000_0000p-1,
             0x1.00001_0000_0000p31,
             0.0
            },

            {-0x1.fffff_0000_0000p-1,
             0x1.00001_0000_0000p31,
             0.0
            },

            {0x1.ffffe_ffff_ffffp-1,
             0x1.00001_0000_0000p31,
             0.0
            },

            {-0x1.ffffe_ffff_ffffp-1,
             0x1.00001_0000_0000p31,
             0.0
            },

            {0x1.00000_ffff_ffffp0,
             0x1.00001_0000_0000p31,
             INFINITY
            },


            {0x1.00001_0000_0000p0,
             0x1.00001_0000_0000p31,
             INFINITY
            },

            {-0x1.00000_ffff_ffffp0,
             0x1.00001_0000_0000p31,
             INFINITY
            },


            {-0x1.00001_0000_0000p0,
             0x1.00001_0000_0000p31,
             INFINITY
            },

            // Now y = -0x1.00001_0000_0000p31

            {0x1.fffff_0000_0000p-1,
             -0x1.00001_0000_0000p31,
             INFINITY
            },

            {-0x1.fffff_0000_0000p-1,
             0x1.00001_0000_0000p31,
             0.0
            },

            {0x1.ffffe_ffff_ffffp-1,
             -0x1.00001_0000_0000p31,
             INFINITY
            },

            {-0x1.ffffe_ffff_ffffp-1,
             -0x1.00001_0000_0000p31,
             INFINITY
            },

            {0x1.00000_ffff_ffffp0,
             -0x1.00001_0000_0000p31,
             0.0
            },


            {0x1.00001_0000_0000p0,
             -0x1.00001_0000_0000p31,
             0.0
            },

            {-0x1.00000_ffff_ffffp0,
             -0x1.00001_0000_0000p31,
             0.0
            },


            {-0x1.00001_0000_0000p0,
             -0x1.00001_0000_0000p31,
             0.0
            },

            //-----------------------

            {0x1.ffffe_ffff_ffffp-1,
             -0x1.00001_0000_0000p31,
             INFINITY
            },

            {0x1.00001_0000_0000p0,
             -0x1.00001_0000_0000p31,
             0.0
            },


            {0x1.0000000000002p0, // 1.0000000000000004
             0x1.f4add4p30,       // 2.1E9
             0x1.00000fa56f1a6p0  // 1.0000009325877754
            },

            // Verify no early overflow
            {0x1.0000000000002p0, // 1.0000000000000004
             0x1.0642acp31,       // 2.2E9
             0x1.000010642b465p0, // 1.0000009769967388
            },

            // Verify proper overflow
            {0x1.0000000000002p0,    // 1.0000000000000004
             0x1.62e42fefa39fp60,    // 1.59828858065033216E18
             0x1.ffffffffffd9fp1023, // 1.7976931348621944E308
            },

        };

        for (double[] testCase: testCases)
            failures += testPowCase(testCase[0], testCase[1], testCase[2]);

        return failures;
    }

    private static int testPowCase(double input1, double input2, double expected) {
        int failures = 0;
        failures += Tests.test("StrictMath.pow(double)", input1, input2,
                               StrictMath.pow(input1, input2), expected);
        return failures;
    }
}
