/*
 * Copyright (c) 2007, 2018, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6613860 6691934 8198613
 * @summary Tests that the pipelines can handle (in somewhat limited
 * manner) mutable Colors
 *
 * @run main/othervm MutableColorTest
 * @run main/othervm -Dsun.java2d.noddraw=true MutableColorTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Transparency;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class MutableColorTest {

    static Image bmImage;
    static Image argbImage;

    static class EvilColor extends Color {
        Color colors[] = { Color.red, Color.green, Color.blue };
        int currentIndex = 0;
        EvilColor() {
            super(Color.red.getRGB());
        }

        @Override
        public int getRGB() {
            return colors[currentIndex].getRGB();
        }
        void nextColor() {
            currentIndex++;
        }
    }

    private static int testImage(Image im,
                                 boolean doClip, boolean doTx)
    {
        int w = im.getWidth(null);
        int h = im.getHeight(null);
        Graphics2D g = (Graphics2D)im.getGraphics();
        EvilColor evilColor = new EvilColor();
        g.setColor(evilColor);
        g.fillRect(0, 0, w, h);
        g.dispose();

        evilColor.nextColor();

        g = (Graphics2D)im.getGraphics();

        if (doTx) {
            g.rotate(Math.PI/2.0, w/2, h/2);
        }
        g.setColor(evilColor);
        g.fillRect(0, 0, w, h);
        if (doClip) {
            g.clip(new Ellipse2D.Float(0, 0, w, h));
        }
        g.fillRect(0, h/3, w, h/3);

        // tests native BlitBg loop
        g.drawImage(bmImage, 0, 2*h/3, evilColor, null);
        // tests General BlitBg loop
        g.drawImage(argbImage, 0, 2*h/3+h/3/2, evilColor, null);

        return evilColor.getRGB();
    }

    private static void testResult(final String desc,
                                   final BufferedImage snapshot,
                                   final int evilColor) {
        for (int y = 0; y < snapshot.getHeight(); y++) {
            for (int x = 0; x < snapshot.getWidth(); x++) {
                int snapRGB = snapshot.getRGB(x, y);
                if (!isSameColor(snapRGB, evilColor)) {
                    System.err.printf("Wrong RGB for %s at (%d,%d): 0x%x " +
                        "instead of 0x%x\n", desc, x, y, snapRGB, evilColor);
                    String fileName = "MutableColorTest_"+desc+".png";
                    try {
                        ImageIO.write(snapshot, "png", new File(fileName));
                        System.err.println("Dumped snapshot to "+fileName);
                    } catch (IOException ex) {}
                    throw new RuntimeException("Test FAILED.");
                }
            }
        }
    }

    public static void main(String[] args) {
        GraphicsConfiguration gc =
            GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration();

        bmImage = gc.createCompatibleImage(64, 64, Transparency.BITMASK);
        argbImage = gc.createCompatibleImage(64, 64, Transparency.TRANSLUCENT);

        if (gc.getColorModel().getPixelSize() > 8) {
            VolatileImage vi =
                gc.createCompatibleVolatileImage(64, 64, Transparency.OPAQUE);
            do {
                if (vi.validate(gc) == VolatileImage.IMAGE_INCOMPATIBLE) {
                    vi = gc.createCompatibleVolatileImage(64, 64,
                                                          Transparency.OPAQUE);
                    vi.validate(gc);
                }

                int color = testImage(vi, false, false);
                testResult("vi_noclip_notx", vi.getSnapshot(), color);

                color = testImage(vi, true, true);
                testResult("vi_clip_tx", vi.getSnapshot(), color);

                color = testImage(vi, true, false);
                testResult("vi_clip_notx", vi.getSnapshot(), color);

                color = testImage(vi, false, true);
                testResult("vi_noclip_tx", vi.getSnapshot(), color);
            } while (vi.contentsLost());
        }

        BufferedImage bi = new BufferedImage(64, 64, BufferedImage.TYPE_INT_RGB);
        int color = testImage(bi, false, false);
        testResult("bi_noclip_notx", bi, color);

        color = testImage(bi, true, true);
        testResult("bi_clip_tx", bi, color);

        color = testImage(bi, true, false);
        testResult("bi_clip_notx", bi, color);

        color = testImage(bi, false, true);
        testResult("bi_noclip_tx", bi, color);

        System.err.println("Test passed.");
    }

    /*
     * We assume that colors with slightly different components
     * are the same. This is done just in order to workaround
     * peculiarities of OGL rendering pipeline on some platforms.
     * See CR 6989217 for more details.
     */
     private static boolean isSameColor(int color1, int color2) {
        final int tolerance = 2;

        for (int i = 0; i < 32; i += 8) {
            int c1 = 0xff & (color1 >> i);
            int c2 = 0xff & (color2 >> i);

            if (Math.abs(c1 - c2) > tolerance) {
                return false;
            }
        }
        return true;
    }
}
