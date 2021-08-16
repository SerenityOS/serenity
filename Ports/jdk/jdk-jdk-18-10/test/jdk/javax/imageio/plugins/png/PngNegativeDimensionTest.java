/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @bug     8190512
 * @summary Test verifies whether PNGImageReader throws IIOException with proper
 *          message or not when IHDR chunk contains negative value for width
 *          and height.
 * @run     main PngNegativeDimensionTest
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.ImageIO;

public class PngNegativeDimensionTest {

    private static String negativeWidthString = "iVBORw0KGgoAAAANSUhEUoAAAAEAA"
            + "AABCAAAAAA6fptVAAAACklEQVQYV2P4DwABAQEAWk1v8QAAAABJRU5ErkJgggo=";

    private static String negativeHeightString = "iVBORw0KGgoAAAANSUhEUgAAAAGAA"
            + "AABCAAAAAA6fptVAAAACklEQVQYV2P4DwABAQEAWk1v8QAAAABJRU5ErkJgggo=";

    private static InputStream input;
    private static Boolean failed = false;

    public static void main(String[] args) {
        // Create InputStream
        byte[] inputBytes = Base64.getDecoder().decode(negativeWidthString);
        input = new ByteArrayInputStream(inputBytes);
        // Attempt to read PNG with negative IHDR width
        readNegativeIHDRWidthImage();

        inputBytes = Base64.getDecoder().decode(negativeHeightString);
        input = new ByteArrayInputStream(inputBytes);
        // Attempt to read PNG with negative IHDR height
        readNegativeIHDRHeightImage();

        if (failed) {
            throw new RuntimeException("Test didnt throw proper IIOException"
                    + " when IHDR width/height is negative");
        }
    }

    private static void readNegativeIHDRWidthImage() {
        try {
            ImageIO.read(input);
        } catch (Exception e) {
            /*
             * We expect the test case to throw IIOException with message
             * under root cause as "Image width <= 0!". If it throws
             * any other message or exception test will fail.
             */
            Throwable cause = e.getCause();
            if (cause == null ||
                (!(cause.getMessage().equals("Image width <= 0!"))))
            {
                failed = true;
            }
        }
    }

    private static void readNegativeIHDRHeightImage() {
        try {
            ImageIO.read(input);
        } catch (Exception e) {
            /*
             * We expect the test case to throw IIOException with message
             * under root cause as "Image height <= 0!". If it throws
             * any other message or exception test will fail.
             */
            Throwable cause = e.getCause();
            if (cause == null ||
                (!(cause.getMessage().equals("Image height <= 0!"))))
            {
                failed = true;
            }
        }
    }
}

