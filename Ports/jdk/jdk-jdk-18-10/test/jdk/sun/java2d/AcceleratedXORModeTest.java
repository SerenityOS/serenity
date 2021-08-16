/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @key headful
 * @bug     8024343 8042098
 * @summary Test verifies that accelerated pipelines
 *          correctly draws primitives in XOR mode.
 * @run main/othervm -Dsun.java2d.xrender=True AcceleratedXORModeTest
 */

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

public class AcceleratedXORModeTest {
    public static void main(String argv[]) {
        String fileName = argv.length > 0 ? argv[0] : null;
        new AcceleratedXORModeTest(fileName).test();
    }

    static final Color backColor = Color.red;
    static final Color color1 = Color.green;
    static final Color color2 = Color.yellow;
    static final Color xorColor1 = Color.blue;
    static final Color xorColor2 = Color.white;

    static final int width = 700, height = 300;

    VolatileImage vImg = null;
    String fileName;

    public AcceleratedXORModeTest(String fileName) {
        this.fileName = fileName;
    }

    void draw(Graphics2D g) {
        g.setColor(backColor);
        g.fillRect(0, 0, width, height);
        g.setXORMode(xorColor1);
        drawPattern(g, 100);
        g.setXORMode(xorColor2);
        drawPattern(g, 400);
        g.dispose();
    }

    void test(BufferedImage bi) {
        comparePattern(bi, 150, xorColor1.getRGB());
        comparePattern(bi, 450, xorColor2.getRGB());
    }

    void comparePattern(BufferedImage bi, int startX, int xorColor) {
        int[] expectedColors = {
            backColor.getRGB() ^ color1.getRGB() ^ xorColor,
            backColor.getRGB() ^ color1.getRGB() ^ xorColor ^
                color2.getRGB() ^ xorColor,
            backColor.getRGB() ^ color2.getRGB() ^ xorColor
        };
        for (int i = 0; i < 3; i++) {
            int x = startX + 100 * i;
            int rgb = bi.getRGB(x, 150);
            if (rgb != expectedColors[i]) {
                String msg = "Colors mismatch: x = " + x +
                        ", got " + new Color(rgb) + ", expected " +
                        new Color(expectedColors[i]);
                System.err.println(msg);
                write(bi);
                throw new RuntimeException("FAILED: " + msg);
            }
        }
    }

    void drawPattern(Graphics2D g, int x) {
        g.setColor(color1);
        g.fillRect(x, 0, 200, 300);
        g.setColor(color2);
        g.fillRect(x+100, 0, 200, 300);
    }

    GraphicsConfiguration getDefaultGC() {
        return GraphicsEnvironment.getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration();
    }

    void createVImg() {
        if (vImg != null) {
            vImg.flush();
            vImg = null;
        }
        vImg = getDefaultGC().createCompatibleVolatileImage(width, height);
    }

    void write(BufferedImage bi) {
        if (fileName != null) {
            try {
                ImageIO.write(bi, "png", new File(fileName));
            } catch (IOException e) {
                System.err.println("Can't write image file " + fileName);
            }
        }
    }

    void test() {
        createVImg();
        BufferedImage bi = null;
        do {
            int valCode = vImg.validate(getDefaultGC());
            if (valCode == VolatileImage.IMAGE_INCOMPATIBLE) {
                createVImg();
            }
            Graphics2D g = vImg.createGraphics();
            draw(g);
            bi = vImg.getSnapshot();
        } while (vImg.contentsLost());
        if (bi != null) {
            test(bi);
            write(bi);
        }
    }
}
