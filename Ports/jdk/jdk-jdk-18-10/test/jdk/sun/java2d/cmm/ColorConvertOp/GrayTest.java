/*
 * Copyright (c) 2012, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     7124245
 * @summary Test verifies that color conversion does not distort
 *          colors in destination image of standard type.
 *
 * @run main GrayTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;

public class GrayTest {
    public static void main(String[] args) {
        GrayTest t = new GrayTest();

        t.doTest(BufferedImage.TYPE_INT_RGB);
        t.doTest(BufferedImage.TYPE_INT_BGR);
        t.doTest(BufferedImage.TYPE_INT_ARGB);
        t.doTest(BufferedImage.TYPE_3BYTE_BGR);
        t.doTest(BufferedImage.TYPE_4BYTE_ABGR);
        System.out.println("Test passed.");
    }

    private static final int w = 3;
    private static final int h = 3;

    private BufferedImage src;
    private BufferedImage dst;

    private ColorConvertOp op;

    public GrayTest() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_GRAY);
        op = new ColorConvertOp(cs, null);
    }

    private void render(Graphics2D g) {
        g.setColor(Color.red);
        g.fillRect(0, 0, w, h);
    }

    private BufferedImage initImage(int type) {
        BufferedImage img = new BufferedImage(w, h, type);
        Graphics2D g = img.createGraphics();

        render(g);

        g.dispose();

        return img;
    }

    public void doTest(int type) {
        System.out.println("Test for type: " + type);
        src = initImage(type);

        dst = initImage(type);

        dst = op.filter(src, dst);

        int pixel = dst.getRGB(1, 1);
        int r = 0xff & (pixel >> 16);
        int g = 0xff & (pixel >>  8);
        int b = 0xff & (pixel      );

        System.out.printf("dst: r:%02x, g: %02x, %02x\n",
                r, g, b);

        if (r != g || r != b) {
            String msg = String.format("Invalid pixel: %08x", pixel);
            throw new RuntimeException(msg);
        }
        System.out.println("Done.");
    }
}
