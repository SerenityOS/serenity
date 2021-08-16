/*
 * Copyright (c) 2009, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     4893408
 *
 * @summary Test verifies that Image I/O jpeg reader correctly handles
 *          destination types if number of color components in destination
 *          differs from number of color components in the jpeg image.
 *          Particularly, it verifies reading YCbCr image as a grayscaled
 *          and reading grayscaled jpeg as a RGB.
 *
 * @run     main ReadAsGrayTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.stream.ImageInputStream;
import static java.awt.image.BufferedImage.TYPE_3BYTE_BGR;
import static java.awt.image.BufferedImage.TYPE_BYTE_GRAY;
import static java.awt.color.ColorSpace.TYPE_GRAY;
import static java.awt.color.ColorSpace.CS_sRGB;

public class ReadAsGrayTest {
    static Color[] colors = new Color[] {
        Color.white, Color.red, Color.green,
        Color.blue, Color.black };

    static final int dx = 50;
    static final int h = 100;

    static ColorSpace sRGB = ColorSpace.getInstance(CS_sRGB);


    public static void main(String[] args) throws IOException {
        System.out.println("Type TYPE_BYTE_GRAY");
        doTest(TYPE_BYTE_GRAY);

        System.out.println("Type TYPE_3BYTE_BGR");
        doTest(TYPE_3BYTE_BGR);

        System.out.println("Test PASSED.");
    }

    private static void doTest(int type) throws IOException {
        BufferedImage src = createTestImage(type);

        File f = new File("test.jpg");

        if (!ImageIO.write(src, "jpg", f)) {
            throw new RuntimeException("Failed to write test image.");
        }

        ImageInputStream iis = ImageIO.createImageInputStream(f);
        ImageReader reader = ImageIO.getImageReaders(iis).next();
        reader.setInput(iis);

        Iterator<ImageTypeSpecifier> types = reader.getImageTypes(0);
        ImageTypeSpecifier srgb = null;
        ImageTypeSpecifier gray = null;
        // look for gray and srgb types
        while ((srgb == null || gray == null) && types.hasNext()) {
            ImageTypeSpecifier t = types.next();
            if (t.getColorModel().getColorSpace().getType() == TYPE_GRAY) {
                gray = t;
            }
            if (t.getColorModel().getColorSpace() == sRGB) {
                srgb = t;
            }
        }
        if (gray == null) {
            throw new RuntimeException("No gray type available.");
        }
        if (srgb == null) {
            throw new RuntimeException("No srgb type available.");
        }

        System.out.println("Read as GRAY...");
        testType(reader, gray, src);

        System.out.println("Read as sRGB...");
        testType(reader, srgb, src);
    }

    private static void testType(ImageReader reader,
                                 ImageTypeSpecifier t,
                                 BufferedImage src)
        throws IOException
    {
        ImageReadParam p = reader.getDefaultReadParam();
        p.setDestinationType(t);
        BufferedImage dst = reader.read(0, p);

        verify(src, dst, t);
    }

    private static void verify(BufferedImage src,
                               BufferedImage dst,
                               ImageTypeSpecifier type)
    {
        BufferedImage test =
                type.createBufferedImage(src.getWidth(), src.getHeight());
        Graphics2D g = test.createGraphics();
        g.drawImage(src, 0, 0, null);
        g.dispose();

        for (int i = 0; i < colors.length; i++) {
            int x = i * dx + dx / 2;
            int y = h / 2;

            Color c_test = new Color(test.getRGB(x, y));
            Color c_dst = new Color(dst.getRGB(x, y));

            if (!compareWithTolerance(c_test, c_dst, 0.01f)) {
                String msg = String.format("Invalid color: %x instead of %x",
                                           c_dst.getRGB(), c_test.getRGB());
                throw new RuntimeException("Test failed: " + msg);
            }
        }
        System.out.println("Verified.");
    }

    private static boolean compareWithTolerance(Color a, Color b, float delta) {
        float[] a_rgb = new float[3];
        a_rgb = a.getRGBColorComponents(a_rgb);
        float[] b_rgb = new float[3];
        b_rgb = b.getRGBColorComponents(b_rgb);

        for (int i = 0; i < 3; i++) {
            if (Math.abs(a_rgb[i] - b_rgb[i]) > delta) {
                return false;
            }
        }
        return true;
    }

    private static BufferedImage createTestImage(int type) {
        BufferedImage img = new BufferedImage(dx * colors.length, h, type);

        Graphics2D g = img.createGraphics();
        for (int i = 0; i < colors.length; i++) {
            g.setColor(colors[i]);
            g.fillRect(i * dx, 0, dx, h);
        }
        g.dispose();

        return img;
    }
}
