/*
 * Copyright (c) 2018, 2020, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;

/**
 * @test
 * @key headful
 * @bug 8215105 8211999
 * @summary tests that Robot can capture the common colors without artifacts
 */
public final class CheckCommonColors {

    private static final Frame frame = new Frame();
    private static Robot robot;

    public static void main(final String[] args) throws Exception {
        robot = new Robot();
        var ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        for (GraphicsDevice gd : ge.getScreenDevices()) {
            try {
                test(gd.getDefaultConfiguration().getBounds());
            } finally {
                frame.dispose();
            }
        }
    }

    private static void test(Rectangle screen) {
        frame.setSize(400, 400);
        frame.setLocation((int)screen.getCenterX() - 200,
                          (int)screen.getCenterY() - 200);
        frame.setUndecorated(true);
        for (final Color color : List.of(Color.WHITE, Color.LIGHT_GRAY,
                                         Color.GRAY, Color.DARK_GRAY,
                                         Color.BLACK, Color.RED, Color.PINK,
                                         Color.ORANGE, Color.YELLOW,
                                         Color.GREEN, Color.MAGENTA, Color.CYAN,
                                         Color.BLUE)) {
            frame.dispose();
            robot.waitForIdle();
            frame.setBackground(color);
            frame.setVisible(true);
            checkPixels(color, true);
            checkPixels(color, false);
        }
    }

    private static void checkPixels(final Color color, boolean useRect) {
        System.out.println("color = " + color + ", useRect = " + useRect);
        int attempt = 0;
        while (true) {
            Point p = frame.getLocationOnScreen();
            p.translate(frame.getWidth() / 2, frame.getHeight() / 2);
            Color pixel;
            Rectangle rect = new Rectangle(p.x, p.y, 1, 1);
            if (useRect) {
                BufferedImage bi = robot.createScreenCapture(rect);
                pixel = new Color(bi.getRGB(0, 0));
            } else {
                pixel = robot.getPixelColor(rect.x, rect.y);
            }
            if (color.equals(pixel)) {
                return;
            }
            frame.repaint();
            if (attempt > 11) {
                System.err.println("Expected: " + color);
                System.err.println("Actual: " + pixel);
                System.err.println("Point: " + p);
                Dimension screenSize =
                        Toolkit.getDefaultToolkit().getScreenSize();
                BufferedImage screen = robot.createScreenCapture(
                        new Rectangle(screenSize));
                try {
                    File output = new File("ScreenCapture.png");
                    System.err.println("Dump screen to: " + output);
                    ImageIO.write(screen, "png", output);
                } catch (IOException ex) {}
                throw new RuntimeException("Too many attempts: " + attempt);
            }
            // skip Robot.waitForIdle to speedup the common case, but also take
            // care about slow systems
            robot.delay((int) Math.pow(2.2, attempt++));
        }
    }
}
