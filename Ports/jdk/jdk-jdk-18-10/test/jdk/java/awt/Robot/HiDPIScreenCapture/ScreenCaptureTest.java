/*
 * Copyright (c) 2017, Oracle and/or its affiliates. All rights reserved.
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
 * @test
 * @key headful
 * @bug 8162959
 * @summary Validate output of createMultiResolutionScreenCapture
 *          new API which returns MultiResolutionImage.
 * @run main/othervm -Dsun.java2d.uiScale=1 ScreenCaptureTest
 * @run main/othervm -Dsun.java2d.uiScale=2 ScreenCaptureTest
 */
import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.Robot;
import java.util.List;

public class ScreenCaptureTest {

    private static Robot robot;
    private static Frame frame;
    private static boolean isHiDPI = true;
    private static final Color[] COLORS = {
        Color.GREEN, Color.BLUE, Color.ORANGE, Color.RED};

    public static void main(String[] args) throws Exception {

        frame = new Frame();
        frame.setBounds(0, 0, 400, 400);
        frame.setUndecorated(true);
        robot = new Robot();
        Panel panel = new Panel(new BorderLayout());
        Canvas canvas = new Canvas() {
            public void paint(Graphics g) {
                super.paint(g);
                int w = getWidth();
                int h = getHeight();
                g.setColor(COLORS[0]);
                g.fillRect(0, 0, w / 2, h / 2);
                g.setColor(COLORS[1]);
                g.fillRect(w / 2, 0, w / 2, h / 2);
                g.setColor(COLORS[2]);
                g.fillRect(0, h / 2, w / 2, h / 2);
                g.setColor(COLORS[3]);
                g.fillRect(w / 2, h / 2, w / 2, h / 2);
            }
        };

        panel.add(canvas);
        frame.add(panel);
        frame.setVisible(true);
        robot.delay(500);
        robot.waitForIdle();

        int w = frame.getWidth();
        int h = frame.getHeight();

        // getPixelColor Test
        // Check pixel color in first quardant GREEN; x=100, y=100
        if (!robot.getPixelColor(w / 4, h / 4).equals(COLORS[0])) {
            throw new RuntimeException("Wrong Pixel Color! Expected GREEN");
        }
        // Check pixel color in second quardant; BLUE, x=300, y=100
        if (!robot.getPixelColor(3 * w / 4, h / 4).equals(COLORS[1])) {
            throw new RuntimeException("Wrong Pixel Color! Expected BLUE");
        }
        // Check pixel color in third quardant; ORANGE, x=100, y=300
        if (!robot.getPixelColor(w / 4, 3 * h / 4).equals(COLORS[2])) {
            throw new RuntimeException("Wrong Pixel Color! Expected ORANGE");
        }
        // Check pixel color in fourth quardant; RED, x=300, y=300
        if (!robot.getPixelColor(3 * w / 4, 3 * h / 4).equals(COLORS[3])) {
            throw new RuntimeException("Wrong Pixel Color! Expected RED");
        }

        // createScreenCaptureTest
        AffineTransform tx = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice().getDefaultConfiguration()
                .getDefaultTransform();

        if (tx.getScaleX() == 1 && tx.getScaleY() == 1) {
            isHiDPI = false;
        }

        MultiResolutionImage image
                = robot.createMultiResolutionScreenCapture(frame.getBounds());
        List<Image> imageList = image.getResolutionVariants();
        int size = imageList.size();
        BufferedImage lowResImage;
        BufferedImage highResImage;

        if (!isHiDPI) {
            // Check if output is MultiResolutionImage with one variant
            if (size != 1) {
                throw new RuntimeException(" Invalid variant size");
            }

            lowResImage = (BufferedImage) imageList.get(0);
            System.out.println(frame.getBounds());
            System.out.println(lowResImage.getWidth()+" "+lowResImage.getHeight());
            if (frame.getWidth() != lowResImage.getWidth()
                        || frame.getHeight() != lowResImage.getHeight()) {
                throw new RuntimeException(" Invalid Image size");
            }

        } else {
            // Check if output contains two variants.
            if (size != 2) {
                throw new RuntimeException(" Invalid variant size");
            }

            // Check if hight resolution image size is scale times low resolution image.
            lowResImage = (BufferedImage) imageList.get(0);
            highResImage = (BufferedImage) imageList.get(1);

            int lW = (int) lowResImage.getWidth();
            int lH = (int) lowResImage.getHeight();
            int hW = (int) highResImage.getWidth();
            int hH = (int) highResImage.getHeight();

            if ( hW != (tx.getScaleX() * lW) || hH != (tx.getScaleY() * lH)) {
                throw new RuntimeException(" Invalid Resolution Variants");
            }

            // Check if both image colors are same at some location.
            if (lowResImage.getRGB(lW / 4, lH / 4)
                    != highResImage.getRGB(hW / 4, hH / 4)) {
                throw new RuntimeException("Wrong image color!");
            }

            if (lowResImage.getRGB(3 * lW / 4, lH / 4)
                    != highResImage.getRGB(3 * hW / 4, hH / 4)) {
                throw new RuntimeException("Wrong image color!");
            }

            if (lowResImage.getRGB(lW / 4, 3 * lH / 4)
                    != highResImage.getRGB(hW / 4, 3 * hH / 4)) {
                throw new RuntimeException("Wrong image color!");
            }

            if (lowResImage.getRGB(3 * lW / 4, 3 * lH / 4)
                    != highResImage.getRGB(3 * hW / 4, 3 * hH / 4)) {
                throw new RuntimeException("Wrong image color!");
            }

        }

        frame.dispose();
    }

}
