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
 * @bug     8190997
 * @summary Test verifies that ImageIO.read() throws proper IIOException
 *          when we have a PNG image with color type PNG_COLOR_PALETTE but
 *          missing the required PLTE chunk.
 * @run     main PngPLTEChunkMissingTest
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;

public class PngPLTEChunkMissingTest {

    // PNG image stream missing the required PLTE chunk
    private static String inputImageBase64 = "iVBORw0KGgoAAAANSUhEUgAAAAEAAAAB"
            + "CAMAAAA6fptVAAAACklEQVQYV2P4DwABAQEAWk1v8QAAAABJRU5ErkJgggo=";

    public static void main(String[] args) throws Exception {

        byte[] inputBytes = Base64.getDecoder().decode(inputImageBase64);
        InputStream in = new ByteArrayInputStream(inputBytes);

        /*
         * Attempt to read a PNG image of color type PNG_COLOR_PALETTE
         * but missing the required PLTE chunk.
         */
        try {
            ImageIO.read(in);
        } catch (IIOException e) {
            /*
             * We expect ImageIO to throw IIOException with proper message
             * instead of throwing NullPointerException.
             */
            Throwable cause = e.getCause();
            if (cause == null ||
                (!(cause.getMessage().
                 equals("Required PLTE chunk missing"))))
            {
                throw e;
            }
        }
    }
}

