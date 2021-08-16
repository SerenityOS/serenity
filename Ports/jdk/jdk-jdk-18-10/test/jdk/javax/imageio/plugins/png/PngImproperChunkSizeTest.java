/*
 * Copyright (c) 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8191023
 * @summary Test verifies that PNGImageReader doesn't throw any undocumented
 *          Exception when we try to create byte array of negative size because
 *          when keyword length is more than chunk size.
 * @run     main PngImproperChunkSizeTest
 */

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;

public class PngImproperChunkSizeTest {

    private static ImageReader reader;

    private static String zTXTMalformedData = "iVBORw0KGgoAAAANSUhEUgAAAAEAAA" +
            "ABCAAAAAA6fptVAAAABHpUWHRhYWFhYWFhYQAAAApJREFUGFdj+A8AAQEBAFpNb" +
            "/EAAAAASUVORK5CYIIK";

    private static String tEXTMalformedData = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB"
            + "CAMAAAA6fptVAAAABHRFWHRhYWFhYWFhYQAAAApJREFUGFdj+A8AAQEBAFpNb"
            + "/EAAAAASUVORK5CYIIK";

    private static String iCCPMalformedData = "iVBORw0KGgoAAAANSUhEUgAAAAEAAA" +
            "ABCAAAAAA6fptVAAAABGlDQ1BhYWFhYWFhYQAAAApJREFUGFdj+A8AAQEBAFpNb" +
            "/EAAAAASUVORK5CYIIK";

    private static ByteArrayInputStream initializeInputStream(String input) {
        byte[] inputBytes = Base64.getDecoder().decode(input);
        return new ByteArrayInputStream(inputBytes);
    }

    private static Boolean readzTXTData(InputStream input) throws IOException {
        // Set input and mark ignoreMetadata = false
        reader.setInput(ImageIO.createImageInputStream(input), true, false);
        try {
            reader.read(0);
        } catch (IIOException e) {
            Throwable cause = e.getCause();
            if (cause == null ||
                !cause.getMessage().
                        equals("zTXt chunk length is not proper"))
            {
                return true;
            }
        }
        return false;
    }

    private static Boolean readtEXTData(InputStream input) throws IOException {
        // Set input and mark ignoreMetadata = false
        reader.setInput(ImageIO.createImageInputStream(input), true, false);
        try {
            reader.read(0);
        } catch (IIOException e) {
            Throwable cause = e.getCause();
            if (cause == null ||
                !cause.getMessage().
                        equals("tEXt chunk length is not proper"))
            {
                return true;
            }
        }
        return false;
    }

    private static Boolean readiCCPData(InputStream input) throws IOException {
        // Set input and mark ignoreMetadata = false
        reader.setInput(ImageIO.createImageInputStream(input), true, false);
        try {
            reader.read(0);
        } catch (IIOException e) {
            Throwable cause = e.getCause();
            if (cause == null ||
                !cause.getMessage().
                        equals("iCCP chunk length is not proper"))
            {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws java.io.IOException {
        reader = ImageIO.getImageReadersByFormatName("png").next();

        InputStream in = initializeInputStream(zTXTMalformedData);
        Boolean zTXTFailed = readzTXTData(in);

        in = initializeInputStream(tEXTMalformedData);
        Boolean tEXTFailed = readtEXTData(in);

        in = initializeInputStream(iCCPMalformedData);
        Boolean iCCPFailed = readiCCPData(in);

        reader.dispose();

        if (zTXTFailed || tEXTFailed || iCCPFailed) {
            throw new RuntimeException("Test didn't throw the required" +
                    " Exception");
        }
    }
}

