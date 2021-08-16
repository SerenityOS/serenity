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

/*
 * @test
 * @bug     8190332
 * @summary Test verifies whether PNGImageReader throws IIOException
 *          or not when IHDR width value is very high.
 * @run     main PngLargeIHDRDimensionTest
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;

public class PngLargeIHDRDimensionTest {

    /*
     * IHDR width is very large and when we try to create buffer to store
     * image information of each row it overflows and we get
     * NegativeArraySizeException without the fix for this bug.
     */
    private static String negativeArraySizeExceptionInput = "iVBORw0KGgoAAAANS"
            + "UhEUg////0AAAABEAIAAAA6fptVAAAACklEQVQYV2P4DwABAQEAWk1v8QAAAAB"
            + "JRU5ErkJgggo=";

    /*
     * IHDR width is ((2 to the power of 31) - 2), which is the maximum VM
     * limit to create an array we get OutOfMemoryError without the fix
     * for this bug.
     */
    private static String outOfMemoryErrorInput = "iVBORw0KGgoAAAANSUhEUgAAAAF/"
            + "///+CAAAAAA6fptVAAAACklEQVQYV2P4DwABAQEAWk1v8QAAAABJRU5"
            + "ErkJgggo=";

    private static InputStream input;
    private static Boolean firstTestFailed = true, secondTestFailed = true;
    public static void main(String[] args) throws java.io.IOException {
        byte[] inputBytes = Base64.getDecoder().
                decode(negativeArraySizeExceptionInput);
        input = new ByteArrayInputStream(inputBytes);

        try {
            ImageIO.read(input);
        } catch (IIOException e) {
            firstTestFailed = false;
        }

        inputBytes = Base64.getDecoder().decode(outOfMemoryErrorInput);
        input = new ByteArrayInputStream(inputBytes);

        try {
            ImageIO.read(input);
        } catch (IIOException e) {
            secondTestFailed = false;
        }

        if (firstTestFailed || secondTestFailed) {
            throw new RuntimeException("Test doesn't throw required"
                    + " IIOException");
        }
    }
}

