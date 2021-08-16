/*
 * Copyright (c) 2019, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8216140
 * @summary Test reversed BOM (U+FFFE) in the middle of a byte buffer
 *      passes through during decoding with UnicodeDecoder.
 * @run testng TestUnicodeReversedBOM
 */
import java.nio.charset.*;
import java.nio.*;
import java.util.*;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

@Test
public class TestUnicodeReversedBOM {
    private static byte[] A_REVERSED_BE =
        {(byte)0x0, (byte)'A', (byte)0xff, (byte)0xfe};
    private static byte[] A_REVERSED_LE =
        {(byte)'A', (byte)0x0, (byte)0xfe, (byte)0xff};
    private static byte[] BOM_REVERSED_BE =
        {(byte)0xfe, (byte)0xff, (byte)0xff, (byte)0xfe};
    private static byte[] BOM_REVERSED_LE =
        {(byte)0xff, (byte)0xfe, (byte)0xfe, (byte)0xff};

    @DataProvider
    // [(byte[])byte array, (Charset)cs]
    public static Object[][] ReversedBOM() {
        return new Object[][] {
            {A_REVERSED_BE, StandardCharsets.UTF_16},
            {A_REVERSED_LE, StandardCharsets.UTF_16},
            {A_REVERSED_BE, StandardCharsets.UTF_16BE},
            {A_REVERSED_LE, StandardCharsets.UTF_16BE},
            {A_REVERSED_BE, StandardCharsets.UTF_16LE},
            {A_REVERSED_LE, StandardCharsets.UTF_16LE},
            {BOM_REVERSED_BE, StandardCharsets.UTF_16},
            {BOM_REVERSED_LE, StandardCharsets.UTF_16},
            {BOM_REVERSED_BE, StandardCharsets.UTF_16BE},
            {BOM_REVERSED_LE, StandardCharsets.UTF_16BE},
            {BOM_REVERSED_BE, StandardCharsets.UTF_16LE},
            {BOM_REVERSED_LE, StandardCharsets.UTF_16LE},
        };
    }

    @Test(dataProvider = "ReversedBOM")
    public void testReversedBOM(byte[] ba, Charset cs) throws CharacterCodingException {
        cs.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(ba));
    }
}
