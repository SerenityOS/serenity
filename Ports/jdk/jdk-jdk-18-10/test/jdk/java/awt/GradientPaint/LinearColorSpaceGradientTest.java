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

import java.awt.*;
import java.awt.MultipleGradientPaint.*;
import java.awt.geom.*;
import java.awt.image.*;

/**
 * @test
 * @key headful
 * @bug 8023483
 * @summary tests whether the colorspace-parameter is applied correctly when
 *          creating a gradient.
 * @author ceisserer
 */
public class LinearColorSpaceGradientTest extends Frame {
    BufferedImage srcImg;
    Image dstImg;

    public LinearColorSpaceGradientTest() {
        srcImg = createSrcImage();
        dstImg = getGraphicsConfiguration().createCompatibleVolatileImage(20,
                20);
    }

    protected void renderToVI(BufferedImage src, Image dst) {
        Graphics2D g = (Graphics2D) dst.getGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, dst.getWidth(null), dst.getHeight(null));

        AffineTransform at = new AffineTransform();
        g.setPaint(new LinearGradientPaint(new Point2D.Float(0, 0),
                new Point2D.Float(20, 0), new float[] { 0.0f, 1.0f },
                new Color[] { Color.green, Color.blue }, CycleMethod.NO_CYCLE,
                ColorSpaceType.LINEAR_RGB, at));

        g.fillRect(-10, -10, 30, 30);
    }

    public void paint(Graphics g1) {
        Graphics2D g = (Graphics2D) g1;
        renderToVI(createSrcImage(), dstImg);
        g.drawImage(dstImg, 20, 20, null);
    }

    public void showFrame() {
        setSize(500, 500);
        setVisible(true);
    }

    public void test() {
        renderToVI(createSrcImage(), dstImg);

        BufferedImage validationImg = new BufferedImage(20, 20,
                BufferedImage.TYPE_INT_RGB);
        Graphics2D valG = (Graphics2D) validationImg.getGraphics();
        valG.drawImage(dstImg, 0, 0, null);

        int b = validationImg.getRGB(10, 10) & 0x000000FF;

        if (b > 150) {
            System.out.println("Passed!");
        } else {
            throw new RuntimeException("Test FAILED!");
        }
    }

    protected BufferedImage createSrcImage() {
        BufferedImage bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.setColor(Color.YELLOW);
        g.fillRect(0, 0, 10, 10);
        g.setColor(Color.black);
        g.drawLine(0, 0, 10, 10);
        return bi;
    }

    public static void main(String[] args) throws Exception {
        boolean show = (args.length > 0 && "-show".equals(args[0]));

        final LinearColorSpaceGradientTest t = new LinearColorSpaceGradientTest();
        if (show) {
            EventQueue.invokeAndWait(new Runnable() {
                public void run() {
                    t.showFrame();
                }
            });
        } else {
            t.test();
        }
    }
}
