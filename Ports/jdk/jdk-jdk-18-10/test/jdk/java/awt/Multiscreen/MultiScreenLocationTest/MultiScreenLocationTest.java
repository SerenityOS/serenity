/*
 * Copyright (c) 2014, 2016, Oracle and/or its affiliates. All rights reserved.
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
  @test
  @key headful
  @bug 8013116
  @summary Robot moves mouse to point which differs from set in mouseMove on
 Unity shell
  @author Oleg Pekhovskiy
  @library ../../regtesthelpers
  @build Util
  @run main MultiScreenLocationTest
 */

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import test.java.awt.regtesthelpers.Util;

public class MultiScreenLocationTest {
    private static final Point mouseOffset = new Point(150, 150);
    private static final Point frameOffset = new Point(100, 100);
    private static final Color color = Color.YELLOW;

    private static String getErrorText(final String name, int screen)
    {
        return name + " test failed on Screen #" + screen + "!";
    }

    public static void main(String[] args) throws AWTException
    {
        GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
        GraphicsDevice[] gds = ge.getScreenDevices();
        if (gds.length < 2) {
            System.out.println("It's a multiscreen test... skipping!");
            return;
        }

        for (int i = 0; i < gds.length; ++i) {
            GraphicsDevice gd = gds[i];
            GraphicsConfiguration gc = gd.getDefaultConfiguration();
            Rectangle screen = gc.getBounds();
            Robot robot = new Robot(gd);

            // check Robot.mouseMove()
            robot.mouseMove(screen.x + mouseOffset.x, screen.y + mouseOffset.y);
            Point mouse = MouseInfo.getPointerInfo().getLocation();
            Point point = screen.getLocation();
            point.translate(mouseOffset.x, mouseOffset.y);
            if (!point.equals(mouse)) {
                throw new RuntimeException(getErrorText("Robot.mouseMove", i));
            }

            // check Robot.getPixelColor()
            Frame frame = new Frame(gc);
            frame.setUndecorated(true);
            frame.setSize(100, 100);
            frame.setLocation(screen.x + frameOffset.x, screen.y + frameOffset.y);
            frame.setBackground(color);
            frame.setVisible(true);
            robot.waitForIdle();
            Rectangle bounds = frame.getBounds();
            if (!Util.testBoundsColor(bounds, color, 5, 1000, robot)) {
                throw new RuntimeException(getErrorText("Robot.getPixelColor", i));
            }

            // check Robot.createScreenCapture()
            BufferedImage image = robot.createScreenCapture(bounds);
            int rgb = color.getRGB();
            if (image.getRGB(0, 0) != rgb
                || image.getRGB(image.getWidth() - 1, 0) != rgb
                || image.getRGB(image.getWidth() - 1, image.getHeight() - 1) != rgb
                || image.getRGB(0, image.getHeight() - 1) != rgb) {
                    throw new RuntimeException(
                            getErrorText("Robot.createScreenCapture", i));
            }
            frame.dispose();
        }

        System.out.println("Test PASSED!");
    }
}
