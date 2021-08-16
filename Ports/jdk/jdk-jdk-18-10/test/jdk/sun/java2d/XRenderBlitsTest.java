/*
 * Copyright (c) 2011, 2016, Oracle and/or its affiliates. All rights reserved.
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
 * @bug     6985593
 * @summary Test verifies that rendering standard images to screen does
 *          not caiuse a crash in case of XRender.
 * @run main/othervm -Dsun.java2d.xrender=True XRenderBlitsTest
 */

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;

public class XRenderBlitsTest {

    private static final int w = 10;
    private static final int h = 10;

    public static void main(String[] args) {
        final CountDownLatch done = new CountDownLatch(1);

        final ArrayList<BufferedImage> images = new ArrayList<BufferedImage>();

        int type = BufferedImage.TYPE_INT_RGB;
        do {
            BufferedImage img = new BufferedImage(w, h, type++);
            Graphics2D g2d = img.createGraphics();
            g2d.setColor(Color.pink);
            g2d.fillRect(0, 0, w, h);
            g2d.dispose();

            images.add(img);
        } while (type <= BufferedImage.TYPE_BYTE_INDEXED);

        Frame f = new Frame("Draw images");
        Component c = new Component() {

            @Override
            public Dimension getPreferredSize() {
                return new Dimension(w * images.size(), h);
            }

            @Override
            public void paint(Graphics g) {
                int x = 0;
                for (BufferedImage img : images) {
                    System.out.println("Draw image " + img.getType());
                    g.drawImage(img, x, 0, this);
                    x += w;
                }
                done.countDown();
            }
        };
        f.add("Center", c);
        f.pack();
        f.setVisible(true);

        // now wait for test results
        try {
        done.await();
        } catch (InterruptedException e) {
        }
        System.out.println("Test passed");
        f.dispose();
    }
}
