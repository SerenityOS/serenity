/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.EventQueue;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.SliderUI;
import javax.swing.plaf.basic.BasicSliderUI;
import javax.swing.plaf.metal.DefaultMetalTheme;
import javax.swing.plaf.metal.MetalLookAndFeel;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;
import static javax.swing.UIManager.getInstalledLookAndFeels;

/**
 * @test
 * @bug 8256713
 * @key headful
 * @summary The thumb should not touch pixels outside its location.
 */
public final class PaintThumbSize {

    private static final int SCALE = 2;
    private static final int SHIFT = 100;

    public static void main(String[] args) throws Exception {
        for (UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            EventQueue.invokeAndWait(() -> setLookAndFeel(laf));
            EventQueue.invokeAndWait(PaintThumbSize::test);
            if (laf.getClassName().contains("Metal")) {
                EventQueue.invokeAndWait(() -> {
                    System.err.println("\tAdditional theme: DefaultMetalTheme");
                    MetalLookAndFeel.setCurrentTheme(new DefaultMetalTheme());
                    test();
                });
            }
        }
    }

    private static void test() {
        BufferedImage bi = new BufferedImage(500, 500, TYPE_INT_ARGB);
        Graphics2D g = bi.createGraphics();
        g.setColor(Color.CYAN);
        g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
        g.setColor(Color.BLACK);

        g.scale(SCALE, SCALE);
        g.translate(SHIFT, SHIFT);

        JSlider slider = new JSlider();
        SliderUI ui = slider.getUI();
        if (ui instanceof BasicSliderUI) {
            BasicSliderUI bui = (BasicSliderUI) ui;
            bui.setThumbLocation(0, 0);
            bui.paintThumb(g);

            for (int y = 0; y < bi.getHeight(); ++y) {
                for (int x = 0; x < bi.getWidth(); ++x) {
                    if (x >= SHIFT * SCALE && y >= SHIFT * SCALE) {
                        continue;
                    }
                    if (bi.getRGB(x, y) != Color.CYAN.getRGB()) {
                        System.err.println("x = " + x);
                        System.err.println("y = " + y);
                        try {
                            ImageIO.write(bi,"png", new File("image.png"));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        throw new RuntimeException("Wrong color");
                    }
                }
            }
        }
        g.dispose();
    }


    private static void setLookAndFeel(UIManager.LookAndFeelInfo laf) {
        try {
            System.err.println("LookAndFeel: " + laf.getClassName());
            UIManager.setLookAndFeel(laf.getClassName());
        } catch (UnsupportedLookAndFeelException ignored) {
            System.err.println("Unsupported LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
