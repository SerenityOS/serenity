/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @test
 * @bug 8244706
 * @summary Verify that the OS header flag in the stream written out by java.util.zip.GZIPOutputStream
 * has the correct expected value
 * @run testng GZIPOutputStreamHeaderTest
 */
public class GZIPOutputStreamHeaderTest {

    private static final int OS_HEADER_INDEX = 9;
    private static final int HEADER_VALUE_OS_UNKNOWN = 255;

    /**
     * Test that the {@code OS} header field in the GZIP output stream
     * has a value of {@code 255} which represents "unknown"
     */
    @Test
    public void testOSHeader() throws Exception {
        final String data = "Hello world!!!";
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (final GZIPOutputStream gzipOutputStream = new GZIPOutputStream(baos);) {
            gzipOutputStream.write(data.getBytes(StandardCharsets.UTF_8));
        }
        final byte[] compressed = baos.toByteArray();
        Assert.assertNotNull(compressed, "Compressed data is null");
        Assert.assertEquals(toUnsignedByte(compressed[OS_HEADER_INDEX]), HEADER_VALUE_OS_UNKNOWN,
                "Unexpected value for OS header");
        // finally verify that the compressed data is readable back to the original
        final String uncompressed;
        try (final ByteArrayOutputStream os = new ByteArrayOutputStream();
             final ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
             final GZIPInputStream gzipInputStream = new GZIPInputStream(bis)) {
            gzipInputStream.transferTo(os);
            uncompressed = new String(os.toByteArray(), StandardCharsets.UTF_8);
        }
        Assert.assertEquals(uncompressed, data, "Unexpected data read from GZIPInputStream");
    }

    private static int toUnsignedByte(final byte b) {
        return b & 0xff;
    }
}
