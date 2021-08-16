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
 * @bug 8146041
 * @summary java.net.URLConnection.guessContentTypeFromStream() does not
 * recognize TIFF streams
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URLConnection;

public class TIFFContentGuesser {
    private static final byte[] LITTLE_ENDIAN_MAGIC =
        new byte[] {(byte)0x49, (byte)0x49, (byte)0x2a, (byte)0};
    private static final byte[] BIG_ENDIAN_MAGIC =
        new byte[] {(byte)0x4d, (byte)0x4d, (byte)0, (byte)0x2a};

    private static final String TIFF_MIME_TYPE = "image/tiff";

    public static void main(String[] args) throws Throwable {
        int failures = 0;

        InputStream stream = new ByteArrayInputStream(LITTLE_ENDIAN_MAGIC);
        String contentType = URLConnection.guessContentTypeFromStream(stream);
        if (contentType == null || !contentType.equals(TIFF_MIME_TYPE)) {
            failures++;
            System.err.println("Test failed for little endian magic");
        }

        stream = new ByteArrayInputStream(BIG_ENDIAN_MAGIC);
        contentType = URLConnection.guessContentTypeFromStream(stream);
        if (contentType == null || !contentType.equals(TIFF_MIME_TYPE)) {
            failures++;
            System.err.println("Test failed for big endian magic");
        }

        if (failures != 0) {
            throw new RuntimeException
                ("Test failed with " + failures +  " error(s)");
        }
    }
}

