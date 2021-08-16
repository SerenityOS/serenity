/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8041465
 * @summary Test verifies that standard ImageIO plugins handle
 *          multiple reads correctly.
 *
 * @run     main MultiReadTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

public class MultiReadTest {

    private static final File pwd = new File(".");

    private static final Color srcColor = Color.red;
    private static final int width = 200;
    private static final int height = 200;

    private static final int max = 5;

    public static void main(String[] args) throws IOException {
        final String[] formats = { "bmp", "png", "gif", "jpg", "tif" };

        for (String f : formats) {
            test(f);
        }
    }

    private static void test(String format) throws IOException {
        System.out.println("Format: " + format);

        BufferedImage src = createSrc();

        ImageInputStream iis = prepareInput(src, format);

        ImageReader reader = ImageIO.getImageReaders(iis).next();

        reader.setInput(iis);

        ImageReadParam p = reader.getDefaultReadParam();
        int cnt = 0;
        do {
            System.out.println("cnt: " + cnt);
            p.setSourceRegion(new Rectangle(width / 4, height / 4,
                                            width / 2, height / 2));

            BufferedImage dst = reader.read(0, p);

            final Color c = new Color(dst.getRGB(10, 10), true);

            if (!sameColor(c, srcColor)) {
                throw new RuntimeException(
                    String.format("Test failed: read color 0x%X\n",
                                  c.getRGB()));
            }
        } while (++cnt < max);
    }

    private static boolean sameColor(Color c1, Color c2) {
        final float delta = 0.1f;

        float[] rgb1 = new float[4];
        float[] rgb2 = new float[4];

        rgb1 = c1.getRGBComponents(rgb1);
        rgb2 = c2.getRGBComponents(rgb2);

        for (int i = 0; i < rgb1.length; i++) {
            if (Math.abs(rgb1[i] - rgb2[i]) > delta) {
                return false;
            }
        }
        return true;
    }

    private static BufferedImage createSrc() {
        BufferedImage img = new BufferedImage(width, height,
                BufferedImage.TYPE_INT_RGB);

        Graphics2D g = img.createGraphics();
        g.setColor(srcColor);
        g.fillRect(0, 0, width, height);
        g.dispose();

        return img;
    }

    private static ImageInputStream prepareInput(BufferedImage src, String format) throws IOException {
        File f = File.createTempFile("src_", "." + format, pwd);

        if (ImageIO.write(src, format, f)) {
            return ImageIO.createImageInputStream(f);
        }
        return null;
    }
}
