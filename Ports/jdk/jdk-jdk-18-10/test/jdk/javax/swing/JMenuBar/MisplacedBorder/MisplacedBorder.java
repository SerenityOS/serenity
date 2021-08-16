/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB_PRE;
import static javax.swing.UIManager.getInstalledLookAndFeels;

/**
 * @test
 * @key headful
 * @bug 8073795
 * @summary JMenuBar has incorrect border when the window is on retina display.
 * @author Sergey Bylokhov
 * @run main/othervm MisplacedBorder
 * @run main/othervm -Dswing.metalTheme=steel MisplacedBorder
 */
public final class MisplacedBorder implements Runnable {

    public static final int W = 400;
    public static final int H = 400;

    public static void main(final String[] args) throws Exception {
        for (final UIManager.LookAndFeelInfo laf : getInstalledLookAndFeels()) {
            SwingUtilities.invokeAndWait(() -> setLookAndFeel(laf));
            SwingUtilities.invokeAndWait(new MisplacedBorder());
        }
        System.out.println("Test passed");
    }

    @Override
    public void run() {
        final JMenuBar menubar = new JMenuBar();
        menubar.add(new JMenu(""));
        menubar.add(new JMenu(""));
        final JFrame frame = new JFrame();
        frame.setUndecorated(true);
        frame.setJMenuBar(menubar);
        frame.setSize(W / 3, H / 3);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        // draw menu bar using standard order.
        final BufferedImage bi1 = step1(menubar);

        // draw menu border on top of the menu bar, nothing should be changed.
        final BufferedImage bi2 = step2(menubar);
        frame.dispose();

        for (int x = 0; x < W; ++x) {
            for (int y = 0; y < H; ++y) {
                if (bi1.getRGB(x, y) != bi2.getRGB(x, y)) {
                    try {
                        ImageIO.write(bi1, "png", new File("image1.png"));
                        ImageIO.write(bi2, "png", new File("image2.png"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    throw new RuntimeException("Failed: wrong color");
                }
            }
        }
    }

    /**
     * Draws standard JMenuBar.
     */
    private BufferedImage step1(final JMenuBar menubar) {
        final BufferedImage bi1 = new BufferedImage(W, H, TYPE_INT_ARGB_PRE);
        final Graphics2D g2d = bi1.createGraphics();
        g2d.scale(2, 2);
        g2d.setColor(Color.RED);
        g2d.fillRect(0, 0, W, H);
        menubar.paintAll(g2d);
        g2d.dispose();
        return bi1;
    }

    /**
     * Draws standard JMenuBar and border on top of it.
     */
    private BufferedImage step2(final JMenuBar menubar) {
        final BufferedImage bi2 = new BufferedImage(W, H, TYPE_INT_ARGB_PRE);
        final Graphics2D g2d2 = bi2.createGraphics();
        g2d2.scale(2, 2);
        g2d2.setColor(Color.RED);
        g2d2.fillRect(0, 0, W, H);
        menubar.paintAll(g2d2);
        menubar.getBorder().paintBorder(menubar, g2d2, menubar.getX(), menubar
                .getX(), menubar.getWidth(), menubar.getHeight());
        g2d2.dispose();
        return bi2;
    }

    private static void setLookAndFeel(final UIManager.LookAndFeelInfo laf) {
        try {
            UIManager.setLookAndFeel(laf.getClassName());
            System.out.println("LookAndFeel: " + laf.getClassName());
        } catch (ClassNotFoundException | InstantiationException |
                UnsupportedLookAndFeelException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
