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
import java.awt.image.*;
import java.util.*;
import javax.swing.*;

/**
 * @test
 * @key headful
 * @bug 8024895
 * @summary tests if changing extra alpha values are honored for transformed blits
 * @author ceisserer
 */
public class EABlitTest extends Frame {
    protected void test() {
        BufferedImage srcImg = createSrcImage();
        Image dstImg = getGraphicsConfiguration().createCompatibleVolatileImage(20, 50);

        //be over-cautious and render twice to avoid BI caching issues
        renderToVI(srcImg, dstImg);
        renderToVI(srcImg, dstImg);

        BufferedImage validationImg = new BufferedImage(20, 50, BufferedImage.TYPE_INT_RGB);
        Graphics2D valG = (Graphics2D) validationImg.getGraphics();
        valG.drawImage(dstImg, 0, 0, null);

        //Loop over all pixel, and count the different pixel values encountered.
        TreeSet<Integer> colorCntSet = new TreeSet<>();
        for (int x=0; x < validationImg.getWidth(); x++) {
            for (int y=0; y < validationImg.getHeight(); y++) {
                int rgb = validationImg.getRGB(x, y);
                colorCntSet.add(rgb);
            }
        }

        //Check if we encountered 3 different color values in total
        if (colorCntSet.size() == 3) {
            System.out.println("Passed!");
        } else {
            throw new RuntimeException("Test FAILED!");
        }
    }

    protected void renderToVI(BufferedImage src, Image dst) {
        Graphics2D g = (Graphics2D) dst.getGraphics();

        g.setColor(Color.WHITE);
        g.fillRect(0, 0, 50, 50);
        g.rotate(0.5f);
        g.setRenderingHint(RenderingHints.KEY_RENDERING,
                           RenderingHints.VALUE_RENDER_QUALITY);

        g.setComposite(AlphaComposite.SrcOver.derive(1f));
        g.drawImage(src, 10, 10, null);

        g.setComposite(AlphaComposite.SrcOver.derive(0.5f));
        g.drawImage(src, 20, 20, null);
    }

    protected BufferedImage createSrcImage() {
        BufferedImage bi = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = (Graphics2D) bi.getGraphics();
        g.setColor(Color.YELLOW);
        g.fillRect(0, 0, 10, 10);
        return bi;
    }

    public static void main(String[] args) {
         new EABlitTest().test();
    }
}
