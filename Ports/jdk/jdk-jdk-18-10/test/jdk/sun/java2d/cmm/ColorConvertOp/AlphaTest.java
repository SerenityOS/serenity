/*
 * Copyright (c) 2013, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     8005930
 * @summary Thest verifies that color conversion does not distort
 *          alpha channel in the destination image.
 *
 * @run     main AlphaTest
 */

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

public class AlphaTest {
    public static void main(String[] args) {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);

        ColorConvertOp op = new ColorConvertOp(cs, null);
        // create source image filled with an opaque color
        BufferedImage src = createSrc();
        int srcAlpha = getAlpha(src);

        System.out.printf("Src alpha: 0x%02x\n", srcAlpha);

        // create clear (transparent black) destination image
        BufferedImage dst = createDst();
        int dstAlpha = getAlpha(dst);
        System.out.printf("Dst alpha: 0x%02x\n", dstAlpha);


        dst = op.filter(src, dst);
        dstAlpha = getAlpha(dst);
        // we expect that destination image is opaque
        // i.e. alpha is transferred from source to
        // the destination
        System.out.printf("Result alpha: 0x%02x\n", dstAlpha);

        if (srcAlpha != dstAlpha) {
            throw new RuntimeException("Test failed!");
        }
        System.out.println("Test passed");
    }

    private static BufferedImage createSrc() {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();
        g.setColor(Color.red);
        g.fillRect(0, 0, w, h);
        g.dispose();

        return img;
    }

    private static BufferedImage createDst() {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = img.createGraphics();
        g.setComposite(AlphaComposite.Clear);
        g.fillRect(0, 0, w, h);
        g.dispose();

        return img;
    }

    private static int getAlpha(BufferedImage img) {
        int argb = img.getRGB(w / 2, h / 2);
        return 0xff & (argb >> 24);
    }

    private static final int w = 100;
    private static final int h = 100;
}
