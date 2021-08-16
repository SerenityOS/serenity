/*
 * Copyright (c) 2006, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.VolatileImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;

/**
 * @test
 * @key headful
 * @bug 6429665 6588884 8198613
 * @summary Tests that the transform is correctly handled
 * @author Dmitri.Trembovetski area=Graphics
 * @run main AcceleratedScaleTest
 * @run main/othervm -Dsun.java2d.d3d=true AcceleratedScaleTest
 */
public class AcceleratedScaleTest {
    private static final int IMAGE_SIZE = 200;
    private static VolatileImage destVI;

    private static void initVI(GraphicsConfiguration gc) {
        int res;
        if (destVI == null) {
            res = VolatileImage.IMAGE_INCOMPATIBLE;
        } else {
            res = destVI.validate(gc);
        }
        if (res == VolatileImage.IMAGE_INCOMPATIBLE) {
            if (destVI != null) destVI.flush();
            destVI = gc.createCompatibleVolatileImage(IMAGE_SIZE, IMAGE_SIZE);
            destVI.validate(gc);
            res = VolatileImage.IMAGE_RESTORED;
        }
        if (res == VolatileImage.IMAGE_RESTORED) {
            Graphics vig = destVI.getGraphics();
            vig.setColor(Color.red);
            vig.fillRect(0, 0, destVI.getWidth(), destVI.getHeight());
            vig.dispose();
        }
    }

    public static void main(String[] args) {
        Frame f = new Frame();
        f.pack();
        GraphicsConfiguration gc = f.getGraphicsConfiguration();
        if (gc.getColorModel().getPixelSize() < 16) {
            System.out.printf("Bit depth: %d . Test considered passed.",
                              gc.getColorModel().getPixelSize());
            f.dispose();
            return;
        }

        BufferedImage bi =
            new BufferedImage(IMAGE_SIZE/4, IMAGE_SIZE/4,
                              BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D)bi.getGraphics();
        g.setColor(Color.red);
        g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        BufferedImage snapshot;
        do {
            initVI(gc);
            g = (Graphics2D)destVI.getGraphics();
            // "accelerate" BufferedImage
            for (int i = 0; i < 5; i++) {
                g.drawImage(bi, 0, 0, null);
            }
            g.setColor(Color.white);
            g.fillRect(0, 0, destVI.getWidth(), destVI.getHeight());

            // this will force the use of Transform primitive instead of
            // Scale (the latter doesn't do bilinear filtering required by
            // VALUE_RENDER_QUALITY, which triggers the bug in D3D pipeline
            g.setRenderingHint(RenderingHints.KEY_RENDERING,
                               RenderingHints.VALUE_RENDER_QUALITY);
            g.drawImage(bi, 0, 0, destVI.getWidth(), destVI.getHeight(), null);
            g.fillRect(0, 0, destVI.getWidth(), destVI.getHeight());

            g.drawImage(bi, 0, 0, destVI.getWidth(), destVI.getHeight(), null);

            snapshot = destVI.getSnapshot();
        } while (destVI.contentsLost());

        f.dispose();
        int whitePixel = Color.white.getRGB();
        for (int y = 0; y < snapshot.getHeight(); y++) {
            for (int x = 0; x < snapshot.getWidth(); x++) {
                if (snapshot.getRGB(x, y) == whitePixel) {
                    System.out.printf("Found untouched pixel at %dx%d\n", x, y);
                    System.out.println("Dumping the dest. image to " +
                                       "AcceleratedScaleTest_dst.png");
                    try {
                        ImageIO.write(snapshot, "png",
                                      new File("AcceleratedScaleTest_dst.png"));
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    throw new RuntimeException("Test failed.");
                }
            }
        }
        System.out.println("Test Passed.");
    }

}
