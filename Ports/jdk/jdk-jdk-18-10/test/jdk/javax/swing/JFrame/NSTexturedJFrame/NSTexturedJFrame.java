/*
 * Copyright (c) 2013, 2018, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Color;
import java.awt.image.BufferedImage;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import jdk.test.lib.Platform;

/**
 * @test
 * @key headful
 * @bug 7124513
 * @requires (os.family == "mac")
 * @summary We should support NSTexturedBackgroundWindowMask style on OSX.
 * @library /test/lib
 *          /lib/client
 * @build ExtendedRobot jdk.test.lib.Platform
 * @run main NSTexturedJFrame
 */

public final class NSTexturedJFrame {

    private static final String BRUSH = "apple.awt.brushMetalLook";
    private static final String STYLE = "Window.style";
    private static final BufferedImage[] images = new BufferedImage[3];
    private static Rectangle bounds;
    private static volatile int step;
    private static JFrame frame;
    private static ExtendedRobot robot;

    public static void main(final String[] args) throws Exception {
        if (!Platform.isOSX()) {
            System.out.println("This test is for OSX, considered passed.");
            return;
        }
        robot = new ExtendedRobot();
        robot.setAutoDelay(50);
        // Default window appearance
        showFrame();
        step++;
        // apple.awt.brushMetalLook appearance
        showFrame();
        step++;
        // Window.style appearance
        showFrame();

        // images on step 1 and 2 should be same
        testImages(images[1], images[2], false);
        // images on step 1 and 2 should be different from default
        testImages(images[0], images[1], true);
        testImages(images[0], images[2], true);
    }

    private static void testImages(BufferedImage img1, BufferedImage img2,
                                   boolean shouldbeDifferent) {
        boolean different = false;
        int tol = 5;
        for (int x = 0; x < img1.getWidth(); ++x) {
            for (int y = 0; y < img1.getHeight(); ++y) {
                Color c1 = new Color(img1.getRGB(x, y));
                Color c2 = new Color(img2.getRGB(x, y));

                if ((Math.abs(c1.getRed() - c2.getRed()) > tol) &&
                    (Math.abs(c1.getBlue() - c2.getBlue()) > tol) &&
                    (Math.abs(c1.getGreen() - c2.getGreen()) > tol )) {

                    different = true;
                }
            }
        }
        if (different != shouldbeDifferent) {
            throw new RuntimeException("Textured property does not work");
        }
    }

    private static void showFrame() throws Exception {
        createUI();
        images[step] = robot.createScreenCapture(bounds);
        SwingUtilities.invokeAndWait(frame::dispose);
        robot.waitForIdle(1000);
    }

    private static void createUI() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            frame = new JFrame();
            frame.setUndecorated(true);
            frame.setSize(400, 400);
            frame.setLocationRelativeTo(null);
            switch (step) {
                case 1:
                    frame.getRootPane().putClientProperty(BRUSH, true);
                    break;
                case 2:
                    frame.getRootPane().putClientProperty(STYLE, "textured");
            }
            frame.setVisible(true);
        });
        robot.waitForIdle(1000);
        SwingUtilities.invokeAndWait(() -> {
            bounds = frame.getBounds();
        });
        robot.waitForIdle(1000);
    }

}
