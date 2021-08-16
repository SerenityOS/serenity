/*
 * Copyright (c) 2019, 2020, Oracle and/or its affiliates. All rights reserved.
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
package jdk.jpackage.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import static org.junit.Assert.assertEquals;

@RunWith(Parameterized.class)
public class CompareDottedVersionTest {

    public CompareDottedVersionTest(boolean greedy, String version1,
            String version2, int result) {
        this.version1 = version1;
        this.version2 = version2;
        this.expectedResult = result;

        if (greedy) {
            createTestee = DottedVersion::greedy;
        } else {
            createTestee = DottedVersion::lazy;
        }
    }

    @Parameters
    public static List<Object[]> data() {
        List<Object[]> data = new ArrayList<>();
        for (var greedy : List.of(true, false)) {
            data.addAll(List.of(new Object[][] {
                { greedy, "00.0.0", "0", 0 },
                { greedy, "00.0.0", "0.000", 0 },
                { greedy, "0.035", "0.0035", 0 },
                { greedy, "0.035", "0.0035.0", 0 },
                { greedy, "1", "1", 0 },
                { greedy, "2", "2.0", 0 },
                { greedy, "2.00", "2.0", 0 },
                { greedy, "1.2.3.4", "1.2.3.4.5", -1 },
                { greedy, "1.2.3.4", "1.2.3.4.0.1", -1 },
                { greedy, "34", "33", 1 },
                { greedy, "34.0.78", "34.1.78", -1 }
            }));
        }

        data.addAll(List.of(new Object[][] {
            { false, "", "1", -1 },
            { false, "1.2.4-R4", "1.2.4-R5", 0 },
            { false, "1.2.4.-R4", "1.2.4.R5", 0 },
            { false, "7+1", "7+4", 0 },
            { false, "2+14", "2-14", 0 },
            { false, "23.4.RC4", "23.3.RC10", 1 },
            { false, "77."  + "9".repeat(1000), "77." + "9".repeat(1000 -1) + "8", 1 },
        }));

        return data;
    }

    @Test
    public void testIt() {
        int actualResult = compare(version1, version2);
        assertEquals(expectedResult, actualResult);

        int actualNegateResult = compare(version2, version1);
        assertEquals(actualResult, -1 * actualNegateResult);
    }

    private int compare(String x, String y) {
        int result = createTestee.apply(x).compareTo(y);

        if (result < 0) {
            return -1;
        }

        if (result > 0) {
            return 1;
        }

        return 0;
    }

    private final String version1;
    private final String version2;
    private final int expectedResult;
    private final Function<String, DottedVersion> createTestee;
}
