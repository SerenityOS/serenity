/*
 * Copyright (c) 2021, Oracle and/or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License version 2 only, as
 *  published by the Free Software Foundation.
 *
 *  This code is distributed in the hope that it will be useful, but WITHOUT
 *  ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  version 2 for more details (a copy is included in the LICENSE file that
 *  accompanied this code).
 *
 *  You should have received a copy of the GNU General Public License version
 *  2 along with this work; if not, write to the Free Software Foundation,
 *  Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *   Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *  or visit www.oracle.com if you need additional information or have any
 *  questions.
 *
 */

import jdk.incubator.foreign.CLinker;
import jdk.incubator.foreign.MemorySegment;

import jdk.incubator.foreign.ResourceScope;
import org.testng.annotations.*;
import static org.testng.Assert.*;

/*
 * @test
 * @requires ((os.arch == "amd64" | os.arch == "x86_64") & sun.arch.data.model == "64") | os.arch == "aarch64"
 * @run testng TestStringEncoding
 */

public class TestStringEncoding {

    @Test(dataProvider = "strings")
    public void testStrings(String testString, int expectedByteLength) {
        try (ResourceScope scope = ResourceScope.newConfinedScope()) {
            MemorySegment text = CLinker.toCString(testString, scope);

            assertEquals(text.byteSize(), expectedByteLength);

            String roundTrip = CLinker.toJavaString(text);
            assertEquals(roundTrip, testString);
        }
    }

    @DataProvider
    public static Object[][] strings() {
        return new Object[][] {
            { "testing",               8 },
            { "",                      1 },
            { "X",                     2 },
            { "12345",                 6 },
            { "yen \u00A5",            7 }, // in UTF-8 2 bytes: 0xC2 0xA5
            { "snowman \u26C4",       12 }, // in UTF-8 three bytes: 0xE2 0x9B 0x84
            { "rainbow \uD83C\uDF08", 13 }  // in UTF-8 four bytes: 0xF0 0x9F 0x8C 0x88
        };
    }
}
