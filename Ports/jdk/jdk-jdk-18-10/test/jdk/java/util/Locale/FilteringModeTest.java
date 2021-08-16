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

/*
 * @test
 * @bug 8210443
 * @summary Check values() and valueOf(String name) of Locale.FilteringMode.
 * @run main FilteringModeTest
 */

import java.util.Arrays;
import java.util.List;
import java.util.Locale.FilteringMode;
import java.util.stream.Collectors;

public class FilteringModeTest {
    private static boolean err = false;
    private static List<String> modeNames = List.of("AUTOSELECT_FILTERING",
                                                    "EXTENDED_FILTERING",
                                                    "IGNORE_EXTENDED_RANGES",
                                                    "MAP_EXTENDED_RANGES",
                                                    "REJECT_EXTENDED_RANGES");

    public static void main(String[] args) throws Exception {
        testValues();
        testValueOf();

        if (err) {
            throw new RuntimeException("Failed.");
        }
    }

    private static void testValueOf() {
        try {
            FilteringMode.valueOf("").name();
            err = true;
            System.err.println("IAE should be thrown for valueOf(\"\").");
        } catch (IllegalArgumentException ex) {
        }

        try {
            FilteringMode.valueOf(null).name();
            err = true;
            System.err.println("NPE should be thrown for valueOf(null).");
        } catch (NullPointerException ex) {
        }

        modeNames.forEach((expectedName) -> {
            String name = FilteringMode.valueOf(expectedName).name();
            if (!expectedName.equals(name)) {
                err = true;
                System.err.println("FilteringMode.valueOf(" + expectedName
                        + ") returned unexpected value. Expected: "
                        + expectedName + ", got: " + name);
            }
        });
    }

    private static void testValues() {
        FilteringMode[] modeArray = FilteringMode.values();
        List<String> modeNames2 = Arrays.stream(modeArray)
                .map(mode -> mode.name())
                .collect(Collectors.toList());

        if (!modeNames.equals(modeNames2)) {
            err = true;
            System.err.println("FilteringMode.values() returned unexpected value. Expected:"
                    + modeNames + " Got:" + modeNames2);
        }
    }
}
