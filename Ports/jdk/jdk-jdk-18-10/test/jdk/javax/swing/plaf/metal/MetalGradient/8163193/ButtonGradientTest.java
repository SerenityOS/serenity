/*
 * Copyright (c) 2016, 2017, Oracle and/or its affiliates. All rights reserved.
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

import java.awt.Image;
import java.awt.Robot;
import java.awt.Rectangle;
import java.awt.Color;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.awt.image.MultiResolutionImage;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.metal.MetalLookAndFeel;

/*
 * @test
 * @bug 8163193 8165213
 * @key headful
 * @summary Metal L&F gradient is lighter on HiDPI display
 * @run main/othervm -Dsun.java2d.uiScale=2 ButtonGradientTest
 */
public class ButtonGradientTest {
    private static JFrame frame;
    private static JButton button;
    private static List<Image> images;
    private static Robot robot;

    public static void main(String[] args) throws Exception {
        try {
            robot = new Robot();
            testGradient();
        } finally {
            SwingUtilities.invokeAndWait(() -> {
                if (frame != null) {
                    frame.dispose();
                }
            });
        }
    }

    private static void testGradient() throws Exception {
        // Create and show the GUI
        SwingUtilities.invokeAndWait(ButtonGradientTest::createAndShowGUI);
        robot.waitForIdle();
        robot.delay(500);

        Rectangle rect = getButtonBounds();
        List<?> gradient = (List) UIManager.get("Button.gradient");
        float ratio = ((Number) gradient.get(0)).floatValue();
        Color c1 = (Color) gradient.get(2);
        Color c2 = (Color) gradient.get(3);
        int mid = (int) (ratio * rect.height);
        // Get the expected gradient color
        Color gradientColor = getGradientColor(rect.width, mid, c1, c2);

        // Get color from robot captured hidpi image of the button
        getImageFromRobot();
        int x = rect.x + rect.width / 2;
        int y = rect.y + mid / 2;
        Color buttonColor = new Color(((BufferedImage)(images.get(1))).getRGB(
                (int)(x), (int)(y)));

        if (!similarColors(buttonColor, gradientColor)) {
            throw new RuntimeException("Button gradient is changed!");
        }
    }

    private static void createAndShowGUI() {

        try {
            UIManager.setLookAndFeel(new MetalLookAndFeel());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        frame = new JFrame();
        frame.setSize(300, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new BorderLayout());
        button = new JButton("");
        panel.add(button);
        frame.getContentPane().add(panel);
        frame.setVisible(true);
    }

    private static void getImageFromRobot() {
        try {
            Point butLoc = button.getLocationOnScreen();
            Dimension butSize = button.getSize();
            MultiResolutionImage multiResolutionImage =
                    robot.createMultiResolutionScreenCapture(
                            new Rectangle((int)butLoc.getX(),
                                    (int)butLoc.getY(), (int)butSize.getWidth(),
                                    (int)butSize.getHeight()));
            images = multiResolutionImage.getResolutionVariants();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        if(images.size() < 2) {
            throw new RuntimeException("HiDpi Image not captured - " +
                    "Check is this HiDpi display system?");
        }
    }

    private static Rectangle getButtonBounds() throws Exception {
        Rectangle[] rectangles = new Rectangle[1];
        SwingUtilities.invokeAndWait(() -> {
            rectangles[0] = button.getBounds();
            rectangles[0].setLocation(button.getLocationOnScreen());
        });
        return rectangles[0];
    }

    private static Color getGradientColor(int w, int h, Color c1, Color c2) {
        GradientPaint gradient = new GradientPaint(0, 0, c1, 0, h, c2,
                true);
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(gradient);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return new Color(img.getRGB(w / 2, h / 2));
    }

    private static boolean similarColors(Color c1, Color c2) {
        return similar(c1.getRed(), c2.getRed())
                && similar(c1.getGreen(), c2.getGreen())
                && similar(c1.getBlue(), c2.getBlue());
    }

    private static boolean similar(int i1, int i2) {
        return Math.abs(i2 - i1) < 6;
    }
}