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
 * @bug 4339415
 * @summary Tests that GIF writer plugin is able to write non-index images
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Random;

import javax.imageio.ImageIO;
import javax.imageio.ImageWriter;

public class IndexingTest {

    protected static final String fname = "itest.gif";

    int w;
    int h;

    Random rnd;

    public IndexingTest() {
        w = h  = 200;
        rnd = new Random();
    }

    public void doTest() {
        ComponentColorModel ccm = createBitmaskColorModel();
        BufferedImage img = createComponentImage(w, h, ccm);

        try {
            ImageWriter w = ImageIO.getImageWritersByFormatName("GIF").next();
            w.setOutput(ImageIO.createImageOutputStream(new File(fname)));
            w.write(img);
        } catch (Exception e) {
            throw new RuntimeException("Test failed.", e);
        }

        BufferedImage dst = null;
        try {
            dst = ImageIO.read(new File(fname));
        } catch (Exception e) {
            throw new RuntimeException("Test failed.", e);
        }

        compareImages(img, dst);

        System.out.println("Test passed.");
    }

    protected static ComponentColorModel createBitmaskColorModel() {
        ComponentColorModel cm =
            new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
                                    true, false, Transparency.BITMASK,
                                    DataBuffer.TYPE_BYTE);
        return cm;
    }

    protected static BufferedImage createComponentImage(int w, int h,
                                                        ComponentColorModel cm)
    {
        WritableRaster wr = cm.createCompatibleWritableRaster(w, h);

        BufferedImage img = new BufferedImage(cm, wr, false, null);
        Graphics2D g = img.createGraphics();
        int width = w / 8;
        Color[] colors = new Color[8];
        colors[0] = Color.red;
        colors[1] = Color.green;
        colors[2] = Color.blue;
        colors[3] = Color.white;
        colors[4] = Color.black;
        colors[5] = new Color(0x80, 0x80, 0x80, 0x00);
        colors[6] = Color.yellow;
        colors[7] = Color.cyan;

        for (int i = 0; i < 8; i++) {
            g.setColor(colors[i]);
            g.fillRect(i * width, 0, width, h);
        }
        return img;
    }

    protected void compareImages(BufferedImage src, BufferedImage dst) {
        int n = 10;
        while (n-- > 0) {
            int x = rnd.nextInt(w);
            int y = rnd.nextInt(h);

            int pSrc = src.getRGB(x, y);
            int pDst = src.getRGB(x, y);

            if (pSrc != pDst) {
                throw new RuntimeException("Images are different");
            }
        }
    }

    public static void main(String[] args) {
        IndexingTest t = new IndexingTest();
        t.doTest();
    }
}
