/*
 * Copyright (c) 2016, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug     8139183
 * @summary Test verifies whether alpha channel of a translucent
 *          image is proper or not after scaling through drawImage.
 * @run     main ScaledImageAlphaTest
 */

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;

public class ScaledImageAlphaTest {

    static final int translucentAlpha = 128, opaqueAlpha = 255;
    static final int[] translucentVariants = new int[] {
        BufferedImage.TYPE_INT_ARGB,
        BufferedImage.TYPE_INT_ARGB_PRE,
        BufferedImage.TYPE_4BYTE_ABGR,
        BufferedImage.TYPE_4BYTE_ABGR_PRE
    };
    static final int[] alphaValues = new int[] {
        translucentAlpha,
        opaqueAlpha
    };
    static int width = 50, height = 50;
    static int scaleX = 5, scaleY = 5, scaleWidth = 40, scaleHeight = 40;

    private static void verifyAlpha(Color color, int alpha) {

        /* if extracted alpha value is equal alpha that we set
         * for background color, alpha channel is not lost
         * while scaling otherwise we have lost alpha channel.
         */
        int extractedAlpha = color.getAlpha();

        if (extractedAlpha != alpha) {
            throw new RuntimeException("Alpha channel for background"
                    + " is lost while scaling");
        }
    }

    private static void validateBufferedImageAlpha() {

        Color backgroundColor, extractedColor;
        // verify for all translucent buffered image types
        for (int type : translucentVariants) {
            // verify for both opaque and translucent background color
            for (int alpha : alphaValues) {
                // create BufferedImage of dimension (50,50)
                BufferedImage img = new
                    BufferedImage(width, height, type);
                Graphics2D imgGraphics = (Graphics2D)img.getGraphics();
                /* scale image to smaller dimension and set any
                 * background color with alpha.
                 */
                backgroundColor = new Color(0, 255, 0, alpha);
                imgGraphics.
                    drawImage(img, scaleX, scaleY, scaleWidth, scaleHeight,
                              backgroundColor, null);
                imgGraphics.dispose();

                /* get pixel information for background color with
                 * scaled coordinates.
                 */
                extractedColor = new Color(img.getRGB(scaleX, scaleY), true);
                verifyAlpha(extractedColor, alpha);
            }
        }
    }

    private static void validateVolatileImageAlpha() {

        Color backgroundColor, extractedColor;
        VolatileImage img;
        BufferedImage bufImg = new
                    BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int alpha : alphaValues) {
            backgroundColor = new Color(0, 255, 0, alpha);
            do {
                img = createVolatileImage(width, height,
                                          Transparency.TRANSLUCENT);
                Graphics2D imgGraphics = (Graphics2D)img.getGraphics();
                // clear VolatileImage as by default it has white opaque image
                imgGraphics.setComposite(AlphaComposite.Clear);
                imgGraphics.fillRect(0,0, width, height);

                imgGraphics.setComposite(AlphaComposite.SrcOver);
                /* scale image to smaller dimension and set background color
                 * to green with translucent alpha.
                 */
                imgGraphics.
                    drawImage(img, scaleX, scaleY, scaleWidth, scaleHeight,
                              backgroundColor, null);
                //get BufferedImage out of VolatileImage
                bufImg = img.getSnapshot();
                imgGraphics.dispose();
            } while (img.contentsLost());

            /* get pixel information for background color with
             * scaled coordinates.
             */
            extractedColor = new Color(bufImg.getRGB(scaleX, scaleY), true);
            verifyAlpha(extractedColor, alpha);
        }
    }

    private static VolatileImage createVolatileImage(int width, int height,
                                                     int transparency) {
        GraphicsEnvironment ge = GraphicsEnvironment.
                                 getLocalGraphicsEnvironment();
        GraphicsConfiguration gc = ge.getDefaultScreenDevice().
                                   getDefaultConfiguration();

        VolatileImage image = gc.createCompatibleVolatileImage(width, height,
                                                               transparency);
        return image;
    }

    public static void main(String[] args) {
        // test alpha channel with different types of BufferedImage
        validateBufferedImageAlpha();
        // test alpha channel with VolatileImage
        validateVolatileImageAlpha();
    }
}
