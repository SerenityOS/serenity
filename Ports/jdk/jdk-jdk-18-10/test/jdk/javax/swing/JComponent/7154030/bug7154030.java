/*
 * Copyright (c) 2012, 2021, Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright (c) 2012 IBM Corporation
 */

import javax.swing.JButton;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;

/*
 * @test
 * @key headful
 * @bug 7154030
 * @summary Swing components fail to hide after calling hide()
 * @author Jonathan Lu
 * @library ../../regtesthelpers/
 * @library /lib/client/
 * @build Util
 * @build ExtendedRobot
 * @run main bug7154030
 */

public class bug7154030 {

    private static JButton button = null;
    private static JFrame frame;
    private static volatile int locx, locy, frw, frh;

    public static void main(String[] args) throws Exception {
        try {
            BufferedImage imageInit = null;

            BufferedImage imageShow = null;

            BufferedImage imageHide = null;

            ExtendedRobot robot = new ExtendedRobot();

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    JDesktopPane desktop = new JDesktopPane();
                    button = new JButton("button");
                    frame = new JFrame();
                    frame.setUndecorated(true);

                    button.setSize(200, 200);
                    button.setLocation(100, 100);
                    button.setForeground(Color.RED);
                    button.setBackground(Color.RED);
                    button.setOpaque(true);
                    button.setVisible(false);
                    desktop.add(button);
                    desktop.setMinimumSize(new Dimension(300, 300));
                    desktop.setMaximumSize(new Dimension(300, 300));

                    frame.setContentPane(desktop);
                    frame.setMinimumSize(new Dimension(350, 350));
                    frame.setMaximumSize(new Dimension(350, 350));
                    frame.pack();
                    frame.setLocationRelativeTo(null);
                    frame.setVisible(true);
                    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                }
            });

            robot.waitForIdle(1000);
            robot.delay(1000);

            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Rectangle screen = new Rectangle(0, 0, (int) screenSize.getWidth(), (int) screenSize.getHeight());
            SwingUtilities.invokeAndWait(() -> {
                        Rectangle bounds = frame.getBounds();
                        Insets insets = frame.getInsets();
                        locx = bounds.x + insets.left;
                        locy = bounds.y + insets.top;
                        frw = bounds.width - insets.left - insets.right;
                        frh = bounds.height - insets.top - insets.bottom;
                    });

            BufferedImage fullScreen = robot.createScreenCapture(screen);
            Graphics g = fullScreen.getGraphics();
            g.setColor(Color.RED);
            g.drawRect(locx - 1, locy - 1, frw + 1, frh + 1);
            imageInit = robot.createScreenCapture(new Rectangle(locx, locy, frw, frh));

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    button.show();
                }
            });

            robot.waitForIdle(500);
            imageShow = robot.createScreenCapture(new Rectangle(locx, locy, frw, frh));
            if (Util.compareBufferedImages(imageInit, imageShow)) {
                ImageIO.write(imageInit, "png", new File("imageInit.png"));
                ImageIO.write(imageShow, "png", new File("imageShow.png"));
                ImageIO.write(fullScreen, "png", new File("fullScreenInit.png"));
                throw new Exception("Failed to show opaque button");
            }

            robot.waitForIdle();

            SwingUtilities.invokeAndWait(new Runnable() {
                @Override
                public void run() {
                    button.hide();
                }
            });

            robot.waitForIdle(500);
            imageHide = robot.createScreenCapture(new Rectangle(locx, locy, frw, frh));

            if (!Util.compareBufferedImages(imageInit, imageHide)) {
                ImageIO.write(imageInit, "png", new File("imageInit.png"));
                ImageIO.write(imageHide, "png", new File("imageHide.png"));
                ImageIO.write(fullScreen, "png", new File("fullScreenInit.png"));
                throw new Exception("Failed to hide opaque button");
            }

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    button.setOpaque(false);
                    button.setBackground(new Color(128, 128, 0));
                    button.setVisible(false);
                }
            });

            robot.waitForIdle(500);
            imageInit = robot.createScreenCapture(new Rectangle(locx, locy, frw, frh));

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    button.show();
                }
            });

            robot.waitForIdle(500);
            imageShow = robot.createScreenCapture(new Rectangle(locx, locy, frw, frh));

            if (Util.compareBufferedImages(imageInit, imageShow)) {
                ImageIO.write(imageInit, "png", new File("imageInit.png"));
                ImageIO.write(imageShow, "png", new File("imageShow.png"));
                ImageIO.write(fullScreen, "png", new File("fullScreenInit.png"));
                throw new Exception("Failed to show non-opaque button");
            }

            SwingUtilities.invokeAndWait(new Runnable() {

                @Override
                public void run() {
                    button.hide();
                }
            });

            robot.waitForIdle(500);
            imageHide = robot.createScreenCapture(new Rectangle(locx, locy, frw, frh));

            if (!Util.compareBufferedImages(imageInit, imageHide)) {
                ImageIO.write(imageInit, "png", new File("imageInit.png"));
                ImageIO.write(imageHide, "png", new File("imageHide.png"));
                ImageIO.write(fullScreen, "png", new File("fullScreenInit.png"));
                throw new Exception("Failed to hide non-opaque button");
            }
        } finally {
            if (frame != null) SwingUtilities.invokeAndWait(() -> frame.dispose());
        }
    }
}
