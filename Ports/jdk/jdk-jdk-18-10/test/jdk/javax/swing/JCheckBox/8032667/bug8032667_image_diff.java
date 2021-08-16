/*
 * Copyright (c) 2014, 2018, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import jdk.test.lib.Platform;

/* @test
 * @bug 8032667
 * @summary [macosx] Components cannot be rendered in HiDPI to BufferedImage
 * @library /test/lib
 * @build jdk.test.lib.Platform
 * @run main bug8032667_image_diff
 */
public class bug8032667_image_diff {

    static final int IMAGE_WIDTH = 130;
    static final int IMAGE_HEIGHT = 50;

    public static void main(String[] args) throws Exception {

        if (!Platform.isOSX()) {
            return;
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {

                JCheckBox checkBox = new JCheckBox();
                checkBox.setSelected(true);
                checkBox.setSize(new Dimension(IMAGE_WIDTH, IMAGE_HEIGHT));

                final BufferedImage image1 = getHiDPIImage(checkBox);
                final BufferedImage image2 = getScaledImage(checkBox);

                if(equal(image1, image2)){
                    throw new RuntimeException("2x image equals to non smooth image");
                }
            }
        });
    }

    static boolean equal(BufferedImage image1, BufferedImage image2) {

        int w = image1.getWidth();
        int h = image1.getHeight();

        if (w != image2.getWidth() || h != image2.getHeight()) {
            return false;
        }

        for (int i = 0; i < w; i++) {
            for (int j = 0; j < h; j++) {
                int color1 = image1.getRGB(i, j);
                int color2 = image2.getRGB(i, j);

                if (color1 != color2) {
                    return false;
                }
            }
        }
        return true;
    }

    static BufferedImage getHiDPIImage(JComponent component) {
        return getImage(component, 2, IMAGE_WIDTH, IMAGE_HEIGHT);
    }

    static BufferedImage getScaledImage(JComponent component) {
        Image image1x = getImage(component, 1, IMAGE_WIDTH, IMAGE_HEIGHT);
        final BufferedImage image2x = new BufferedImage(
                2 * IMAGE_WIDTH, 2 * IMAGE_HEIGHT, BufferedImage.TYPE_INT_ARGB);
        final Graphics g = image2x.getGraphics();
        ((Graphics2D) g).scale(2, 2);
        g.drawImage(image1x, 0, 0, null);
        g.dispose();
        return image2x;
    }

    static BufferedImage getImage(JComponent component, int scale, int width, int height) {
        final BufferedImage image = new BufferedImage(
                scale * width, scale * height, BufferedImage.TYPE_INT_ARGB);
        final Graphics g = image.getGraphics();
        ((Graphics2D) g).scale(scale, scale);
        component.paint(g);
        g.dispose();
        return image;
    }
}
