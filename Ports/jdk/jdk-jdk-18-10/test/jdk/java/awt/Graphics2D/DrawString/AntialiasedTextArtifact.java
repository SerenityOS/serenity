/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 8015070
 * @summary Tests for artifacts around the edges of anti-aliased text
 *          drawn over translucent background color.
 */
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;

public class AntialiasedTextArtifact {
    /* Image dimensions */
    private static final int TEST_IMAGE_WIDTH  = 2800;
    private static final int TEST_IMAGE_HEIGHT = 100;
    private static final String TEST_STRING    =
        "The quick brown fox jumps over the lazy dog. 0123456789.";

    /*
     * The artifacts appear when text is drawn ontop of translucent
     * background. In other words, a background with alpha channel.
     * Hence we test the algorithm for image types that contain either
     * straight alpha channel or pre-multiplied alpha channel. In
     * addition we test the images with other common pixel formats.
     */
    private static final int[] TYPES = {BufferedImage.TYPE_INT_ARGB,
                                        BufferedImage.TYPE_INT_ARGB_PRE,
                                        BufferedImage.TYPE_4BYTE_ABGR,
                                        BufferedImage.TYPE_4BYTE_ABGR_PRE,
                                        BufferedImage.TYPE_INT_RGB,
                                        BufferedImage.TYPE_INT_BGR,
                                        BufferedImage.TYPE_3BYTE_BGR};

    public static void main(String[] args) throws IOException {
        /* Iterate over different image types */
        for (int type : TYPES) {
            BufferedImage testImg = getBufferedImage(type);

            /* Draw anti-aliased string and check for artifacts */
            drawAntialiasedString(testImg);
            checkArtifact(testImg);
        }
    }

    private static BufferedImage getBufferedImage(int imageType) {
        /* Create a Graphics2D object from the given image type */
        BufferedImage image = new BufferedImage(TEST_IMAGE_WIDTH,
                                                TEST_IMAGE_HEIGHT,
                                                imageType);
        return image;
    }

    private static void drawAntialiasedString(BufferedImage image) {
        /* Create Graphics2D object */
        Graphics2D graphics = (Graphics2D) image.getGraphics();

        /* Fill the image with translucent color */
        graphics.setColor(new Color(127, 127, 127, 127));
        graphics.fillRect(0, 0, TEST_IMAGE_WIDTH, TEST_IMAGE_HEIGHT);

        /* Drawstring with Antialiasing hint */
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                                  RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        Font font = new Font("Verdana" , Font.PLAIN, 60);
        graphics.setFont(font);
        graphics.setColor(new Color(255, 0, 0));
        graphics.drawString(TEST_STRING, 10, 75);
        graphics.dispose();
    }

    private static void checkArtifact(BufferedImage image) throws IOException {
        int componentMask   = 0xff;
        int colorThreshold  = 200;
        int rowIndex = 0;
        int colIndex = 0;

        /* Loop through every pixel to check for possible artifact */
        for (rowIndex = 0; rowIndex < image.getHeight(); rowIndex++) {
            for (colIndex = 0; colIndex < image.getWidth(); colIndex++) {
                /*
                 * API: getRGB(x,y) returns color in INT_ARGB color space.
                 * Extract individual color components with a simple mask.
                 */
                int colorValue = image.getRGB(colIndex, rowIndex);
                int colorComponent1 = colorValue & componentMask;
                int colorComponent2 = (colorValue>>8) & componentMask;
                int colorComponent3 = (colorValue>>16) & componentMask;

                /*
                 * Artifacts are predominantly a subjective decision based on
                 * the quality of the rendered image content. However, in the
                 * current use-case, the artifacts around the edges of the anti
                 * aliased text appear like spots of white pixels without any
                 * relation to the color of foreground text or the background
                 * translucent shape.
                 *
                 * To identify the artifact pixels, each color component from
                 * the testImage is compared with a constant threshold. The
                 * component threshold has been set based on observation from
                 * different experiments on mulitple Java versions.
                 */
                if (colorComponent1 >= colorThreshold
                        && colorComponent2 >= colorThreshold
                        && colorComponent3 >= colorThreshold) {
                    /* Artifact has been noticed. Report error. */
                    throw new RuntimeException("Test Failed.");
                }
            }
        }
    }
}
