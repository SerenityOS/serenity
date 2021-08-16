/*
 * Copyright (c) 2013, 2016, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Insets;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;

/**
 * @test
 * @key headful
 * @bug 8003173 7019055
 * @summary Full-screen windows should have the proper insets.
 * @author Sergey Bylokhov
 */
public final class FullScreenInsets {

    private static boolean passed = true;
    private static Robot robot = null;

    public static void main(final String[] args) {
        final GraphicsEnvironment ge = GraphicsEnvironment
                .getLocalGraphicsEnvironment();
        final GraphicsDevice[] devices = ge.getScreenDevices();

        final Window wGreen = new Frame();
        wGreen.setBackground(Color.GREEN);
        wGreen.setSize(300, 300);
        wGreen.setVisible(true);
        sleep();
        final Insets iGreen = wGreen.getInsets();
        final Dimension sGreen = wGreen.getSize();

        final Window wRed = new Frame();
        wRed.setBackground(Color.RED);
        wRed.setSize(300, 300);
        wRed.setVisible(true);
        sleep();
        final Insets iRed = wGreen.getInsets();
        final Dimension sRed = wGreen.getSize();

        for (final GraphicsDevice device : devices) {
            if (!device.isFullScreenSupported()) {
                continue;
            }
            device.setFullScreenWindow(wGreen);
            sleep();
            testWindowBounds(device.getDisplayMode(), wGreen);
            testColor(wGreen, Color.GREEN);

            device.setFullScreenWindow(wRed);
            sleep();
            testWindowBounds(device.getDisplayMode(), wRed);
            testColor(wRed, Color.RED);

            device.setFullScreenWindow(null);
            sleep();
            testInsets(wGreen.getInsets(), iGreen);
            testInsets(wRed.getInsets(), iRed);
            testSize(wGreen.getSize(), sGreen);
            testSize(wRed.getSize(), sRed);
        }
        wGreen.dispose();
        wRed.dispose();
        if (!passed) {
            throw new RuntimeException("Test failed");
        }
    }

    private static void testSize(final Dimension actual, final Dimension exp) {
        if (!exp.equals(actual)) {
            System.err.println(" Wrong window size:" +
                               " Expected: " + exp + " Actual: " + actual);
            passed = false;
        }
    }

    private static void testInsets(final Insets actual, final Insets exp) {
        if (!actual.equals(exp)) {
            System.err.println(" Wrong window insets:" +
                               " Expected: " + exp + " Actual: " + actual);
            passed = false;
        }
    }

    private static void testWindowBounds(final DisplayMode dm, final Window w) {
        if (w.getWidth() != dm.getWidth() || w.getHeight() != dm.getHeight()) {
            System.err.println(" Wrong window bounds:" +
                               " Expected: width = " + dm.getWidth()
                               + ", height = " + dm.getHeight() + " Actual: "
                               + w.getSize());
            passed = false;
        }
    }

    private static void testColor(final Window w, final Color color) {
        final Robot r;
        try {
            r = new Robot(w.getGraphicsConfiguration().getDevice());
        } catch (AWTException e) {
            e.printStackTrace();
            passed = false;
            return;
        }
        final BufferedImage bi = r.createScreenCapture(w.getBounds());
        for (int y = 0; y < bi.getHeight(); y++) {
            for (int x = 0; x < bi.getWidth(); x++) {
                if (bi.getRGB(x, y) != color.getRGB()) {
                    System.err.println(
                            "Incorrect pixel at " + x + "x" + y + " : " +
                            Integer.toHexString(bi.getRGB(x, y)) +
                            " ,expected : " + Integer.toHexString(
                                    color.getRGB()));
                    passed = false;
                    return;
                }
            }
        }
    }

    private static void sleep() {
        if(robot == null) {
            try {
                robot = new Robot();
            }catch(AWTException ae) {
                ae.printStackTrace();
                throw new RuntimeException("Cannot create Robot.");
            }
        }
        robot.waitForIdle();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException ignored) {
        }
    }
}
