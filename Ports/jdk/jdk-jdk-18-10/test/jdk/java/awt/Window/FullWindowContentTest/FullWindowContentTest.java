/*
 * Copyright (c) 2018, 2019, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
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

/**
 * @test
 * @key headful
 * @bug 8211301
 * @summary [macosx] support full window content options
 * @author Alan Snyder
 * @run main FullWindowContentTest
 * @requires (os.family == "mac")
 */

import java.awt.AWTException;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.lang.reflect.InvocationTargetException;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JRootPane;
import javax.swing.SwingUtilities;

public class FullWindowContentTest
{
    static FullWindowContentTest theTest;
    private Robot robot;
    private JFrame frame;
    private JRootPane rootPane;
    static boolean isTransparentSupported = getOSVersion() >= 1010;

    private int DELAY = 1000;

    public FullWindowContentTest() {
        try {
            robot = new Robot();
        } catch (AWTException ex) {
            throw new RuntimeException(ex);
        }
    }

    public void performTest() {

        runSwing(() -> {
            frame = new JFrame("Test");
            frame.setBounds(200, 200, 300, 100);
            rootPane = frame.getRootPane();
            JComponent contentPane = (JComponent) frame.getContentPane();
            contentPane.setBackground(Color.RED);
            rootPane.putClientProperty("apple.awt.fullWindowContent", true);
            rootPane.putClientProperty("apple.awt.transparentTitleBar", true);
            frame.setVisible(true);
        });

        robot.delay(DELAY);
        checkTransparent();

        runSwing(() -> rootPane.putClientProperty("apple.awt.transparentTitleBar", false));

        robot.delay(DELAY);
        checkTranslucent();

        runSwing(() -> rootPane.putClientProperty("apple.awt.fullWindowContent", false));

        robot.delay(DELAY);
        checkNormal();

        runSwing(() -> rootPane.putClientProperty("apple.awt.fullWindowContent", true));

        robot.delay(DELAY);
        checkTranslucent();

        runSwing(() -> rootPane.putClientProperty("apple.awt.transparentTitleBar", true));

        robot.delay(DELAY);
        checkTransparent();

        runSwing(() -> frame.dispose());

        frame = null;
        rootPane = null;
    }

    private void checkTransparent() {
        if (isTransparentSupported) {
            Color c = getTestPixel();
            int delta = c.getRed() - c.getBlue();
            if (delta < 200) {
                throw new RuntimeException("Test failed: did not find transparent title bar color");
            }
            checkContent();
        } else {
            checkTranslucent();
        }
    }

    private void checkTranslucent() {
        Color c = getTestPixel();
        int delta = c.getRed() - c.getBlue();
        if (delta < 40 || delta > 150) {
            throw new RuntimeException("Test failed: did not find translucent title bar color");
        }
        checkContent();
    }

    private void checkNormal() {
        Color c = getTestPixel();
        int delta = c.getRed() - c.getBlue();
        if (delta < -40 || delta > 40) {
            throw new RuntimeException("Test failed: did not find normal title bar color");
        }
        checkContent();
    }

    private void checkContent() {
        // Check the bottom of the content area to make sure the insets were changed.
        Color c = getContentPixel();
        int delta = c.getRed() - c.getBlue();
        if (delta < 200) {
            throw new RuntimeException("Test failed: did not find content color");
        }
    }

    private Color getContentPixel() {
        Rectangle bounds = frame.getBounds();
        Color c = robot.getPixelColor(bounds.x + 80, bounds.y + bounds.height - 10);
        return c;
    }

    private Color getTestPixel() {
        Rectangle bounds = frame.getBounds();
        BufferedImage screenImage = robot.createScreenCapture(bounds);
        int rgb = screenImage.getRGB(80, 10);
        int red = (rgb >> 16) & 0xFF;
        int green = (rgb >> 8) & 0xFF;
        int blue = rgb & 0xFF;
        Color c = new Color(red, green, blue);

        // Note: the following code returns significantly wrong values.
        // For example, it returns 42 24 24 for a translucent red that should be more like 243 151 151.

//        Color c = robot.getPixelColor(bounds.x + 80, bounds.y + 10);

        return c;
    }

    public void dispose() {
        if (frame != null) {
            frame.dispose();
            frame = null;
        }
    }

    private static int getOSVersion() {
        String s = System.getProperty("os.version");
        int p = s.indexOf('.');
        int major = Integer.parseInt(s.substring(0, p));
        s = s.substring(p+1);
        p = s.indexOf('.');
        int minor = Integer.parseInt(p >= 0 ? s.substring(0, p) : s);
        return major * 100 + minor;
    }

    private static void runSwing(Runnable r) {
        try {
            SwingUtilities.invokeAndWait(r);
        } catch (InterruptedException e) {
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        if (!System.getProperty("os.name").contains("OS X")) {
            System.out.println("This test is for MacOS only. Automatically passed on other platforms.");
            return;
        }

        try {
            runSwing(() -> theTest = new FullWindowContentTest());
            theTest.performTest();
        } finally {
            if (theTest != null) {
                runSwing(() -> theTest.dispose());
            }
        }
    }
}
