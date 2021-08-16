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

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.concurrent.Callable;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

/*
   @test
   @key headful
   @bug 7156657 8186617
   @summary Version 7 doesn't support translucent popup menus against a
            translucent window
   @library ../../regtesthelpers
*/
public class bug7156657 {
    private static JFrame lowerFrame;

    private static JFrame frame;

    private static JPopupMenu popupMenu;

    public static void main(String[] args) throws Exception {
        final Robot robot = new Robot();

        Boolean skipTest = Util.invokeOnEDT(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                frame = createFrame();
                if (!frame.getGraphicsConfiguration().isTranslucencyCapable()) {
                    System.out.println("Translucency is not supported, the test skipped");

                    return true;
                }

                lowerFrame = createFrame();
                lowerFrame.getContentPane().setBackground(Color.RED);
                lowerFrame.setVisible(true);

                popupMenu = new JPopupMenu();
                popupMenu.setOpaque(false);
                popupMenu.add(new TransparentMenuItem("1111"));
                popupMenu.add(new TransparentMenuItem("2222"));
                popupMenu.add(new TransparentMenuItem("3333"));

                setOpaque(frame, false);
                JPanel pnContent = new JPanel();
                pnContent.setBackground(new Color(255, 255, 255, 128));
                frame.add(pnContent);
                frame.setVisible(true);

                return false;
            }
        });

        if (skipTest) {
            return;
        }

        robot.waitForIdle();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                popupMenu.show(frame, 0, 0);
            }
        });

        robot.waitForIdle();

        Rectangle popupRectangle = Util.invokeOnEDT(new Callable<Rectangle>() {
            @Override
            public Rectangle call() throws Exception {
                return new Rectangle(popupMenu.getLocationOnScreen(),
                        popupMenu.getSize());
            }
        });

        BufferedImage redBackgroundCapture = robot.createScreenCapture(popupRectangle);
        BufferedImage redFrame = robot.createScreenCapture(frame.getBounds());

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                lowerFrame.getContentPane().setBackground(Color.GREEN);
                lowerFrame.invalidate();
            }
        });

        robot.waitForIdle();
        robot.delay(1000); // Give frame time to repaint

        BufferedImage greenBackgroundCapture = robot.createScreenCapture(popupRectangle);
        BufferedImage greenFrame = robot.createScreenCapture(frame.getBounds());

        if (Util.compareBufferedImages(redBackgroundCapture, greenBackgroundCapture)) {
            try {
                GraphicsDevice[] devices = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices();
                for (int i = 0; i < devices.length; i++) {
                    GraphicsConfiguration[] screens = devices[i].getConfigurations();
                    for (int j = 0; j < screens.length; j++) {
                        BufferedImage fullScreen = robot.createScreenCapture(screens[j].getBounds());
                        if (screens[j].getBounds().intersects(popupRectangle)) {
                            Graphics g = fullScreen.getGraphics();
                            g.setColor(Color.CYAN);
                            g.drawRect(popupRectangle.x - 1, popupRectangle.y - 1,
                                    popupRectangle.width + 2, popupRectangle.height + 2);
                            g.dispose();
                        }
                        ImageIO.write(fullScreen, "png", new File("dev" + i + "scr" + j + ".png"));
                    }
                }
                ImageIO.write(redFrame, "png", new File("redframe.png"));
                ImageIO.write(redBackgroundCapture, "png", new File("redbg.png"));
                ImageIO.write(greenFrame, "png", new File("greenframe.png"));
                ImageIO.write(greenBackgroundCapture, "png", new File("greenbg.png"));
            } finally {
                SwingUtilities.invokeAndWait(() -> {
                    frame.dispose();
                    lowerFrame.dispose();
                });
            }
            robot.waitForIdle();
            throw new RuntimeException("The test failed");
        }

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                popupMenu.setVisible(false);
                frame.dispose();
                lowerFrame.dispose();
            }
        });

        System.out.println("The test passed");
    }

    public static void setOpaque(Window window, boolean opaque) {
        Color bg = window.getBackground();
        if (bg == null) {
            bg = new Color(0, 0, 0, 0);
        }
        window.setBackground(new Color(bg.getRed(), bg.getGreen(), bg.getBlue(),
                                       opaque ? 255 : 0));
    }

    private static JFrame createFrame() {
        JFrame result = new JFrame();

        result.setSize(400, 300);
        result.setLocationRelativeTo(null);
        result.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        result.setUndecorated(true);

        return result;
    }

    private static class TransparentMenuItem extends JMenuItem {
        public TransparentMenuItem(String text) {
            super(text);
            setOpaque(false);
        }

        @Override
        public void paint(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
            super.paint(g2);
            g2.dispose();
        }
    }
}
