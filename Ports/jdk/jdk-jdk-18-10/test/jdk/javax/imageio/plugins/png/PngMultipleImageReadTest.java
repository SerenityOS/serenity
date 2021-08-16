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
 * @bug     8191431
 * @summary Test verifies that whether we can use same PNGImageReader instance
 *          to read multiple images or not. It also verifies whether
 *          imageStartPosition in PNGImageReader is updated properly when we
 *          use same PNGImageReader instance to read multiple images.
 * @run     main PngMultipleImageReadTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class PngMultipleImageReadTest {

    private static final ImageReader PNG_READER =
            ImageIO.getImageReadersByMIMEType("image/png").next();

    public static void main(String[] args) throws IOException {

        /*
         * First we create a PNG image without palette so that the IDAT
         * start position in the stream is at some position 'x'.
         */
        BufferedImage imageWithoutPalette =
                new BufferedImage(20, 20, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g1 = imageWithoutPalette.createGraphics();
        g1.setColor(Color.WHITE);
        g1.fillRect(0, 0, 20, 20);
        g1.dispose();
        // write and read the image without palette
        writeAndReadImage(imageWithoutPalette);

        /*
         * We create another PNG image with PLTE(palette) chunk so that
         * now the IDAT start position is at some 'x + y'.
         */
        IndexColorModel cm = new IndexColorModel(
                3,
                1,
                new byte[]{10}, // r
                new byte[]{10}, // g
                new byte[]{10}); // b
        BufferedImage imageWithPalette = new BufferedImage(
                10, 10,
                BufferedImage.TYPE_BYTE_INDEXED,
                cm);
        Graphics2D g2 = imageWithPalette.createGraphics();
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, 10, 10);
        g2.dispose();
        // write and read the image with palette
        writeAndReadImage(imageWithPalette);
    }

    private static void writeAndReadImage(BufferedImage image)
            throws IOException {
        File output = File.createTempFile("output", ".png");
        ImageInputStream stream = null;
        try {
            ImageIO.write(image, "png", output);

            stream = ImageIO.createImageInputStream(output);
            ImageReadParam param = PNG_READER.getDefaultReadParam();
            PNG_READER.setInput(stream, true, true);
            PNG_READER.read(0, param);
        } finally {
            if (stream != null) {
                stream.close();
            }
            Files.delete(output.toPath());
        }
    }
}

