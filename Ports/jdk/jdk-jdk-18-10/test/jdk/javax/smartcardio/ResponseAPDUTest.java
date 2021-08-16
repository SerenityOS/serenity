/*
 * Copyright (c) 2007, 2020, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8049021 8255546
 * @summary Construct ResponseAPDU from byte array and check NR< SW, SW1, SW2 and toString
 * @run testng ResponseAPDUTest
 */
import javax.smartcardio.ResponseAPDU;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ResponseAPDUTest {

    static final byte[] R1 = {(byte) 0x07, (byte) 0xA0, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x62, (byte) 0x81, (byte) 0x01,
        (byte) 0x04, (byte) 0x01, (byte) 0x00, (byte) 0x00, (byte) 0x24,
        (byte) 0x05, (byte) 0x00, (byte) 0x0B, (byte) 0x04, (byte) 0xB0,
        (byte) 0x25, (byte) 0x90, (byte) 0x00};
    static final ResponseAPDU RAPDU = new ResponseAPDU(R1);
    static byte[] expectedData;
    static int expectedNr, expectedSw1, expectedSw2, expectedSw;
    static String expectedToString;

    @BeforeClass
    public static void setUpClass() throws Exception {
        //expected values for data,nr,sw1,sw2 and sw

        int apduLen = R1.length;
        expectedData = new byte[apduLen - 2];
        for (int i = 0; i < (apduLen - 2); i++) {
            expectedData[i] = R1[i];
        }

        expectedNr = expectedData.length;
        expectedSw1 = R1[apduLen - 2] & 0xff;
        expectedSw2 = R1[apduLen - 1] & 0xff;
        expectedSw = (expectedSw1 << 8) | expectedSw2;

        expectedToString = "ResponseAPDU: " + R1.length +
                " bytes, SW=" + Integer.toHexString(expectedSw);
    }

    @Test
    public static void test() {
        assertEquals(RAPDU.getBytes(), R1);
        assertEquals(RAPDU.getData(), expectedData);
        assertEquals(RAPDU.getNr(), expectedNr);
        assertEquals(RAPDU.getSW(), expectedSw);
        assertEquals(RAPDU.getSW1(), expectedSw1);
        assertEquals(RAPDU.getSW2(), expectedSw2);
        assertEquals(RAPDU.toString(), expectedToString);
    }
}
