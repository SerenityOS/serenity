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
 * @bug 8151269
 * @author a.stepanov
 * @summary Multiresolution image: check that the base resolution variant
 *          source is passed to the corresponding ImageConsumer
 * @run main MultiresolutionSourceTest
 */

import java.awt.*;
import java.awt.image.*;

public class MultiresolutionSourceTest {

    private static class Checker implements ImageConsumer {

        private final int refW, refH, refType;
        private final boolean refHasAlpha;
        private final Color refColor;

        public Checker(int     w,
                       int     h,
                       Color   c,
                       boolean hasAlpha,
                       int     transferType) {
            refW = w;
            refH = h;
            refColor = c;
            refHasAlpha = hasAlpha;
            refType = transferType;
        }

        @Override
        public void imageComplete(int status) {}

        @Override
        public void setColorModel(ColorModel model) {

            boolean a = model.hasAlpha();
            if (a != refHasAlpha) {
                throw new RuntimeException("invalid hasAlpha: " + a);
            }

            int tt = model.getTransferType();
            if (tt != refType) {
                throw new RuntimeException("invalid transfer type: " + tt);
            }
        }

        @Override
        public void setDimensions(int w, int h) {

            if (w != refW) { throw new RuntimeException("invalid width: " + w +
                ", expected: " + refW); }

            if (h != refH) { throw new RuntimeException("invalid height: " + h +
                ", expected: " + refH); }
        }

        @Override
        public void setHints(int flags) {}

        @Override
        public void setPixels(int x, int y, int w, int h, ColorModel model,
                              byte pixels[], int offset, int scansize) {

            for (int i = 0; i < pixels.length; i++) {
                int p = pixels[i];
                // just in case...
                Color c = model.hasAlpha() ?
                    new Color(model.getRed  (p),
                              model.getGreen(p),
                              model.getBlue (p),
                              model.getAlpha(p)) :
                    new Color(model.getRGB(p));

                if (!c.equals(refColor)) {
                    throw new RuntimeException("invalid color: " + c +
                        ", expected: " + refColor);
                }
            }
        }

        @Override
        public void setPixels(int x, int y, int w, int h, ColorModel model,
                              int pixels[], int offset, int scansize) {

            for (int i = 0; i < pixels.length; i++) {
                int p = pixels[i];
                Color c = model.hasAlpha() ?
                    new Color(model.getRed  (p),
                              model.getGreen(p),
                              model.getBlue (p),
                              model.getAlpha(p)) :
                    new Color(model.getRGB(p));

                if (!c.equals(refColor)) {
                    throw new RuntimeException("invalid color: " + c +
                        ", expected: " + refColor);
                }
            }
        }

        @Override
        public void setProperties(java.util.Hashtable props) {}
    }

    private static BufferedImage generateImage(int w, int h, Color c, int type) {

        BufferedImage img = new BufferedImage(w, h, type);
        Graphics g = img.getGraphics();
        g.setColor(c);
        g.fillRect(0, 0, w, h);
        return img;
    }

    public static void main(String[] args) {

        final int w1 = 20, w2 = 100, h1 = 30, h2 = 50;
        final Color
            c1 = new Color(255, 0, 0, 100), c2 = Color.BLACK, gray = Color.GRAY;

        BufferedImage img1 =
            generateImage(w1, h1, c1, BufferedImage.TYPE_INT_ARGB);

        BufferedImage dummy =
            generateImage(w1 + 5, h1 + 5, gray, BufferedImage.TYPE_BYTE_GRAY);

        BufferedImage img2 =
            generateImage(w2, h2, c2, BufferedImage.TYPE_BYTE_BINARY);

        BufferedImage vars[] = new BufferedImage[] {img1, dummy, img2};

        // default base image index (zero)
        BaseMultiResolutionImage mri1 = new BaseMultiResolutionImage(vars);
        // base image index = 2
        BaseMultiResolutionImage mri2 = new BaseMultiResolutionImage(2, vars);

        // do checks
        mri1.getSource().startProduction(
                new Checker(w1, h1, c1, true, DataBuffer.TYPE_INT));

        mri2.getSource().startProduction(
                new Checker(w2, h2, c2, false, DataBuffer.TYPE_BYTE));
    }
}
