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



import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import java.util.Base64;

import static java.nio.charset.StandardCharsets.US_ASCII;

/**
 * @test
 * @bug 8007799 8176379
 * @summary test Encoder with linemax == 0, line separator should not appear in encoded data
 */

public class Base64GetEncoderTest {

    public static void main(String args[]) throws Throwable {

        for (int maxlen = -4; maxlen < 4; maxlen++) {

            final Base64.Encoder encoder = Base64.getMimeEncoder(maxlen, "$$$".getBytes(US_ASCII));

            testEncodeToString(encoder);
            testWrapEncode1(encoder);
            testEncodeToStringWithLongInputData(encoder);
            testWrapEncode2(encoder);
        }
    }

    private static void testWrapEncode2(final Base64.Encoder encoder)
            throws IOException {
        System.err.println("\nEncoder.wrap test II ");
        final byte[] secondTestBuffer =
                "api/java_util/Base64/index.html#GetEncoderMimeCustom[noLineSeparatorInEncodedString]"
                .getBytes(US_ASCII);
        String base64EncodedString;
        ByteArrayOutputStream secondEncodingStream = new ByteArrayOutputStream();
        OutputStream base64EncodingStream = encoder.wrap(secondEncodingStream);
        base64EncodingStream.write(secondTestBuffer);
        base64EncodingStream.close();

        final byte[] encodedByteArray = secondEncodingStream.toByteArray();

        System.err.print("result = " + new String(encodedByteArray, US_ASCII)
                + "  after wrap Base64 encoding of string");

        base64EncodedString = new String(encodedByteArray, US_ASCII);

        if (base64EncodedString.contains("$$$")) {
            throw new RuntimeException(
                    "Base64 encoding contains line separator after wrap 2 invoked  ... \n");
        }
    }

    private static void testEncodeToStringWithLongInputData(
            final Base64.Encoder encoder) {
        System.err.println("\n\nEncoder.encodeToStringWithLongInputData test  ");

        final byte[] secondTestBuffer =
                "api/java_util/Base64/index.html#GetEncoderMimeCustom[noLineSeparatorInEncodedString]"
                .getBytes(US_ASCII);
        String base64EncodedString;
        base64EncodedString = encoder.encodeToString(secondTestBuffer);

        System.err.println("Second Base64 encoded string is "
                + base64EncodedString);

        if (base64EncodedString.contains("$$$")) {
            throw new RuntimeException(
                    "Base64 encoding contains line separator after encodeToString invoked  ... \n");
        }
    }

    private static void testWrapEncode1(final Base64.Encoder encoder)
            throws IOException {
        System.err.println("\nEncoder.wrap test I ");

        final byte[] bytesIn = "fo".getBytes(US_ASCII);
        String base64EncodedString;
        ByteArrayOutputStream encodingStream = new ByteArrayOutputStream();
        OutputStream encoding = encoder.wrap(encodingStream);
        encoding.write(bytesIn);
        encoding.close();

        final byte[] encodedBytes = encodingStream.toByteArray();

        System.err.print("result = " + new String(encodedBytes, US_ASCII)
                + "  after the Base64 encoding \n");

        base64EncodedString = new String(encodedBytes, US_ASCII);

        if (base64EncodedString.contains("$$$")) {
            throw new RuntimeException(
                    "Base64 encoding contains line separator after wrap I test ... \n");
        }
    }

    private static void testEncodeToString(final Base64.Encoder encoder) {
        final byte[] bytesIn = "fo".getBytes(US_ASCII);

        System.err.println("\nEncoder.encodeToString test  ");

        String base64EncodedString = encoder.encodeToString(bytesIn);

        System.err.println("Base64 encoded string is " + base64EncodedString);

        if (base64EncodedString.contains("$$$")) {
            throw new RuntimeException("Base64 encoding contains line separator after Encoder.encodeToString invoked ... \n");
        }
    }
}
