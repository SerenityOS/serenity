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
 * @bug     8191174
 * @summary Test verifies that PNGImageReader doesn't throw any undocumented
 *          Exception because of faulty calculation of scanline stride in
 *          PNGImageReader.
 * @run     main PngReaderLargeWidthStrideTest
 */

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Base64;
import javax.imageio.IIOException;
import javax.imageio.ImageIO;

public class PngReaderLargeWidthStrideTest {

    private static String inputImageBase64 = "iVBORw0KGgoAAAANSUhEUk////8AAAA"
            + "BCAAAAAA6fptVAAAACklEQVQYV2P4DwABAQEAWk1v8QAAAABJRU5ErkJgggo=";

    public static void main(String[] args) throws Exception {

        byte[] inputBytes = Base64.getDecoder().decode(inputImageBase64);
        InputStream in = new ByteArrayInputStream(inputBytes);

        try {
            ImageIO.read(in);
        } catch (IIOException e) {
            // Do nothing we expect it to throw IIOException, but if it throws
            // any other exception test will fail.
        }
    }
}

