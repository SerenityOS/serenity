/*
 * Copyright (c) 2014, 2019, Oracle and/or its affiliates. All rights reserved.
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
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsEnvironment;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.SplashScreen;
import java.awt.TextField;
import java.awt.Window;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import sun.java2d.SunGraphics2D;


/**
 * @test
 * @key headful
 * @bug 8043869 8075244 8078082 8145173 8151787 8212213
 * @summary Tests the HiDPI splash screen support for windows and MAC
 * @modules java.desktop/sun.java2d
 * @run main MultiResolutionSplashTest GENERATE_IMAGES
 * @run main/othervm -splash:splash1.png MultiResolutionSplashTest TEST_SPLASH 0
 * @run main/othervm -splash:splash2 MultiResolutionSplashTest TEST_SPLASH 1
 * @run main/othervm -splash:splash3. MultiResolutionSplashTest TEST_SPLASH 2
 * @run main/othervm -splash:splash1.png MultiResolutionSplashTest TEST_FOCUS
 */
public class MultiResolutionSplashTest {

    private static final int IMAGE_WIDTH = 300;
    private static final int IMAGE_HEIGHT = 200;
    private static boolean isMac;

    static {
        isMac = System.getProperty("os.name").contains("OS X");
    }
    private static final ImageInfo[] tests = {
        new ImageInfo("splash1.png", "splash1@2x.png", Color.BLUE, Color.GREEN),
        new ImageInfo("splash2", "splash2@2x", Color.WHITE, Color.BLACK),
        new ImageInfo("splash3.", "splash3@2x.", Color.YELLOW, Color.RED)
    };

    public static void main(String[] args) throws Exception {

        String test = args[0];
        switch (test) {
            case "GENERATE_IMAGES":
                generateImages();
                break;
            case "TEST_SPLASH":
                int index = Integer.parseInt(args[1]);
                testSplash(tests[index]);
                break;
            case "TEST_FOCUS":
                testFocus();
                break;
            default:
                throw new RuntimeException("Unknown test: " + test);
        }
    }

    static void testSplash(ImageInfo test) throws Exception {
        SplashScreen splashScreen = SplashScreen.getSplashScreen();

        if (splashScreen == null) {
            throw new RuntimeException("Splash screen is not shown!");
        }

        Graphics2D g = splashScreen.createGraphics();
        Rectangle splashBounds = splashScreen.getBounds();
        int screenX = (int) splashBounds.getCenterX();
        int screenY = (int) splashBounds.getCenterY();
        if (splashBounds.width != IMAGE_WIDTH) {
            throw new RuntimeException(
                    "SplashScreen#getBounds has wrong width");
        }
        if (splashBounds.height != IMAGE_HEIGHT) {
            throw new RuntimeException(
                    "SplashScreen#getBounds has wrong height");
        }

        Robot robot = new Robot();
        Color splashScreenColor = robot.getPixelColor(screenX, screenY);
        float scaleFactor = getScaleFactor();
        Color testColor = (1 < scaleFactor) ? test.color2x : test.color1x;

        if (!compare(testColor, splashScreenColor)) {
            throw new RuntimeException(
                    "Image with wrong resolution is used for splash screen!");
        }
    }

    static void testFocus() throws Exception {

        Robot robot = new Robot();
        robot.setAutoDelay(50);

        Frame frame = new Frame();
        frame.setSize(100, 100);
        String test = "123";
        TextField textField = new TextField(test);
        textField.selectAll();
        frame.add(textField);
        frame.setVisible(true);
        robot.waitForIdle();

        robot.keyPress(KeyEvent.VK_A);
        robot.keyRelease(KeyEvent.VK_A);
        robot.keyPress(KeyEvent.VK_B);
        robot.keyRelease(KeyEvent.VK_B);
        robot.waitForIdle();

        frame.dispose();

        if (!textField.getText().equals("ab")) {
            throw new RuntimeException("Focus is lost! " +
                "Expected 'ab' got " + "'" + textField.getText() + "'.");
        }
    }

    static boolean compare(Color c1, Color c2) {
        return compare(c1.getRed(), c2.getRed())
                && compare(c1.getGreen(), c2.getGreen())
                && compare(c1.getBlue(), c2.getBlue());
    }

    static boolean compare(int n, int m) {
        return Math.abs(n - m) <= 50;
    }

    static float getScaleFactor() {

        final Dialog dialog = new Dialog((Window) null);
        dialog.setSize(100, 100);
        dialog.setModal(true);
        final float[] scaleFactors = new float[1];
        Panel panel = new Panel() {

            @Override
            public void paint(Graphics g) {
                float scaleFactor = 1;
                if (g instanceof SunGraphics2D) {
                    scaleFactor = getScreenScaleFactor();
                }
                scaleFactors[0] = scaleFactor;
                dialog.setVisible(false);
            }
        };

        dialog.add(panel);
        dialog.setVisible(true);
        dialog.dispose();

        return scaleFactors[0];
    }

    static void generateImages() throws Exception {
        for (ImageInfo test : tests) {
            generateImage(test.name1x, test.color1x, 1);
            generateImage(test.name2x, test.color2x, getScreenScaleFactor());
        }
    }

    static void generateImage(String name, Color color, float scale) throws Exception {
        File file = new File(name);
        if (file.exists()) {
            return;
        }
        BufferedImage image = new BufferedImage((int) (scale * IMAGE_WIDTH),
                (int) (scale * IMAGE_HEIGHT), BufferedImage.TYPE_INT_RGB);
        Graphics g = image.getGraphics();
        g.setColor(color);
        g.fillRect(0, 0, (int) (scale * IMAGE_WIDTH), (int) (scale * IMAGE_HEIGHT));
        ImageIO.write(image, "png", file);
    }

    static float getScreenScaleFactor() {
        return (float) GraphicsEnvironment.
                getLocalGraphicsEnvironment().
                getDefaultScreenDevice().getDefaultConfiguration().
                getDefaultTransform().getScaleX();
    }

    static class ImageInfo {

        final String name1x;
        final String name2x;
        final Color color1x;
        final Color color2x;

        public ImageInfo(String name1x, String name2x, Color color1x, Color color2x) {
            this.name1x = name1x;
            if (!isMac) {
                float scale = getScreenScaleFactor();
                StringBuffer buff = new StringBuffer();
                if (scale - (int) scale > 0) {
                    buff.append("@").append((int) (scale * 100)).append("pct");
                } else {
                    buff.append("@").append((int) scale).append("x");
                }
                StringBuffer buffer = new StringBuffer();
                String[] splitStr = name1x.split("\\.");
                if (splitStr.length == 2) {
                    this.name2x = buffer.append(splitStr[0]).append(buff)
                            .append(".").append(splitStr[1]).toString();
                } else {
                    if (name1x.indexOf(".") > 0) {
                        this.name2x = buffer.append(splitStr[0]).append(buff).append(".").toString();
                    } else {
                        this.name2x = buffer.append(splitStr[0]).append(buff).toString();
                    }
                }
            } else {
                this.name2x = name2x;
            }
            this.color1x = color1x;
            this.color2x = color2x;
        }
    }
}

