/*
 * Copyright (c) 2001, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 4405224 8012229
 * @summary Test color conversion for images with alpha
 */

import java.awt.Color;
import java.awt.Transparency;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.SampleModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.DataBuffer;
import java.awt.color.ColorSpace;


public class ColCvtAlpha {

    public static void main(String args[]) {
        BufferedImage src
            = new BufferedImage(1, 10, BufferedImage.TYPE_INT_ARGB);

        // Set src pixel values
        Color pelColor = new Color(100, 100, 100, 128);
        for (int i = 0; i < 10; i++) {
            src.setRGB(0, i, pelColor.getRGB());
        }

        ColorModel cm = new ComponentColorModel
            (ColorSpace.getInstance(ColorSpace.CS_GRAY),
             new int [] {8,8}, true,
             src.getColorModel().isAlphaPremultiplied(),
             Transparency.TRANSLUCENT,
             DataBuffer.TYPE_BYTE);

        SampleModel sm = new PixelInterleavedSampleModel
            (DataBuffer.TYPE_BYTE, 100, 100, 2, 200,
             new int [] { 0, 1 });

        WritableRaster wr = Raster.createWritableRaster(sm, new Point(0,0));

        BufferedImage dst =
            new BufferedImage(cm, wr, cm.isAlphaPremultiplied(), null);
        dst = dst.getSubimage(0, 0, 1, 10);

        ColorConvertOp op = new ColorConvertOp(null);

        op.filter(src, dst);

        for (int i = 0; i < 10; i++) {
            int rgb = (dst.getRGB(0, i) >> 24) & 0xff;
            if (rgb != 128) {
                throw new RuntimeException(
                    "Incorrect destination alpha value: " + rgb);
            }
        }

    }
}
