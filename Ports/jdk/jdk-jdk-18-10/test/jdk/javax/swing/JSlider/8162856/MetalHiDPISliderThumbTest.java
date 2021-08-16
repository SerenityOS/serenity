/*
 * Copyright (c) 2016, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

/*
 * @test
 * @bug 8162856
 * @summary Bad rendering of Swing UI controls with Metal L&F on HiDPI display
 * @run main MetalHiDPISliderThumbTest
 */
public class MetalHiDPISliderThumbTest {

    public static void main(String[] args) throws Exception {

        SwingUtilities.invokeAndWait(() -> {

            try {
                UIManager.setLookAndFeel(new MetalLookAndFeel());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            if (!testSliderThumb(true)) {
                throw new RuntimeException("Horizontal Slider Thumb is not scaled!");
            }

            if (!testSliderThumb(false)) {
                throw new RuntimeException("Vertical Slider Thumb is not scaled!");
            }
        });
    }

    private static boolean testSliderThumb(boolean horizontal) {
        int scale = 3;

        int w = horizontal ? 100 : 20;
        int h = horizontal ? 20 : 100;

        JSlider testSlider = new JSlider();
        testSlider.setSize(w, h);
        Dimension size = new Dimension(w, h);
        testSlider.setPreferredSize(size);
        testSlider.setMinimumSize(size);
        testSlider.setMaximumSize(size);
        testSlider.setOrientation(horizontal ? JSlider.HORIZONTAL : JSlider.VERTICAL);

        int sw = scale * w;
        int sh = scale * h;

        final BufferedImage img = new BufferedImage(sw, sh, BufferedImage.TYPE_INT_RGB);

        Graphics2D g = img.createGraphics();
        g.scale(scale, scale);
        testSlider.paint(g);
        g.dispose();

        if (horizontal) {
            int y = sh / 2;

            int xMin = 0;
            int rgb = img.getRGB(xMin, y);
            for (int i = 0; i < sw; i++) {
                if (img.getRGB(i, y) != rgb) {
                    xMin = i;
                    break;
                }
            }

            int xMax = sw - 1;
            rgb = img.getRGB(xMax, y);
            for (int i = sw - 1; i > 0; i--) {
                if (img.getRGB(i, y) != rgb) {
                    xMax = i;
                    break;
                }
            }

            int d = 3 * scale;
            int xc = (xMin + xMax) / 2 - d;
            rgb = img.getRGB(xc, y);

            for (int x = xMin + d; x < xc; x++) {
                if (img.getRGB(x, y) != rgb) {
                    return true;
                }
            }
        } else {
            int x = sw / 2;

            int yMin = 0;
            int rgb = img.getRGB(x, yMin);
            for (int i = 0; i < sh; i++) {
                if (img.getRGB(x, i) != rgb) {
                    yMin = i;
                    break;
                }
            }

            int yMax = sh - 1;
            rgb = img.getRGB(x, yMax);
            for (int i = sh - 1; i > 0; i--) {
                if (img.getRGB(x, i) != rgb) {
                    yMax = i;
                    break;
                }
            }

            int d = 3 * scale;
            int yc = (yMin + yMax) / 2 - d;
            rgb = img.getRGB(x, yc);

            for (int y = yMin + d; y < yc; y++) {
                if (img.getRGB(x, y) != rgb) {
                    return true;
                }
            }
        }
        return false;
    }
}
