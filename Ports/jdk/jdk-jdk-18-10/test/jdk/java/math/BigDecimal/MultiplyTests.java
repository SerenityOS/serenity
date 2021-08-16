/*
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6850606
 * @summary Test BigDecimal.multiply(BigDecimal)
 * @author xlu
 */

import java.math.*;
import static java.math.BigDecimal.*;

public class MultiplyTests {

    private static int multiplyTests() {
        int failures = 0;

        BigDecimal[] bd1 = {
            new BigDecimal("123456789"),
            new BigDecimal("1234567898"),
            new BigDecimal("12345678987")
        };

        BigDecimal[] bd2 = {
            new BigDecimal("987654321"),
            new BigDecimal("8987654321"),
            new BigDecimal("78987654321")
        };

        // Two dimensonal array recording bd1[i] * bd2[j] &
        // 0 <= i <= 2 && 0 <= j <= 2;
        BigDecimal[][] expectedResults = {
            {new BigDecimal("121932631112635269"),
             new BigDecimal("1109586943112635269"),
             new BigDecimal("9751562173112635269")
            },
            { new BigDecimal("1219326319027587258"),
              new BigDecimal("11095869503027587258"),
              new BigDecimal("97515622363027587258")
            },
            { new BigDecimal("12193263197189452827"),
              new BigDecimal("110958695093189452827"),
              new BigDecimal("975156224183189452827")
            }
        };

        for (int i = 0; i < bd1.length; i++) {
            for (int j = 0; j < bd2.length; j++) {
                if (!bd1[i].multiply(bd2[j]).equals(expectedResults[i][j])) {
                    failures++;
                }
            }
        }

        BigDecimal x = BigDecimal.valueOf(8L, 1);
        BigDecimal xPower = BigDecimal.valueOf(-1L);
        try {
            for (int i = 0; i < 100; i++) {
                xPower = xPower.multiply(x);
            }
        } catch (Exception ex) {
            failures++;
        }
        return failures;
    }

    public static void main(String[] args) {
        int failures = 0;

        failures += multiplyTests();

        if (failures > 0) {
            throw new RuntimeException("Incurred " + failures +
                                       " failures while testing multiply.");
        }
    }
}
