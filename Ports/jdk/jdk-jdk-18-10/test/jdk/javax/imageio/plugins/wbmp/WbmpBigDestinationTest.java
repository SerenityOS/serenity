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
 * @bug 4929367
 * @summary tests what BMP image was decoded correctly if destination buffered
 *          image is bigger than source image
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriter;

public class WbmpBigDestinationTest {
    static String format = "WBMP";
    public static void main(String[] args) {
        try {
            BufferedImage src = new BufferedImage(100, 100,
                                                  BufferedImage.TYPE_BYTE_BINARY);
            Graphics2D g = src.createGraphics();
            g.setColor(Color.white);
            g.fillRect(0,0,100, 100);

            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            ImageWriter iw =
                (ImageWriter)ImageIO.getImageWritersByFormatName(format).next();
            if (iw == null) {
                throw new RuntimeException("No writer available. Test failed.");
            }

            iw.setOutput(ImageIO.createImageOutputStream(baos));
            iw.write(src);

            byte[] data = baos.toByteArray();

            ImageReader ir =
                (ImageReader)ImageIO.getImageReadersByFormatName(format).next();
            ir.setInput(
                ImageIO.createImageInputStream(
                    new ByteArrayInputStream(data)));

            Iterator specifiers = ir.getImageTypes(0);
            ImageTypeSpecifier typeSpecifier = null;

            if (specifiers.hasNext()) {
                typeSpecifier = (ImageTypeSpecifier) specifiers.next();
            }
            ImageReadParam param = new ImageReadParam();
            BufferedImage dst = typeSpecifier.createBufferedImage(200, 200);
            param.setDestination(dst);

            ir.read(0, param);

            checkResults(src,dst);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Unexpected exception. Test failed.");
        }
    }

    private static void checkResults(BufferedImage src, BufferedImage dst) {
        for(int x=0; x<src.getWidth(); x++) {
            for(int y=0; y<src.getHeight(); y++) {
                int srcRgb = src.getRGB(x,y);
                int dstRgb = dst.getRGB(x,y);
                if (srcRgb != dstRgb) {
                    throw new RuntimeException("Images are different at point ["
                                               + x + "," + y + "]");
                }
            }
        }
    }
}
