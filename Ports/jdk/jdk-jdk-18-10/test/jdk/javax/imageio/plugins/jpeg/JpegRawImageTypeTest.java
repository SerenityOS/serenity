/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8143562
 * @summary Test verifies whether getRawImageType API returns proper raw
 *          image type when color space is of type YCbCr.
 * @run     main JpegRawImageTypeTest
 */

import java.io.File;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;

public class JpegRawImageTypeTest {

    public static void main(String[] args) throws Exception {

        //nomarkers.jpg has YCbCr color space
        String fileName = "nomarkers.jpg";
        String sep = System.getProperty("file.separator");
        String dir = System.getProperty("test.src", ".");
        String filePath = dir+sep+fileName;
        System.out.println("Test file: " + filePath);
        File imageFile = new File(filePath);

        ImageInputStream inputStream = ImageIO.
            createImageInputStream(imageFile);
        Iterator<ImageReader> readers = ImageIO.getImageReaders(inputStream);

        if(readers.hasNext()) {
            ImageReader reader = readers.next();
            reader.setInput(inputStream);

            ImageTypeSpecifier typeSpecifier = reader.getRawImageType(0);
            //check if ImageTypeSpecifier is null for YCbCr JPEG Image
            if (typeSpecifier == null) {
                throw new RuntimeException("ImageReader returns null raw image"
                    + " type");
            }
        }
    }
}
