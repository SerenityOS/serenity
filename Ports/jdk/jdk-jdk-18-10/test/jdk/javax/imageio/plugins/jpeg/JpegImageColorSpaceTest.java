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
 * @bug     8041501
 * @summary Test verifies if there is no JFIF & EXIF header
 *          and sampling factor is same of JPEG image, then
 *          imageIO should not override colorspace determined
 *          in IJG library.
 * @run     main JpegImageColorSpaceTest
 */

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class JpegImageColorSpaceTest {

    public static void main(String args[]) throws Exception {

        String fileName = "nomarkers.jpg";
        String sep = System.getProperty("file.separator");
        String dir = System.getProperty("test.src", ".");
        String filePath = dir+sep+fileName;
        System.out.println("Test file: " + filePath);
        File imageFile = new File(filePath);

        BufferedImage bufferedImage = ImageIO.read(imageFile);
        int imageWidth = bufferedImage.getWidth();
        int imageHeight = bufferedImage.getHeight();

        for (int i = 0; i < imageWidth; i++) {
            for(int j = 0; j < imageHeight; j++) {
                /*
                * Since image is white we check individual pixel values from
                * BufferedImage to verify if ImageIO.read() is done with proper
                * color space or not.
                */
                if (bufferedImage.getRGB(i, j) != Color.white.getRGB()) {
                    // color space is not proper
                    throw new RuntimeException("ColorSpace is not determined "
                            + "properly by ImageIO");
               }
            }
        }
    }
}
