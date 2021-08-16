/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8166884
 * @summary Checks the subsequent call to parse the same language ranges
 *          which must generate the same list of language ranges
 *          i.e. the priority list containing equivalents, as in the
 *          first call
 */

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class Bug8166994 {

    public static void main(String[] args) {
        List<String> list = Arrays.asList("ccq-aa", "ybd-aa", "rki-aa");
        String ranges = "ccq-aa";
        testParseConsistency(list, ranges);

        // consecutive call to check the language range parse consistency
        testParseConsistency(list, ranges);

        // another case with ranges consisting of multiple equivalents and
        // single equivalents
        list = Arrays.asList("gfx-xz", "oun-xz", "mwj-xz", "vaj-xz",
                "taj-xy", "tsf-xy");
        ranges = "gfx-xz, taj-xy";
        testParseConsistency(list, ranges);
        // consecutive call to check the language range parse consistency
        testParseConsistency(list, ranges);

    }

    private static void testParseConsistency(List<String> list, String ranges) {
        List<String> priorityList = parseRanges(ranges);
        if (!list.equals(priorityList)) {
            throw new RuntimeException("Failed to parse the language range ["
                    + ranges + "], Expected: " + list + " Found: "
                    + priorityList);
        }
    }

    private static List<String> parseRanges(String s) {
        return Locale.LanguageRange.parse(s).stream()
                .map(Locale.LanguageRange::getRange)
                .collect(Collectors.toList());
    }

}

