/*
 * Copyright (c) 2003, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4826548
 * @summary Tests for reading PNG images with 5,6,7 and 8 colors in palette
 */

import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

public class ShortPaletteTest {

    public static void main(String[] args) {

        for (int numberColors = 2; numberColors <= 16; numberColors++) {
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                BufferedImage image = createImage(numberColors);
                ImageIO.write(image, "png", baos);
                baos.close();
                System.out.println("Number of colors: " + numberColors);
                byte[] buffer = baos.toByteArray();
                ByteArrayInputStream bais = new ByteArrayInputStream(buffer);
                ImageIO.read(bais);
                System.out.println("OK");
            } catch (ArrayIndexOutOfBoundsException e) {
                e.printStackTrace();
                throw new RuntimeException("Test failed.");
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Unexpected exception was thrown."
                                           + " Test failed.");
            }
        }
    }

    private static IndexColorModel createColorModel(int numberColors) {

        byte[] colors = new byte[numberColors*3];
        int depth = 4;
        int startIndex = 0;

        return new IndexColorModel(depth,
                                   numberColors,
                                   colors,
                                   startIndex,
                                   false);
    }

    private static BufferedImage createImage(int numberColors) {
        return new BufferedImage(32,
                                 32,
                                 BufferedImage.TYPE_BYTE_BINARY,
                                 createColorModel(numberColors));
    }
}
