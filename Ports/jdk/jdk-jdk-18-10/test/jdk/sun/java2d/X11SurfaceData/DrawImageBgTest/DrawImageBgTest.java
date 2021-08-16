/*
 * Copyright (c) 2007, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug 6603887
 * @summary Verifies that drawImage with bg color works correctly for ICM image
 * @run main/othervm DrawImageBgTest
 * @run main/othervm -Dsun.java2d.pmoffscreen=true DrawImageBgTest
 */
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.VolatileImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class DrawImageBgTest {
    public static void main(String[] args) {
        GraphicsConfiguration gc =
            GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration();

        if (gc.getColorModel().getPixelSize() <= 8) {
            System.out.println("8-bit color model, test considered passed");
            return;
        }

        /*
         * Set up images:
         * 1.) VolatileImge for rendering to,
         * 2.) BufferedImage for reading back the contents of the VI
         * 3.) The image triggering the problem
         */
        VolatileImage vImg = null;
        BufferedImage readBackBImg;

        // create a BITMASK ICM such that the transparent color is
        // tr. black (and it's the first in the color map so a buffered image
        // created with this ICM is transparent
        byte r[] = { 0x00, (byte)0xff};
        byte g[] = { 0x00, (byte)0xff};
        byte b[] = { 0x00, (byte)0xff};
        IndexColorModel icm = new IndexColorModel(8, 2, r, g, b, 0);
        WritableRaster wr = icm.createCompatibleWritableRaster(25, 25);
        BufferedImage tImg = new BufferedImage(icm, wr, false, null);

        do {
            if (vImg == null ||
                vImg.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE)
            {
                vImg = gc.createCompatibleVolatileImage(tImg.getWidth(),
                                                        tImg.getHeight());
            }

            Graphics viG = vImg.getGraphics();
            viG.setColor(Color.red);
            viG.fillRect(0, 0, vImg.getWidth(), vImg.getHeight());

            viG.drawImage(tImg, 0, 0, Color.green, null);
            viG.fillRect(0, 0, vImg.getWidth(), vImg.getHeight());
            viG.drawImage(tImg, 0, 0, Color.white, null);

            readBackBImg = vImg.getSnapshot();
        } while (vImg.contentsLost());

        for (int x = 0; x < readBackBImg.getWidth(); x++) {
            for (int y = 0; y < readBackBImg.getHeight(); y++) {
                int currPixel = readBackBImg.getRGB(x, y);
                if (currPixel != Color.white.getRGB()) {
                    String fileName = "DrawImageBgTest.png";
                    try {
                        ImageIO.write(readBackBImg, "png", new File(fileName));
                        System.err.println("Dumped image to " + fileName);
                    } catch (IOException ex) {}
                    throw new
                        RuntimeException("Test Failed: found wrong color: 0x"+
                                         Integer.toHexString(currPixel));
                }
            }
        }
        System.out.println("Test Passed.");
    }
}
