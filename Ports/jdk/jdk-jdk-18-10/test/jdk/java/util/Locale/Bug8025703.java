/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8025703
 * @summary Verify implementation for Locale matching.
 * @run main Bug8025703
 */

import java.util.*;
import java.util.Locale.LanguageRange;

public class Bug8025703 {

    public static void main(String[] args) {
        boolean err = false;

        String[][] mappings = {{"ilw", "gal"},
                               {"meg", "cir"},
                               {"pcr", "adx"},
                               {"xia", "acn"},
                               {"yos", "zom"}};

        for (int i = 0; i < mappings.length; i++) {
            List<LanguageRange> got = LanguageRange.parse(mappings[i][0]);
            ArrayList<LanguageRange> expected = new ArrayList<>();
            expected.add(new LanguageRange(mappings[i][0], 1.0));
            expected.add(new LanguageRange(mappings[i][1], 1.0));

            if (!expected.equals(got)) {
                err = true;
                System.err.println("Incorrect language ranges. ");
                for (LanguageRange lr : expected) {
                    System.err.println("  Expected: range="
                        + lr.getRange() + ", weight=" + lr.getWeight());
                }
                for (LanguageRange lr : got) {
                    System.err.println("  Got:      range="
                        + lr.getRange() + ", weight=" + lr.getWeight());
                }
            }
        }

        if (err) {
            throw new RuntimeException("Failed.");
        }
    }

}

