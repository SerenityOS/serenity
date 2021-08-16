/*
 * Copyright (c) 2013, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @test CdsDifferentCompactStrings
 * @summary CDS (class data sharing) requires the same -XX:[+-]CompactStrings
 *          setting between archive creation time and load time.
 * @requires vm.cds
 * @library /test/lib
 * @modules java.base/jdk.internal.misc
 *          java.management
 */

import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

public class CdsDifferentCompactStrings {
    public static void main(String[] args) throws Exception {
        createAndLoadSharedArchive("+", "-");
        createAndLoadSharedArchive("-", "+");
    }

    private static void createAndLoadSharedArchive(String create, String load)
        throws Exception
    {
        String createCompactStringsArgument = "-XX:" + create + "CompactStrings";
        String loadCompactStringsArgument   = "-XX:" + load   + "CompactStrings";

        CDSTestUtils.createArchiveAndCheck(createCompactStringsArgument);

        OutputAnalyzer out = CDSTestUtils.runWithArchive(loadCompactStringsArgument);
        CDSTestUtils.checkMappingFailure(out);

        out.shouldMatch("The shared archive file's CompactStrings " +
                        "setting .* does not equal the current CompactStrings setting")
            .shouldHaveExitValue(1);
    }
}
