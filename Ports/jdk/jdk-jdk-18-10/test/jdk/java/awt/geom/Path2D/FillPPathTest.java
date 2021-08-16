/*
 * Copyright (c) 2007, Oracle and/or its affiliates. All rights reserved.
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
 * @bug 6599363
 * @summary Verifies correct handling of unclosed segments
 * @run main FillPPathTest
 */


import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;
import java.awt.color.*;

public class FillPPathTest {
    static final int IMG_WIDTH = 100;
    static final int IMG_HEIGHT = 100;

    private static BufferedImage createCustomImage() {
        ColorSpace cs = ColorSpace.getInstance(ColorSpace.CS_sRGB);
        ComponentColorModel cm =
            new ComponentColorModel(cs, false, false, Transparency.OPAQUE,
                                    DataBuffer.TYPE_FLOAT);
        WritableRaster raster =
            cm.createCompatibleWritableRaster(IMG_WIDTH, IMG_HEIGHT);
        return new BufferedImage(cm, raster, false, null);
    }

    public static void main(String[] args) throws Exception {

        Path2D oddShape = new Path2D.Double();
        oddShape.moveTo(50,10);
        oddShape.curveTo(50,10,10,20,50,30);
        oddShape.moveTo(50,30);

        BufferedImage img = new BufferedImage(IMG_WIDTH, IMG_HEIGHT,
                                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = img.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0,0,IMG_WIDTH,IMG_HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.fill(oddShape);

        if (img.getRGB(60, 20) != Color.BLACK.getRGB()) {
            throw new RuntimeException("Error. Invalid pixel at (60,20)");
        }

        img = createCustomImage();

        g2d = img.createGraphics();
        g2d.setColor(Color.BLACK);
        g2d.fillRect(0,0,IMG_WIDTH,IMG_HEIGHT);
        g2d.setColor(Color.WHITE);
        g2d.fill(oddShape);

        if (img.getRGB(60, 20) != Color.BLACK.getRGB()) {
            throw new RuntimeException("Error. Invalid pixel at (60,20)");
        }
    }
}
