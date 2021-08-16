/*
 * Copyright (c) 2012, 2018, Oracle and/or its affiliates. All rights reserved.
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


import org.testng.annotations.Test;

import java.io.UnsupportedEncodingException;
import java.util.Map;

import static jdk.test.lib.Asserts.assertEquals;


/*
    Unit test for the utility functions in JdpTestUtil.
    These are not meant to be by automatically run by JTREG.
    They exists to support test development and should be run by the test developer.
*/

public class JdpTestUtilTest {

    @Test
    public void testDecodeEntry() throws UnsupportedEncodingException {
        byte[] data = {'x', 0, 4, 'a', 'l', 'e', 'x'};
        String result = JdpTestUtil.decodeEntry(data, 1);
        assertEquals("alex", result);
    }

    @Test
    public void testDecode2ByteInt() {
        byte[] data = {'x', (byte) 0xff, (byte) 0xff};
        int value = JdpTestUtil.decode2ByteInt(data, 1);
        assertEquals(65535, value);
    }

    @Test
    public void testDecode4ByteInt() {
        byte[] data = {'x', (byte) 0xff, (byte) 0xff, (byte) 0xff, (byte) 0xff};
        int value = JdpTestUtil.decode4ByteInt(data, 1);
        assertEquals(0xffffffff, value);

    }

    @Test
    public void testReadRawPayload() throws UnsupportedEncodingException {
        byte[] data = {0, 3, 'f', 'o', 'o', 0, 4, 'b', 'a', 'r', 's'};
        Map<String, String> payload = JdpTestUtil.readRawPayload(data, data.length);

        assertEquals(1, payload.size());
        assertEquals("bars", payload.get("foo"));
    }

    @Test
    public void testReadPayload() throws UnsupportedEncodingException {
        byte[] data = {1, 2, 3, 4, 1, 2, 0, 3, 'f', 'o', 'o', 0, 4, 'b', 'a', 'r', 's'};
        Map<String, String> payload = JdpTestUtil.readPayload(data);

        assertEquals(1, payload.size());
        assertEquals("bars", payload.get("foo"));
    }

}
