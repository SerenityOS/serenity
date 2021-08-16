/*
 * Copyright (c) 2005, 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6275211 6276621
 * @summary Tests that GIF writer plugin is able to write indexed images if
 *          palette size is not a power of two
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

public class OddPaletteTest {

    private static int w = 100;
    private static int h = 100;

    public static void main(String[] args) {
        BufferedImage[] srcs = new BufferedImage[2];
        srcs[0] = createTestImage(7); // bug 6275211
        srcs[1] = createTestImage(1); // bug 6276621

        for (int i = 0; i < srcs.length; i++) {
            doTest(srcs[i]);
        }
    }

    private static void doTest(BufferedImage src) {
        ImageWriter w = ImageIO.getImageWritersByFormatName("GIF").next();
        if (w == null) {
            throw new RuntimeException("No writer available!");
        }

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            w.setOutput(ios);
            w.write(src);
        } catch (IOException e) {
            throw new RuntimeException("Test failed.", e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException("Test failed.", e);
        }
    }

    private static BufferedImage createTestImage(int paletteSize) {
        byte[] r = new byte[paletteSize];
        byte[] g = new byte[paletteSize];
        byte[] b = new byte[paletteSize];

        int shift = 256 / paletteSize;
        for (int i = 0; i < paletteSize; i++) {
            r[i] = g[i] = b[i] = (byte)(shift * i);
        }

        int numBits = getNumBits(paletteSize);

        System.out.println("num of bits " + numBits);

        IndexColorModel icm =
            new IndexColorModel(numBits, paletteSize,  r, g, b);

        BufferedImage img = new BufferedImage(w, h,
                                              BufferedImage.TYPE_BYTE_INDEXED,
                                              icm);
        Graphics2D  g2d = img.createGraphics();
        g2d.setColor(Color.white);
        g2d.fillRect(0, 0, w, h);
        g2d.setColor(Color.black);
        g2d.drawLine(0, 0, w, h);
        g2d.drawLine(0, h, w, 0);

        return img;
    }

    private static int getNumBits(int paletteSize) {
        if (paletteSize < 0) {
            throw new IllegalArgumentException("negative palette size: " +
                                               paletteSize);
        }
        if (paletteSize < 2) {
            return 1;
        }
        int numBits = 0;

        paletteSize--;

        while (paletteSize > 0) {
            numBits++;
            paletteSize = paletteSize >> 1;
        }
        return numBits;
    }
}
