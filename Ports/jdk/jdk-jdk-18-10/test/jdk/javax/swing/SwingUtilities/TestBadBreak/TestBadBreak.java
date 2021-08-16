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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;

import static java.awt.image.BufferedImage.TYPE_INT_ARGB;

/**
 * @test
 * @key headful
 * @bug 8015085 8079253
 * @summary Shortening via " ... " is broken for Strings containing a combining
 *          diaeresis.
 * @run main TestBadBreak
 */
public class TestBadBreak {

    static JFrame frame;
    static Robot robot;
    static final String withCombiningDiaeresis =    "123p://.aaaaaaaaaaaaaaaaaaaaaa.123/a\u0308" ;
    static final String withoutCombiningDiaeresis = "123p://.aaaaaaaaaaaaaaaaaaaaaa.123/\u00E4" ;

    public static void main(final String[] args) throws Exception {
        robot = new Robot();
        final BufferedImage bi1 = new BufferedImage(200, 90, TYPE_INT_ARGB);
        final BufferedImage bi2 = new BufferedImage(200, 90, TYPE_INT_ARGB);
        test(withCombiningDiaeresis, bi1);
        test(withoutCombiningDiaeresis, bi2);
        for (int x = 0; x < bi1.getWidth(); ++x) {
            for (int y = 0; y < bi1.getHeight(); ++y) {
                if (bi1.getRGB(x, y) != bi2.getRGB(x, y)) {
                    ImageIO.write(bi1, "png", new File("image1.png"));
                    ImageIO.write(bi2, "png", new File("image2.png"));
                    throw new RuntimeException("Wrong color");
                }
            }
        }
    }

    private static void test(final String text, final BufferedImage i1)
            throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                frame = new JFrame();
                final JLabel label = new JLabel(text) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2d = i1.createGraphics();
                        super.paintComponent(g2d);
                        g2d.dispose();
                    }
                };
                label.setOpaque(true);
                frame.getContentPane().add(label);
                frame.setBounds(200, 200, 200, 90);
            }
        });
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(() -> frame.setVisible(true));
        robot.waitForIdle();
        SwingUtilities.invokeAndWait(frame::dispose);
        robot.waitForIdle();
    }
}
