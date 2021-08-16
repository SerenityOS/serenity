/*
 * Copyright (c) 2015, 2019, Oracle and/or its affiliates. All rights reserved.
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
 *
 */

/*
 * @test
 * @summary Check most common errors in file format
 * @requires vm.cds.archived.java.heap
 * @library /test/lib /test/hotspot/jtreg/runtime/cds/appcds
 * @build HelloString
 * @run driver InvalidFileFormat
 */

import java.io.File;
import jdk.test.lib.cds.CDSTestUtils;
import jdk.test.lib.process.OutputAnalyzer;

// Checking most common error use cases
// This file is not an exhastive test of various shared data file corruption
// Note on usability intent: the shared data file is created and handled by
// the previledge person in the server environment.
public class InvalidFileFormat {
    public static void main(String[] args) throws Exception {
        SharedStringsUtils.run(args, InvalidFileFormat::test);
    }

    public static void test(String[] args) throws Exception {
        SharedStringsUtils.buildJar("HelloString");

        test("NonExistentFile.txt", "Unable to get hashtable dump file size");
        test("InvalidHeader.txt", "wrong version of hashtable dump file");
        test("InvalidVersion.txt", "wrong version of hashtable dump file");
        test("CorruptDataLine.txt", "Unknown data type. Corrupted at line 2");
        test("InvalidSymbol.txt", "Unexpected character. Corrupted at line 2");
        test("InvalidSymbolFormat.txt", "Unrecognized format. Corrupted at line 9");
        test("OverflowPrefix.txt", "Num overflow. Corrupted at line 4");
        test("UnrecognizedPrefix.txt", "Unrecognized format. Corrupted at line 5");
        test("TruncatedString.txt", "Truncated. Corrupted at line 3");
        test("LengthOverflow.txt", "string length too large: 2147483647");
    }

    private static void
        test(String dataFileName, String expectedWarning) throws Exception {
        System.out.println("Filename for testcase: " + dataFileName);

        OutputAnalyzer out = SharedStringsUtils.dumpWithoutChecks(TestCommon.list("HelloString"),
                                 "invalidFormat" + File.separator + dataFileName);

        CDSTestUtils.checkMappingFailure(out);
        out.shouldContain(expectedWarning).shouldHaveExitValue(1);
    }

}
